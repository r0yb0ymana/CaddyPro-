package com.caddypro.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for cached weather data
 *
 * AC15: Cached weather shown immediately on screen open
 * AC18: Weather cache persists across app restarts via Room
 */
@Entity(tableName = "weather_cache")
data class WeatherEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "temperature_c")
    val temperatureC: Double,

    @ColumnInfo(name = "wind_speed_kmh")
    val windSpeedKmh: Double,

    @ColumnInfo(name = "wind_direction_deg")
    val windDirectionDeg: Int,

    @ColumnInfo(name = "humidity")
    val humidity: Int,

    @ColumnInfo(name = "condition_code")
    val conditionCode: Int,

    @ColumnInfo(name = "condition_description")
    val conditionDescription: String,

    @ColumnInfo(name = "feels_like_c")
    val feelsLikeC: Double,

    @ColumnInfo(name = "pressure_hpa")
    val pressureHpa: Int,

    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long = System.currentTimeMillis()
)
