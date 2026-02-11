package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName

/**
 * Enum representing the role in a conversation turn.
 */
enum class Role {
    @SerializedName("user")
    USER,

    @SerializedName("assistant")
    ASSISTANT
}

/**
 * Domain model representing a single turn in the conversation history.
 *
 * Used for maintaining context across multiple user inputs.
 *
 * Spec reference: navcaddy-engine.md R6
 *
 * @property role Who spoke in this turn
 * @property content The message content
 * @property timestamp Unix timestamp of the turn
 */
data class ConversationTurn(
    @SerializedName("role")
    val role: Role,

    @SerializedName("content")
    val content: String,

    @SerializedName("timestamp")
    val timestamp: Long
)
