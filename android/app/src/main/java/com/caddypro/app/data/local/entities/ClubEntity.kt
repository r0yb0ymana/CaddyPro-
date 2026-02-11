package com.caddypro.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias
import java.util.UUID

/**
 * Room entity for Club
 *
 * Represents a golf club with its characteristics and performance data.
 */
@Entity(
    tableName = "clubs",
    foreignKeys = [
        ForeignKey(
            entity = BagEntity::class,
            parentColumns = ["id"],
            childColumns = ["bag_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bag_id")]
)
data class ClubEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "bag_id")
    val bagId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: ClubType,

    @ColumnInfo(name = "loft")
    val loft: Float? = null,

    @ColumnInfo(name = "carry_distance")
    val carryDistance: Int,

    @ColumnInfo(name = "total_distance")
    val totalDistance: Int,

    @ColumnInfo(name = "miss_bias")
    val missBias: MissBias = MissBias.STRAIGHT,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "synced")
    val synced: Boolean = false
)
