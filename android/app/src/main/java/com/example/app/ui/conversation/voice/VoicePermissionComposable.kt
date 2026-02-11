package caddypro.ui.conversation.voice

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Composable for handling voice input permission request flow.
 *
 * This composable manages the permission request lifecycle and shows
 * appropriate dialogs when needed.
 *
 * @param permissionHandler Permission handler for rationale messages
 * @param onPermissionResult Callback when permission is granted or denied
 * @param shouldRequestPermission Whether to trigger the permission request
 * @param onRequestHandled Callback when request is handled
 */
@Composable
fun VoicePermissionRequest(
    permissionHandler: VoicePermissionHandler,
    onPermissionResult: (Boolean) -> Unit,
    shouldRequestPermission: Boolean,
    onRequestHandled: () -> Unit
) {
    var showRationale by remember { mutableStateOf(false) }
    var showDeniedDialog by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionResult(true)
        } else {
            // Permission denied
            showDeniedDialog = true
            onPermissionResult(false)
        }
    }

    // Trigger permission request when needed
    LaunchedEffect(shouldRequestPermission) {
        if (shouldRequestPermission) {
            if (permissionHandler.hasRecordAudioPermission()) {
                // Permission already granted
                onPermissionResult(true)
                onRequestHandled()
            } else {
                // Show rationale first
                showRationale = true
            }
        }
    }

    // Rationale dialog
    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
                onRequestHandled()
            },
            title = { Text("Microphone Access") },
            text = { Text(permissionHandler.getPermissionRationale()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        onRequestHandled()
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        onPermissionResult(false)
                        onRequestHandled()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Permission denied dialog
    if (showDeniedDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeniedDialog = false
            },
            title = { Text("Permission Denied") },
            text = { Text(permissionHandler.getTemporarilyDeniedMessage()) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeniedDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
