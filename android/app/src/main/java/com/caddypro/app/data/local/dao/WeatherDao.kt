package com.caddypro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.caddypro.app.data.local.entities.WeatherEntity

/**
 * DAO for weather cache operations
 *
 * AC15: Cached weather shown immediately
 * AC18: Persists across app restarts
 */
@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather_cache ORDER BY fetched_at DESC LIMIT 1")
    suspend fun getLatestWeather(): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("DELETE FROM weather_cache WHERE fetched_at < :beforeTimestamp")
    suspend fun deleteOldEntries(beforeTimestamp: Long)
}
