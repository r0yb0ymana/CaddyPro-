package caddypro.data.navcaddy.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a conversation turn in the session history.
 *
 * Maps to domain [ConversationTurn] model via [toDomain] extension.
 *
 * Spec reference: navcaddy-engine.md R6
 *
 * Indexes:
 * - session_id: for retrieving conversation history for a session
 * - timestamp: for ordering and limiting history
 */
@Entity(
    tableName = "conversation_turns",
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["timestamp"])
    ]
)
data class ConversationTurnEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "role")
    val role: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
