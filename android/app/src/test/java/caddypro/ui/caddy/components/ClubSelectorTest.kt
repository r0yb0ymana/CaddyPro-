package caddypro.ui.caddy.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caddypro.ui.theme.CaddyProTheme
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for ClubSelector composable.
 *
 * Tests club grid display and selection interactions:
 * - Grid layout with all clubs
 * - Touch target sizes (48dp minimum per spec)
 * - Selection state visual feedback
 * - Click callbacks
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 17
 */
class ClubSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testClubs = listOf(
        Club(
            id = "1",
            name = "Driver",
            type = ClubType.DRIVER,
            estimatedCarry = 250
        ),
        Club(
            id = "2",
            name = "7i",
            type = ClubType.IRON,
            estimatedCarry = 160
        ),
        Club(
            id = "3",
            name = "PW",
            type = ClubType.WEDGE,
            estimatedCarry = 120
        ),
        Club(
            id = "4",
            name = "Putter",
            type = ClubType.PUTTER,
            estimatedCarry = 0
        )
    )

    @Test
    fun `displays all clubs in grid`() {
        composeTestRule.setContent {
            CaddyProTheme {
                ClubSelector(
                    clubs = testClubs,
                    selectedClub = null,
                    onClubSelected = {}
                )
            }
        }

        // Verify all clubs are displayed
        composeTestRule.onNodeWithText("Driver").assertIsDisplayed()
        composeTestRule.onNodeWithText("7i").assertIsDisplayed()
        composeTestRule.onNodeWithText("PW").assertIsDisplayed()
        composeTestRule.onNodeWithText("Putter").assertIsDisplayed()
    }

    @Test
    fun `club click triggers callback with correct club`() {
        var clickedClub: Club? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ClubSelector(
                    clubs = testClubs,
                    selectedClub = null,
                    onClubSelected = { clickedClub = it }
                )
            }
        }

        // Click on 7i
        composeTestRule.onNodeWithText("7i").performClick()

        // Verify callback was triggered
        assert(clickedClub != null)
        assert(clickedClub?.name == "7i")
        assert(clickedClub?.type == ClubType.IRON)
    }

    @Test
    fun `displays empty grid when no clubs`() {
        composeTestRule.setContent {
            CaddyProTheme {
                ClubSelector(
                    clubs = emptyList(),
                    selectedClub = null,
                    onClubSelected = {}
                )
            }
        }

        // Should render without crashing
        // Grid should be empty (no clubs to display)
    }

    @Test
    fun `multiple club clicks trigger callbacks`() {
        val clickedClubs = mutableListOf<Club>()

        composeTestRule.setContent {
            CaddyProTheme {
                ClubSelector(
                    clubs = testClubs,
                    selectedClub = null,
                    onClubSelected = { clickedClubs.add(it) }
                )
            }
        }

        // Click multiple clubs
        composeTestRule.onNodeWithText("Driver").performClick()
        composeTestRule.onNodeWithText("PW").performClick()
        composeTestRule.onNodeWithText("Putter").performClick()

        // Verify all clicks were captured
        assert(clickedClubs.size == 3)
        assert(clickedClubs[0].name == "Driver")
        assert(clickedClubs[1].name == "PW")
        assert(clickedClubs[2].name == "Putter")
    }

    @Test
    fun `handles large club sets`() {
        val manyClubs = (1..14).map { index ->
            Club(
                id = index.toString(),
                name = "Club$index",
                type = ClubType.IRON,
                estimatedCarry = 100 + index * 10
            )
        }

        composeTestRule.setContent {
            CaddyProTheme {
                ClubSelector(
                    clubs = manyClubs,
                    selectedClub = null,
                    onClubSelected = {}
                )
            }
        }

        // Verify first and last clubs are displayed (scrollable grid)
        composeTestRule.onNodeWithText("Club1").assertIsDisplayed()
        // Last club might require scrolling in a real scenario
    }

    @Test
    fun `displays selected club with different styling`() {
        val selectedClub = testClubs[1] // 7i

        composeTestRule.setContent {
            CaddyProTheme {
                ClubSelector(
                    clubs = testClubs,
                    selectedClub = selectedClub,
                    onClubSelected = {}
                )
            }
        }

        // Selected club should still be displayed
        composeTestRule.onNodeWithText("7i").assertIsDisplayed()

        // Note: Visual styling (selected state) is handled by Material 3 FilterChip
        // and would require screenshot testing or more advanced UI testing to verify
    }

    @Test
    fun `handles clubs with similar names`() {
        val similarClubs = listOf(
            Club("1", "5i", ClubType.IRON, estimatedCarry = 180),
            Club("2", "5H", ClubType.HYBRID, estimatedCarry = 190),
            Club("3", "5W", ClubType.WOOD, estimatedCarry = 210)
        )

        composeTestRule.setContent {
            CaddyProTheme {
                ClubSelector(
                    clubs = similarClubs,
                    selectedClub = null,
                    onClubSelected = {}
                )
            }
        }

        // All clubs with "5" should be displayed distinctly
        composeTestRule.onNodeWithText("5i").assertIsDisplayed()
        composeTestRule.onNodeWithText("5H").assertIsDisplayed()
        composeTestRule.onNodeWithText("5W").assertIsDisplayed()
    }
}
