import Foundation
import Observation

/// Manages the current session context for conversation continuity.
///
/// Tracks current round state, hole, last shot, last recommendation,
/// and conversation history. Provides @Observable state for SwiftUI integration.
///
/// Spec R6: Session Context
/// - Tracks current round state (hole, score, conditions)
/// - Maintains conversation history for follow-up queries
/// - Provides context to LLM for better understanding
/// - Supports "what about my 7-iron?" follow-up queries
/// - Session persists during app lifecycle, clears on app restart
@Observable
@MainActor
final class SessionContextManager {
    /// Current session context
    private(set) var context: SessionContext

    /// Conversation history manager
    private let conversationHistory: ConversationHistory

    /// Context injector for LLM
    private let contextInjector: ContextInjector

    init(
        conversationHistory: ConversationHistory = ConversationHistory(),
        contextInjector: ContextInjector = ContextInjector()
    ) {
        self.context = SessionContext.empty
        self.conversationHistory = conversationHistory
        self.contextInjector = contextInjector
    }

    // MARK: - Round Management

    /// Updates the current round information.
    ///
    /// - Parameters:
    ///   - roundId: Unique identifier for the round
    ///   - courseName: Name of the golf course
    func updateRound(roundId: String, courseName: String) {
        let round = Round(
            id: roundId,
            startTime: Date(),
            courseName: courseName,
            scores: [:]
        )

        context = SessionContext(
            sessionId: context.sessionId,
            currentRound: round,
            currentHole: context.currentHole,
            lastShot: context.lastShot,
            lastRecommendation: context.lastRecommendation,
            conversationHistory: context.conversationHistory
        )
    }

    /// Updates the current hole being played.
    ///
    /// - Parameters:
    ///   - holeNumber: The hole number (1-18)
    ///   - par: Par for the hole
    func updateHole(holeNumber: Int, par: Int) {
        guard (1...18).contains(holeNumber) else {
            return
        }

        context = SessionContext(
            sessionId: context.sessionId,
            currentRound: context.currentRound,
            currentHole: holeNumber,
            lastShot: context.lastShot,
            lastRecommendation: context.lastRecommendation,
            conversationHistory: context.conversationHistory
        )
    }

    // MARK: - Shot Tracking

    /// Records a shot in the session context.
    ///
    /// Updates the last shot reference for conversation continuity.
    ///
    /// - Parameter shot: The shot to record
    func recordShot(_ shot: Shot) {
        context = SessionContext(
            sessionId: context.sessionId,
            currentRound: context.currentRound,
            currentHole: context.currentHole,
            lastShot: shot,
            lastRecommendation: context.lastRecommendation,
            conversationHistory: context.conversationHistory
        )
    }

    /// Records a recommendation given to the user.
    ///
    /// Allows follow-up queries like "What if the wind picks up?"
    ///
    /// - Parameter recommendation: The recommendation text
    func recordRecommendation(_ recommendation: String) {
        context = SessionContext(
            sessionId: context.sessionId,
            currentRound: context.currentRound,
            currentHole: context.currentHole,
            lastShot: context.lastShot,
            lastRecommendation: recommendation,
            conversationHistory: context.conversationHistory
        )
    }

    // MARK: - Conversation Management

    /// Adds a conversation turn to the history.
    ///
    /// Maintains last 10 turns for context.
    ///
    /// - Parameters:
    ///   - userInput: The user's input
    ///   - assistantResponse: The assistant's response
    func addConversationTurn(userInput: String, assistantResponse: String) {
        // Add user turn
        let userTurn = ConversationTurn(
            role: .user,
            content: userInput,
            timestamp: Date()
        )
        conversationHistory.addTurn(userTurn)

        // Add assistant turn
        let assistantTurn = ConversationTurn(
            role: .assistant,
            content: assistantResponse,
            timestamp: Date()
        )
        conversationHistory.addTurn(assistantTurn)

        // Update context with new history
        let recentTurns = conversationHistory.getRecent(count: 10)
        context = SessionContext(
            sessionId: context.sessionId,
            currentRound: context.currentRound,
            currentHole: context.currentHole,
            lastShot: context.lastShot,
            lastRecommendation: context.lastRecommendation,
            conversationHistory: recentTurns
        )
    }

    /// Clears the entire session context.
    ///
    /// Called when starting a new session or on user request.
    func clearSession() {
        conversationHistory.clear()
        context = SessionContext.empty
    }

    /// Clears only the conversation history (keeps round state).
    func clearConversationHistory() {
        conversationHistory.clear()
        context = SessionContext(
            sessionId: context.sessionId,
            currentRound: context.currentRound,
            currentHole: context.currentHole,
            lastShot: context.lastShot,
            lastRecommendation: context.lastRecommendation,
            conversationHistory: []
        )
    }

    // MARK: - Context Retrieval

    /// Returns the current session context for LLM.
    ///
    /// - Returns: Current SessionContext
    func getCurrentContext() -> SessionContext {
        return context
    }

    /// Builds a context prompt for LLM injection.
    ///
    /// - Returns: Formatted context string for LLM
    func buildContextPrompt() -> String {
        return contextInjector.buildContextPrompt(from: context)
    }

    /// Returns the last user input from conversation history.
    ///
    /// Useful for detecting follow-up queries.
    ///
    /// - Returns: Last user input or nil
    func getLastUserInput() -> String? {
        return conversationHistory.getLastUserInput()
    }

    /// Returns recent conversation turns.
    ///
    /// - Parameter count: Number of recent turns to retrieve (default: 5)
    /// - Returns: Array of conversation turns
    func getRecentConversation(count: Int = 5) -> [ConversationTurn] {
        return conversationHistory.getRecent(count: count)
    }
}
