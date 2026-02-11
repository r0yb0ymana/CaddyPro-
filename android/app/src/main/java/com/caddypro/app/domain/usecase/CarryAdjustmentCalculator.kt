package com.caddypro.app.domain.usecase

import com.caddypro.app.domain.model.AdjustedClubDistance
import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.ShotDirection
import com.caddypro.app.domain.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Carry distance adjustment calculator
 *
 * adjustedCarry = baseCarry
 *     * temperatureFactor(tempC)     // ~1yd per 5C from 20C baseline
 *     * altitudeFactor(altM)         // ~2% per 300m above sea level
 *     * windFactor(windSpeed, windDir, shotDir)  // headwind/tailwind
 *     * humidityFactor(humidity)     // minimal effect, ~1yd
 *
 * AC5: Every club in active bag shows adjusted carry distance
 * AC9: Putter excluded from carry adjustments
 */
@Singleton
class CarryAdjustmentCalculator @Inject constructor() {

    /**
     * Calculate adjusted distances for all clubs
     */
    fun calculateAdjustments(
        clubs: List<Club>,
        weather: WeatherData,
        altitudeM: Double,
        shotDirection: ShotDirection
    ): List<AdjustedClubDistance> {
        return clubs.map { club ->
            calculateAdjustment(club, weather, altitudeM, shotDirection)
        }
    }

    /**
     * Calculate adjusted distance for a single club
     */
    fun calculateAdjustment(
        club: Club,
        weather: WeatherData,
        altitudeM: Double,
        shotDirection: ShotDirection
    ): AdjustedClubDistance {
        val baseCarry = club.carryDistance

        // AC9: Putter excluded from carry adjustments
        if (club.type == ClubType.PUTTER || baseCarry <= 0) {
            return AdjustedClubDistance(
                clubId = club.id,
                clubName = club.name,
                clubType = club.type,
                baseCarry = baseCarry,
                adjustedCarry = baseCarry,
                delta = 0,
                temperatureEffect = 0.0,
                altitudeEffect = 0.0,
                windEffect = 0.0,
                humidityEffect = 0.0
            )
        }

        val tempFactor = temperatureFactor(weather.temperatureC)
        val altFactor = altitudeFactor(altitudeM)
        val windFactor = windFactor(weather.windSpeedKmh, weather.windDirectionDeg, shotDirection)
        val humFactor = humidityFactor(weather.humidity)

        val adjustedCarry = (baseCarry * tempFactor * altFactor * windFactor * humFactor).roundToInt()
        val delta = adjustedCarry - baseCarry

        // Individual effects in yards
        val tempEffect = baseCarry * (tempFactor - 1.0)
        val altEffect = baseCarry * (altFactor - 1.0)
        val windEffect = baseCarry * (windFactor - 1.0)
        val humEffect = baseCarry * (humFactor - 1.0)

        return AdjustedClubDistance(
            clubId = club.id,
            clubName = club.name,
            clubType = club.type,
            baseCarry = baseCarry,
            adjustedCarry = adjustedCarry,
            delta = delta,
            temperatureEffect = tempEffect,
            altitudeEffect = altEffect,
            windEffect = windEffect,
            humidityEffect = humEffect
        )
    }

    /**
     * Temperature factor
     * Baseline: 20C
     * Effect: ~1 yard per 5C deviation per 150 yards of carry
     * Simplified: 1.0 + (tempC - 20.0) * 0.00133
     */
    fun temperatureFactor(tempC: Double): Double {
        return 1.0 + (tempC - TEMP_BASELINE_C) * TEMP_FACTOR_PER_DEGREE
    }

    /**
     * Altitude factor
     * Baseline: sea level (0m)
     * Effect: ~2% per 300m above sea level
     */
    fun altitudeFactor(altitudeM: Double): Double {
        if (altitudeM <= 0) return 1.0
        return 1.0 + (altitudeM / 300.0) * 0.02
    }

    /**
     * Wind factor
     * Decomposes wind into headwind/tailwind component based on shot direction
     * Headwind: reduces carry ~1% per 5 km/h
     * Tailwind: increases carry ~0.5% per 5 km/h (asymmetric)
     */
    fun windFactor(
        windSpeedKmh: Double,
        windDirectionDeg: Int,
        shotDirection: ShotDirection
    ): Double {
        if (windSpeedKmh <= 0) return 1.0

        // Wind direction is where wind comes FROM
        // We need the tailwind component (positive = tailwind, negative = headwind)
        val shotDirRad = Math.toRadians(shotDirection.degrees.toDouble())

        // Tailwind component: positive when wind blows in the same direction as the shot
        // Wind FROM direction X means wind BLOWS TOWARDS direction X + 180
        val windTowardsDeg = (windDirectionDeg + 180) % 360
        val windTowardsRad = Math.toRadians(windTowardsDeg.toDouble())

        // Component of wind in shot direction (positive = tailwind)
        val tailwindComponent = windSpeedKmh * cos(windTowardsRad - shotDirRad)

        return if (tailwindComponent >= 0) {
            // Tailwind: +0.5% per 5 km/h
            1.0 + (tailwindComponent / 5.0) * TAILWIND_FACTOR
        } else {
            // Headwind: -1% per 5 km/h
            1.0 + (tailwindComponent / 5.0) * HEADWIND_FACTOR
        }
    }

    /**
     * Humidity factor
     * Higher humidity = slightly longer (less dense air)
     * Effect: ~1 yard per 25% humidity change from 50% baseline
     * Practically negligible
     */
    fun humidityFactor(humidity: Int): Double {
        return 1.0 + (humidity - HUMIDITY_BASELINE) * HUMIDITY_FACTOR_PER_PERCENT
    }

    companion object {
        const val TEMP_BASELINE_C = 20.0
        const val TEMP_FACTOR_PER_DEGREE = 0.00133

        const val HUMIDITY_BASELINE = 50
        const val HUMIDITY_FACTOR_PER_PERCENT = 0.00003

        const val TAILWIND_FACTOR = 0.005
        const val HEADWIND_FACTOR = 0.01
    }
}
