package com.caddypro.app.domain.model

/**
 * Weather data domain model for Forecaster HUD
 */
data class WeatherData(
    val temperatureC: Double,
    val windSpeedKmh: Double,
    val windDirectionDeg: Int,
    val humidity: Int,
    val conditionCode: Int,
    val conditionDescription: String,
    val feelsLikeC: Double,
    val pressureHpa: Int,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    val isStale: Boolean
        get() = System.currentTimeMillis() - fetchedAt > STALE_THRESHOLD_MS

    companion object {
        const val STALE_THRESHOLD_MS = 30 * 60 * 1000L // 30 minutes
        const val REFRESH_INTERVAL_MS = 10 * 60 * 1000L // 10 minutes
    }
}
