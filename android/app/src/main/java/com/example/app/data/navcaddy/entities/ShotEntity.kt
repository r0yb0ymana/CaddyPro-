package caddypro.data.navcaddy.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a persisted golf shot.
 *
 * Maps to domain [Shot] model via [toDomain] extension.
 *
 * Spec reference: navcaddy-engine.md R5, C5 (attributable memory)
 *
 * Indexes:
 * - timestamp: for retention queries and recent pattern analysis
 * - club_id: for club-specific pattern queries
 */
@Entity(
    tableName = "shots",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["club_id"])
    ]
)
data class ShotEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "club_id")
    val clubId: String,

    @ColumnInfo(name = "club_name")
    val clubName: String,

    @ColumnInfo(name = "club_type")
    val clubType: String,

    @ColumnInfo(name = "miss_direction")
    val missDirection: String?,

    @ColumnInfo(name = "lie")
    val lie: String,

    @ColumnInfo(name = "is_user_tagged_pressure")
    val isUserTaggedPressure: Boolean,

    @ColumnInfo(name = "is_inferred_pressure")
    val isInferredPressure: Boolean,

    @ColumnInfo(name = "scoring_context")
    val scoringContext: String?,

    @ColumnInfo(name = "hole_number")
    val holeNumber: Int?,

    @ColumnInfo(name = "notes")
    val notes: String?
)
