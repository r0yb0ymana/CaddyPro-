package caddypro.data.caddy.repository

import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.WeatherData

/**
 * Repository interface for weather data operations.
 *
 * Provides access to current weather conditions with caching and error handling.
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 5
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds)
 */
interface WeatherRepository {

    /**
     * Fetch current weather for a given location.
     *
     * Uses a 5-minute cache to minimize API calls and improve performance.
     * On network failure, returns cached data if available.
     *
     * @param location Geographic coordinates to fetch weather for
     * @return Result containing WeatherData on success, or error on failure (when no cache available)
     */
    suspend fun getCurrentWeather(location: Location): Result<WeatherData>

    /**
     * Get the most recently cached weather data, if any.
     *
     * Does not perform network requests or check cache validity.
     *
     * @return Cached WeatherData if available, null otherwise
     */
    suspend fun getCachedWeather(): WeatherData?
}
