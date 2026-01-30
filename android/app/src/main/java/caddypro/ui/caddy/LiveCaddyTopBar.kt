package caddypro.ui.caddy

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import caddypro.domain.navcaddy.context.RoundState
import caddypro.ui.theme.CaddyProTheme

/**
 * Top bar for Live Caddy Mode screen.
 *
 * Displays:
 * - Back navigation button
 * - Round context (hole number and par)
 * - End round button with confirmation dialog
 *
 * Adapts to round state:
 * - Shows "Live Caddy" title when no round active
 * - Shows hole info when round is active
 * - End round button only visible when round is active
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 18
 *
 * @param roundState Current round state (null if no active round)
 * @param onNavigateBack Callback to navigate back
 * @param onEndRound Callback to end the current round
 * @param modifier Optional modifier for the top bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCaddyTopBar(
    roundState: RoundState?,
    onNavigateBack: () -> Unit,
    onEndRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEndRoundDialog by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        title = {
            if (roundState != null) {
                Text(
                    text = "Hole ${roundState.currentHole} • Par ${roundState.currentPar}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            } else {
                Text(
                    text = "Live Caddy",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
        },
        actions = {
            // End round button only shown when round is active
            if (roundState != null) {
                IconButton(onClick = { showEndRoundDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "End round"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )

    // End round confirmation dialog
    if (showEndRoundDialog) {
        EndRoundConfirmationDialog(
            onConfirm = {
                showEndRoundDialog = false
                onEndRound()
            },
            onDismiss = {
                showEndRoundDialog = false
            }
        )
    }
}

/**
 * Confirmation dialog for ending the round.
 *
 * Prevents accidental round termination by requiring explicit confirmation.
 *
 * Spec reference: R1 (Live Round Context)
 *
 * @param onConfirm Callback when user confirms ending the round
 * @param onDismiss Callback when user cancels
 */
@Composable
private fun EndRoundConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("End Round?")
        },
        text = {
            Text(
                "Are you sure you want to end this round? Your shots will be saved and the round will be finalized."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    "End Round",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================================
// Preview Composables
// ============================================================================

@Preview(name = "Top Bar - No Round", showBackground = true)
@Composable
private fun LiveCaddyTopBarNoRoundPreview() {
    CaddyProTheme {
        LiveCaddyTopBar(
            roundState = null,
            onNavigateBack = {},
            onEndRound = {}
        )
    }
}

@Preview(name = "Top Bar - Active Round", showBackground = true)
@Composable
private fun LiveCaddyTopBarActiveRoundPreview() {
    CaddyProTheme {
        LiveCaddyTopBar(
            roundState = RoundState(
                roundId = 1L,
                courseId = 1L,
                currentHole = 7,
                currentPar = 3,
                shotsOnCurrentHole = 2,
                totalScore = 8
            ),
            onNavigateBack = {},
            onEndRound = {}
        )
    }
}

@Preview(name = "End Round Dialog", showBackground = true)
@Composable
private fun EndRoundConfirmationDialogPreview() {
    CaddyProTheme {
        EndRoundConfirmationDialog(
            onConfirm = {},
            onDismiss = {}
        )
    }
}
