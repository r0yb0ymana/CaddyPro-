package com.caddypro.app.domain.repository

import com.caddypro.app.domain.model.WeatherData

/**
 * Repository interface for weather data operations
 *
 * AC2: Weather data refreshes automatically every 10 minutes
 * AC15: Cached weather shown immediately on screen open
 * AC17: Base distances shown when no cache and no network
 */
interface WeatherRepository {

    /**
     * Get current weather for a location.
     * Returns cached data if fresh enough, otherwise fetches from API.
     */
    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherData>

    /**
     * Get cached weather data (may be stale or null)
     */
    suspend fun getCachedWeather(): WeatherData?

    /**
     * Force refresh weather from API
     */
    suspend fun refreshWeather(latitude: Double, longitude: Double): Result<WeatherData>
}
