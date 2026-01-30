import Foundation

/// Represents a single turn in the conversation history.
struct ConversationTurn: Codable, Hashable, Identifiable {
    enum Role: String, Codable, Hashable {
        case user
        case assistant
    }

    let id: String
    let role: Role
    let content: String
    let timestamp: Date

    init(
        id: String = UUID().uuidString,
        role: Role,
        content: String,
        timestamp: Date = Date()
    ) {
        self.id = id
        self.role = role
        self.content = content
        self.timestamp = timestamp
    }
}
