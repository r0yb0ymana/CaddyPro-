package caddypro.ui.caddy.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caddypro.domain.caddy.usecases.ShotResult
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.ui.theme.CaddyProTheme
import com.example.app.domain.navcaddy.models.Club
import com.example.app.domain.navcaddy.models.ClubType

/**
 * Shot Logger component for quick shot logging during a round.
 *
 * Provides a fast, low-friction flow optimized for one-second logging:
 * 1. Select club (grid of club buttons)
 * 2. Select result (lie: fairway, rough, bunker, water, OB, green)
 * 3. Select miss direction if hazard/rough (left, right, short, long)
 *
 * Design features:
 * - Large touch targets (48dp+ for clubs, 64dp for results)
 * - Material 3 styling with high contrast colors
 * - Progressive disclosure (result only shown after club selection)
 * - Haptic feedback on shot save
 * - Color-coded results (green=good, red=bad)
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 17
 * Acceptance criteria: A4 (Shot logger speed and persistence)
 *
 * @param clubs List of clubs available for selection
 * @param selectedClub Currently selected club (null if no selection)
 * @param onClubSelected Callback when a club is selected
 * @param onShotLogged Callback when shot is logged with result
 * @param modifier Modifier for the container
 */
@Composable
fun ShotLogger(
    clubs: List<Club>,
    selectedClub: Club?,
    onClubSelected: (Club) -> Unit,
    onShotLogged: (ShotResult) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for tracking lie selection (before miss direction if needed)
    var pendingLie by remember { mutableStateOf<Lie?>(null) }

    val view = LocalView.current
    val performHapticFeedback = remember {
        {
            view.performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Log Shot",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        // Step 1: Select Club
        Text(
            text = "1. Select Club",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ClubSelector(
            clubs = clubs,
            selectedClub = selectedClub,
            onClubSelected = {
                onClubSelected(it)
                // Reset pending lie when club changes
                pendingLie = null
            }
        )

        // Step 2: Select Result (only visible if club selected)
        AnimatedVisibility(visible = selectedClub != null && pendingLie == null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "2. Select Result",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ShotResultSelector(
                    onResultSelected = { lie ->
                        // Check if miss direction is needed
                        if (requiresMissDirection(lie)) {
                            pendingLie = lie
                        } else {
                            // Log shot immediately without miss direction
                            performHapticFeedback()
                            onShotLogged(ShotResult(lie = lie, missDirection = null))
                            pendingLie = null
                        }
                    }
                )
            }
        }

        // Step 3: Select Miss Direction (conditional on hazard/rough)
        AnimatedVisibility(visible = pendingLie != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. Miss Direction",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                MissDirectionSelector(
                    onDirectionSelected = { missDirection ->
                        // Log shot with lie and miss direction
                        performHapticFeedback()
                        onShotLogged(
                            ShotResult(
                                lie = pendingLie!!,
                                missDirection = missDirection
                            )
                        )
                        pendingLie = null
                    }
                )
            }
        }
    }
}

/**
 * Determines if a lie requires miss direction selection.
 *
 * Hazard shots (water/OB) require miss direction for analytics quality.
 * Rough shots optionally benefit from direction tracking.
 *
 * @param lie The shot result lie
 * @return True if miss direction should be captured
 */
private fun requiresMissDirection(lie: Lie): Boolean {
    return when (lie) {
        Lie.HAZARD -> true  // Required per spec and ShotResult validation
        Lie.ROUGH -> true   // Optional but helpful for pattern analysis
        else -> false
    }
}

// Preview functions for development and testing

@Preview(name = "Shot Logger - Initial State", showBackground = true)
@Composable
private fun PreviewShotLoggerInitial() {
    CaddyProTheme {
        ShotLogger(
            clubs = previewClubs(),
            selectedClub = null,
            onClubSelected = {},
            onShotLogged = {}
        )
    }
}

@Preview(name = "Shot Logger - Club Selected", showBackground = true)
@Composable
private fun PreviewShotLoggerClubSelected() {
    CaddyProTheme {
        ShotLogger(
            clubs = previewClubs(),
            selectedClub = previewClubs()[0],
            onClubSelected = {},
            onShotLogged = {}
        )
    }
}

@Preview(name = "Shot Logger - Full Screen", showBackground = true, heightDp = 800)
@Composable
private fun PreviewShotLoggerFullScreen() {
    CaddyProTheme {
        ShotLogger(
            clubs = previewClubs(),
            selectedClub = previewClubs()[3],
            onClubSelected = {},
            onShotLogged = {}
        )
    }
}

/**
 * Preview helper - generates sample club list.
 */
private fun previewClubs(): List<Club> {
    return listOf(
        Club(
            id = "1",
            name = "Driver",
            type = ClubType.DRIVER,
            estimatedCarry = 250
        ),
        Club(
            id = "2",
            name = "3W",
            type = ClubType.WOOD,
            estimatedCarry = 230
        ),
        Club(
            id = "3",
            name = "5H",
            type = ClubType.HYBRID,
            estimatedCarry = 200
        ),
        Club(
            id = "4",
            name = "7i",
            type = ClubType.IRON,
            estimatedCarry = 160
        ),
        Club(
            id = "5",
            name = "9i",
            type = ClubType.IRON,
            estimatedCarry = 135
        ),
        Club(
            id = "6",
            name = "PW",
            type = ClubType.WEDGE,
            estimatedCarry = 120
        ),
        Club(
            id = "7",
            name = "SW",
            type = ClubType.WEDGE,
            estimatedCarry = 90
        ),
        Club(
            id = "8",
            name = "Putter",
            type = ClubType.PUTTER,
            estimatedCarry = 0
        )
    )
}
