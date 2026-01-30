package caddypro.data.caddy.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service interface for OpenWeatherMap API.
 *
 * Provides real-time weather data for the Forecaster HUD.
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 2
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds)
 *
 * API Documentation: https://openweathermap.org/current
 */
interface WeatherApiService {

    /**
     * Fetch current weather conditions for a geographic location.
     *
     * @param latitude Latitude in decimal degrees
     * @param longitude Longitude in decimal degrees
     * @param apiKey OpenWeatherMap API key
     * @param units Unit system (default "metric" for Celsius and m/s)
     * @return WeatherApiResponse containing current weather data
     */
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherApiResponse
}
