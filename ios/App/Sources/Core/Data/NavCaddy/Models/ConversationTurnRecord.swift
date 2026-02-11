import SwiftData
import Foundation

/// SwiftData model for persisting conversation turns.
///
/// Each turn represents a user or assistant message in the conversation history.
/// Spec R6: Conversation history limited to last 10 turns.
@Model
final class ConversationTurnRecord {
    var id: UUID = UUID()
    var role: String // ConversationTurn.Role as raw value
    var content: String
    var timestamp: Date

    var session: SessionRecord?

    init(
        id: UUID = UUID(),
        role: String,
        content: String,
        timestamp: Date = Date()
    ) {
        self.id = id
        self.role = role
        self.content = content
        self.timestamp = timestamp
    }
}

// MARK: - Domain Conversion

extension ConversationTurnRecord {
    /// Creates a SwiftData record from a domain ConversationTurn
    convenience init(from turn: ConversationTurn) {
        self.init(
            id: UUID(uuidString: turn.id) ?? UUID(),
            role: turn.role.rawValue,
            content: turn.content,
            timestamp: turn.timestamp
        )
    }

    /// Converts this record to a domain ConversationTurn
    func toDomain() -> ConversationTurn? {
        guard let role = ConversationTurn.Role(rawValue: role) else {
            return nil
        }

        return ConversationTurn(
            id: id.uuidString,
            role: role,
            content: content,
            timestamp: timestamp
        )
    }
}
