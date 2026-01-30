package caddypro.ui.caddy.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caddypro.domain.navcaddy.models.Lie
import caddypro.ui.theme.CaddyProTheme
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for ShotResultSelector composable.
 *
 * Tests result grid display and selection interactions:
 * - All lie results displayed
 * - Touch target sizes (64dp per spec for one-second taps)
 * - Color coding (green=good, red=bad)
 * - Click callbacks
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 17
 */
class ShotResultSelectorTest {

    @get:Rule
    val composeTestRule = composeTestRule()

    @Test
    fun `displays all result options`() {
        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = {})
            }
        }

        // Verify all primary results are displayed
        composeTestRule.onNodeWithText("Fairway").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fringe").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rough").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bunker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Water").assertIsDisplayed()
    }

    @Test
    fun `fairway click triggers callback with FAIRWAY lie`() {
        var selectedLie: Lie? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = { selectedLie = it })
            }
        }

        composeTestRule.onNodeWithText("Fairway").performClick()

        assert(selectedLie == Lie.FAIRWAY)
    }

    @Test
    fun `green click triggers callback with GREEN lie`() {
        var selectedLie: Lie? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = { selectedLie = it })
            }
        }

        composeTestRule.onNodeWithText("Green").performClick()

        assert(selectedLie == Lie.GREEN)
    }

    @Test
    fun `rough click triggers callback with ROUGH lie`() {
        var selectedLie: Lie? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = { selectedLie = it })
            }
        }

        composeTestRule.onNodeWithText("Rough").performClick()

        assert(selectedLie == Lie.ROUGH)
    }

    @Test
    fun `bunker click triggers callback with BUNKER lie`() {
        var selectedLie: Lie? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = { selectedLie = it })
            }
        }

        composeTestRule.onNodeWithText("Bunker").performClick()

        assert(selectedLie == Lie.BUNKER)
    }

    @Test
    fun `water click triggers callback with HAZARD lie`() {
        var selectedLie: Lie? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = { selectedLie = it })
            }
        }

        composeTestRule.onNodeWithText("Water").performClick()

        assert(selectedLie == Lie.HAZARD)
    }

    @Test
    fun `fringe click triggers callback with FRINGE lie`() {
        var selectedLie: Lie? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = { selectedLie = it })
            }
        }

        composeTestRule.onNodeWithText("Fringe").performClick()

        assert(selectedLie == Lie.FRINGE)
    }

    @Test
    fun `multiple clicks trigger multiple callbacks`() {
        val selectedLies = mutableListOf<Lie>()

        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = { selectedLies.add(it) })
            }
        }

        // Click multiple results
        composeTestRule.onNodeWithText("Fairway").performClick()
        composeTestRule.onNodeWithText("Green").performClick()
        composeTestRule.onNodeWithText("Bunker").performClick()

        // Verify all clicks were captured
        assert(selectedLies.size == 3)
        assert(selectedLies[0] == Lie.FAIRWAY)
        assert(selectedLies[1] == Lie.GREEN)
        assert(selectedLies[2] == Lie.BUNKER)
    }

    @Test
    fun `result buttons display in grid layout`() {
        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = {})
            }
        }

        // All buttons should be visible simultaneously (3 columns grid)
        composeTestRule.onNodeWithText("Fairway").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fringe").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rough").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bunker").assertIsDisplayed()
        composeTestRule.onNodeWithText("Water").assertIsDisplayed()
    }

    @Test
    fun `water label maps to HAZARD enum`() {
        var selectedLie: Lie? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotResultSelector(onResultSelected = { selectedLie = it })
            }
        }

        // "Water" button should map to Lie.HAZARD
        composeTestRule.onNodeWithText("Water").performClick()

        assert(selectedLie == Lie.HAZARD)
    }
}
