package caddypro.data.navcaddy.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a persisted miss pattern.
 *
 * Maps to domain [MissPattern] model via [toDomain] extension.
 *
 * Spec reference: navcaddy-engine.md R5, C5 (attributable memory)
 *
 * Indexes:
 * - direction: for direction-specific pattern queries
 * - club_id: for club-specific pattern queries
 * - last_occurrence: for retention and decay calculations
 */
@Entity(
    tableName = "miss_patterns",
    indices = [
        Index(value = ["direction"]),
        Index(value = ["club_id"]),
        Index(value = ["last_occurrence"])
    ]
)
data class MissPatternEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "direction")
    val direction: String,

    @ColumnInfo(name = "club_id")
    val clubId: String?,

    @ColumnInfo(name = "club_name")
    val clubName: String?,

    @ColumnInfo(name = "club_type")
    val clubType: String?,

    @ColumnInfo(name = "frequency")
    val frequency: Int,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "is_user_tagged_pressure")
    val isUserTaggedPressure: Boolean?,

    @ColumnInfo(name = "is_inferred_pressure")
    val isInferredPressure: Boolean?,

    @ColumnInfo(name = "scoring_context")
    val scoringContext: String?,

    @ColumnInfo(name = "last_occurrence")
    val lastOccurrence: Long
)
