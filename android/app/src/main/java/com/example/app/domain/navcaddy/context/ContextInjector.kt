package caddypro.domain.navcaddy.context

import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.Role
import caddypro.domain.navcaddy.models.SessionContext

/**
 * Prepares session context for injection into LLM prompts.
 *
 * Formats round state, hole information, last shot details, and conversation history
 * in a way that helps the LLM understand the user's current situation for better responses.
 *
 * Spec reference: navcaddy-engine.md R6 (Session Context)
 */
object ContextInjector {

    /**
     * Build a formatted context prompt from session context.
     *
     * The prompt includes:
     * - Current round and course information
     * - Current hole and par
     * - Last shot details (if available)
     * - Last recommendation (if available)
     * - Recent conversation history
     *
     * @param context The session context to format
     * @return Formatted context string for LLM prompt
     */
    fun buildContextPrompt(context: SessionContext): String {
        if (isContextEmpty(context)) {
            return ""
        }

        return buildString {
            appendLine("## Current Context")
            appendLine()

            // Round information
            context.currentRound?.let { round ->
                appendLine("**Round Information:**")
                round.courseName?.let { appendLine("- Course: $it") }
                appendLine("- Round ID: ${round.id}")
                appendLine()
            }

            // Current hole
            context.currentHole?.let { hole ->
                appendLine("**Current Position:**")
                appendLine("- Hole: $hole")
                // Try to get par from round state if available
                context.currentRound?.scores?.get(hole)?.let { score ->
                    appendLine("- Score on this hole: $score")
                }
                appendLine()
            }

            // Last shot
            context.lastShot?.let { shot ->
                appendLine("**Last Shot:**")
                appendLine("- Club: ${shot.club.name}")
                shot.missDirection?.let { appendLine("- Miss: $it") }
                appendLine("- Lie: ${shot.lie}")
                if (shot.pressureContext.toString() != "NORMAL") {
                    appendLine("- Pressure: ${shot.pressureContext}")
                }
                shot.notes?.let { appendLine("- Notes: $it") }
                appendLine()
            }

            // Last recommendation
            context.lastRecommendation?.let { recommendation ->
                appendLine("**Last Recommendation:**")
                appendLine(recommendation)
                appendLine()
            }

            // Conversation history
            if (context.conversationHistory.isNotEmpty()) {
                appendLine("**Recent Conversation:**")
                formatConversationHistory(context.conversationHistory).forEach { line ->
                    appendLine(line)
                }
            }
        }.trim()
    }

    /**
     * Build a concise one-line context summary.
     *
     * Useful for UI display or short context references.
     *
     * @param context The session context to summarize
     * @return Concise context summary
     */
    fun buildContextSummary(context: SessionContext): String {
        val parts = mutableListOf<String>()

        context.currentRound?.courseName?.let { parts.add(it) }
        context.currentHole?.let { parts.add("Hole $it") }
        context.lastShot?.let { shot ->
            parts.add("Last: ${shot.club.name}")
        }

        return if (parts.isEmpty()) {
            "No active session"
        } else {
            parts.joinToString(" • ")
        }
    }

    /**
     * Extract key context elements for entity enrichment.
     *
     * Returns a map of context keys that can be used to provide defaults
     * or hints during entity extraction.
     *
     * @param context The session context
     * @return Map of context keys to values
     */
    fun extractContextHints(context: SessionContext): Map<String, String> {
        val hints = mutableMapOf<String>()

        context.lastShot?.let { shot ->
            hints["last_club"] = shot.club.name
            shot.missDirection?.let { hints["last_miss"] = it.toString() }
            hints["last_lie"] = shot.lie.toString()
        }

        context.currentHole?.let { hints["current_hole"] = it.toString() }
        context.currentRound?.courseName?.let { hints["course"] = it }
        context.lastRecommendation?.let { hints["last_recommendation"] = it }

        return hints
    }

    /**
     * Check if the context is empty (no useful information).
     */
    private fun isContextEmpty(context: SessionContext): Boolean {
        return context.currentRound == null &&
                context.currentHole == null &&
                context.lastShot == null &&
                context.lastRecommendation == null &&
                context.conversationHistory.isEmpty()
    }

    /**
     * Format conversation history for prompt injection.
     *
     * @param history List of conversation turns
     * @return List of formatted conversation lines
     */
    private fun formatConversationHistory(history: List<ConversationTurn>): List<String> {
        return history.map { turn ->
            val role = when (turn.role) {
                Role.USER -> "User"
                Role.ASSISTANT -> "Assistant"
            }
            "- $role: ${turn.content}"
        }
    }

    /**
     * Build context for follow-up query detection.
     *
     * Returns a simplified context focused on the last exchange,
     * useful for determining if the current query is a follow-up.
     *
     * @param context The session context
     * @return Follow-up context string
     */
    fun buildFollowUpContext(context: SessionContext): String {
        val lastUserInput = context.conversationHistory
            .lastOrNull { it.role == Role.USER }?.content
        val lastAssistantResponse = context.conversationHistory
            .lastOrNull { it.role == Role.ASSISTANT }?.content

        if (lastUserInput == null || lastAssistantResponse == null) {
            return ""
        }

        return buildString {
            appendLine("Last exchange:")
            appendLine("User: $lastUserInput")
            appendLine("Assistant: $lastAssistantResponse")
        }
    }
}
