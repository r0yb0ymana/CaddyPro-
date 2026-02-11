package caddypro.ui.conversation

import caddypro.domain.navcaddy.error.RecoveryAction
import caddypro.domain.navcaddy.models.IntentType

/**
 * UI state for the conversation screen.
 *
 * Represents the current state of the conversational interface including
 * messages, loading states, errors, and network connectivity.
 *
 * Spec reference: navcaddy-engine.md R1, R7, C6
 * Plan reference: navcaddy-engine-plan.md Task 18, Task 23, Task 24
 */
data class ConversationState(
    val messages: List<ConversationMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentInput: String = "",
    val isVoiceInputActive: Boolean = false,
    val recoveryActions: List<RecoveryAction> = emptyList(),
    val fallbackSuggestions: List<FallbackSuggestion> = emptyList(),
    val showFallbackSuggestions: Boolean = false,
    val isOffline: Boolean = false
)

/**
 * Represents a single message in the conversation.
 */
sealed class ConversationMessage {
    abstract val id: String
    abstract val timestamp: Long

    /**
     * User message (right-aligned, primary color).
     */
    data class User(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val text: String
    ) : ConversationMessage()

    /**
     * Assistant message (left-aligned, with Bones branding).
     */
    data class Assistant(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val text: String,
        val hasDisclaimer: Boolean = false,
        val patternReferencesCount: Int = 0
    ) : ConversationMessage()

    /**
     * Clarification message with suggestion chips.
     */
    data class Clarification(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val message: String,
        val suggestions: List<ClarificationSuggestion>
    ) : ConversationMessage() {
        init {
            require(suggestions.size <= 3) { "Maximum 3 suggestions allowed" }
            require(suggestions.isNotEmpty()) { "At least 1 suggestion required" }
        }
    }

    /**
     * Error message with recovery options (for conversation errors).
     */
    data class Error(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        val message: String,
        val isRecoverable: Boolean = true,
        val recoveryActions: List<RecoveryAction> = emptyList()
    ) : ConversationMessage()

    companion object {
        private var idCounter = 0L
        fun generateId(): String = "msg_${System.currentTimeMillis()}_${idCounter++}"
    }
}

/**
 * Represents a clarification suggestion chip.
 */
data class ClarificationSuggestion(
    val intentType: IntentType,
    val label: String,
    val description: String
)

/**
 * Represents a fallback suggestion when LLM/network unavailable.
 */
data class FallbackSuggestion(
    val intentType: IntentType,
    val label: String,
    val description: String
)
