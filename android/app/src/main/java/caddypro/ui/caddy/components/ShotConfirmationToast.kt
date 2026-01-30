package caddypro.ui.caddy.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caddypro.ui.theme.CaddyProTheme
import kotlinx.coroutines.delay

/**
 * Toast component for shot logging confirmation.
 *
 * Displays a brief confirmation message when a shot is successfully logged.
 * The toast:
 * - Appears with slide-in animation from bottom
 * - Shows shot details (club + result)
 * - Auto-dismisses after 2 seconds
 * - Can be manually dismissed via button
 * - Uses Material 3 Snackbar styling
 *
 * This provides visual confirmation alongside haptic feedback for shot logging,
 * ensuring the user knows the shot was saved even in outdoor/bright conditions.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 22
 * Acceptance criteria: A4 (shot saved with haptic confirmation)
 *
 * @param visible Whether the toast should be visible
 * @param shotDetails Formatted string describing the logged shot (e.g., "7i → Fairway")
 * @param onDismiss Callback when toast is dismissed (auto or manual)
 * @param modifier Modifier for the toast container
 */
@Composable
fun ShotConfirmationToast(
    visible: Boolean,
    shotDetails: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-dismiss after 2 seconds
    LaunchedEffect(visible) {
        if (visible) {
            delay(2000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            },
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50) // Green success color
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Shot logged: $shotDetails")
            }
        }
    }
}

// Preview functions for development

@Preview(name = "Shot Confirmation Toast - Visible", showBackground = true)
@Composable
private fun PreviewShotConfirmationToastVisible() {
    CaddyProTheme {
        ShotConfirmationToast(
            visible = true,
            shotDetails = "7i → Fairway",
            onDismiss = {}
        )
    }
}

@Preview(name = "Shot Confirmation Toast - Complex", showBackground = true)
@Composable
private fun PreviewShotConfirmationToastComplex() {
    CaddyProTheme {
        ShotConfirmationToast(
            visible = true,
            shotDetails = "Driver → Rough (Right)",
            onDismiss = {}
        )
    }
}

@Preview(name = "Shot Confirmation Toast - Hidden", showBackground = true)
@Composable
private fun PreviewShotConfirmationToastHidden() {
    CaddyProTheme {
        ShotConfirmationToast(
            visible = false,
            shotDetails = "7i → Fairway",
            onDismiss = {}
        )
    }
}
