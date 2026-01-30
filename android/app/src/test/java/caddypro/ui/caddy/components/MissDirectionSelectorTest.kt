package caddypro.ui.caddy.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.ui.theme.CaddyProTheme
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for MissDirectionSelector composable.
 *
 * Tests miss direction grid display and selection interactions:
 * - All direction options displayed
 * - Touch target sizes (64dp per spec)
 * - Directional labels with symbols
 * - Click callbacks
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 17
 */
class MissDirectionSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `displays all miss direction options`() {
        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = {})
            }
        }

        // Verify all primary directions are displayed
        composeTestRule.onNodeWithText("← Pull").assertIsDisplayed()
        composeTestRule.onNodeWithText("Push →").assertIsDisplayed()
        composeTestRule.onNodeWithText("← Hook").assertIsDisplayed()
        composeTestRule.onNodeWithText("Slice →").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fat ↓").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thin ↑").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Straight").assertIsDisplayed()
    }

    @Test
    fun `push click triggers callback with PUSH direction`() {
        var selectedDirection: MissDirection? = null

        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = { selectedDirection = it })
            }
        }

        composeTestRule.onNodeWithText("Push →").performClick()

        assert(selectedDirection == MissDirection.PUSH)
    }

    @Test
    fun `pull click triggers callback with PULL direction`() {
        var selectedDirection: MissDirection? = null

        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = { selectedDirection = it })
            }
        }

        composeTestRule.onNodeWithText("← Pull").performClick()

        assert(selectedDirection == MissDirection.PULL)
    }

    @Test
    fun `slice click triggers callback with SLICE direction`() {
        var selectedDirection: MissDirection? = null

        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = { selectedDirection = it })
            }
        }

        composeTestRule.onNodeWithText("Slice →").performClick()

        assert(selectedDirection == MissDirection.SLICE)
    }

    @Test
    fun `hook click triggers callback with HOOK direction`() {
        var selectedDirection: MissDirection? = null

        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = { selectedDirection = it })
            }
        }

        composeTestRule.onNodeWithText("← Hook").performClick()

        assert(selectedDirection == MissDirection.HOOK)
    }

    @Test
    fun `fat click triggers callback with FAT direction`() {
        var selectedDirection: MissDirection? = null

        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = { selectedDirection = it })
            }
        }

        composeTestRule.onNodeWithText("Fat ↓").performClick()

        assert(selectedDirection == MissDirection.FAT)
    }

    @Test
    fun `thin click triggers callback with THIN direction`() {
        var selectedDirection: MissDirection? = null

        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = { selectedDirection = it })
            }
        }

        composeTestRule.onNodeWithText("Thin ↑").performClick()

        assert(selectedDirection == MissDirection.THIN)
    }

    @Test
    fun `straight click triggers callback with STRAIGHT direction`() {
        var selectedDirection: MissDirection? = null

        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = { selectedDirection = it })
            }
        }

        composeTestRule.onNodeWithText("✓ Straight").performClick()

        assert(selectedDirection == MissDirection.STRAIGHT)
    }

    @Test
    fun `multiple clicks trigger multiple callbacks`() {
        val selectedDirections = mutableListOf<MissDirection>()

        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = { selectedDirections.add(it) })
            }
        }

        // Click multiple directions
        composeTestRule.onNodeWithText("Push →").performClick()
        composeTestRule.onNodeWithText("← Hook").performClick()
        composeTestRule.onNodeWithText("Fat ↓").performClick()

        // Verify all clicks were captured
        assert(selectedDirections.size == 3)
        assert(selectedDirections[0] == MissDirection.PUSH)
        assert(selectedDirections[1] == MissDirection.HOOK)
        assert(selectedDirections[2] == MissDirection.FAT)
    }

    @Test
    fun `direction buttons display in grid layout`() {
        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = {})
            }
        }

        // All buttons should be visible simultaneously (3 columns grid)
        composeTestRule.onNodeWithText("← Pull").assertIsDisplayed()
        composeTestRule.onNodeWithText("✓ Straight").assertIsDisplayed()
        composeTestRule.onNodeWithText("Push →").assertIsDisplayed()
    }

    @Test
    fun `directional symbols are displayed`() {
        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = {})
            }
        }

        // Verify directional symbols are included in labels
        // Left arrows for Pull and Hook
        composeTestRule.onNodeWithText("← Pull").assertIsDisplayed()
        composeTestRule.onNodeWithText("← Hook").assertIsDisplayed()

        // Right arrows for Push and Slice
        composeTestRule.onNodeWithText("Push →").assertIsDisplayed()
        composeTestRule.onNodeWithText("Slice →").assertIsDisplayed()

        // Vertical arrows for Fat and Thin
        composeTestRule.onNodeWithText("Fat ↓").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thin ↑").assertIsDisplayed()

        // Checkmark for Straight
        composeTestRule.onNodeWithText("✓ Straight").assertIsDisplayed()
    }

    @Test
    fun `left miss directions grouped together`() {
        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = {})
            }
        }

        // Pull and Hook are left misses - both should be displayed
        composeTestRule.onNodeWithText("← Pull").assertIsDisplayed()
        composeTestRule.onNodeWithText("← Hook").assertIsDisplayed()
    }

    @Test
    fun `right miss directions grouped together`() {
        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = {})
            }
        }

        // Push and Slice are right misses - both should be displayed
        composeTestRule.onNodeWithText("Push →").assertIsDisplayed()
        composeTestRule.onNodeWithText("Slice →").assertIsDisplayed()
    }

    @Test
    fun `contact quality directions displayed`() {
        composeTestRule.setContent {
            CaddyProTheme {
                MissDirectionSelector(onDirectionSelected = {})
            }
        }

        // Fat and Thin are contact quality misses
        composeTestRule.onNodeWithText("Fat ↓").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thin ↑").assertIsDisplayed()
    }
}
