package com.caddypro.app.ui.forecaster

import com.caddypro.app.domain.model.AdjustedClubDistance
import com.caddypro.app.domain.model.ShotDirection
import com.caddypro.app.domain.model.WeatherData

/**
 * Forecaster HUD UI state
 */
data class ForecasterState(
    val weather: WeatherData? = null,
    val adjustedDistances: List<AdjustedClubDistance> = emptyList(),
    val shotDirection: ShotDirection = ShotDirection.N,
    val altitudeM: Double = 0.0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasWeatherData: Boolean = false,
    val isWeatherStale: Boolean = false,
    val errorMessage: String? = null,
    val selectedClub: AdjustedClubDistance? = null,
    val showAdjustmentDetail: Boolean = false,
    val useMetric: Boolean = true
) {
    val lastUpdatedText: String
        get() {
            val weather = weather ?: return "No data"
            val ageMs = System.currentTimeMillis() - weather.fetchedAt
            val ageMin = (ageMs / 60000).toInt()
            return when {
                ageMin < 1 -> "Just now"
                ageMin == 1 -> "1 min ago"
                ageMin < 60 -> "$ageMin min ago"
                else -> "${ageMin / 60}h ago"
            }
        }
}
