import SwiftData
import Foundation

/// SwiftData model for persisting session context.
///
/// Maintains short-term context for conversation continuity.
/// Spec R6: Tracks current round, hole, and conversation history.
@Model
final class SessionRecord {
    @Attribute(.unique) var id: String = "current" // Singleton session record

    var roundId: String?
    var currentHole: Int?
    var lastRecommendation: String?
    var updatedAt: Date

    @Relationship(deleteRule: .cascade)
    var conversationTurns: [ConversationTurnRecord] = []

    init(
        id: String = "current",
        roundId: String? = nil,
        currentHole: Int? = nil,
        lastRecommendation: String? = nil,
        updatedAt: Date = Date(),
        conversationTurns: [ConversationTurnRecord] = []
    ) {
        self.id = id
        self.roundId = roundId
        self.currentHole = currentHole
        self.lastRecommendation = lastRecommendation
        self.updatedAt = updatedAt
        self.conversationTurns = conversationTurns
    }
}

// MARK: - Domain Conversion

extension SessionRecord {
    /// Creates a SwiftData record from a domain SessionContext
    convenience init(from context: SessionContext) {
        let turns = context.conversationHistory.map { ConversationTurnRecord(from: $0) }
        self.init(
            id: "current",
            roundId: context.currentRound?.id,
            currentHole: context.currentHole,
            lastRecommendation: context.lastRecommendation,
            updatedAt: Date(),
            conversationTurns: turns
        )
    }

    /// Converts this record to a domain SessionContext
    func toDomain() -> SessionContext {
        // Note: Round reconstruction simplified - may need repository lookup
        let round: Round? = roundId.map { id in
            Round(id: id, startTime: Date(), courseName: nil, scores: [:])
        }

        let turns = conversationTurns.compactMap { $0.toDomain() }

        return SessionContext(
            currentRound: round,
            currentHole: currentHole,
            lastShot: nil, // Not persisted in this model
            lastRecommendation: lastRecommendation,
            conversationHistory: turns
        )
    }
}
