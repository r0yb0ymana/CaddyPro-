package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing the current session context.
 *
 * Maintains short-term state for conversational follow-ups and context-aware responses.
 *
 * Spec reference: navcaddy-engine.md R6
 *
 * @property currentRound Active round if in progress
 * @property currentHole Current hole number if in a round
 * @property lastShot Most recent shot recorded
 * @property lastRecommendation Last recommendation provided to user
 * @property conversationHistory Recent conversation turns (typically last 10)
 */
data class SessionContext(
    @SerializedName("current_round")
    val currentRound: Round? = null,

    @SerializedName("current_hole")
    val currentHole: Int? = null,

    @SerializedName("last_shot")
    val lastShot: Shot? = null,

    @SerializedName("last_recommendation")
    val lastRecommendation: String? = null,

    @SerializedName("conversation_history")
    val conversationHistory: List<ConversationTurn> = emptyList()
) {
    init {
        currentHole?.let {
            require(it in 1..18) { "Current hole must be between 1 and 18" }
        }
    }

    companion object {
        /** Maximum number of conversation turns to retain */
        const val MAX_HISTORY_SIZE = 10

        /** Empty session context for initialization */
        val empty = SessionContext()
    }

    /**
     * Returns a new SessionContext with the turn added and history limited to MAX_HISTORY_SIZE.
     *
     * @param turn The conversation turn to add
     * @return New SessionContext with updated history
     */
    fun addingTurn(turn: ConversationTurn): SessionContext {
        val newHistory = (conversationHistory + turn).takeLast(MAX_HISTORY_SIZE)
        return copy(conversationHistory = newHistory)
    }
}
