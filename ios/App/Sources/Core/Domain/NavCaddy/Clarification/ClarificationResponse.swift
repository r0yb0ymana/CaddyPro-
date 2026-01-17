import Foundation

/// Response containing clarification questions and suggested intents.
///
/// Generated when user input is ambiguous and intent confidence is below
/// the clarification threshold (< 0.50).
///
/// Spec reference: navcaddy-engine.md A3, navcaddy-engine-plan.md Task 8
struct ClarificationResponse: Equatable {
    /// Clarification question(s) to present to user (1-2 targeted questions)
    let message: String

    /// List of suggested intents (max 3) as selectable chips
    let suggestions: [IntentSuggestion]

    /// The original ambiguous user input
    let originalInput: String

    init(message: String, suggestions: [IntentSuggestion], originalInput: String) {
        precondition(!suggestions.isEmpty, "At least 1 suggestion required")
        precondition(suggestions.count <= 3, "Maximum 3 suggestions allowed")
        precondition(!message.isEmpty, "Message cannot be empty")

        self.message = message
        self.suggestions = suggestions
        self.originalInput = originalInput
    }
}

/// Suggested intent option for clarification chips.
///
/// Each suggestion represents an intent that the user might have meant,
/// with user-friendly labels and descriptions.
struct IntentSuggestion: Equatable, Identifiable {
    var id: IntentType { intentType }

    /// The intent type this suggestion represents
    let intentType: IntentType

    /// Short, user-friendly chip label (e.g., "Check Recovery", "Adjust Club")
    let label: String

    /// Brief description of what this intent does
    let description: String

    init(intentType: IntentType, label: String, description: String) {
        precondition(!label.isEmpty, "Label cannot be empty")
        precondition(!description.isEmpty, "Description cannot be empty")

        self.intentType = intentType
        self.label = label
        self.description = description
    }
}
