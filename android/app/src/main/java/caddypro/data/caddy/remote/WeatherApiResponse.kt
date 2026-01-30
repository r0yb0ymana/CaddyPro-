package caddypro.data.caddy.remote

import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.WeatherData
import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object for OpenWeatherMap API response.
 *
 * Maps the JSON response from OpenWeatherMap's current weather endpoint
 * to a structured Kotlin object.
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 2
 *
 * API Response Structure:
 * {
 *   "wind": { "speed": 5.2, "deg": 180 },
 *   "main": { "temp": 18.5, "humidity": 65 },
 *   "dt": 1640000000,
 *   "coord": { "lat": 37.7749, "lon": -122.4194 }
 * }
 */
data class WeatherApiResponse(
    @SerializedName("wind")
    val wind: Wind,

    @SerializedName("main")
    val main: Main,

    @SerializedName("dt")
    val timestamp: Long,

    @SerializedName("coord")
    val coord: Coord
) {
    /**
     * Wind data from API.
     *
     * @property speed Wind speed in m/s (when units=metric)
     * @property deg Wind direction in degrees (meteorological: from direction)
     */
    data class Wind(
        @SerializedName("speed")
        val speed: Double,

        @SerializedName("deg")
        val deg: Int
    )

    /**
     * Main atmospheric data from API.
     *
     * @property temp Temperature in Celsius (when units=metric)
     * @property humidity Relative humidity percentage
     */
    data class Main(
        @SerializedName("temp")
        val temp: Double,

        @SerializedName("humidity")
        val humidity: Int
    )

    /**
     * Coordinate data from API.
     *
     * @property lat Latitude in decimal degrees
     * @property lon Longitude in decimal degrees
     */
    data class Coord(
        @SerializedName("lat")
        val lat: Double,

        @SerializedName("lon")
        val lon: Double
    )
}

/**
 * Extension function to map API response to domain model.
 *
 * Converts the DTO structure from OpenWeatherMap to our internal WeatherData model.
 *
 * @param location Location to associate with this weather data (uses API coords by default)
 * @return WeatherData domain model
 */
fun WeatherApiResponse.toDomain(location: Location? = null): WeatherData {
    return WeatherData(
        windSpeedMps = wind.speed,
        windDegrees = wind.deg,
        temperatureCelsius = main.temp,
        humidity = main.humidity,
        timestamp = timestamp * 1000, // Convert from Unix seconds to milliseconds
        location = location ?: Location(
            latitude = coord.lat,
            longitude = coord.lon
        )
    )
}
