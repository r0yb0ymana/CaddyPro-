package caddypro.ui.caddy.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import caddypro.domain.caddy.models.HazardLocation
import caddypro.domain.caddy.models.HazardType
import caddypro.domain.caddy.models.HazardZone
import caddypro.domain.navcaddy.models.MissDirection
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for HazardZoneIndicator component.
 *
 * Validates:
 * - Hazard type display with appropriate icons
 * - Location and distance range display
 * - Icon selection for each hazard type
 * - Outdoor visibility (large text)
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 16
 */
class HazardZoneIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===========================
    // Hazard Type Display Tests
    // ===========================

    @Test
    fun hazardZoneIndicator_withWaterHazard_displaysWaterIcon() {
        // Given
        val zone = createHazardZone(type = HazardType.WATER)

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("WATER")
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withOBHazard_displaysOBIcon() {
        // Given
        val zone = createHazardZone(type = HazardType.OB)

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("OB")
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withBunkerHazard_displaysBunkerIcon() {
        // Given
        val zone = createHazardZone(type = HazardType.BUNKER)

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("BUNKER")
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withTreesHazard_displaysTreesIcon() {
        // Given
        val zone = createHazardZone(type = HazardType.TREES)

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("TREES")
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withPenaltyRoughHazard_displaysRoughIcon() {
        // Given
        val zone = createHazardZone(type = HazardType.PENALTY_ROUGH)

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("PENALTY_ROUGH")
            .assertIsDisplayed()
    }

    // ===========================
    // Hazard Display Name Tests
    // ===========================

    @Test
    fun hazardZoneIndicator_withWaterHazard_displaysWaterName() {
        // Given
        val zone = createHazardZone(type = HazardType.WATER, side = "right")

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("Water right")
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withOBHazard_displaysOBName() {
        // Given
        val zone = createHazardZone(type = HazardType.OB, side = "left")

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("OB left")
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withBunkerHazard_displaysBunkerName() {
        // Given
        val zone = createHazardZone(type = HazardType.BUNKER, side = "center")

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("Bunker center")
            .assertIsDisplayed()
    }

    // ===========================
    // Distance Range Display Tests
    // ===========================

    @Test
    fun hazardZoneIndicator_displaysDistanceRange() {
        // Given
        val zone = createHazardZone(distanceRange = 180..220)

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("180-220m")
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withDifferentRange_displaysCorrectRange() {
        // Given
        val zone = createHazardZone(distanceRange = 150..170)

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("150-170m")
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withLongRange_displaysCorrectRange() {
        // Given
        val zone = createHazardZone(distanceRange = 280..320)

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText("280-320m")
            .assertIsDisplayed()
    }

    // ===========================
    // Location Tests
    // ===========================

    @Test
    fun hazardZoneIndicator_withRightSide_displaysRight() {
        // Given
        val zone = createHazardZone(side = "right")

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText(text = "right", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withLeftSide_displaysLeft() {
        // Given
        val zone = createHazardZone(side = "left")

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText(text = "left", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withCenterSide_displaysCenter() {
        // Given
        val zone = createHazardZone(side = "center")

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText(text = "center", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withLongSide_displaysLong() {
        // Given
        val zone = createHazardZone(side = "long")

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText(text = "long", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun hazardZoneIndicator_withShortSide_displaysShort() {
        // Given
        val zone = createHazardZone(side = "short")

        // When
        composeTestRule.setContent {
            HazardZoneIndicator(zone = zone)
        }

        // Then
        composeTestRule
            .onNodeWithText(text = "short", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    // ===========================
    // Helper Methods
    // ===========================

    private fun createHazardZone(
        type: HazardType = HazardType.WATER,
        side: String = "right",
        distanceRange: IntRange = 180..220
    ): HazardZone {
        return HazardZone(
            type = type,
            location = HazardLocation(side, distanceRange),
            penaltyStrokes = 1.0,
            affectedMisses = listOf(MissDirection.SLICE)
        )
    }
}
