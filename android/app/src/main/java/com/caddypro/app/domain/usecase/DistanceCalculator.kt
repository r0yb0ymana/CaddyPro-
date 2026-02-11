package com.caddypro.app.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Haversine distance calculator for GPS coordinates.
 *
 * AC15: Haversine formula used for distance calculation
 */
@Singleton
class DistanceCalculator @Inject constructor() {

    companion object {
        private const val EARTH_RADIUS_METERS = 6_371_000.0
        private const val METERS_TO_YARDS = 1.09361
    }

    /**
     * Calculate distance in meters between two points.
     */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculate distance in yards between two points.
     */
    fun distanceYards(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        return (distanceMeters(lat1, lon1, lat2, lon2) * METERS_TO_YARDS).roundToInt()
    }

    /**
     * Calculate distance in meters between two points, rounded.
     */
    fun distanceMetersRounded(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        return distanceMeters(lat1, lon1, lat2, lon2).roundToInt()
    }
}
