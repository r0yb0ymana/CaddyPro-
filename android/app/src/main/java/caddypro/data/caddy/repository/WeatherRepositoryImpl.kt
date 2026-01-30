package caddypro.data.caddy.repository

import caddypro.data.caddy.remote.WeatherApiService
import caddypro.data.caddy.remote.toDomain
import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.WeatherData
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Implementation of WeatherRepository with caching and error handling.
 *
 * Caches weather data for 5 minutes to reduce API calls and improve performance.
 * Falls back to cached data on network errors.
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 5
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds)
 *
 * @property weatherApiService Retrofit service for OpenWeatherMap API
 * @property apiKey OpenWeatherMap API key from BuildConfig
 */
@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService,
    @Named("WeatherApiKey") private val apiKey: String
) : WeatherRepository {

    @Volatile
    private var cachedWeather: WeatherData? = null

    @Volatile
    private var cacheTimestamp: Long = 0

    companion object {
        /**
         * Cache validity duration in milliseconds (5 minutes).
         *
         * Per spec: R2 requires real-time weather, but 5-minute cache balances
         * freshness with API rate limits and performance.
         */
        private const val CACHE_VALIDITY_MS = 5 * 60 * 1000L // 5 minutes
    }

    /**
     * Fetch current weather for a given location.
     *
     * Strategy:
     * 1. Check cache validity - if valid, return cached data immediately
     * 2. Otherwise, fetch from API
     * 3. On success, update cache and return data
     * 4. On failure, return cached data if available, otherwise propagate error
     *
     * @param location Geographic coordinates to fetch weather for
     * @return Result containing WeatherData on success, or error on failure (when no cache available)
     */
    override suspend fun getCurrentWeather(location: Location): Result<WeatherData> {
        // Check cache first
        if (isCacheValid()) {
            cachedWeather?.let { return Result.success(it) }
        }

        return try {
            val response = weatherApiService.getCurrentWeather(
                latitude = location.latitude,
                longitude = location.longitude,
                apiKey = apiKey
            )

            val weatherData = response.toDomain(location)

            // Update cache
            cachedWeather = weatherData
            cacheTimestamp = System.currentTimeMillis()

            Result.success(weatherData)
        } catch (e: Exception) {
            // Return cached data if available, otherwise failure
            cachedWeather?.let {
                Result.success(it)
            } ?: Result.failure(e)
        }
    }

    /**
     * Get the most recently cached weather data, if any.
     *
     * Does not perform network requests or check cache validity.
     *
     * @return Cached WeatherData if available, null otherwise
     */
    override suspend fun getCachedWeather(): WeatherData? = cachedWeather

    /**
     * Check if the current cache is still valid.
     *
     * Cache is valid if:
     * - Weather data exists in cache
     * - Age is less than CACHE_VALIDITY_MS
     *
     * @return true if cache is valid, false otherwise
     */
    private fun isCacheValid(): Boolean {
        if (cachedWeather == null) return false

        val age = System.currentTimeMillis() - cacheTimestamp
        return age < CACHE_VALIDITY_MS
    }
}
