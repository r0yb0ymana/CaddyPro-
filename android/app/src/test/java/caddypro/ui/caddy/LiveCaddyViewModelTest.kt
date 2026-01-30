package caddypro.ui.caddy

import app.cash.turbine.test
import caddypro.analytics.NavCaddyAnalytics
import caddypro.data.caddy.local.LiveCaddySettingsDataStore
import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.LiveCaddySettings
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.WeatherData
import caddypro.domain.caddy.usecases.EndRoundUseCase
import caddypro.domain.caddy.usecases.GetLiveCaddyContextUseCase
import caddypro.domain.caddy.usecases.LiveCaddyContext
import caddypro.domain.caddy.usecases.LogShotUseCase
import caddypro.domain.caddy.usecases.RoundSummary
import caddypro.domain.caddy.usecases.ShotResult
import caddypro.domain.caddy.usecases.UpdateHoleUseCase
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import com.example.app.domain.navcaddy.models.Club
import com.example.app.domain.navcaddy.models.ClubType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for LiveCaddyViewModel.
 *
 * Validates:
 * - Initial state and context loading
 * - Shot logging with error handling
 * - Haptic feedback confirmation (Task 22, A4)
 * - Round management (advance hole, end round)
 * - HUD visibility toggles
 * - Settings updates
 * - Offline-first support
 *
 * Spec reference: live-caddy-mode.md R1-R7
 * Plan reference: live-caddy-mode-plan.md Task 13, Task 22
 * Acceptance criteria: A1-A4 (all)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveCaddyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLiveCaddyContext: GetLiveCaddyContextUseCase
    private lateinit var logShot: LogShotUseCase
    private lateinit var endRound: EndRoundUseCase
    private lateinit var updateHole: UpdateHoleUseCase
    private lateinit var settingsDataStore: LiveCaddySettingsDataStore
    private lateinit var analytics: NavCaddyAnalytics

    private lateinit var viewModel: LiveCaddyViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getLiveCaddyContext = mockk()
        logShot = mockk()
        endRound = mockk()
        updateHole = mockk()
        settingsDataStore = mockk()
        analytics = mockk(relaxed = true)

        // Default settings flow
        every { settingsDataStore.getSettings() } returns flowOf(LiveCaddySettings.default())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial state is loading with default values`() = runTest {
        // Given: Mock context returns success
        val mockContext = createMockContext()
        coEvery { getLiveCaddyContext() } returns Result.success(mockContext)

        // When: ViewModel is created
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State is loaded successfully
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.roundState)
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(LiveCaddySettings.default(), state.settings)
        }
    }

    @Test
    fun `loadContext success updates state with context data`() = runTest {
        // Given: Mock context with all data
        val mockRoundState = createMockRoundState()
        val mockWeather = createMockWeather()
        val mockReadiness = createMockReadiness()
        val mockHoleStrategy = createMockHoleStrategy()

        val mockContext = LiveCaddyContext(
            roundState = mockRoundState,
            weather = mockWeather,
            readiness = mockReadiness,
            holeStrategy = mockHoleStrategy
        )
        coEvery { getLiveCaddyContext() } returns Result.success(mockContext)

        // When: ViewModel loads context
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State contains all context data
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(mockRoundState, state.roundState)
            assertEquals(mockWeather, state.weather)
            assertEquals(mockReadiness, state.readiness)
            assertEquals(mockHoleStrategy, state.holeStrategy)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }
    }

    @Test
    fun `loadContext failure sets error state`() = runTest {
        // Given: Mock context returns failure
        val errorMessage = "No active round"
        coEvery { getLiveCaddyContext() } returns Result.failure(Exception(errorMessage))

        // When: ViewModel loads context
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State shows error
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(errorMessage, state.error)
        }

        // And: Error is logged to analytics
        verify {
            analytics.logError(
                errorType = any(),
                message = any(),
                isRecoverable = true,
                throwable = any()
            )
        }
    }

    // ========== Shot Logger Tests ==========

    @Test
    fun `selectClub opens shot logger with selected club`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")

        // When: User selects a club
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Shot logger is visible with selected club
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testClub, state.selectedClub)
            assertTrue(state.isShotLoggerVisible)
            assertFalse(state.lastShotConfirmed)
        }
    }

    @Test
    fun `logShot success confirms shot and closes logger`() = runTest {
        // Given: ViewModel with selected club
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // And: LogShot use case succeeds
        coEvery { logShot(any(), any()) } returns Result.success(Unit)

        // When: User logs a shot
        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Shot is confirmed and logger closes
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.lastShotConfirmed)
            assertFalse(state.isShotLoggerVisible)
            assertNull(state.selectedClub)
            assertNull(state.error)
        }

        // And: LogShot use case was called
        coVerify { logShot(testClub, shotResult) }
    }

    @Test
    fun `logShot success sets formatted shot details for toast`() = runTest {
        // Given: ViewModel with selected club
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("Driver")
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // And: LogShot use case succeeds
        coEvery { logShot(any(), any()) } returns Result.success(Unit)

        // When: User logs a shot with miss direction
        val shotResult = ShotResult(lie = Lie.ROUGH, missDirection = MissDirection.RIGHT)
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Shot details are formatted correctly
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Driver → ROUGH (RIGHT)", state.lastShotDetails)
            assertTrue(state.lastShotConfirmed)
        }
    }

    @Test
    fun `logShot without miss direction formats details correctly`() = runTest {
        // Given: ViewModel with selected club
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // And: LogShot use case succeeds
        coEvery { logShot(any(), any()) } returns Result.success(Unit)

        // When: User logs a shot without miss direction
        val shotResult = ShotResult(lie = Lie.GREEN, missDirection = null)
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Shot details formatted without miss direction
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("7-iron → GREEN", state.lastShotDetails)
            assertTrue(state.lastShotConfirmed)
        }
    }

    @Test
    fun `dismissShotConfirmation resets confirmation state`() = runTest {
        // Given: ViewModel with confirmed shot
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { logShot(any(), any()) } returns Result.success(Unit)
        viewModel.onAction(LiveCaddyAction.LogShot(ShotResult(lie = Lie.FAIRWAY, missDirection = null)))
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User dismisses the confirmation toast
        viewModel.onAction(LiveCaddyAction.DismissShotConfirmation)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Confirmation state is reset
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.lastShotConfirmed)
            assertEquals("", state.lastShotDetails)
        }
    }

    @Test
    fun `logShot without selected club sets error`() = runTest {
        // Given: ViewModel without selected club
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User tries to log shot without selecting club
        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("No club selected for shot logging", state.error)
        }

        // And: LogShot use case was not called
        coVerify(exactly = 0) { logShot(any(), any()) }
    }

    @Test
    fun `logShot failure sets error state`() = runTest {
        // Given: ViewModel with selected club
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // And: LogShot use case fails
        val errorMessage = "Failed to persist shot"
        coEvery { logShot(any(), any()) } returns Result.failure(Exception(errorMessage))

        // When: User logs a shot
        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(errorMessage, state.error)
        }

        // And: Error is logged to analytics
        verify {
            analytics.logError(
                errorType = any(),
                message = any(),
                isRecoverable = true,
                throwable = any()
            )
        }
    }

    @Test
    fun `dismissShotLogger closes logger without logging`() = runTest {
        // Given: ViewModel with shot logger open
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User dismisses shot logger
        viewModel.onAction(LiveCaddyAction.DismissShotLogger)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Logger is closed and club is cleared
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isShotLoggerVisible)
            assertNull(state.selectedClub)
        }

        // And: LogShot use case was not called
        coVerify(exactly = 0) { logShot(any(), any()) }
    }

    // ========== Round Management Tests ==========

    @Test
    fun `advanceHole success reloads context`() = runTest {
        // Given: ViewModel with active round
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // And: UpdateHole use case succeeds
        coEvery { updateHole(any(), any()) } returns Result.success(createMockRoundState())

        // When: User advances to next hole
        viewModel.onAction(LiveCaddyAction.AdvanceHole(2))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: UpdateHole was called with holeNumber=2 and default par=4
        coVerify { updateHole(2, 4) }

        // And: Context was reloaded (called twice: init + after advance)
        coVerify(exactly = 2) { getLiveCaddyContext() }
    }

    @Test
    fun `advanceHole failure sets error state`() = runTest {
        // Given: ViewModel with active round
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // And: UpdateHole use case fails
        val errorMessage = "Failed to update hole"
        coEvery { updateHole(any(), any()) } returns Result.failure(Exception(errorMessage))

        // When: User advances hole
        viewModel.onAction(LiveCaddyAction.AdvanceHole(2))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(errorMessage, state.error)
        }

        // And: Error is logged to analytics
        verify {
            analytics.logError(
                errorType = any(),
                message = any(),
                isRecoverable = true,
                throwable = any()
            )
        }
    }

    @Test
    fun `endRound success clears round state`() = runTest {
        // Given: ViewModel with active round
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // And: EndRound use case succeeds
        val roundSummary = RoundSummary(
            roundId = "round-1",
            courseName = "Pebble Beach",
            finalScore = 85,
            holesPlayed = 18,
            endTime = System.currentTimeMillis()
        )
        coEvery { endRound() } returns Result.success(roundSummary)

        // When: User ends round
        viewModel.onAction(LiveCaddyAction.EndRound)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Round state is cleared
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.roundState)
            assertNull(state.weather)
            assertNull(state.holeStrategy)
            assertNull(state.error)
        }

        // And: EndRound use case was called with default parameters
        coVerify { endRound() }
    }

    @Test
    fun `endRound failure sets error state`() = runTest {
        // Given: ViewModel with active round
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // And: EndRound use case fails
        val errorMessage = "Failed to end round"
        coEvery { endRound() } returns Result.failure(Exception(errorMessage))

        // When: User ends round
        viewModel.onAction(LiveCaddyAction.EndRound)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(errorMessage, state.error)
        }

        // And: Error is logged to analytics
        verify {
            analytics.logError(
                errorType = any(),
                message = any(),
                isRecoverable = true,
                throwable = any()
            )
        }
    }

    // ========== HUD Visibility Tests ==========

    @Test
    fun `toggleWeatherHud updates visibility state`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User expands weather HUD
        viewModel.onAction(LiveCaddyAction.ToggleWeatherHud(expanded = true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Weather HUD is expanded
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isWeatherHudExpanded)
        }

        // When: User collapses weather HUD
        viewModel.onAction(LiveCaddyAction.ToggleWeatherHud(expanded = false))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Weather HUD is collapsed
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isWeatherHudExpanded)
        }
    }

    @Test
    fun `toggleReadinessDetails updates visibility state`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User shows readiness details
        viewModel.onAction(LiveCaddyAction.ToggleReadinessDetails(visible = true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Readiness details are visible
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isReadinessDetailsVisible)
        }
    }

    @Test
    fun `toggleStrategyMap updates visibility state`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User shows strategy map
        viewModel.onAction(LiveCaddyAction.ToggleStrategyMap(visible = true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Strategy map is visible
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isStrategyMapVisible)
        }
    }

    // ========== Settings Tests ==========

    @Test
    fun `updateSettings persists to DataStore`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        coEvery { settingsDataStore.saveSettings(any()) } returns Unit
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User updates settings
        val newSettings = LiveCaddySettings.outdoor()
        viewModel.onAction(LiveCaddyAction.UpdateSettings(newSettings))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Settings are saved to DataStore
        coVerify { settingsDataStore.saveSettings(newSettings) }
    }

    @Test
    fun `updateSettings failure sets error state`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())

        val errorMessage = "Disk full"
        coEvery { settingsDataStore.saveSettings(any()) } throws Exception(errorMessage)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User updates settings
        viewModel.onAction(LiveCaddyAction.UpdateSettings(LiveCaddySettings.outdoor()))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is set
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(errorMessage, state.error)
        }

        // And: Error is logged to analytics
        verify {
            analytics.logError(
                errorType = any(),
                message = any(),
                isRecoverable = true,
                throwable = any()
            )
        }
    }

    // ========== Helper Methods ==========

    private fun createViewModel(): LiveCaddyViewModel {
        return LiveCaddyViewModel(
            getLiveCaddyContext = getLiveCaddyContext,
            logShot = logShot,
            endRound = endRound,
            updateHole = updateHole,
            settingsDataStore = settingsDataStore,
            analytics = analytics
        )
    }

    private fun createMockContext(): LiveCaddyContext {
        return LiveCaddyContext(
            roundState = createMockRoundState(),
            weather = createMockWeather(),
            readiness = createMockReadiness(),
            holeStrategy = createMockHoleStrategy()
        )
    }

    private fun createMockRoundState(): RoundState {
        return RoundState(
            roundId = "round-1",
            courseName = "Pebble Beach",
            currentHole = 1,
            currentPar = 4,
            totalScore = 0,
            holesCompleted = 0,
            conditions = null
        )
    }

    private fun createMockWeather(): WeatherData {
        return mockk(relaxed = true)
    }

    private fun createMockReadiness(): ReadinessScore {
        return mockk(relaxed = true)
    }

    private fun createMockHoleStrategy(): HoleStrategy {
        return mockk(relaxed = true)
    }

    private fun createTestClub(name: String): Club {
        return Club(
            id = "club-1",
            name = name,
            type = ClubType.IRON,
            estimatedCarry = 150
        )
    }
}
