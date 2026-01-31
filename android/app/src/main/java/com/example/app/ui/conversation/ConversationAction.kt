package caddypro.ui.conversation

import com.example.app.domain.navcaddy.error.RecoveryAction
import caddypro.domain.navcaddy.models.IntentType

/**
 * User actions in the conversation screen.
 *
 * Sealed class representing all possible user interactions with the
 * conversational interface.
 *
 * Spec reference: navcaddy-engine.md R1, R7
 * Plan reference: navcaddy-engine-plan.md Task 18, Task 23
 */
sealed interface ConversationAction {
    /**
     * User submitted text input.
     */
    data class SendMessage(val text: String) : ConversationAction

    /**
     * User updated the input text (typing).
     */
    data class UpdateInput(val text: String) : ConversationAction

    /**
     * User started voice input.
     */
    data object StartVoiceInput : ConversationAction

    /**
     * User stopped voice input.
     */
    data object StopVoiceInput : ConversationAction

    /**
     * Voice input completed with transcription.
     */
    data class VoiceInputComplete(val transcription: String) : ConversationAction

    /**
     * Voice input failed with error.
     */
    data class VoiceInputError(val error: String) : ConversationAction

    /**
     * User tapped a clarification suggestion chip.
     */
    data class SelectSuggestion(val intentType: IntentType, val label: String) : ConversationAction

    /**
     * User dismissed an error message.
     */
    data object DismissError : ConversationAction

    /**
     * User confirmed a mid-confidence intent.
     */
    data class ConfirmIntent(val intentId: String) : ConversationAction

    /**
     * User rejected a mid-confidence intent.
     */
    data class RejectIntent(val intentId: String) : ConversationAction

    /**
     * Retry after error.
     */
    data object Retry : ConversationAction

    /**
     * Clear conversation history.
     */
    data object ClearConversation : ConversationAction

    /**
     * User selected a recovery action (Task 23).
     */
    data class SelectRecoveryAction(val action: RecoveryAction) : ConversationAction

    /**
     * User selected a fallback suggestion (Task 23).
     */
    data class SelectFallbackSuggestion(val intentType: IntentType, val label: String) : ConversationAction

    /**
     * Show fallback suggestions manually.
     */
    data object ShowFallbackSuggestions : ConversationAction

    /**
     * Hide fallback suggestions.
     */
    data object HideFallbackSuggestions : ConversationAction
}
