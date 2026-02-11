package com.caddypro.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for Bag
 *
 * Represents a collection of clubs. Players can have multiple bags
 * (e.g., tournament vs casual), but only one is active at a time.
 */
@Entity(
    tableName = "bags",
    foreignKeys = [
        ForeignKey(
            entity = PlayerProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profile_id")]
)
data class BagEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "profile_id")
    val profileId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "synced")
    val synced: Boolean = false
)
