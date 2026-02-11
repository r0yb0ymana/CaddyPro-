package com.caddypro.app.domain.model

/**
 * Computed adjusted club distance for Forecaster HUD
 * Not persisted - calculated from weather + club data
 */
data class AdjustedClubDistance(
    val clubId: String,
    val clubName: String,
    val clubType: ClubType,
    val baseCarry: Int,
    val adjustedCarry: Int,
    val delta: Int,
    val temperatureEffect: Double,
    val altitudeEffect: Double,
    val windEffect: Double,
    val humidityEffect: Double
)
