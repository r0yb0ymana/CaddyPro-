import Foundation

/// Manages conversation turns with fixed-size circular buffer.
///
/// Maintains last 10 conversation turns for context injection into LLM.
/// Automatically prunes older turns when capacity is exceeded.
///
/// Spec R6: Maintains conversation history for follow-up queries
final class ConversationHistory {
    /// Maximum number of conversation turns to maintain
    private let maxTurns: Int

    /// Internal storage for conversation turns
    private var turns: [ConversationTurn]

    init(maxTurns: Int = 10) {
        self.maxTurns = maxTurns
        self.turns = []
    }

    /// Adds a conversation turn to the history.
    ///
    /// If the history exceeds maxTurns, the oldest turn is removed.
    ///
    /// - Parameter turn: The conversation turn to add
    func addTurn(_ turn: ConversationTurn) {
        turns.append(turn)

        // Prune to max size (circular buffer behavior)
        if turns.count > maxTurns {
            turns = Array(turns.suffix(maxTurns))
        }
    }

    /// Returns the most recent N conversation turns.
    ///
    /// - Parameter count: Number of recent turns to return
    /// - Returns: Array of conversation turns (may be fewer than count)
    func getRecent(count: Int) -> [ConversationTurn] {
        let actualCount = min(count, turns.count)
        return Array(turns.suffix(actualCount))
    }

    /// Returns the last user input from the conversation history.
    ///
    /// Useful for detecting follow-up queries.
    ///
    /// - Returns: Last user input content, or nil if no user turns exist
    func getLastUserInput() -> String? {
        return turns.last(where: { $0.role == .user })?.content
    }

    /// Returns the last assistant response from the conversation history.
    ///
    /// - Returns: Last assistant response content, or nil if no assistant turns exist
    func getLastAssistantResponse() -> String? {
        return turns.last(where: { $0.role == .assistant })?.content
    }

    /// Clears all conversation turns.
    func clear() {
        turns.removeAll()
    }

    /// Returns all conversation turns.
    ///
    /// - Returns: Array of all conversation turns
    func getAll() -> [ConversationTurn] {
        return turns
    }

    /// Returns the number of conversation turns.
    ///
    /// - Returns: Turn count
    func count() -> Int {
        return turns.count
    }

    /// Returns whether the conversation history is empty.
    ///
    /// - Returns: True if no turns exist
    func isEmpty() -> Bool {
        return turns.isEmpty
    }
}
