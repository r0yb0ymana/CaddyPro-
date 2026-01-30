package caddypro.ui.caddy.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import caddypro.domain.caddy.models.HazardLocation
import caddypro.domain.caddy.models.HazardType
import caddypro.domain.caddy.models.HazardZone
import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.LandingZone
import caddypro.domain.caddy.models.PersonalizationContext
import caddypro.domain.navcaddy.models.MissDirection
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for PinSeekerMap component.
 *
 * Validates:
 * - Strategy display with all components
 * - Null strategy handling
 * - Personalization context display
 * - Risk callout limits (max 3)
 * - Material 3 design compliance
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 16
 * Acceptance criteria: A3 (Hazard-aware landing zone)
 */
class PinSeekerMapTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===========================
    // Display Tests
    // ===========================

    @Test
    fun pinSeekerMap_withStrategy_displaysHoleHeader() {
        // Given
        val strategy = createTestStrategy(holeNumber = 7)

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then
        composeTestRule
            .onNodeWithText("Hole 7 Strategy")
            .assertIsDisplayed()
    }

    @Test
    fun pinSeekerMap_withStrategy_displaysPersonalizationContext() {
        // Given
        val strategy = createTestStrategy(
            handicap = 9,
            dominantMiss = MissDirection.SLICE,
            readinessScore = 78
        )

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then
        composeTestRule
            .onNodeWithText("HCP 9 • Slice miss • Readiness 78")
            .assertIsDisplayed()
    }

    @Test
    fun pinSeekerMap_withDangerZones_displaysDangerZonesCard() {
        // Given
        val strategy = createTestStrategy(
            dangerZones = listOf(
                createHazardZone(type = HazardType.WATER, side = "right")
            )
        )

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then
        composeTestRule
            .onNodeWithText("Danger Zones")
            .assertIsDisplayed()
    }

    @Test
    fun pinSeekerMap_withLandingZone_displaysRecommendedTarget() {
        // Given
        val strategy = createTestStrategy(
            landingZone = LandingZone(
                targetLine = 275,
                idealDistance = 230,
                safetyMargin = 15,
                visualCue = "Aim 10 yards left of fairway bunker"
            )
        )

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then
        composeTestRule
            .onNodeWithText("Recommended Target")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Aim 10 yards left of fairway bunker")
            .assertIsDisplayed()
    }

    @Test
    fun pinSeekerMap_withRiskCallouts_displaysRiskCalloutsList() {
        // Given
        val strategy = createTestStrategy(
            riskCallouts = listOf("Right miss brings water into play")
        )

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then
        composeTestRule
            .onNodeWithText("Right miss brings water into play")
            .assertIsDisplayed()
    }

    // ===========================
    // Null Strategy Tests
    // ===========================

    @Test
    fun pinSeekerMap_withNullStrategy_displaysEmptyPlaceholder() {
        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = null)
        }

        // Then
        composeTestRule
            .onNodeWithText("Course Data Not Available")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Strategy map requires course hazard data. Please ensure course data is downloaded or available.")
            .assertIsDisplayed()
    }

    @Test
    fun pinSeekerMap_withNullStrategy_doesNotDisplayStrategyComponents() {
        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = null)
        }

        // Then
        composeTestRule
            .onNodeWithText("Danger Zones")
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("Recommended Target")
            .assertDoesNotExist()
    }

    // ===========================
    // Risk Callout Limit Tests
    // ===========================

    @Test
    fun pinSeekerMap_withMoreThanThreeCallouts_displaysOnlyFirstThree() {
        // Given: 5 risk callouts
        val strategy = createTestStrategy(
            riskCallouts = listOf(
                "Callout 1",
                "Callout 2",
                "Callout 3",
                "Callout 4",
                "Callout 5"
            )
        )

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then: Only first 3 displayed
        composeTestRule.onNodeWithText("Callout 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Callout 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Callout 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Callout 4").assertDoesNotExist()
        composeTestRule.onNodeWithText("Callout 5").assertDoesNotExist()
    }

    @Test
    fun pinSeekerMap_withExactlyThreeCallouts_displaysAll() {
        // Given: Exactly 3 risk callouts
        val strategy = createTestStrategy(
            riskCallouts = listOf(
                "Callout 1",
                "Callout 2",
                "Callout 3"
            )
        )

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then: All 3 displayed
        composeTestRule.onNodeWithText("Callout 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Callout 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Callout 3").assertIsDisplayed()
    }

    // ===========================
    // Miss Direction Display Tests
    // ===========================

    @Test
    fun pinSeekerMap_withSliceMiss_displaysSliceInHeader() {
        // Given
        val strategy = createTestStrategy(dominantMiss = MissDirection.SLICE)

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then
        composeTestRule
            .onNodeWithText(text = "Slice miss", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun pinSeekerMap_withHookMiss_displaysHookInHeader() {
        // Given
        val strategy = createTestStrategy(dominantMiss = MissDirection.HOOK)

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then
        composeTestRule
            .onNodeWithText(text = "Hook miss", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun pinSeekerMap_withStraightMiss_displaysStraightInHeader() {
        // Given
        val strategy = createTestStrategy(dominantMiss = MissDirection.STRAIGHT)

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then
        composeTestRule
            .onNodeWithText(text = "Straight miss", substring = true)
            .assertIsDisplayed()
    }

    // ===========================
    // Empty Danger Zones Tests
    // ===========================

    @Test
    fun pinSeekerMap_withNoDangerZones_doesNotDisplayDangerZonesCard() {
        // Given
        val strategy = createTestStrategy(dangerZones = emptyList())

        // When
        composeTestRule.setContent {
            PinSeekerMap(strategy = strategy)
        }

        // Then
        composeTestRule
            .onNodeWithText("Danger Zones")
            .assertDoesNotExist()
    }

    // ===========================
    // Helper Methods
    // ===========================

    private fun createTestStrategy(
        holeNumber: Int = 7,
        dangerZones: List<HazardZone> = listOf(
            createHazardZone(type = HazardType.WATER, side = "right")
        ),
        landingZone: LandingZone = LandingZone(
            targetLine = 275,
            idealDistance = 230,
            safetyMargin = 15,
            visualCue = "Aim 10 yards left of fairway bunker"
        ),
        riskCallouts: List<String> = listOf("Right miss brings water into play"),
        handicap: Int = 9,
        dominantMiss: MissDirection = MissDirection.SLICE,
        readinessScore: Int = 78
    ): HoleStrategy {
        return HoleStrategy(
            holeNumber = holeNumber,
            dangerZones = dangerZones,
            recommendedLandingZone = landingZone,
            riskCallouts = riskCallouts,
            personalizedFor = PersonalizationContext(
                handicap = handicap,
                dominantMiss = dominantMiss,
                clubDistances = mapOf("Driver" to 240, "3W" to 220),
                readinessScore = readinessScore
            )
        )
    }

    private fun createHazardZone(
        type: HazardType,
        side: String,
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
