package caddypro.domain.navcaddy.context

import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.Role

/**
 * Manages conversation history with a fixed-size circular buffer.
 *
 * Maintains the last N conversation turns for context-aware follow-up queries.
 *
 * Spec reference: navcaddy-engine.md R6 (Session Context)
 *
 * @property maxSize Maximum number of turns to retain (default: 10)
 */
class ConversationHistory(
    private val maxSize: Int = DEFAULT_MAX_SIZE
) {
    private val turns = mutableListOf<ConversationTurn>()

    /**
     * Add a new conversation turn to the history.
     *
     * If the history exceeds maxSize, the oldest turn is removed.
     *
     * @param turn The conversation turn to add
     */
    fun addTurn(turn: ConversationTurn) {
        synchronized(turns) {
            turns.add(turn)
            // Keep only the most recent maxSize turns
            if (turns.size > maxSize) {
                turns.removeAt(0)
            }
        }
    }

    /**
     * Get the most recent N conversation turns.
     *
     * @param count Number of recent turns to retrieve (default: all)
     * @return List of conversation turns, most recent last
     */
    fun getRecent(count: Int = maxSize): List<ConversationTurn> {
        return synchronized(turns) {
            turns.takeLast(count)
        }
    }

    /**
     * Get all conversation turns.
     *
     * @return List of all conversation turns, most recent last
     */
    fun getAll(): List<ConversationTurn> {
        return synchronized(turns) {
            turns.toList()
        }
    }

    /**
     * Get the last user input from the conversation.
     *
     * @return The most recent user message, or null if none exists
     */
    fun getLastUserInput(): String? {
        return synchronized(turns) {
            turns.lastOrNull { it.role == Role.USER }?.content
        }
    }

    /**
     * Get the last assistant response from the conversation.
     *
     * @return The most recent assistant message, or null if none exists
     */
    fun getLastAssistantResponse(): String? {
        return synchronized(turns) {
            turns.lastOrNull { it.role == Role.ASSISTANT }?.content
        }
    }

    /**
     * Get the last N turns of a specific role.
     *
     * @param role The role to filter by
     * @param count Number of turns to retrieve
     * @return List of conversation turns for the specified role
     */
    fun getRecentByRole(role: Role, count: Int = maxSize): List<ConversationTurn> {
        return synchronized(turns) {
            turns.filter { it.role == role }.takeLast(count)
        }
    }

    /**
     * Clear all conversation history.
     */
    fun clear() {
        synchronized(turns) {
            turns.clear()
        }
    }

    /**
     * Get the current number of turns in history.
     */
    fun size(): Int {
        return synchronized(turns) {
            turns.size
        }
    }

    /**
     * Check if the history is empty.
     */
    fun isEmpty(): Boolean {
        return synchronized(turns) {
            turns.isEmpty()
        }
    }

    companion object {
        /** Default maximum number of conversation turns to retain */
        const val DEFAULT_MAX_SIZE = 10
    }
}
