package caddypro.ui.caddy.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import caddypro.domain.caddy.models.LandingZone
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for LandingZoneIndicator component.
 *
 * Validates:
 * - Visual cue display (prominent)
 * - Target line display
 * - Distance display
 * - Safety margin display
 * - Icon presence
 * - Large text for outdoor visibility
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 16
 * Acceptance criteria: A3 (Hazard-aware landing zone)
 */
class LandingZoneIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===========================
    // Visual Cue Display Tests
    // ===========================

    @Test
    fun landingZoneIndicator_displaysVisualCue() {
        // Given
        val zone = createLandingZone(
            visualCue = "Aim 10 yards left of fairway bunker"
        )

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("Aim 10 yards left of fairway bunker")
            .assertIsDisplayed()
    }

    @Test
    fun landingZoneIndicator_withConservativeCue_displaysCue() {
        // Given
        val zone = createLandingZone(
            visualCue = "Play short of water hazard"
        )

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("Play short of water hazard")
            .assertIsDisplayed()
    }

    @Test
    fun landingZoneIndicator_withAggressiveCue_displaysCue() {
        // Given
        val zone = createLandingZone(
            visualCue = "Attack flag over bunker"
        )

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("Attack flag over bunker")
            .assertIsDisplayed()
    }

    @Test
    fun landingZoneIndicator_withLongVisualCue_displaysCue() {
        // Given
        val zone = createLandingZone(
            visualCue = "Aim at the large tree behind green, favor left side"
        )

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("Aim at the large tree behind green, favor left side")
            .assertIsDisplayed()
    }

    // ===========================
    // Target Line Display Tests
    // ===========================

    @Test
    fun landingZoneIndicator_displaysTargetLine() {
        // Given
        val zone = createLandingZone(targetLine = 275)

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("Target Line")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("275°")
            .assertIsDisplayed()
    }

    @Test
    fun landingZoneIndicator_withDifferentTargetLine_displaysCorrectValue() {
        // Given
        val zone = createLandingZone(targetLine = 90)

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("90°")
            .assertIsDisplayed()
    }

    @Test
    fun landingZoneIndicator_withZeroTargetLine_displaysZero() {
        // Given
        val zone = createLandingZone(targetLine = 0)

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("0°")
            .assertIsDisplayed()
    }

    // ===========================
    // Distance Display Tests
    // ===========================

    @Test
    fun landingZoneIndicator_displaysDistance() {
        // Given
        val zone = createLandingZone(idealDistance = 230)

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("Distance")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("230m")
            .assertIsDisplayed()
    }

    @Test
    fun landingZoneIndicator_withShortDistance_displaysCorrectValue() {
        // Given
        val zone = createLandingZone(idealDistance = 150)

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("150m")
            .assertIsDisplayed()
    }

    @Test
    fun landingZoneIndicator_withLongDistance_displaysCorrectValue() {
        // Given
        val zone = createLandingZone(idealDistance = 285)

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("285m")
            .assertIsDisplayed()
    }

    // ===========================
    // Safety Margin Display Tests
    // ===========================

    @Test
    fun landingZoneIndicator_displaysSafetyMargin() {
        // Given
        val zone = createLandingZone(safetyMargin = 15)

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("Margin")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("±15m")
            .assertIsDisplayed()
    }

    @Test
    fun landingZoneIndicator_withHighReadiness_displaysSmallMargin() {
        // Given: Small safety margin (high readiness)
        val zone = createLandingZone(safetyMargin = 10)

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("±10m")
            .assertIsDisplayed()
    }

    @Test
    fun landingZoneIndicator_withLowReadiness_displaysLargeMargin() {
        // Given: Large safety margin (low readiness)
        val zone = createLandingZone(safetyMargin = 25)

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("±25m")
            .assertIsDisplayed()
    }

    // ===========================
    // Icon Display Tests
    // ===========================

    @Test
    fun landingZoneIndicator_displaysTargetIcon() {
        // Given
        val zone = createLandingZone()

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Target")
            .assertIsDisplayed()
    }

    // ===========================
    // All Details Display Test
    // ===========================

    @Test
    fun landingZoneIndicator_displaysAllDetails() {
        // Given
        val zone = createLandingZone(
            targetLine = 275,
            idealDistance = 230,
            safetyMargin = 15,
            visualCue = "Aim 10 yards left of fairway bunker"
        )

        // When
        composeTestRule.setContent {
            LandingZoneIndicator(zone = zone)
        }

        // Then: All components are displayed
        composeTestRule.onNodeWithText("Aim 10 yards left of fairway bunker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Target Line").assertIsDisplayed()
        composeTestRule.onNodeWithText("275°").assertIsDisplayed()
        composeTestRule.onNodeWithText("Distance").assertIsDisplayed()
        composeTestRule.onNodeWithText("230m").assertIsDisplayed()
        composeTestRule.onNodeWithText("Margin").assertIsDisplayed()
        composeTestRule.onNodeWithText("±15m").assertIsDisplayed()
    }

    // ===========================
    // Helper Methods
    // ===========================

    private fun createLandingZone(
        targetLine: Int = 275,
        idealDistance: Int = 230,
        safetyMargin: Int = 15,
        visualCue: String = "Aim 10 yards left of fairway bunker"
    ): LandingZone {
        return LandingZone(
            targetLine = targetLine,
            idealDistance = idealDistance,
            safetyMargin = safetyMargin,
            visualCue = visualCue
        )
    }
}
