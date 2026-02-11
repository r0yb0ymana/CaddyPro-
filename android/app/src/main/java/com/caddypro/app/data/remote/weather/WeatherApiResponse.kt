package com.caddypro.app.data.remote.weather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenWeatherMap Current Weather API response
 * Endpoint: /data/2.5/weather
 */
@Serializable
data class WeatherApiResponse(
    val main: MainData,
    val wind: WindData,
    val weather: List<WeatherCondition>,
    val name: String = ""
)

@Serializable
data class MainData(
    val temp: Double,
    val humidity: Int,
    val pressure: Int,
    @SerialName("feels_like")
    val feelsLike: Double
)

@Serializable
data class WindData(
    val speed: Double,
    val deg: Int = 0
)

@Serializable
data class WeatherCondition(
    val id: Int,
    val description: String,
    val icon: String = ""
)
