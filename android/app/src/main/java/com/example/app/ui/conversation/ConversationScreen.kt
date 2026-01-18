package caddypro.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caddypro.ui.conversation.voice.VoicePermissionHandler
import caddypro.ui.conversation.voice.VoicePermissionRequest

/**
 * Main conversation screen composable.
 *
 * Features:
 * - LazyColumn for message list with auto-scroll to bottom
 * - ConversationInputBar at bottom
 * - Loading indicator during processing
 * - Empty state when no messages
 * - Menu for clearing conversation
 * - Voice input with permission handling (Task 20)
 *
 * Spec reference: navcaddy-engine.md R1, R7
 * Plan reference: navcaddy-engine-plan.md Task 18, Task 20
 */
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionHandler = remember { VoicePermissionHandler(context) }

    var shouldRequestPermission by remember { mutableStateOf(false) }

    // Handle permission request
    VoicePermissionRequest(
        permissionHandler = permissionHandler,
        onPermissionResult = { isGranted ->
            if (isGranted) {
                // Permission granted, start voice input
                viewModel.onAction(ConversationAction.StartVoiceInput)
            }
        },
        shouldRequestPermission = shouldRequestPermission,
        onRequestHandled = {
            shouldRequestPermission = false
        }
    )

    ConversationContent(
        state = uiState,
        onAction = { action ->
            // Intercept voice input start to check permission
            if (action is ConversationAction.StartVoiceInput) {
                if (permissionHandler.hasRecordAudioPermission()) {
                    viewModel.onAction(action)
                } else {
                    shouldRequestPermission = true
                }
            } else {
                viewModel.onAction(action)
            }
        },
        modifier = modifier
    )
}

/**
 * Stateless content composable for conversation screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationContent(
    state: ConversationState,
    onAction: (ConversationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bones",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear conversation") },
                            onClick = {
                                onAction(ConversationAction.ClearConversation)
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            ConversationInputBar(
                value = state.currentInput,
                onValueChange = { onAction(ConversationAction.UpdateInput(it)) },
                onSend = {
                    onAction(ConversationAction.SendMessage(state.currentInput))
                },
                onMicClick = {
                    if (state.isVoiceInputActive) {
                        onAction(ConversationAction.StopVoiceInput)
                    } else {
                        onAction(ConversationAction.StartVoiceInput)
                    }
                },
                isVoiceActive = state.isVoiceInputActive,
                enabled = !state.isLoading
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.messages.isEmpty()) {
                // Empty state
                EmptyConversationState(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                // Message list
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.messages,
                        key = { it.id }
                    ) { message ->
                        MessageItem(
                            message = message,
                            onAction = onAction
                        )
                    }

                    // Loading indicator at bottom
                    if (state.isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual message item.
 */
@Composable
private fun MessageItem(
    message: ConversationMessage,
    onAction: (ConversationAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (message) {
        is ConversationMessage.User -> {
            UserMessageBubble(
                message = message,
                modifier = modifier
            )
        }

        is ConversationMessage.Assistant -> {
            AssistantMessageBubble(
                message = message,
                modifier = modifier
            )
        }

        is ConversationMessage.Clarification -> {
            ClarificationBubble(
                message = message,
                onSuggestionClick = { suggestion ->
                    onAction(
                        ConversationAction.SelectSuggestion(
                            intentType = suggestion.intentType,
                            label = suggestion.label
                        )
                    )
                },
                modifier = modifier
            )
        }

        is ConversationMessage.Error -> {
            ErrorMessageBubble(
                message = message,
                onRetry = if (message.isRecoverable) {
                    { onAction(ConversationAction.Retry) }
                } else {
                    null
                },
                modifier = modifier
            )
        }
    }
}

/**
 * Empty state when no messages are present.
 */
@Composable
private fun EmptyConversationState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Start a conversation with Bones",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Ask about club selection, check your recovery, enter scores, or get coaching tips.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
