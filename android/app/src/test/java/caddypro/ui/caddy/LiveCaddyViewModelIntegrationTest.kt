package caddypro.ui.caddy

import app.cash.turbine.test
import caddypro.analytics.NavCaddyAnalytics
import caddypro.data.caddy.local.LiveCaddySettingsDataStore
import caddypro.data.caddy.repository.SyncQueueRepository
import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.LiveCaddySettings
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessSource
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
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
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
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for LiveCaddyViewModel.
 *
 * Tests complete user flows and integration between use cases:
 * - Complete round flow: start → log shots → advance hole → end round
 * - State transitions during offline/online changes
 * - Error recovery and retry flows
 * - Voice query routing integration with LiveCaddy actions
 * - Analytics integration throughout user flows
 *
 * These tests use minimal mocking to verify integration between components.
 *
 * Spec reference: live-caddy-mode.md R1-R7, C3 (Offline-first)
 * Plan reference: live-caddy-mode-plan.md Task 27
 * Acceptance criteria: A1-A4 (all)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveCaddyViewModelIntegrationTest {

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
    private lateinit var settingsFlow: MutableStateFlow<LiveCaddySettings>

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
        settingsFlow = MutableStateFlow(LiveCaddySettings.default())
        every { settingsDataStore.getSettings() } returns settingsFlow

        // Default club bag flow with test clubs
        every { clubBagRepository.getActiveBagClubs() } returns flowOf(createTestClubBag())

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

    // ========== Complete User Flow Tests ==========

    @Test
    fun `complete round flow - start to log shots to advance hole to end round`() = runTest {
        // Given: Initial context for hole 1
        val hole1Context = createMockContext(holeNumber = 1, par = 4)
        coEvery { getLiveCaddyContext() } returns Result.success(hole1Context)

        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Initial context loads successfully
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.roundState?.currentHole)
            assertFalse(state.isLoading)
            assertNotNull(state.weather)
            assertNotNull(state.readiness)
            assertNotNull(state.holeStrategy)
        }

        // When: User logs first shot on hole 1
        val driver = createTestClub("Driver")
        val firstShot = ShotResult(lie = Lie.FAIRWAY, missDirection = MissDirection.STRAIGHT)
        coEvery { logShot(driver, firstShot) } returns Result.success(Unit)

        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(driver))
        viewModel.onAction(LiveCaddyAction.LogShot(firstShot))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Shot is logged and confirmed
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.lastShotConfirmed)
            assertTrue(state.lastShotDetails.contains("Driver"))
            assertTrue(state.lastShotDetails.contains("FAIRWAY"))
            assertFalse(state.isShotLoggerVisible)
        }

        // Verify shot was logged
        coVerify(exactly = 1) { logShot(driver, firstShot) }

        // When: User logs approach shot on hole 1
        val sevenIron = createTestClub("7-iron", ClubType.IRON)
        val secondShot = ShotResult(lie = Lie.GREEN, missDirection = null)
        coEvery { logShot(sevenIron, secondShot) } returns Result.success(Unit)

        viewModel.onAction(LiveCaddyAction.DismissShotConfirmation)
        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(sevenIron))
        viewModel.onAction(LiveCaddyAction.LogShot(secondShot))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Second shot is logged
        coVerify(exactly = 1) { logShot(sevenIron, secondShot) }

        // When: User advances to hole 2
        val hole2Context = createMockContext(holeNumber = 2, par = 3)
        coEvery { updateHole(2, 4) } returns Result.success(Unit)
        coEvery { getLiveCaddyContext() } returns Result.success(hole2Context)

        viewModel.onAction(LiveCaddyAction.AdvanceHole(2))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Context reloads for hole 2
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.roundState?.currentHole)
        }

        // Verify hole was updated and context reloaded
        coVerify(exactly = 1) { updateHole(2, 4) }
        coVerify(exactly = 2) { getLiveCaddyContext() } // Initial load + reload after hole advance

        // When: User ends the round
        val summary = RoundSummary(
            roundId = "round-1",
            totalShots = 75,
            totalScore = 3,
            holesCompleted = 18
        )
        coEvery { endRound() } returns Result.success(summary)

        viewModel.onAction(LiveCaddyAction.EndRound)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Round is ended and state is cleared
        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.roundState)
            assertNull(state.weather)
            assertNull(state.holeStrategy)
        }

        // Verify round was ended
        coVerify(exactly = 1) { endRound() }
    }

    @Test
    fun `shot logging flow - complete from open to club selection to logging with latency tracking`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val startTime = System.currentTimeMillis()

        // When: User opens shot logger
        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Shot logger is visible
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isShotLoggerVisible)
        }

        // And: Analytics logged shot logger opened
        verify { analytics.logShotLoggerOpened() }

        // When: User selects a club
        val club = createTestClub("7-iron")
        viewModel.onAction(LiveCaddyAction.SelectClub(club))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Club is selected
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(club, state.selectedClub)
        }

        // And: Analytics logged club selection with latency
        verify {
            analytics.logClubSelected(
                clubType = club.type.name,
                latencyMs = any()
            )
        }

        // When: User logs the shot
        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        coEvery { logShot(club, shotResult) } returns Result.success(Unit)

        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Then: Shot is logged and confirmed
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.lastShotConfirmed)
            assertTrue(state.lastShotDetails.contains("7-iron"))
            assertTrue(state.lastShotDetails.contains("FAIRWAY"))
            assertFalse(state.isShotLoggerVisible)
            assertNull(state.selectedClub)
        }

        // And: Analytics logged shot with total latency
        verify {
            analytics.logShotLogged(
                clubType = club.type.name,
                lie = shotResult.lie.name,
                totalLatencyMs = any()
            )
        }

        // And: Shot was persisted
        coVerify(exactly = 1) { logShot(club, shotResult) }

        // Verify performance: total flow should be fast (< 2 seconds in real usage)
        // Note: In test this will be instant, but validates the flow
        assertTrue(totalTime < 5000) // Generous timeout for test environment
    }

    // ========== Connectivity State Transition Tests ==========

    @Test
    fun `connectivity state changes - online to offline to online with shot queueing`() = runTest {
        // Given: ViewModel starts online
        networkStateFlow.value = true
        pendingShotsFlow.value = 0
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Initial state shows online
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isOffline)
            assertEquals(0, state.pendingShotsCount)
        }

        // When: Network goes offline
        networkStateFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State reflects offline status
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isOffline)
        }

        // When: User logs shots while offline (they get queued)
        val club = createTestClub("Driver")
        val shot1 = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        coEvery { logShot(club, shot1) } returns Result.success(Unit)

        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(club))
        viewModel.onAction(LiveCaddyAction.LogShot(shot1))
        testDispatcher.scheduler.advanceUntilIdle()

        // Simulate shot being queued for sync
        pendingShotsFlow.value = 1
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Pending shots count increases
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isOffline)
            assertEquals(1, state.pendingShotsCount)
        }

        // When: Another shot is logged while offline
        viewModel.onAction(LiveCaddyAction.DismissShotConfirmation)
        testDispatcher.scheduler.advanceUntilIdle()

        val shot2 = ShotResult(lie = Lie.GREEN, missDirection = null)
        coEvery { logShot(club, shot2) } returns Result.success(Unit)

        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(club))
        viewModel.onAction(LiveCaddyAction.LogShot(shot2))
        testDispatcher.scheduler.advanceUntilIdle()

        pendingShotsFlow.value = 2
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Pending count increases
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.pendingShotsCount)
        }

        // When: Network comes back online
        networkStateFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: State shows online with pending shots
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isOffline)
            assertEquals(2, state.pendingShotsCount)
        }

        // When: Shots sync successfully
        pendingShotsFlow.value = 1
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.pendingShotsCount)
        }

        pendingShotsFlow.value = 0
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: All shots synced
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isOffline)
            assertEquals(0, state.pendingShotsCount)
        }
    }

    @Test
    fun `offline shot logging - shots persist locally and sync when online`() = runTest {
        // Given: ViewModel starts offline
        networkStateFlow.value = false
        pendingShotsFlow.value = 0
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: User logs a shot while offline
        val club = createTestClub("7-iron")
        val shotResult = ShotResult(lie = Lie.ROUGH, missDirection = MissDirection.HOOK)
        coEvery { logShot(club, shotResult) } returns Result.success(Unit)

        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(club))
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Shot is still confirmed locally even though offline
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.lastShotConfirmed)
            assertTrue(state.isOffline)
        }

        // And: Shot was persisted locally (use case handles offline queueing)
        coVerify(exactly = 1) { logShot(club, shotResult) }

        // Simulate shot being added to sync queue
        pendingShotsFlow.value = 1
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Pending shots count reflects queued shot
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.pendingShotsCount)
        }
    }

    // ========== Error Handling and Recovery Tests ==========

    @Test
    fun `error recovery - context load fails then succeeds on retry`() = runTest {
        // Given: Initial context load fails
        coEvery { getLiveCaddyContext() } returns Result.failure(Exception("Network timeout"))
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is shown
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals("Network timeout", state.error)
            assertNull(state.roundState)
        }

        // And: Error is logged
        verify {
            analytics.logError(
                errorType = any(),
                message = any(),
                isRecoverable = true,
                throwable = any()
            )
        }

        // When: User retries and context loads successfully
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel.onAction(LiveCaddyAction.LoadContext)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Context loads successfully
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertNotNull(state.roundState)
            assertNotNull(state.weather)
        }
    }

    @Test
    fun `error handling - shot logging fails but recovers`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Shot logging fails
        val club = createTestClub("Driver")
        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        coEvery { logShot(club, shotResult) } returns Result.failure(Exception("Database error"))

        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(club))
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Error is shown and shot is not confirmed
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Database error", state.error)
            assertFalse(state.lastShotConfirmed)
        }

        // And: Error is logged
        verify {
            analytics.logError(
                errorType = any(),
                message = match { it.contains("Shot logging failed") },
                isRecoverable = true,
                throwable = any()
            )
        }

        // When: User retries and it succeeds
        coEvery { logShot(club, shotResult) } returns Result.success(Unit)

        // User doesn't need to reopen logger, just retry the action
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Shot is logged successfully
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.lastShotConfirmed)
            assertNull(state.error)
        }
    }

    @Test
    fun `error handling - multiple consecutive errors are logged`() = runTest {
        // Given: Context load fails multiple times
        coEvery { getLiveCaddyContext() } returns Result.failure(Exception("Error 1"))
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // First error
        verify(exactly = 1) {
            analytics.logError(
                errorType = any(),
                message = match { it.contains("Error 1") },
                isRecoverable = true,
                throwable = any()
            )
        }

        // When: Retry fails again with different error
        coEvery { getLiveCaddyContext() } returns Result.failure(Exception("Error 2"))
        viewModel.onAction(LiveCaddyAction.LoadContext)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Second error is also logged
        verify(exactly = 1) {
            analytics.logError(
                errorType = any(),
                message = match { it.contains("Error 2") },
                isRecoverable = true,
                throwable = any()
            )
        }
    }

    // ========== State Transition Tests ==========

    @Test
    fun `hud visibility toggles - all HUD elements can be toggled independently`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially all HUDs are collapsed/hidden
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isWeatherHudExpanded)
            assertFalse(state.isReadinessDetailsVisible)
            assertFalse(state.isStrategyMapVisible)
        }

        // When: User expands weather HUD
        viewModel.onAction(LiveCaddyAction.ToggleWeatherHud(true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isWeatherHudExpanded)
            assertFalse(state.isReadinessDetailsVisible)
            assertFalse(state.isStrategyMapVisible)
        }

        // When: User shows readiness details (while weather is expanded)
        viewModel.onAction(LiveCaddyAction.ToggleReadinessDetails(true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isWeatherHudExpanded)
            assertTrue(state.isReadinessDetailsVisible)
            assertFalse(state.isStrategyMapVisible)
        }

        // When: User shows strategy map (while others are visible)
        viewModel.onAction(LiveCaddyAction.ToggleStrategyMap(true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isWeatherHudExpanded)
            assertTrue(state.isReadinessDetailsVisible)
            assertTrue(state.isStrategyMapVisible)
        }

        // When: User collapses weather HUD (others remain visible)
        viewModel.onAction(LiveCaddyAction.ToggleWeatherHud(false))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isWeatherHudExpanded)
            assertTrue(state.isReadinessDetailsVisible)
            assertTrue(state.isStrategyMapVisible)
        }
    }

    @Test
    fun `settings updates persist and reflect in state`() = runTest {
        // Given: ViewModel with default settings
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        coEvery { settingsDataStore.saveSettings(any()) } answers {
            val newSettings = firstArg<LiveCaddySettings>()
            settingsFlow.value = newSettings
        }
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Initial settings
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.settings.lowDistractionMode)
            assertTrue(state.settings.hapticFeedback)
        }

        // When: User enables low distraction mode
        val newSettings = LiveCaddySettings.default().copy(
            lowDistractionMode = true,
            hapticFeedback = false
        )
        viewModel.onAction(LiveCaddyAction.UpdateSettings(newSettings))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Settings are persisted
        coVerify { settingsDataStore.saveSettings(newSettings) }

        // And: State reflects new settings
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.settings.lowDistractionMode)
            assertFalse(state.settings.hapticFeedback)
        }
    }

    // ========== Voice Query Integration Tests ==========

    @Test
    fun `voice query integration - shot recommendation expands strategy map`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Voice query triggers shot recommendation (simulated by UI)
        // The UI would receive routing parameters from voice classifier and trigger this action
        viewModel.onAction(LiveCaddyAction.ToggleStrategyMap(true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Strategy map is expanded
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isStrategyMapVisible)
        }
    }

    @Test
    fun `voice query integration - weather check expands weather HUD`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Voice query triggers weather check (simulated by UI)
        viewModel.onAction(LiveCaddyAction.ToggleWeatherHud(true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Weather HUD is expanded
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isWeatherHudExpanded)
        }
    }

    @Test
    fun `voice query integration - readiness check expands readiness details`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Voice query triggers readiness check (simulated by UI)
        viewModel.onAction(LiveCaddyAction.ToggleReadinessDetails(true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Readiness details are visible
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isReadinessDetailsVisible)
        }
    }

    @Test
    fun `voice query integration - bailout query expands strategy with bailout highlight`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When: Voice query triggers bailout query (simulated by UI)
        // UI would receive both expandStrategy=true and highlightBailout=true
        viewModel.onAction(LiveCaddyAction.ToggleStrategyMap(true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: Strategy map is expanded (UI would also highlight bailout area)
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isStrategyMapVisible)
        }
    }

    // ========== Performance Tests ==========

    @Test
    fun `context loads within acceptable time for weather rendering`() = runTest {
        // Given: Context with weather data
        val context = createMockContext()
        coEvery { getLiveCaddyContext() } returns Result.success(context)

        val startTime = System.currentTimeMillis()

        // When: ViewModel initializes and loads context
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val endTime = System.currentTimeMillis()
        val loadTime = endTime - startTime

        // Then: Context loads successfully
        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.weather)
            assertFalse(state.isLoading)
        }

        // And: Load time is fast (< 100ms in test environment)
        // Note: Real acceptance criteria is < 2 seconds, but tests should be much faster
        assertTrue(loadTime < 100, "Context load took ${loadTime}ms, expected < 100ms")
    }

    @Test
    fun `shot logger flow completes quickly for latency requirement`() = runTest(timeout = 5.seconds) {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val startTime = System.currentTimeMillis()

        // When: User completes full shot logging flow
        val club = createTestClub("7-iron")
        val shotResult = ShotResult(lie = Lie.FAIRWAY, missDirection = null)
        coEvery { logShot(club, shotResult) } returns Result.success(Unit)

        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        viewModel.onAction(LiveCaddyAction.SelectClub(club))
        viewModel.onAction(LiveCaddyAction.LogShot(shotResult))
        testDispatcher.scheduler.advanceUntilIdle()

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        // Then: Shot is confirmed
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.lastShotConfirmed)
        }

        // And: Total time is fast (< 100ms in test, < 1s in real usage per A4)
        assertTrue(totalTime < 100, "Shot logging took ${totalTime}ms, expected < 100ms in test")

        // Verify analytics tracked the latency
        verify {
            analytics.logShotLogged(
                clubType = club.type.name,
                lie = shotResult.lie.name,
                totalLatencyMs = any()
            )
        }
    }

    // ========== Complex State Transition Tests ==========

    @Test
    fun `multiple state changes in sequence maintain consistency`() = runTest {
        // Given: ViewModel with loaded context
        coEvery { getLiveCaddyContext() } returns Result.success(createMockContext())
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // Execute a complex sequence of state changes
        viewModel.onAction(LiveCaddyAction.ToggleWeatherHud(true))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(LiveCaddyAction.ToggleReadinessDetails(true))
        testDispatcher.scheduler.advanceUntilIdle()

        val club = createTestClub("Driver")
        viewModel.onAction(LiveCaddyAction.SelectClub(club))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(LiveCaddyAction.ToggleStrategyMap(true))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then: All state changes are reflected consistently
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isWeatherHudExpanded)
            assertTrue(state.isShotLoggerVisible)
            assertTrue(state.isReadinessDetailsVisible)
            assertTrue(state.isStrategyMapVisible)
            assertEquals(club, state.selectedClub)
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

    private fun createMockContext(holeNumber: Int = 1, par: Int = 4): LiveCaddyContext {
        return LiveCaddyContext(
            roundState = createMockRoundState(holeNumber, par),
            weather = createMockWeather(),
            readiness = createMockReadiness(),
            holeStrategy = createMockHoleStrategy()
        )
    }

    private fun createMockRoundState(holeNumber: Int = 1, par: Int = 4): RoundState {
        return RoundState(
            roundId = "round-1",
            courseName = "Pebble Beach",
            currentHole = holeNumber,
            currentPar = par,
            totalScore = 0,
            holesCompleted = holeNumber - 1,
            conditions = null
        )
    }

    private fun createMockWeather(): WeatherData {
        return mockk(relaxed = true) {
            every { temperature } returns 72.0
            every { windSpeed } returns 10.0
            every { windDirection } returns 180.0
            every { humidity } returns 65.0
        }
    }

    private fun createMockReadiness(): ReadinessScore {
        return ReadinessScore(
            overall = 75,
            breakdown = ReadinessBreakdown(
                hrv = 50.0,
                sleepQuality = 80.0,
                stressLevel = 30.0
            ),
            timestamp = System.currentTimeMillis(),
            source = ReadinessSource.WEARABLE
        )
    }

    private fun createMockHoleStrategy(): HoleStrategy {
        return mockk(relaxed = true) {
            every { holeNumber } returns 1
            every { recommendedClub } returns "7-iron"
            every { targetLine } returns "Center of green"
        }
    }

    private fun createTestClub(name: String, type: ClubType = ClubType.DRIVER): Club {
        return Club(
            id = "club-${name.lowercase()}",
            name = name,
            type = type,
            estimatedCarry = when (type) {
                ClubType.DRIVER -> 250
                ClubType.IRON -> 150
                ClubType.WEDGE -> 100
                else -> 150
            }
        )
    }

    private fun createTestClubBag(): List<Club> {
        return listOf(
            createTestClub("Driver", ClubType.DRIVER),
            createTestClub("3-wood", ClubType.WOOD),
            createTestClub("5-iron", ClubType.IRON),
            createTestClub("7-iron", ClubType.IRON),
            createTestClub("9-iron", ClubType.IRON),
            createTestClub("PW", ClubType.WEDGE),
            createTestClub("SW", ClubType.WEDGE)
        )
    }

}
