package caddypro.ui.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

/**
 * Input bar for the conversation screen.
 *
 * Features:
 * - TextField for text input
 * - Mic button (toggles to Send when text present)
 * - Animated state transitions
 * - Keyboard handling
 *
 * Spec reference: navcaddy-engine.md R1, R7
 * Plan reference: navcaddy-engine-plan.md Task 18
 */
@Composable
fun ConversationInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isVoiceActive: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Text input field
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = {
                    Text(
                        text = if (isVoiceActive) "Listening..." else "Ask Bones anything...",
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                enabled = enabled && !isVoiceActive,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (value.isNotBlank()) {
                            onSend()
                        }
                    }
                ),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = MaterialTheme.shapes.large
            )

            // Mic/Send button with animated transition
            AnimatedContent(
                targetState = value.isNotBlank(),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "Input button animation"
            ) { hasText ->
                if (hasText) {
                    // Send button
                    IconButton(
                        onClick = onSend,
                        enabled = enabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message"
                        )
                    }
                } else {
                    // Mic button
                    IconButton(
                        onClick = onMicClick,
                        enabled = enabled,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isVoiceActive) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (isVoiceActive) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = if (isVoiceActive) {
                                "Stop voice input"
                            } else {
                                "Start voice input"
                            }
                        )
                    }
                }
            }
        }
    }
}
