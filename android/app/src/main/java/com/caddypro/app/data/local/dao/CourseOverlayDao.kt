package com.caddypro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.caddypro.app.data.local.entities.CourseOverlayEntity

/**
 * DAO for course overlay cache operations
 */
@Dao
interface CourseOverlayDao {

    /**
     * Find cached overlay data near a location.
     * Uses a simple bounding box check (~500m at equator).
     */
    @Query("""
        SELECT * FROM course_overlay_cache
        WHERE center_lat BETWEEN :lat - 0.005 AND :lat + 0.005
        AND center_lon BETWEEN :lon - 0.005 AND :lon + 0.005
        ORDER BY fetched_at DESC
        LIMIT 1
    """)
    suspend fun getCachedNear(lat: Double, lon: Double): CourseOverlayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CourseOverlayEntity)

    @Query("DELETE FROM course_overlay_cache WHERE fetched_at < :beforeTimestamp")
    suspend fun deleteOldEntries(beforeTimestamp: Long)
}
