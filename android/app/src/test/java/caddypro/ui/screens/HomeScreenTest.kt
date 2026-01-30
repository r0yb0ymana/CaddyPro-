package caddypro.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caddypro.domain.navcaddy.context.CourseConditions
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.context.SessionContextManager
import caddypro.ui.theme.AppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for HomeScreen with Live Caddy entry points.
 *
 * Tests:
 * - Display of "Start Round" card when no active round
 * - Display of "Resume Round" card when round is active
 * - Quick action visibility based on round state
 * - Round details display (course, hole, score)
 * - Navigation callbacks
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 25
 * Acceptance criteria (Task 25 verification):
 * - [ ] Home screen shows "Resume Round" if active round exists
 * - [ ] Resume Round navigates to LiveCaddyScreen with context restored
 * - [ ] Start Round button navigates to round setup flow
 * - [ ] Quick actions are only visible when round is active
 * - [ ] Round details (course, hole, score) display correctly
 */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSessionContextManager: SessionContextManager
    private lateinit var sessionContextFlow: MutableStateFlow<caddypro.domain.navcaddy.models.SessionContext>

    @Before
    fun setup() {
        mockSessionContextManager = mockk(relaxed = true)
        sessionContextFlow = MutableStateFlow(caddypro.domain.navcaddy.models.SessionContext.empty)

        every { mockSessionContextManager.context } returns sessionContextFlow
    }

    // ============================================================================
    // No Active Round Tests
    // ============================================================================

    @Test
    fun `when no active round, displays Start Round card`() {
        // Given: No active round
        every { mockSessionContextManager.getCurrentRoundState() } returns null

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Start Round card is displayed
        composeTestRule.onNodeWithText("Start a Round").assertIsDisplayed()
        composeTestRule.onNodeWithText("Access Live Caddy features including weather forecasting, readiness tracking, and hole strategy.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Round").assertIsDisplayed()
    }

    @Test
    fun `when no active round, does not display Resume Round card`() {
        // Given: No active round
        every { mockSessionContextManager.getCurrentRoundState() } returns null

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Resume Round card is not displayed
        composeTestRule.onNodeWithText("Round in Progress").assertDoesNotExist()
    }

    @Test
    fun `when no active round, does not display quick actions`() {
        // Given: No active round
        every { mockSessionContextManager.getCurrentRoundState() } returns null

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Quick actions are not displayed
        composeTestRule.onNodeWithText("Quick Actions").assertDoesNotExist()
        composeTestRule.onNodeWithText("Weather").assertDoesNotExist()
        composeTestRule.onNodeWithText("Strategy").assertDoesNotExist()
    }

    @Test
    fun `when Start Round clicked, calls onStartRound callback`() {
        // Given: No active round
        every { mockSessionContextManager.getCurrentRoundState() } returns null
        var startRoundCalled = false

        // When: HomeScreen is displayed and Start Round is clicked
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = { startRoundCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Start Round").performClick()

        // Then: onStartRound callback is invoked
        assert(startRoundCalled) { "onStartRound callback should be called" }
    }

    // ============================================================================
    // Active Round Tests
    // ============================================================================

    @Test
    fun `when active round exists, displays Resume Round card`() {
        // Given: Active round
        val roundState = RoundState(
            roundId = "round-123",
            courseName = "Pebble Beach",
            currentHole = 7,
            currentPar = 4,
            totalScore = 38,
            holesCompleted = 6
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Resume Round card is displayed with correct information
        composeTestRule.onNodeWithText("Round in Progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pebble Beach").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hole 7 of 18").assertIsDisplayed()
        composeTestRule.onNodeWithText("38 total").assertIsDisplayed()
        composeTestRule.onNodeWithText("Resume Round").assertIsDisplayed()
    }

    @Test
    fun `when active round exists, does not display Start Round card`() {
        // Given: Active round
        val roundState = RoundState(
            roundId = "round-123",
            courseName = "Augusta National",
            currentHole = 1,
            currentPar = 4,
            totalScore = 0,
            holesCompleted = 0
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Start Round card is not displayed
        composeTestRule.onNodeWithText("Start a Round").assertDoesNotExist()
    }

    @Test
    fun `when active round exists, displays quick actions`() {
        // Given: Active round
        val roundState = RoundState(
            roundId = "round-123",
            courseName = "St Andrews",
            currentHole = 14,
            currentPar = 5,
            totalScore = 82,
            holesCompleted = 13
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Quick actions are displayed
        composeTestRule.onNodeWithText("Quick Actions").assertIsDisplayed()
        composeTestRule.onNodeWithText("Weather").assertIsDisplayed()
        composeTestRule.onNodeWithText("Strategy").assertIsDisplayed()
    }

    @Test
    fun `when Resume Round clicked, calls onNavigateToLiveCaddy callback`() {
        // Given: Active round
        val roundState = RoundState(
            roundId = "round-456",
            courseName = "Pinehurst No. 2",
            currentHole = 10,
            currentPar = 4,
            totalScore = 52,
            holesCompleted = 9
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState
        var navigateToLiveCaddyCalled = false

        // When: HomeScreen is displayed and Resume Round is clicked
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = { navigateToLiveCaddyCalled = true },
                    onStartRound = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Resume Round").performClick()

        // Then: onNavigateToLiveCaddy callback is invoked
        assert(navigateToLiveCaddyCalled) { "onNavigateToLiveCaddy callback should be called" }
    }

    @Test
    fun `when Weather quick action clicked, calls onNavigateToLiveCaddy callback`() {
        // Given: Active round
        val roundState = RoundState(
            roundId = "round-789",
            courseName = "TPC Sawgrass",
            currentHole = 17,
            currentPar = 3,
            totalScore = 88,
            holesCompleted = 16
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState
        var navigateToLiveCaddyCalled = false

        // When: HomeScreen is displayed and Weather quick action is clicked
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = { navigateToLiveCaddyCalled = true },
                    onStartRound = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Weather").performClick()

        // Then: onNavigateToLiveCaddy callback is invoked
        assert(navigateToLiveCaddyCalled) { "onNavigateToLiveCaddy callback should be called when Weather clicked" }
    }

    @Test
    fun `when Strategy quick action clicked, calls onNavigateToLiveCaddy callback`() {
        // Given: Active round
        val roundState = RoundState(
            roundId = "round-101",
            courseName = "Bethpage Black",
            currentHole = 4,
            currentPar = 4,
            totalScore = 15,
            holesCompleted = 3
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState
        var navigateToLiveCaddyCalled = false

        // When: HomeScreen is displayed and Strategy quick action is clicked
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = { navigateToLiveCaddyCalled = true },
                    onStartRound = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Strategy").performClick()

        // Then: onNavigateToLiveCaddy callback is invoked
        assert(navigateToLiveCaddyCalled) { "onNavigateToLiveCaddy callback should be called when Strategy clicked" }
    }

    // ============================================================================
    // Round Details Display Tests
    // ============================================================================

    @Test
    fun `displays correct round details with course conditions`() {
        // Given: Active round with course conditions
        val roundState = RoundState(
            roundId = "round-202",
            courseName = "Torrey Pines",
            currentHole = 12,
            currentPar = 4,
            totalScore = 65,
            holesCompleted = 11,
            conditions = CourseConditions(
                weather = "Sunny",
                windSpeed = 15,
                windDirection = "SW",
                temperature = 75
            )
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Round details and conditions are displayed correctly
        composeTestRule.onNodeWithText("Torrey Pines").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hole 12 of 18").assertIsDisplayed()
        composeTestRule.onNodeWithText("65 total").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sunny, 15 mph SW wind, 75°F").assertIsDisplayed()
    }

    @Test
    fun `displays round details without score when no holes completed`() {
        // Given: Active round at the start (hole 1, no holes completed)
        val roundState = RoundState(
            roundId = "round-303",
            courseName = "Shinnecock Hills",
            currentHole = 1,
            currentPar = 4,
            totalScore = 0,
            holesCompleted = 0
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Round details are displayed, but score is not shown
        composeTestRule.onNodeWithText("Shinnecock Hills").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hole 1 of 18").assertIsDisplayed()
        composeTestRule.onNodeWithText("0 total").assertDoesNotExist()
    }

    @Test
    fun `displays round details at final hole`() {
        // Given: Active round at hole 18
        val roundState = RoundState(
            roundId = "round-404",
            courseName = "Oakmont",
            currentHole = 18,
            currentPar = 4,
            totalScore = 92,
            holesCompleted = 17
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Round details show hole 18
        composeTestRule.onNodeWithText("Oakmont").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hole 18 of 18").assertIsDisplayed()
        composeTestRule.onNodeWithText("92 total").assertIsDisplayed()
    }

    // ============================================================================
    // Welcome Section Tests
    // ============================================================================

    @Test
    fun `always displays welcome message`() {
        // Given: No active round
        every { mockSessionContextManager.getCurrentRoundState() } returns null

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Welcome message is displayed
        composeTestRule.onNodeWithText("Welcome to CaddyPro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your intelligent golf caddy").assertIsDisplayed()
    }

    @Test
    fun `displays welcome message even with active round`() {
        // Given: Active round
        val roundState = RoundState(
            roundId = "round-505",
            courseName = "Merion",
            currentHole = 8,
            currentPar = 3,
            totalScore = 42,
            holesCompleted = 7
        )
        every { mockSessionContextManager.getCurrentRoundState() } returns roundState

        // When: HomeScreen is displayed
        composeTestRule.setContent {
            AppTheme {
                HomeScreen(
                    sessionContextManager = mockSessionContextManager,
                    onNavigateToLiveCaddy = {},
                    onStartRound = {}
                )
            }
        }

        // Then: Welcome message is still displayed
        composeTestRule.onNodeWithText("Welcome to CaddyPro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Your intelligent golf caddy").assertIsDisplayed()
    }
}
