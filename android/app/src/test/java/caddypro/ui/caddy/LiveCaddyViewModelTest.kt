package caddypro.ui.caddy

import app.cash.turbine.test
import caddypro.analytics.NavCaddyAnalytics
import caddypro.data.caddy.local.LiveCaddySettingsDataStore
import caddypro.data.caddy.repository.SyncQueueRepository
import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.LiveCaddySettings
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.WeatherData
import caddypro.domain.caddy.repositories.ClubBagRepository
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
import caddypro.domain.navcaddy.offline.NetworkMonitor
import com.example.app.domain.navcaddy.models.Club
import com.example.app.domain.navcaddy.models.ClubType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * - Analytics & latency tracking (Task 21)
 * - Network connectivity monitoring (Task 20)
 *
 * Spec reference: live-caddy-mode.md R1-R7, C3
 * Plan reference: live-caddy-mode-plan.md Task 13, Task 20, Task 21, Task 22
 * Acceptance criteria: A1-A4 (all)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveCaddyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getLiveCaddyContext: GetLiveCaddyContextUseCase
    private lateinit var logShot: LogShotUseCase
    private lateinit var endRound: EndRoundUseCase
    private lateinit var updateHole: UpdateHoleUseCase
    private lateinit var clubBagRepository: ClubBagRepository
    private lateinit var settingsDataStore: LiveCaddySettingsDataStore
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var syncQueueRepository: SyncQueueRepository
    private lateinit var analytics: NavCaddyAnalytics

    private lateinit var viewModel: LiveCaddyViewModel

    // Mutable flows for testing state changes
    private lateinit var networkStateFlow: MutableStateFlow<Boolean>
    private lateinit var pendingShotsFlow: MutableStateFlow<Int>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getLiveCaddyContext = mockk()
        logShot = mockk()
        endRound = mockk()
        updateHole = mockk()
        clubBagRepository = mockk()
        settingsDataStore = mockk()
        networkMonitor = mockk()
        syncQueueRepository = mockk()
        analytics = mockk(relaxed = true)

        // Default settings flow
        every { settingsDataStore.getSettings() } returns flowOf(LiveCaddySettings.default())

        // Default club bag flow
        every { clubBagRepository.getActiveBagClubs() } returns flowOf(emptyList())

        // Default network state (online)
        networkStateFlow = MutableStateFlow(true)
        every { networkMonitor.isOnline } returns networkStateFlow

        // Default pending shots count (none)
        pendingShotsFlow = MutableStateFlow(0)
        every { syncQueueRepository.observePendingCount() } returns pendingShotsFlow
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
    fun `showShotLogger opens logger and tracks analytics event`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User shows shot logger
        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Shot logger is visible
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isShotLoggerVisible)
            assertFalse(state.lastShotConfirmed)
        }

        // And: Analytics event was tracked (Task 21)
        verify { analytics.logShotLoggerOpened() }
    }

    @Test
    fun `selectClub tracks club selection latency`() = runTest {
        // Given: ViewModel with shot logger open
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")

        // When: User selects a club
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Club selection is tracked with latency (Task 21)
        verify {
            analytics.logClubSelected(
                clubType = testClub.type.name,
                latencyMs = any()
            )
        }

        // And: Shot logger shows selected club
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(testClub, state.selectedClub)
        }
    }

    @Test
    fun `logShot success tracks total latency analytics`() = runTest {
        // Given: ViewModel with selected club
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // And: LogShot use case succeeds
        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        coEvery { logShot(testClub, shotResult) } returns Result.success(Unit)

        // When: User logs the shot
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Total latency is tracked (Task 21)
        verify {
            analytics.logShotLogged(
                clubType = testClub.type.name,
                lie = shotResult.lie.name,
                totalLatencyMs = any()
            )
        }

        // And: Shot confirmation is shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.lastShotConfirmed)
            assertTrue(state.lastShotDetails.contains(testClub.name))
            assertTrue(state.lastShotDetails.contains(shotResult.lie.name))
            assertFalse(state.isShotLoggerVisible)
        }
    }

    @Test
    fun `logShot failure without selected club shows error`() = runTest {
        // Given: ViewModel without selected club
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User tries to log shot without selecting club
        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("No club selected for shot logging", state.error)
            assertFalse(state.lastShotConfirmed)
        }
    }

    @Test
    fun `logShot use case failure logs error and shows message`() = runTest {
        // Given: ViewModel with selected club
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // And: LogShot use case fails
        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        val errorMessage = "Database error"
        coEvery { logShot(testClub, shotResult) } returns Result.failure(Exception(errorMessage))

        // When: User logs the shot
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(errorMessage, state.error)
            assertFalse(state.lastShotConfirmed)
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
    fun `dismissShotLogger hides logger and clears selection`() = runTest {
        // Given: ViewModel with shot logger open and club selected
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User dismisses shot logger
        viewModel.onAction(LiveCaddyAction.DismissShotLogger)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Logger is hidden and club is cleared
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isShotLoggerVisible)
            assertNull(state.selectedClub)
        }
    }

    @Test
    fun `dismissShotConfirmation clears confirmation state`() = runTest {
        // Given: ViewModel with shot confirmed
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val testClub = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(testClub))
        testDispatcher.scheduler.advanceUntilIdle()

        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        coEvery { logShot(testClub, shotResult) } returns Result.success(Unit)
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User dismisses confirmation
        viewModel.onAction(LiveCaddyAction.DismissShotConfirmation)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Confirmation state is cleared
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.lastShotConfirmed)
            assertEquals("", state.lastShotDetails)
        }
    }

    // ========== Network Connectivity Tests (Task 20) ==========

    @Test
    fun `initial state reflects online status`() = runTest {
        // Given: Network is online
        networkStateFlow.value = true
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())

        // When: ViewModel is created
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State shows online
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isOffline)
        }
    }

    @Test
    fun `network state change updates isOffline flag`() = runTest {
        // Given: ViewModel with initial online state
        networkStateFlow.value = true
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Network goes offline
        networkStateFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State reflects offline status
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isOffline)
        }
    }

    @Test
    fun `pending shots count updates from repository`() = runTest {
        // Given: ViewModel with initial pending count
        pendingShotsFlow.value = 0
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Pending shots count changes
        pendingShotsFlow.value = 3
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State reflects new count
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(3, state.pendingShotsCount)
        }
    }

    @Test
    fun `offline with pending shots shows correct state`() = runTest {
        // Given: ViewModel starts online with no pending shots
        networkStateFlow.value = true
        pendingShotsFlow.value = 0
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Network goes offline and shots are queued
        networkStateFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        pendingShotsFlow.value = 5
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State shows offline with pending shots
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isOffline)
            assertEquals(5, state.pendingShotsCount)
        }
    }

    @Test
    fun `online with pending shots shows syncing state`() = runTest {
        // Given: ViewModel with pending shots
        networkStateFlow.value = true
        pendingShotsFlow.value = 2
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State shows online with pending shots (syncing)
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isOffline)
            assertEquals(2, state.pendingShotsCount)
        }
    }

    @Test
    fun `pending shots decrement as sync completes`() = runTest {
        // Given: ViewModel with pending shots
        pendingShotsFlow.value = 5
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Shots sync one by one
        pendingShotsFlow.value = 4
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(4, state.pendingShotsCount)
        }

        pendingShotsFlow.value = 3
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            var state = awaitItem()
            assertEquals(3, state.pendingShotsCount)
        }

        // And: Eventually all shots synced
        pendingShotsFlow.value = 0
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.pendingShotsCount)
        }
    }

    // ========== Round Management Tests ==========

    @Test
    fun `advanceHole success reloads context`() = runTest {
        // Given: ViewModel with loaded context
        val mockContext = createMockContext()
        coEvery { getLiveCaddyContext() } returns Result.success(mockContext)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // And: UpdateHole use case succeeds
        coEvery { updateHole(2, 4) } returns Result.success(Unit)

        // When: User advances to hole 2
        viewModel.onAction(LiveCaddyAction.AdvanceHole(2))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Context is reloaded
        coVerify(exactly = 2) { getLiveCaddyContext() }
    }

    @Test
    fun `advanceHole failure shows error`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // And: UpdateHole use case fails
        val errorMessage = "Invalid hole number"
        coEvery { updateHole(any(), any()) } returns Result.failure(Exception(errorMessage))

        // When: User advances to hole 2
        viewModel.onAction(LiveCaddyAction.AdvanceHole(2))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is shown
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
        val mockSummary = mockk<RoundSummary>(relaxed = true)
        coEvery { endRound() } returns Result.success(mockSummary)

        // When: User ends the round
        viewModel.onAction(LiveCaddyAction.EndRound)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Round state is cleared
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.roundState)
            assertNull(state.weather)
            assertNull(state.holeStrategy)
        }
    }

    @Test
    fun `endRound failure shows error`() = runTest {
        // Given: ViewModel with active round
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // And: EndRound use case fails
        val errorMessage = "Failed to save round"
        coEvery { endRound() } returns Result.failure(Exception(errorMessage))

        // When: User ends the round
        viewModel.onAction(LiveCaddyAction.EndRound)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is shown
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

    // ========== HUD Toggle Tests ==========

    @Test
    fun `toggleWeatherHud updates expansion state`() = runTest {
        // Given: ViewModel with collapsed weather HUD
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User expands weather HUD
        viewModel.onAction(LiveCaddyAction.ToggleWeatherHud(true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Weather HUD is expanded
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isWeatherHudExpanded)
        }

        // When: User collapses weather HUD
        viewModel.onAction(LiveCaddyAction.ToggleWeatherHud(false))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Weather HUD is collapsed
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isWeatherHudExpanded)
        }
    }

    @Test
    fun `toggleReadinessDetails updates visibility state`() = runTest {
        // Given: ViewModel with hidden readiness details
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User shows readiness details
        viewModel.onAction(LiveCaddyAction.ToggleReadinessDetails(true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Readiness details are visible
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isReadinessDetailsVisible)
        }
    }

    @Test
    fun `toggleStrategyMap updates visibility state`() = runTest {
        // Given: ViewModel with hidden strategy map
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User shows strategy map
        viewModel.onAction(LiveCaddyAction.ToggleStrategyMap(true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Strategy map is visible
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isStrategyMapVisible)
        }
    }

    // ========== Settings Tests ==========

    @Test
    fun `updateSettings saves to datastore`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        coEvery { settingsDataStore.saveSettings(any()) } returns Unit
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User updates settings
        val newSettings = LiveCaddySettings.default().copy(lowDistractionMode = true)
        viewModel.onAction(LiveCaddyAction.UpdateSettings(newSettings))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Settings are saved
        coVerify { settingsDataStore.saveSettings(newSettings) }
    }

    @Test
    fun `updateSettings failure shows error`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // And: SaveSettings throws exception
        val errorMessage = "Failed to write to storage"
        coEvery { settingsDataStore.saveSettings(any()) } throws Exception(errorMessage)

        // When: User updates settings
        val newSettings = LiveCaddySettings.default().copy(lowDistractionMode = true)
        viewModel.onAction(LiveCaddyAction.UpdateSettings(newSettings))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is shown
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
            clubBagRepository = clubBagRepository,
            settingsDataStore = settingsDataStore,
            networkMonitor = networkMonitor,
            syncQueueRepository = syncQueueRepository,
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
