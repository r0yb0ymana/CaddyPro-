package com.caddypro.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.caddypro.app.domain.model.ShotType
import java.util.UUID

/**
 * Room entity for shots
 */
@Entity(tableName = "shots")
data class ShotEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "round_id")
    val roundId: String,

    @ColumnInfo(name = "hole_number")
    val holeNumber: Int,

    @ColumnInfo(name = "shot_number")
    val shotNumber: Int,

    @ColumnInfo(name = "club_id")
    val clubId: String,

    @ColumnInfo(name = "club_name")
    val clubName: String,

    @ColumnInfo(name = "shot_type")
    val shotType: ShotType,

    @ColumnInfo(name = "start_latitude")
    val startLatitude: Double? = null,

    @ColumnInfo(name = "start_longitude")
    val startLongitude: Double? = null,

    @ColumnInfo(name = "end_latitude")
    val endLatitude: Double? = null,

    @ColumnInfo(name = "end_longitude")
    val endLongitude: Double? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "synced")
    val synced: Boolean = false
)
