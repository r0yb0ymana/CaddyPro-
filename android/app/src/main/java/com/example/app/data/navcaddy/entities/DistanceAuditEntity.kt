package caddypro.data.navcaddy.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an audit trail entry for distance changes.
 *
 * Maps to domain [DistanceAuditEntry] model via [toDomain] extension.
 *
 * All distances are in meters (metric units).
 *
 * Spec reference: player-profile-bag-management.md R3
 * Plan reference: player-profile-bag-management-plan.md Task 3
 *
 * Indexes:
 * - club_id: for retrieving audit history for a specific club
 * - timestamp: for sorting audit entries chronologically
 */
@Entity(
    tableName = "distance_audits",
    indices = [
        Index(value = ["club_id"]),
        Index(value = ["timestamp"])
    ]
)
data class DistanceAuditEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "club_id")
    val clubId: String,

    @ColumnInfo(name = "old_estimated")
    val oldEstimated: Int,

    @ColumnInfo(name = "new_estimated")
    val newEstimated: Int,

    @ColumnInfo(name = "inferred_value")
    val inferredValue: Int?,

    @ColumnInfo(name = "confidence")
    val confidence: Double?,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "was_accepted")
    val wasAccepted: Boolean
)
