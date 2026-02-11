package caddypro.domain.navcaddy.clarification

import caddypro.domain.navcaddy.models.IntentType

/**
 * Response containing clarification questions and suggested intents.
 *
 * Generated when user input is ambiguous and intent confidence is below
 * the clarification threshold (< 0.50).
 *
 * Spec reference: navcaddy-engine.md A3, navcaddy-engine-plan.md Task 8
 *
 * @property message Clarification question(s) to present to user (1-2 targeted questions)
 * @property suggestions List of suggested intents (max 3) as selectable chips
 * @property originalInput The original ambiguous user input
 */
data class ClarificationResponse(
    val message: String,
    val suggestions: List<IntentSuggestion>,
    val originalInput: String
) {
    init {
        require(suggestions.size <= 3) { "Maximum 3 suggestions allowed" }
        require(suggestions.isNotEmpty()) { "At least 1 suggestion required" }
        require(message.isNotBlank()) { "Message cannot be blank" }
    }
}

/**
 * Suggested intent option for clarification chips.
 *
 * Each suggestion represents an intent that the user might have meant,
 * with user-friendly labels and descriptions.
 *
 * @property intentType The intent type this suggestion represents
 * @property label Short, user-friendly chip label (e.g., "Check Recovery", "Adjust Club")
 * @property description Brief description of what this intent does
 */
data class IntentSuggestion(
    val intentType: IntentType,
    val label: String,
    val description: String
) {
    init {
        require(label.isNotBlank()) { "Label cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
    }
}
