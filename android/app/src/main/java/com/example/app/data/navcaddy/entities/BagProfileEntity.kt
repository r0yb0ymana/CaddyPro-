package caddypro.data.navcaddy.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a persisted bag profile.
 *
 * Maps to domain [BagProfile] model via [toDomain] extension.
 *
 * Spec reference: player-profile-bag-management.md R1
 * Plan reference: player-profile-bag-management-plan.md Task 3
 *
 * Indexes:
 * - is_active: for quick retrieval of the active bag
 * - created_at: for sorting bags by creation date
 */
@Entity(
    tableName = "bag_profiles",
    indices = [
        Index(value = ["is_active"]),
        Index(value = ["created_at"])
    ]
)
data class BagProfileEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
