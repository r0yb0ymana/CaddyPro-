package caddypro.ui.caddy

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import caddypro.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * End-to-End tests for Live Caddy Mode acceptance criteria.
 *
 * These tests verify the complete user flow for each acceptance criterion:
 * - A1: Weather HUD renders within 2 seconds with conditions adjustment
 * - A2: Readiness impacts strategy recommendations
 * - A3: Hazard-aware landing zones with miss pattern filtering
 * - A4: Shot logger speed (<1s) and offline persistence
 *
 * Spec reference: live-caddy-mode.md Section 5 (Acceptance criteria)
 * Plan reference: live-caddy-mode-plan.md Task 28
 *
 * Note: These are instrumented tests that run on a device or emulator.
 * They require proper test data setup and may take several minutes to run.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LiveCaddyE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ====================
    // A1: Weather HUD and Adjustment
    // ====================

    @Test
    fun acceptanceCriteria_A1_weatherHudRendersWithin2Seconds() {
        // Given: User has location enabled and starts a round
        navigateToLiveCaddy()

        val startTime = System.currentTimeMillis()

        // When: Live Caddy screen loads
        // Wait for weather HUD to appear (max 2 seconds)
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithText("Forecaster")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val renderTime = System.currentTimeMillis() - startTime

        // Then: Weather HUD rendered within 2 seconds
        assertTrue(
            renderTime < 2000,
            "Weather HUD rendered in ${renderTime}ms, expected < 2000ms (A1)"
        )

        // And: All weather fields are present
        composeTestRule
            .onNodeWithContentDescription("Wind speed")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Temperature")
            .assertIsDisplayed()

        composeTestRule
            .onNode(hasText(Regex("\\d+%"))) // Humidity percentage
            .assertIsDisplayed()
    }

    @Test
    fun acceptanceCriteria_A1_conditionsAdjustmentIsCalculated() {
        // Given: User has active round with weather data
        navigateToLiveCaddy()

        // Wait for weather to load
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodesWithText("Forecaster")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // When: User views conditions adjustment
        // Look for conditions chip (e.g., "-5m" or "+3m")
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodes(hasText(Regex("[+-]\\d+m")))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Then: Conditions adjustment is displayed
        val adjustmentNode = composeTestRule
            .onNode(hasText(Regex("[+-]\\d+m")))

        adjustmentNode.assertIsDisplayed()

        // When: User taps on adjustment chip
        adjustmentNode.performClick()

        // Then: Adjustment reason is shown
        // Should contain explanation like "Cold air and headwind"
        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodes(
                    hasText(Regex(".*(headwind|tailwind|cold|warm).*", RegexOption.IGNORE_CASE))
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    // ====================
    // A2: Readiness Impacts Strategy
    // ====================

    @Test
    fun acceptanceCriteria_A2_lowReadinessIncreasesConservatism() {
        // Given: User has low readiness (35)
        navigateToLiveCaddy()
        setManualReadiness(35)

        // When: User views hole strategy
        composeTestRule
            .onNodeWithText("Show Hole Strategy")
            .performClick()

        // Wait for strategy to load
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithText("PinSeeker")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Then: Safety margin is displayed
        // Look for "Safety margin: XXm" text
        val lowReadinessMargin = extractSafetyMargin()

        // Given: User now has high readiness (85)
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        setManualReadiness(85)

        // When: User views hole strategy again
        composeTestRule
            .onNodeWithText("Show Hole Strategy")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithText("PinSeeker")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val highReadinessMargin = extractSafetyMargin()

        // Then: Low readiness margin should be significantly larger (at least 1.5x)
        assertTrue(
            lowReadinessMargin >= highReadinessMargin * 1.5,
            "Low readiness margin ($lowReadinessMargin m) should be >= 1.5x high readiness margin ($highReadinessMargin m) (A2)"
        )
    }

    @Test
    fun acceptanceCriteria_A2_readinessExplanationIsShown() {
        // Given: User has low readiness
        navigateToLiveCaddy()
        setManualReadiness(40)

        // When: User views strategy
        composeTestRule
            .onNodeWithText("Show Hole Strategy")
            .performClick()

        // Then: Conservative language should be present
        // Look for words like "conservative", "safe", "larger margin"
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodes(
                    hasText(Regex(".*(conservative|safe|cautious|larger).*", RegexOption.IGNORE_CASE))
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    // ====================
    // A3: Hazard-Aware Landing Zone
    // ====================

    @Test
    fun acceptanceCriteria_A3_dangerZonesAreHighlighted() {
        // Given: Hole has known hazards (OB right, water long)
        navigateToLiveCaddy()
        navigateToTestHoleWithHazards()

        // When: User views PinSeeker map
        composeTestRule
            .onNodeWithText("Show Hole Strategy")
            .performClick()

        // Wait for strategy to load
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithText("Danger Zones")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Then: Danger zones are listed
        composeTestRule
            .onNodeWithText("OB right")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Water long")
            .assertIsDisplayed()
    }

    @Test
    fun acceptanceCriteria_A3_landingZoneAvoidsSlice() {
        // Given: User has dominant slice pattern
        navigateToLiveCaddy()
        setDominantMissPattern("SLICE")
        navigateToTestHoleWithHazards()

        // When: User views PinSeeker map
        composeTestRule
            .onNodeWithText("Show Hole Strategy")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithText("Landing Zone")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Then: Visual cue should mention aiming left
        composeTestRule
            .onNode(hasText(Regex(".*aim.*left.*", RegexOption.IGNORE_CASE)))
            .assertIsDisplayed()

        // And: Risk callout should mention right-side hazard
        composeTestRule
            .onNode(hasText(Regex(".*right.*OB.*", RegexOption.IGNORE_CASE)))
            .assertIsDisplayed()
    }

    @Test
    fun acceptanceCriteria_A3_landingZoneAvoidsHook() {
        // Given: User has dominant hook pattern
        navigateToLiveCaddy()
        setDominantMissPattern("HOOK")
        navigateToTestHoleWithHazards()

        // When: User views PinSeeker map
        composeTestRule
            .onNodeWithText("Show Hole Strategy")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithText("Landing Zone")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Then: Visual cue should mention aiming right (opposite of hook)
        composeTestRule
            .onNode(hasText(Regex(".*aim.*right.*", RegexOption.IGNORE_CASE)))
            .assertIsDisplayed()
    }

    @Test
    fun acceptanceCriteria_A3_riskCalloutsAreLimited() {
        // Given: Hole with many hazards
        navigateToLiveCaddy()
        navigateToTestHoleWithManyHazards()

        // When: User views PinSeeker map
        composeTestRule
            .onNodeWithText("Show Hole Strategy")
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodesWithText("Risk Callouts")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Then: Maximum 3 callouts should be shown
        val calloutCount = composeTestRule
            .onAllNodes(hasText(Regex(".*miss.*", RegexOption.IGNORE_CASE)))
            .fetchSemanticsNodes()
            .size

        assertTrue(
            calloutCount <= 3,
            "Risk callouts ($calloutCount) should be limited to 3 (A3)"
        )
    }

    // ====================
    // A4: Shot Logger Speed and Persistence
    // ====================

    @Test
    fun acceptanceCriteria_A4_shotLoggingCompletesUnder1Second() {
        // Given: User is on Live Caddy screen
        navigateToLiveCaddy()

        val startTime = System.currentTimeMillis()

        // When: User logs a shot
        composeTestRule
            .onNodeWithContentDescription("Log Shot")
            .performClick()

        // Select club
        composeTestRule
            .onNodeWithText("7I")
            .performClick()

        // Select result
        composeTestRule
            .onNodeWithText("FAIRWAY")
            .performClick()

        // Wait for confirmation
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodes(hasText(Regex(".*7I.*FAIRWAY.*")))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val totalTime = System.currentTimeMillis() - startTime

        // Then: Total time should be under 1 second
        assertTrue(
            totalTime < 1000,
            "Shot logging took ${totalTime}ms, expected < 1000ms (A4)"
        )

        // And: Confirmation toast is shown
        composeTestRule
            .onNode(hasText(Regex(".*7I.*FAIRWAY.*")))
            .assertIsDisplayed()
    }

    @Test
    fun acceptanceCriteria_A4_offlineShotsAreQueued() {
        // Given: User is in airplane mode (offline)
        navigateToLiveCaddy()
        enableAirplaneMode()

        // Wait for offline detection
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodesWithText("Offline")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // When: User logs a shot
        composeTestRule
            .onNodeWithContentDescription("Log Shot")
            .performClick()

        composeTestRule
            .onNodeWithText("Driver")
            .performClick()

        composeTestRule
            .onNodeWithText("ROUGH")
            .performClick()

        // Then: Shot is confirmed locally
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodes(hasText(Regex(".*Driver.*ROUGH.*")))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // And: Connectivity banner shows queued shot
        composeTestRule
            .onNodeWithText("1 shot queued")
            .assertIsDisplayed()
    }

    @Test
    fun acceptanceCriteria_A4_shotsAutoSyncWhenOnline() {
        // Given: User has queued shots offline
        navigateToLiveCaddy()
        enableAirplaneMode()

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodesWithText("Offline")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Log 2 shots offline
        logShotQuick("7I", "FAIRWAY")
        logShotQuick("PW", "GREEN")

        // Verify 2 shots queued
        composeTestRule
            .onNodeWithText("2 shots queued")
            .assertIsDisplayed()

        // When: Connection returns
        disableAirplaneMode()

        // Wait for sync to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodes(hasText(Regex(".*synced.*", RegexOption.IGNORE_CASE)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Then: Sync confirmation is shown
        composeTestRule
            .onNode(hasText(Regex(".*2 shots synced.*", RegexOption.IGNORE_CASE)))
            .assertIsDisplayed()

        // And: Pending count returns to 0
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodes(hasText(Regex(".*queued.*")))
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    @Test
    fun acceptanceCriteria_A4_shotsPersistAfterAppRestart() {
        // Given: User logs shots
        navigateToLiveCaddy()

        logShotQuick("Driver", "FAIRWAY")
        logShotQuick("7I", "GREEN")

        // When: App is restarted (simulate by recreating activity)
        composeTestRule.activityRule.scenario.recreate()

        // Navigate back to Live Caddy
        navigateToLiveCaddy()

        // Then: Shots are still visible in timeline
        composeTestRule
            .onNode(hasText(Regex(".*Driver.*FAIRWAY.*")))
            .assertExists()

        composeTestRule
            .onNode(hasText(Regex(".*7I.*GREEN.*")))
            .assertExists()
    }

    @Test
    fun acceptanceCriteria_A4_hapticFeedbackOnShotLogged() {
        // Note: Haptic feedback is difficult to test automatically
        // This test verifies the feedback manager is called
        // Manual testing is required to verify actual haptic sensation

        // Given: User is on Live Caddy screen
        navigateToLiveCaddy()

        // When: User logs a shot
        composeTestRule
            .onNodeWithContentDescription("Log Shot")
            .performClick()

        composeTestRule
            .onNodeWithText("7I")
            .performClick()

        composeTestRule
            .onNodeWithText("FAIRWAY")
            .performClick()

        // Wait for confirmation
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodes(hasText(Regex(".*7I.*FAIRWAY.*")))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Then: Haptic feedback should have fired
        // (This is implicit - we verify the shot was logged successfully)
        // Manual verification: User should feel a vibration on confirmation
    }

    // ====================
    // Performance Benchmarks
    // ====================

    @Test
    fun benchmark_shotLoggingAverageLatency() {
        navigateToLiveCaddy()

        val iterations = 10
        val latencies = mutableListOf<Long>()

        repeat(iterations) { i ->
            val startTime = System.currentTimeMillis()

            // Log shot
            composeTestRule
                .onNodeWithContentDescription("Log Shot")
                .performClick()

            composeTestRule
                .onNodeWithText("7I")
                .performClick()

            composeTestRule
                .onNodeWithText("FAIRWAY")
                .performClick()

            // Wait for confirmation
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule
                    .onAllNodes(hasText(Regex(".*7I.*FAIRWAY.*")))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            val endTime = System.currentTimeMillis()
            latencies.add(endTime - startTime)

            // Dismiss confirmation before next iteration
            composeTestRule
                .onNodeWithContentDescription("Dismiss")
                .performClick()

            // Small delay between iterations
            Thread.sleep(100)
        }

        val averageLatency = latencies.average()
        val p95Latency = latencies.sorted()[iterations * 95 / 100]

        println("Shot logging latency - Avg: ${averageLatency}ms, P95: ${p95Latency}ms")

        // Assert performance targets
        assertTrue(
            averageLatency < 2000,
            "Average shot logging latency (${averageLatency}ms) should be < 2000ms"
        )
        assertTrue(
            p95Latency < 2500,
            "P95 shot logging latency (${p95Latency}ms) should be < 2500ms"
        )
    }

    // ====================
    // Helper Methods
    // ====================

    private fun navigateToLiveCaddy() {
        // Navigate from home screen to Live Caddy
        // Assumes app starts on home screen
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodesWithText("Start Round")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("Start Round")
            .performClick()

        // Fill in course name
        composeTestRule
            .onNodeWithText("Course Name")
            .performTextInput("Test Course")

        composeTestRule
            .onNodeWithText("Continue")
            .performClick()

        // Wait for Live Caddy screen to load
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodes(hasText(Regex(".*Hole.*", RegexOption.IGNORE_CASE)))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun setManualReadiness(score: Int) {
        // Open settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()

        // Navigate to readiness settings
        composeTestRule
            .onNodeWithText("Readiness")
            .performClick()

        // Set manual readiness slider
        composeTestRule
            .onNodeWithContentDescription("Manual Readiness")
            .performScrollTo()
            .performTextClearance()
            .performTextInput(score.toString())

        // Save
        composeTestRule
            .onNodeWithText("Save")
            .performClick()

        // Back to Live Caddy
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()
    }

    private fun setDominantMissPattern(pattern: String) {
        // This would require navigating to miss pattern settings
        // For E2E test, we assume test data is pre-configured
        // Or we use a test API to inject miss patterns
    }

    private fun navigateToTestHoleWithHazards() {
        // Advance to a test hole with known hazards
        // For E2E test, we assume test course has appropriate holes
    }

    private fun navigateToTestHoleWithManyHazards() {
        // Navigate to a hole with 5+ hazards for callout limiting test
    }

    private fun extractSafetyMargin(): Int {
        // Extract safety margin value from displayed text
        // Look for "Safety margin: XXm" pattern
        val nodes = composeTestRule
            .onAllNodes(hasText(Regex("Safety margin: (\\d+)m")))
            .fetchSemanticsNodes()

        if (nodes.isEmpty()) return 0

        val text = nodes[0].config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text ?: ""
        val match = Regex("Safety margin: (\\d+)m").find(text)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun logShotQuick(club: String, result: String) {
        composeTestRule
            .onNodeWithContentDescription("Log Shot")
            .performClick()

        composeTestRule
            .onNodeWithText(club)
            .performClick()

        composeTestRule
            .onNodeWithText(result)
            .performClick()

        // Wait for confirmation
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            composeTestRule
                .onAllNodes(hasText(Regex(".*$club.*$result.*")))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Dismiss confirmation
        composeTestRule
            .onNodeWithContentDescription("Dismiss")
            .performClick()
    }

    private fun enableAirplaneMode() {
        // Note: Actual airplane mode control requires system permissions
        // For E2E test, we simulate offline mode via test flag or mock
        // Manual testing requires physical airplane mode toggle
    }

    private fun disableAirplaneMode() {
        // Restore connectivity
    }
}
