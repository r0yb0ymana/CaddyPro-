package com.caddypro.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for cached course overlay data from Overpass API.
 * Stores the full parsed data as a JSON blob keyed by approximate location.
 */
@Entity(tableName = "course_overlay_cache")
data class CourseOverlayEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "center_lat")
    val centerLat: Double,
    @ColumnInfo(name = "center_lon")
    val centerLon: Double,
    @ColumnInfo(name = "course_name")
    val courseName: String?,
    @ColumnInfo(name = "data_json")
    val dataJson: String,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long = System.currentTimeMillis()
)
