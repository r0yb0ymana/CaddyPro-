package com.caddypro.app.data.remote.weather

import com.caddypro.app.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OpenWeatherMap API service
 *
 * Fetches current weather data for carry distance adjustments.
 * Rate limit: 60 calls/minute (free tier), we cache for 10 minutes.
 */
@Singleton
class WeatherApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double
    ): WeatherApiResponse {
        return httpClient.get(BASE_URL) {
            parameter("lat", latitude)
            parameter("lon", longitude)
            parameter("units", "metric")
            parameter("appid", BuildConfig.OPENWEATHER_API_KEY)
        }.body()
    }

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"
    }
}
