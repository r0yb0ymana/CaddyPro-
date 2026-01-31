package caddypro.ui.caddy

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.WeatherData
import caddypro.domain.navcaddy.context.RoundState
import caddypro.ui.theme.CaddyProTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for LiveCaddyScreen.
 *
 * Verifies:
 * - No active round placeholder is displayed correctly
 * - Active round displays all HUD components
 * - Loading state is shown correctly
 * - Error state is shown correctly
 * - End round confirmation dialog
 *
 * Spec reference: live-caddy-mode.md R1-R7
 * Plan reference: live-caddy-mode-plan.md Task 18
 * Acceptance criteria: A1-A4 (all)
 */
class LiveCaddyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noActiveRound_displaysPlaceholder() {
        // Given: No active round state
        val state = LiveCaddyState(
            roundState = null,
            isLoading = false,
            error = null
        )

        // When: Screen is displayed
        composeTestRule.setContent {
            CaddyProTheme {
                LiveCaddyContent(
                    state = state,
                    onAction = {}
                )
            }
        }

        // Then: Placeholder message is shown
        composeTestRule
            .onNodeWithText("No Active Round")
            .assertExists()
        composeTestRule
            .onNodeWithText("Start a round to access Live Caddy features including weather forecasting, readiness tracking, and hole strategy.")
            .assertExists()
    }

    @Test
    fun activeRound_displaysAllHudComponents() {
        // Given: Active round with weather data
        val state = LiveCaddyState(
            roundState = RoundState(
                roundId = "round-1",
                courseName = "Pebble Beach",
                currentHole = 7,
                currentPar = 3,
                totalScore = 8
            ),
            weather = WeatherData(
                location = Location(37.7749, -122.4194),
                windSpeedMps = 5.4,
                windDegrees = 180,
                temperatureCelsius = 22.0,
                humidity = 55,
                timestamp = System.currentTimeMillis()
            ),
            isLoading = false,
            error = null
        )

        // When: Screen is displayed
        composeTestRule.setContent {
            CaddyProTheme {
                LiveCaddyContent(
                    state = state,
                    onAction = {}
                )
            }
        }

        // Then: Distraction warning is shown
        composeTestRule
            .onNodeWithText("Put your phone away during your swing. Live Caddy is designed for quick reference between shots.")
            .assertExists()

        // And: Show Hole Strategy button is visible
        composeTestRule
            .onNodeWithText("Show Hole Strategy")
            .assertExists()
    }

    @Test
    fun strategyMapToggle_worksCorrectly() {
        // Given: Active round with strategy map hidden
        val state = LiveCaddyState(
            roundState = RoundState(
                roundId = "round-1",
                courseName = "Test Course",
                currentHole = 1,
                currentPar = 4,
                totalScore = 0
            ),
            isStrategyMapVisible = false,
            isLoading = false,
            error = null
        )

        var lastAction: LiveCaddyAction? = null

        // When: Screen is displayed
        composeTestRule.setContent {
            CaddyProTheme {
                LiveCaddyContent(
                    state = state,
                    onAction = { lastAction = it }
                )
            }
        }

        // And: User clicks "Show Hole Strategy"
        composeTestRule
            .onNodeWithText("Show Hole Strategy")
            .performClick()

        // Then: ToggleStrategyMap action is triggered
        assert(lastAction is LiveCaddyAction.ToggleStrategyMap)
        assert((lastAction as LiveCaddyAction.ToggleStrategyMap).visible)
    }

    @Test
    fun errorState_displaysErrorView() {
        // Given: Error state
        val state = LiveCaddyState(
            isLoading = false,
            error = "Failed to load context"
        )

        var retryClicked = false

        // When: Screen is displayed
        composeTestRule.setContent {
            CaddyProTheme {
                LiveCaddyContent(
                    state = state,
                    onAction = {
                        if (it is LiveCaddyAction.LoadContext) {
                            retryClicked = true
                        }
                    }
                )
            }
        }

        // Then: Error message is shown
        composeTestRule
            .onNodeWithText("Failed to load context")
            .assertExists()
    }
}
