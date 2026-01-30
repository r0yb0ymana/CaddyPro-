package caddypro.ui.caddy.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Banner displaying network connectivity status and pending shots count.
 *
 * Shows when:
 * - Device is offline (red error container)
 * - Device is online but has pending shots (tertiary container)
 *
 * Auto-hides when online with no pending shots.
 *
 * Spec reference: live-caddy-mode.md C3 (Offline-first), R6 (offline queueing)
 * Plan reference: live-caddy-mode-plan.md Task 20
 * Acceptance criteria: A4 (shot logger with poor reception)
 *
 * @param isOnline True if device has network connectivity
 * @param pendingShotsCount Number of shots queued for sync (0 = none pending)
 * @param modifier Optional modifier for the banner
 */
@Composable
fun ConnectivityBanner(
    isOnline: Boolean,
    pendingShotsCount: Int,
    modifier: Modifier = Modifier
) {
    // Only show banner when offline or has pending shots
    val shouldShow = !isOnline || pendingShotsCount > 0

    AnimatedVisibility(
        visible = shouldShow,
        modifier = modifier,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline) {
                    // Online with pending shots - tertiary (informational)
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    // Offline - error (warning)
                    MaterialTheme.colorScheme.errorContainer
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isOnline) {
                        "Syncing $pendingShotsCount shots"
                    } else {
                        "Offline: $pendingShotsCount shots queued"
                    }
                }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOnline) {
                        Icons.Default.CloudUpload
                    } else {
                        Icons.Default.CloudOff
                    },
                    contentDescription = if (isOnline) "Syncing" else "Offline",
                    tint = if (isOnline) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (isOnline) {
                        "Syncing $pendingShotsCount ${if (pendingShotsCount == 1) "shot" else "shots"}..."
                    } else {
                        "Offline: $pendingShotsCount ${if (pendingShotsCount == 1) "shot" else "shots"} queued"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOnline) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview(name = "Offline with pending shots")
@Composable
private fun ConnectivityBannerOfflinePreview() {
    MaterialTheme {
        ConnectivityBanner(
            isOnline = false,
            pendingShotsCount = 3
        )
    }
}

@Preview(name = "Online syncing shots")
@Composable
private fun ConnectivityBannerSyncingPreview() {
    MaterialTheme {
        ConnectivityBanner(
            isOnline = true,
            pendingShotsCount = 2
        )
    }
}

@Preview(name = "Online no pending shots (hidden)")
@Composable
private fun ConnectivityBannerHiddenPreview() {
    MaterialTheme {
        ConnectivityBanner(
            isOnline = true,
            pendingShotsCount = 0
        )
    }
}

@Preview(name = "Offline single shot")
@Composable
private fun ConnectivityBannerOfflineSinglePreview() {
    MaterialTheme {
        ConnectivityBanner(
            isOnline = false,
            pendingShotsCount = 1
        )
    }
}
