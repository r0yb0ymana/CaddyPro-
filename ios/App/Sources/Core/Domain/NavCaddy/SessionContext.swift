import Foundation

/// Represents the short-term session context for conversation continuity.
///
/// Spec R6: Maintains current round, hole, last shot, and conversation history.
struct SessionContext: Codable, Hashable {
    let sessionId: String
    let currentRound: Round?
    let currentHole: Int?
    let lastShot: Shot?
    let lastRecommendation: String?
    let conversationHistory: [ConversationTurn]

    init(
        sessionId: String = UUID().uuidString,
        currentRound: Round? = nil,
        currentHole: Int? = nil,
        lastShot: Shot? = nil,
        lastRecommendation: String? = nil,
        conversationHistory: [ConversationTurn] = []
    ) {
        self.sessionId = sessionId
        self.currentRound = currentRound
        self.currentHole = currentHole
        self.lastShot = lastShot
        self.lastRecommendation = lastRecommendation
        self.conversationHistory = conversationHistory
    }

    /// Returns a new context with the conversation turn added
    func addingTurn(_ turn: ConversationTurn) -> SessionContext {
        var history = conversationHistory
        history.append(turn)

        // Keep only last 10 turns as per spec
        if history.count > 10 {
            history = Array(history.suffix(10))
        }

        return SessionContext(
            sessionId: sessionId,
            currentRound: currentRound,
            currentHole: currentHole,
            lastShot: lastShot,
            lastRecommendation: lastRecommendation,
            conversationHistory: history
        )
    }

    /// Returns a new context with all fields cleared
    static var empty: SessionContext {
        SessionContext()
    }

    /// Whether a round is currently active.
    var isRoundActive: Bool {
        currentRound != nil
    }
}
