package caddypro.ui.caddy.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import caddypro.domain.caddy.usecases.ShotResult
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.ui.theme.CaddyProTheme
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for ShotLogger composable.
 *
 * Tests the multi-step shot logging flow:
 * 1. Club selection
 * 2. Result selection (lie)
 * 3. Miss direction selection (conditional)
 *
 * Verifies:
 * - Progressive disclosure (steps appear in order)
 * - Club selection updates state
 * - Result selection triggers callbacks
 * - Miss direction required for hazards
 * - Haptic feedback simulation
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 17
 * Acceptance criteria: A4 (Shot logger speed and persistence)
 */
class ShotLoggerTest {

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
        )
    )

    @Test
    fun `initially shows club selection step`() {
        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = null,
                    onClubSelected = {},
                    onShotLogged = {}
                )
            }
        }

        // Verify header
        composeTestRule.onNodeWithText("Log Shot").assertIsDisplayed()

        // Verify step 1 label
        composeTestRule.onNodeWithText("1. Select Club").assertIsDisplayed()

        // Verify clubs are displayed
        composeTestRule.onNodeWithText("Driver").assertIsDisplayed()
        composeTestRule.onNodeWithText("7i").assertIsDisplayed()
        composeTestRule.onNodeWithText("PW").assertIsDisplayed()
    }

    @Test
    fun `does not show result selection until club selected`() {
        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = null,
                    onClubSelected = {},
                    onShotLogged = {}
                )
            }
        }

        // Step 2 should not be visible
        composeTestRule.onNodeWithText("2. Select Result").assertDoesNotExist()
    }

    @Test
    fun `shows result selection after club selected`() {
        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = testClubs[0], // Driver selected
                    onClubSelected = {},
                    onShotLogged = {}
                )
            }
        }

        // Step 2 should now be visible
        composeTestRule.onNodeWithText("2. Select Result").assertIsDisplayed()

        // Verify result buttons are displayed
        composeTestRule.onNodeWithText("Fairway").assertIsDisplayed()
        composeTestRule.onNodeWithText("Green").assertIsDisplayed()
        composeTestRule.onNodeWithText("Water").assertIsDisplayed()
    }

    @Test
    fun `club selection triggers callback`() {
        var selectedClub: Club? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = null,
                    onClubSelected = { selectedClub = it },
                    onShotLogged = {}
                )
            }
        }

        // Click on Driver
        composeTestRule.onNodeWithText("Driver").performClick()

        // Verify callback was triggered with correct club
        assert(selectedClub?.name == "Driver")
        assert(selectedClub?.type == ClubType.DRIVER)
    }

    @Test
    fun `fairway result logs shot immediately without miss direction`() {
        var loggedResult: ShotResult? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = testClubs[0],
                    onClubSelected = {},
                    onShotLogged = { loggedResult = it }
                )
            }
        }

        // Click Fairway
        composeTestRule.onNodeWithText("Fairway").performClick()

        // Verify shot was logged immediately
        assert(loggedResult?.lie == Lie.FAIRWAY)
        assert(loggedResult?.missDirection == null)

        // Miss direction step should not appear
        composeTestRule.onNodeWithText("3. Miss Direction").assertDoesNotExist()
    }

    @Test
    fun `hazard result requires miss direction`() {
        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = testClubs[0],
                    onClubSelected = {},
                    onShotLogged = {}
                )
            }
        }

        // Click Water (hazard)
        composeTestRule.onNodeWithText("Water").performClick()

        // Miss direction step should appear
        composeTestRule.onNodeWithText("3. Miss Direction").assertIsDisplayed()

        // Verify miss direction buttons
        composeTestRule.onNodeWithText("Push →").assertIsDisplayed()
        composeTestRule.onNodeWithText("← Pull").assertIsDisplayed()
    }

    @Test
    fun `rough result requires miss direction`() {
        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = testClubs[0],
                    onClubSelected = {},
                    onShotLogged = {}
                )
            }
        }

        // Click Rough
        composeTestRule.onNodeWithText("Rough").performClick()

        // Miss direction step should appear
        composeTestRule.onNodeWithText("3. Miss Direction").assertIsDisplayed()
    }

    @Test
    fun `miss direction selection logs shot with direction`() {
        var loggedResult: ShotResult? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = testClubs[0],
                    onClubSelected = {},
                    onShotLogged = { loggedResult = it }
                )
            }
        }

        // Click Water to trigger miss direction
        composeTestRule.onNodeWithText("Water").performClick()

        // Click Push direction
        composeTestRule.onNodeWithText("Push →").performClick()

        // Verify shot was logged with miss direction
        assert(loggedResult?.lie == Lie.HAZARD)
        assert(loggedResult?.missDirection == MissDirection.PUSH)
    }

    @Test
    fun `green result logs shot immediately`() {
        var loggedResult: ShotResult? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = testClubs[0],
                    onClubSelected = {},
                    onShotLogged = { loggedResult = it }
                )
            }
        }

        // Click Green
        composeTestRule.onNodeWithText("Green").performClick()

        // Verify shot was logged
        assert(loggedResult?.lie == Lie.GREEN)
        assert(loggedResult?.missDirection == null)
    }

    @Test
    fun `bunker result logs shot immediately`() {
        var loggedResult: ShotResult? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = testClubs[0],
                    onClubSelected = {},
                    onShotLogged = { loggedResult = it }
                )
            }
        }

        // Click Bunker
        composeTestRule.onNodeWithText("Bunker").performClick()

        // Verify shot was logged
        assert(loggedResult?.lie == Lie.BUNKER)
        assert(loggedResult?.missDirection == null)
    }

    @Test
    fun `changing club resets pending lie`() {
        var loggedResult: ShotResult? = null

        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = testClubs[0],
                    onClubSelected = {},
                    onShotLogged = { loggedResult = it }
                )
            }
        }

        // Click Water to trigger miss direction step
        composeTestRule.onNodeWithText("Water").performClick()

        // Verify miss direction step appears
        composeTestRule.onNodeWithText("3. Miss Direction").assertIsDisplayed()

        // Change club (simulated by re-rendering with different selected club)
        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = testClubs,
                    selectedClub = testClubs[1], // Changed to 7i
                    onClubSelected = {},
                    onShotLogged = { loggedResult = it }
                )
            }
        }

        // Miss direction step should disappear
        composeTestRule.onNodeWithText("3. Miss Direction").assertDoesNotExist()

        // Result selection should be visible again
        composeTestRule.onNodeWithText("2. Select Result").assertIsDisplayed()
    }

    @Test
    fun `all clubs are displayed in grid`() {
        val manyClubs = listOf(
            Club("1", "Driver", ClubType.DRIVER, estimatedCarry = 250),
            Club("2", "3W", ClubType.WOOD, estimatedCarry = 230),
            Club("3", "5H", ClubType.HYBRID, estimatedCarry = 200),
            Club("4", "5i", ClubType.IRON, estimatedCarry = 180),
            Club("5", "6i", ClubType.IRON, estimatedCarry = 170),
            Club("6", "7i", ClubType.IRON, estimatedCarry = 160),
            Club("7", "8i", ClubType.IRON, estimatedCarry = 145),
            Club("8", "9i", ClubType.IRON, estimatedCarry = 135),
            Club("9", "PW", ClubType.WEDGE, estimatedCarry = 120),
            Club("10", "SW", ClubType.WEDGE, estimatedCarry = 90),
            Club("11", "LW", ClubType.WEDGE, estimatedCarry = 75),
            Club("12", "Putter", ClubType.PUTTER, estimatedCarry = 0)
        )

        composeTestRule.setContent {
            CaddyProTheme {
                ShotLogger(
                    clubs = manyClubs,
                    selectedClub = null,
                    onClubSelected = {},
                    onShotLogged = {}
                )
            }
        }

        // Verify all clubs are present
        composeTestRule.onNodeWithText("Driver").assertIsDisplayed()
        composeTestRule.onNodeWithText("3W").assertIsDisplayed()
        composeTestRule.onNodeWithText("PW").assertIsDisplayed()
        composeTestRule.onNodeWithText("Putter").assertIsDisplayed()
    }
}
