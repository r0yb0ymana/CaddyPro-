package com.caddypro.app.data.local.mappers

import com.caddypro.app.data.local.entities.WeatherEntity
import com.caddypro.app.data.remote.weather.WeatherApiResponse
import com.caddypro.app.domain.model.WeatherData

/**
 * Mappers between WeatherEntity, WeatherApiResponse, and WeatherData domain model
 */

fun WeatherEntity.toDomain(): WeatherData {
    return WeatherData(
        temperatureC = temperatureC,
        windSpeedKmh = windSpeedKmh,
        windDirectionDeg = windDirectionDeg,
        humidity = humidity,
        conditionCode = conditionCode,
        conditionDescription = conditionDescription,
        feelsLikeC = feelsLikeC,
        pressureHpa = pressureHpa,
        fetchedAt = fetchedAt
    )
}

fun WeatherApiResponse.toEntity(latitude: Double, longitude: Double): WeatherEntity {
    return WeatherEntity(
        latitude = latitude,
        longitude = longitude,
        temperatureC = main.temp,
        windSpeedKmh = wind.speed * 3.6, // m/s to km/h
        windDirectionDeg = wind.deg,
        humidity = main.humidity,
        conditionCode = weather.firstOrNull()?.id ?: 0,
        conditionDescription = weather.firstOrNull()?.description ?: "Unknown",
        feelsLikeC = main.feelsLike,
        pressureHpa = main.pressure
    )
}

fun WeatherApiResponse.toDomain(): WeatherData {
    return WeatherData(
        temperatureC = main.temp,
        windSpeedKmh = wind.speed * 3.6, // m/s to km/h
        windDirectionDeg = wind.deg,
        humidity = main.humidity,
        conditionCode = weather.firstOrNull()?.id ?: 0,
        conditionDescription = weather.firstOrNull()?.description ?: "Unknown",
        feelsLikeC = main.feelsLike,
        pressureHpa = main.pressure
    )
}
