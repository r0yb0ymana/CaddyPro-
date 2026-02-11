package caddypro.data.navcaddy.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing the current session context.
 *
 * Maps to domain [SessionContext] model via [toDomain] extension.
 *
 * Spec reference: navcaddy-engine.md R6
 *
 * Note: Single row with id="current" to persist session across app restarts.
 * Conversation history is stored separately in [ConversationTurnEntity].
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String = "current",

    @ColumnInfo(name = "round_id")
    val roundId: String?,

    @ColumnInfo(name = "current_hole")
    val currentHole: Int?,

    @ColumnInfo(name = "last_recommendation")
    val lastRecommendation: String?,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
