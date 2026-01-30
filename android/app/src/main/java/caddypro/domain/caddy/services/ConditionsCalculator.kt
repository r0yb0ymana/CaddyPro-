package caddypro.domain.caddy.services

import caddypro.domain.caddy.models.ConditionsAdjustment
import caddypro.domain.caddy.models.WeatherData
import caddypro.domain.caddy.models.WindComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that calculates carry distance adjustments based on weather conditions.
 *
 * Analyzes temperature, humidity, and wind to compute multipliers that affect
 * ball carry distance. Used by the Forecaster HUD to provide conditions-aware
 * club recommendations.
 *
 * Physics model (simplified for MVP per spec):
 * - Air density: affected by temperature and humidity
 * - Temperature: cold air is denser, reduces carry
 * - Wind: headwind reduces carry, tailwind increases carry
 * - Humidity: minimal effect (incorporated into air density proxy)
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 7
 * Acceptance criteria: A1 (Conditions adjustment applied to carry)
 *
 * @see WeatherData
 * @see ConditionsAdjustment
 */
@Singleton
class ConditionsCalculator @Inject constructor() {

    /**
     * Calculate carry distance adjustment based on weather conditions.
     *
     * Combines air density (temperature/humidity), temperature effects, and wind
     * into a single multiplier with a human-readable explanation.
     *
     * Expected results per spec:
     * - 5m/s headwind at 0°C = ~12% carry reduction
     * - Temperature at 0°C reduces carry by ~7.5% vs 15°C
     * - Tailwind of 5m/s increases carry by ~5%
     *
     * @param weather Current weather conditions
     * @param targetBearing Shot direction in degrees (0-360, 0 = North)
     * @param baseCarryMeters Expected carry distance in standard conditions
     * @return ConditionsAdjustment with modifier and reason string
     */
    fun calculateAdjustment(
        weather: WeatherData,
        targetBearing: Int,
        baseCarryMeters: Int
    ): ConditionsAdjustment {
        require(targetBearing in 0..360) { "Target bearing must be 0-360 degrees" }
        require(baseCarryMeters > 0) { "Base carry must be positive" }

        // Calculate component effects
        val airDensity = airDensityFactor(weather.temperatureCelsius, weather.humidity)
        val wind = weather.windComponent(targetBearing)

        // Simplified model (acceptable for MVP per spec)
        val tempEffect = temperatureCarryEffect(weather.temperatureCelsius)
        val windEffect = windCarryEffect(wind.headwind)

        // Combine effects multiplicatively
        val totalModifier = airDensity * tempEffect * windEffect
        val adjustedCarry = (baseCarryMeters * totalModifier).toInt()
        val carryDiff = adjustedCarry - baseCarryMeters

        return ConditionsAdjustment(
            carryModifier = totalModifier,
            reason = buildReasonString(weather, wind, carryDiff)
        )
    }

    /**
     * Calculate air density factor based on temperature and humidity.
     *
     * Air density normalization:
     * - 1.0 at 15°C (standard conditions)
     * - Increases at cold temps (denser air)
     * - Decreases at warm temps (thinner air)
     * - Humidity effect is minimal, incorporated into temperature proxy
     *
     * Formula: 1.0 + ((15 - temp) * 0.002)
     * - At 0°C: 1.0 + (15 * 0.002) = 1.03 (3% denser)
     * - At 30°C: 1.0 + (-15 * 0.002) = 0.97 (3% thinner)
     *
     * @param tempC Temperature in Celsius
     * @param humidity Relative humidity percentage (0-100)
     * @return Air density multiplier (1.0 = standard)
     */
    private fun airDensityFactor(tempC: Double, humidity: Int): Double {
        // 1.0 at 15°C, increases at cold temps
        return 1.0 + ((15.0 - tempC) * 0.002)
    }

    /**
     * Calculate temperature effect on carry distance.
     *
     * Temperature carry effect:
     * - Cold air = denser = ball slows faster = less carry
     * - Warm air = thinner = ball slows slower = more carry
     * - Reference point: 15°C = neutral (1.0)
     *
     * Formula: 1.0 - ((15 - temp) * 0.005)
     * - At 0°C: 1.0 - (15 * 0.005) = 0.925 (7.5% reduction)
     * - At 30°C: 1.0 - (-15 * 0.005) = 1.075 (7.5% increase)
     *
     * Note: This is independent of air density factor above.
     * Air density affects launch, temperature affects flight drag.
     *
     * @param tempC Temperature in Celsius
     * @return Temperature carry multiplier (1.0 = standard)
     */
    private fun temperatureCarryEffect(tempC: Double): Double {
        // Cold air = denser = less carry
        return 1.0 - ((15.0 - tempC) * 0.005)
    }

    /**
     * Calculate wind effect on carry distance.
     *
     * Wind carry effect:
     * - Headwind (negative) reduces carry
     * - Tailwind (positive) increases carry
     * - Crosswind does not directly affect carry (only lateral movement)
     *
     * Formula: 1.0 + (headwind * 0.01)
     * - 5m/s headwind: 1.0 + (-5 * 0.01) = 0.95 (5% reduction)
     * - 5m/s tailwind: 1.0 + (5 * 0.01) = 1.05 (5% increase)
     *
     * Note: headwind is negative for into-wind, positive for with-wind
     * per WindComponent convention.
     *
     * @param headwindMps Headwind component in m/s (negative = into wind, positive = with wind)
     * @return Wind carry multiplier (1.0 = no wind)
     */
    private fun windCarryEffect(headwindMps: Double): Double {
        // Headwind (negative) reduces, tailwind (positive) increases
        return 1.0 + (headwindMps * 0.01)
    }

    /**
     * Build human-readable explanation of conditions adjustment.
     *
     * Format examples:
     * - "+5m due to warm air (22°C), 3m/s tailwind"
     * - "-8m due to cold air (8°C), 4m/s headwind"
     * - "+2m due to 2m/s tailwind"
     * - "No adjustment (ideal conditions)"
     *
     * @param weather Current weather conditions
     * @param wind Decomposed wind components
     * @param carryDiff Carry difference in meters (positive = more carry, negative = less)
     * @return Human-readable reason string
     */
    private fun buildReasonString(
        weather: WeatherData,
        wind: WindComponent,
        carryDiff: Int
    ): String {
        if (carryDiff == 0) {
            return "No adjustment (ideal conditions)"
        }

        val factors = mutableListOf<String>()

        // Temperature factor
        val tempDiff = weather.temperatureCelsius - 15.0
        when {
            tempDiff > 5.0 -> factors.add("warm air (${weather.temperatureCelsius.toInt()}°C)")
            tempDiff < -5.0 -> factors.add("cold air (${weather.temperatureCelsius.toInt()}°C)")
        }

        // Wind factor
        val windSpeedMps = kotlin.math.abs(wind.headwind)
        if (windSpeedMps > 1.0) {
            val windType = if (wind.headwind < 0) "headwind" else "tailwind"
            factors.add("${windSpeedMps.toInt()}m/s $windType")
        }

        val sign = if (carryDiff > 0) "+" else ""
        val factorText = if (factors.isNotEmpty()) {
            " due to ${factors.joinToString(", ")}"
        } else {
            " due to conditions"
        }

        return "$sign${carryDiff}m$factorText"
    }
}
