package caddypro.ui.caddy.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for RiskCalloutsList component.
 *
 * Validates:
 * - Risk callout display
 * - Warning icons for each callout
 * - Empty list handling
 * - Maximum 3 callouts per spec
 * - Personalized callout content
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 16
 * Acceptance criteria: A3 (Hazard-aware landing zone)
 */
class RiskCalloutsListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ===========================
    // Display Tests
    // ===========================

    @Test
    fun riskCalloutsList_withCallouts_displaysTitle() {
        // Given
        val callouts = listOf("Right miss brings water into play")

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Risk Callouts")
            .assertIsDisplayed()
    }

    @Test
    fun riskCalloutsList_withSingleCallout_displaysCallout() {
        // Given
        val callouts = listOf("Right miss brings water into play at 180-220m")

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Right miss brings water into play at 180-220m")
            .assertIsDisplayed()
    }

    @Test
    fun riskCalloutsList_withMultipleCallouts_displaysAllCallouts() {
        // Given
        val callouts = listOf(
            "Right miss brings water into play at 180-220m",
            "Left miss is OB stroke and distance",
            "Aggressive carry over bunker at 240m"
        )

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Right miss brings water into play at 180-220m")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Left miss is OB stroke and distance")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Aggressive carry over bunker at 240m")
            .assertIsDisplayed()
    }

    // ===========================
    // Warning Icon Tests
    // ===========================

    @Test
    fun riskCalloutsList_withSingleCallout_displaysWarningIcon() {
        // Given
        val callouts = listOf("Right miss brings water into play")

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then: One warning icon is displayed
        composeTestRule
            .onAllNodesWithContentDescription("Risk")
            .assertCountEquals(1)
    }

    @Test
    fun riskCalloutsList_withMultipleCallouts_displaysMultipleWarningIcons() {
        // Given
        val callouts = listOf(
            "Right miss brings water into play",
            "Left miss is OB stroke and distance",
            "Aggressive carry over bunker at 240m"
        )

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then: Three warning icons are displayed
        composeTestRule
            .onAllNodesWithContentDescription("Risk")
            .assertCountEquals(3)
    }

    // ===========================
    // Empty List Tests
    // ===========================

    @Test
    fun riskCalloutsList_withEmptyList_doesNotDisplayTitle() {
        // Given
        val callouts = emptyList<String>()

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Risk Callouts")
            .assertDoesNotExist()
    }

    @Test
    fun riskCalloutsList_withEmptyList_doesNotDisplayAnyContent() {
        // Given
        val callouts = emptyList<String>()

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then: No warning icons displayed
        composeTestRule
            .onAllNodesWithContentDescription("Risk")
            .assertCountEquals(0)
    }

    // ===========================
    // Personalized Callout Tests
    // ===========================

    @Test
    fun riskCalloutsList_withSliceCallout_displaysSliceWarning() {
        // Given: Callout references slice miss
        val callouts = listOf("Your slice pattern brings water into play right")

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Your slice pattern brings water into play right")
            .assertIsDisplayed()
    }

    @Test
    fun riskCalloutsList_withHookCallout_displaysHookWarning() {
        // Given: Callout references hook miss
        val callouts = listOf("Hook pattern brings trees into play left")

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Hook pattern brings trees into play left")
            .assertIsDisplayed()
    }

    @Test
    fun riskCalloutsList_withReadinessCallout_displaysReadinessWarning() {
        // Given: Callout references low readiness
        val callouts = listOf("Long miss finds water - play conservative")

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Long miss finds water - play conservative")
            .assertIsDisplayed()
    }

    // ===========================
    // Long Text Tests
    // ===========================

    @Test
    fun riskCalloutsList_withLongCallout_displaysFullText() {
        // Given: Long callout text
        val callouts = listOf(
            "Your dominant right miss pattern brings the water hazard on the right side into play between 180-220 meters from the tee"
        )

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Your dominant right miss pattern brings the water hazard on the right side into play between 180-220 meters from the tee")
            .assertIsDisplayed()
    }

    // ===========================
    // Multiple Callout Scenarios
    // ===========================

    @Test
    fun riskCalloutsList_withTwoCallouts_displaysBothWithIcons() {
        // Given
        val callouts = listOf(
            "Short miss leaves difficult uphill pitch",
            "Wind at 3 o'clock - aim left edge"
        )

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Short miss leaves difficult uphill pitch")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Wind at 3 o'clock - aim left edge")
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithContentDescription("Risk")
            .assertCountEquals(2)
    }

    @Test
    fun riskCalloutsList_withMaxCallouts_displaysAllThree() {
        // Given: Exactly 3 callouts (maximum per spec)
        val callouts = listOf(
            "Callout 1",
            "Callout 2",
            "Callout 3"
        )

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then: All 3 displayed with icons
        composeTestRule.onNodeWithText("Callout 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Callout 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Callout 3").assertIsDisplayed()
        composeTestRule
            .onAllNodesWithContentDescription("Risk")
            .assertCountEquals(3)
    }

    // ===========================
    // Context-Specific Callouts
    // ===========================

    @Test
    fun riskCalloutsList_withWindCallout_displaysWindWarning() {
        // Given
        val callouts = listOf("Strong crosswind from right - adjust aim 10 yards left")

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Strong crosswind from right - adjust aim 10 yards left")
            .assertIsDisplayed()
    }

    @Test
    fun riskCalloutsList_withDistanceCallout_displaysDistanceWarning() {
        // Given
        val callouts = listOf("Lay up zone at 150m to avoid water at 180m")

        // When
        composeTestRule.setContent {
            RiskCalloutsList(callouts = callouts)
        }

        // Then
        composeTestRule
            .onNodeWithText("Lay up zone at 150m to avoid water at 180m")
            .assertIsDisplayed()
    }
}
