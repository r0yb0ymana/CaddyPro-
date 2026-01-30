package caddypro.data.navcaddy.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing the club-bag association with club data.
 *
 * This is a join table that also stores club-specific data for each bag.
 * The same physical club can have different settings in different bags.
 *
 * All distances are in meters (metric units).
 *
 * Spec reference: player-profile-bag-management.md R2
 * Plan reference: player-profile-bag-management-plan.md Task 3
 *
 * Indexes:
 * - bag_id: for retrieving all clubs in a bag
 * - club_id: for finding club across bags
 * - bag_id + position: for ordering clubs within a bag
 *
 * Foreign keys:
 * - bag_id references bag_profiles(id) with CASCADE delete
 */
@Entity(
    tableName = "bag_clubs",
    foreignKeys = [
        ForeignKey(
            entity = BagProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["bag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bag_id"]),
        Index(value = ["club_id"]),
        Index(value = ["bag_id", "position"])
    ]
)
data class BagClubEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "bag_id")
    val bagId: String,

    @ColumnInfo(name = "club_id")
    val clubId: String,

    @ColumnInfo(name = "position")
    val position: Int,

    // Club data fields
    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "loft")
    val loft: Double?,

    @ColumnInfo(name = "estimated_carry")
    val estimatedCarry: Int,

    @ColumnInfo(name = "inferred_carry")
    val inferredCarry: Int?,

    @ColumnInfo(name = "inferred_confidence")
    val inferredConfidence: Double?,

    // Miss bias fields (embedded)
    @ColumnInfo(name = "miss_bias_direction")
    val missBiasDirection: String?,

    @ColumnInfo(name = "miss_bias_type")
    val missBiasType: String?,

    @ColumnInfo(name = "miss_bias_is_user_defined")
    val missBiasIsUserDefined: Boolean?,

    @ColumnInfo(name = "miss_bias_confidence")
    val missBiasConfidence: Double?,

    @ColumnInfo(name = "miss_bias_last_updated")
    val missBiasLastUpdated: Long?,

    // Additional club fields
    @ColumnInfo(name = "shaft")
    val shaft: String?,

    @ColumnInfo(name = "flex")
    val flex: String?,

    @ColumnInfo(name = "notes")
    val notes: String?
)
