import Foundation

/**
 * UI models for the conversation screen.
 *
 * Represents the current state of the conversational interface including
 * messages, loading states, and errors.
 *
 * Spec reference: navcaddy-engine.md R1, R7
 * Plan reference: navcaddy-engine-plan.md Task 19
 */

// MARK: - Conversation State

/// State for the conversation screen.
struct ConversationState {
    var messages: [ConversationMessage] = []
    var isLoading: Bool = false
    var error: String? = nil
    var currentInput: String = ""
    var isVoiceInputActive: Bool = false
}

// MARK: - Conversation Message

/// Represents a single message in the conversation.
enum ConversationMessage: Identifiable, Equatable {
    case user(UserMessage)
    case assistant(AssistantMessage)
    case clarification(ClarificationMessage)
    case error(ErrorMessage)

    var id: String {
        switch self {
        case .user(let msg): return msg.id
        case .assistant(let msg): return msg.id
        case .clarification(let msg): return msg.id
        case .error(let msg): return msg.id
        }
    }

    var timestamp: Date {
        switch self {
        case .user(let msg): return msg.timestamp
        case .assistant(let msg): return msg.timestamp
        case .clarification(let msg): return msg.timestamp
        case .error(let msg): return msg.timestamp
        }
    }
}

// MARK: - Message Types

/// User message (right-aligned, primary color).
struct UserMessage: Identifiable, Equatable {
    let id: String
    let timestamp: Date
    let text: String

    init(id: String = UUID().uuidString, timestamp: Date = Date(), text: String) {
        self.id = id
        self.timestamp = timestamp
        self.text = text
    }
}

/// Assistant message (left-aligned, with Bones branding).
struct AssistantMessage: Identifiable, Equatable {
    let id: String
    let timestamp: Date
    let text: String
    let hasDisclaimer: Bool
    let patternReferencesCount: Int

    init(
        id: String = UUID().uuidString,
        timestamp: Date = Date(),
        text: String,
        hasDisclaimer: Bool = false,
        patternReferencesCount: Int = 0
    ) {
        self.id = id
        self.timestamp = timestamp
        self.text = text
        self.hasDisclaimer = hasDisclaimer
        self.patternReferencesCount = patternReferencesCount
    }
}

/// Clarification message with suggestion chips.
struct ClarificationMessage: Identifiable, Equatable {
    let id: String
    let timestamp: Date
    let message: String
    let suggestions: [ClarificationSuggestion]

    init(
        id: String = UUID().uuidString,
        timestamp: Date = Date(),
        message: String,
        suggestions: [ClarificationSuggestion]
    ) {
        precondition(!suggestions.isEmpty, "At least 1 suggestion required")
        precondition(suggestions.count <= 3, "Maximum 3 suggestions allowed")

        self.id = id
        self.timestamp = timestamp
        self.message = message
        self.suggestions = suggestions
    }
}

/// Error message (for conversation errors).
struct ErrorMessage: Identifiable, Equatable {
    let id: String
    let timestamp: Date
    let message: String
    let isRecoverable: Bool

    init(
        id: String = UUID().uuidString,
        timestamp: Date = Date(),
        message: String,
        isRecoverable: Bool = true
    ) {
        self.id = id
        self.timestamp = timestamp
        self.message = message
        self.isRecoverable = isRecoverable
    }
}

/// Represents a clarification suggestion chip.
struct ClarificationSuggestion: Identifiable, Equatable {
    let intentType: IntentType
    let label: String
    let description: String

    var id: String { intentType.rawValue }
}

// MARK: - Conversation Actions

/// User actions in the conversation screen.
enum ConversationAction {
    case sendMessage(String)
    case updateInput(String)
    case startVoiceInput
    case stopVoiceInput
    case voiceInputComplete(transcription: String)
    case voiceInputError(error: String)
    case selectSuggestion(intentType: IntentType, label: String)
    case dismissError
    case confirmIntent(intentId: String)
    case rejectIntent(intentId: String)
    case retry
    case clearConversation
}
