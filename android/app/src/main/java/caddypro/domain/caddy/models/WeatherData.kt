package caddypro.domain.caddy.models

import kotlin.math.cos
import kotlin.math.sin

/**
 * Domain model representing weather data for golf course conditions.
 *
 * Contains real-time weather information used to compute carry distance adjustments
 * and provide conditions-aware strategy recommendations.
 *
 * This is a pure domain model - serialization concerns belong in the data layer DTOs.
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 2
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds)
 *
 * @property windSpeedMps Wind speed in meters per second
 * @property windDegrees Wind direction in degrees (0-359, meteorological convention: from direction)
 * @property temperatureCelsius Temperature in degrees Celsius
 * @property humidity Relative humidity percentage (0-100)
 * @property timestamp Unix timestamp (epoch milliseconds) when data was fetched
 * @property location Geographic location where weather was measured
 */
data class WeatherData(
    val windSpeedMps: Double,
    val windDegrees: Int,
    val temperatureCelsius: Double,
    val humidity: Int,
    val timestamp: Long,
    val location: Location
) {
    init {
        require(windSpeedMps >= 0) { "Wind speed cannot be negative" }
        require(windDegrees in 0 until 360) {
            "Invalid wind direction: $windDegrees. Must be between 0 and 359 degrees"
        }
        require(humidity in 0..100) { "Humidity must be between 0 and 100" }
        require(timestamp > 0) { "Timestamp must be positive" }
    }

    /**
     * Calculate wind components relative to a target bearing.
     *
     * Decomposes wind vector into headwind (along shot line) and crosswind
     * (perpendicular to shot line) components using vector projection.
     *
     * Convention:
     * - Wind degrees: meteorological (from direction, 0 = North, 90 = East)
     * - Target bearing: navigation (to direction, 0 = North, 90 = East)
     * - Headwind: negative = into wind (hurts), positive = with wind (helps)
     * - Crosswind: positive = right-to-left, negative = left-to-right
     *
     * Example:
     * - Wind from 180° (South) at 5m/s, target 0° (North) = -5m/s headwind
     * - Wind from 0° (North) at 5m/s, target 0° (North) = +5m/s tailwind
     * - Wind from 90° (East) at 5m/s, target 0° (North) = +5m/s crosswind (right-to-left)
     *
     * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
     * Plan reference: live-caddy-mode-plan.md Task 7
     *
     * @param targetBearing Target direction in degrees (0-360, 0 = North)
     * @return WindComponent with headwind and crosswind values in m/s
     */
    fun windComponent(targetBearing: Int): WindComponent {
        require(targetBearing in 0..360) { "Target bearing must be 0-360 degrees" }

        // Calculate relative angle between wind direction and target bearing
        // Wind degrees is "from" direction, so add 180 to get "to" direction
        val windToDirection = (windDegrees + 180) % 360
        val relativeAngle = (windToDirection - targetBearing + 360) % 360
        val relativeAngleRad = Math.toRadians(relativeAngle.toDouble())

        // Project wind vector onto target bearing
        // cos(0°) = 1 (directly behind = max tailwind)
        // cos(180°) = -1 (directly ahead = max headwind)
        val headwind = windSpeedMps * cos(relativeAngleRad)

        // sin(90°) = 1 (from right = right-to-left crosswind)
        // sin(270°) = -1 (from left = left-to-right crosswind)
        val crosswind = windSpeedMps * sin(relativeAngleRad)

        return WindComponent(
            headwind = headwind,
            crosswind = crosswind
        )
    }

    /**
     * Calculate an air density proxy based on temperature and humidity.
     *
     * Air density affects launch conditions and ball flight. Cold air is denser,
     * resulting in different ball behavior than warm air.
     *
     * Formula: 1.0 + ((15 - temp) * 0.002)
     * - Standard conditions: 15°C = 1.0
     * - Cold air (0°C): 1.03 (3% denser)
     * - Warm air (30°C): 0.97 (3% thinner)
     *
     * Note: Humidity has minimal effect and is incorporated into the temperature proxy.
     *
     * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
     * Plan reference: live-caddy-mode-plan.md Task 7
     *
     * @return Air density proxy (1.0 = standard conditions at 15°C)
     */
    fun airDensityProxy(): Double {
        // Standard: 15°C = 1.0, colder = denser (>1.0), warmer = less dense (<1.0)
        return 1.0 + ((15.0 - temperatureCelsius) * 0.002)
    }

    /**
     * Calculate overall conditions adjustment for carry distance.
     *
     * This is a convenience method that creates a ConditionsCalculator instance
     * and delegates to it. For production use, prefer injecting ConditionsCalculator
     * and calling calculateAdjustment directly to avoid creating calculator instances.
     *
     * Note: This method requires additional context (target bearing, base carry)
     * that aren't properties of WeatherData itself. It's here for convenience,
     * but production code should use ConditionsCalculator directly.
     *
     * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
     * Plan reference: live-caddy-mode-plan.md Task 7
     *
     * @param targetBearing Shot direction in degrees (0-360, 0 = North)
     * @param baseCarryMeters Expected carry distance in standard conditions
     * @return ConditionsAdjustment with carry modifier and explanation
     */
    fun conditionsAdjustment(targetBearing: Int, baseCarryMeters: Int): ConditionsAdjustment {
        // Create temporary calculator instance (not ideal, but preserves API)
        // Production code should inject ConditionsCalculator instead
        val calculator = caddypro.domain.caddy.services.ConditionsCalculator()
        return calculator.calculateAdjustment(this, targetBearing, baseCarryMeters)
    }
}

/**
 * Wind components decomposed relative to a target bearing.
 *
 * @property headwind Wind component along target line in m/s (positive = tailwind/helping, negative = headwind/hurting)
 * @property crosswind Wind component perpendicular to target line in m/s (positive = right-to-left, negative = left-to-right)
 */
data class WindComponent(
    val headwind: Double,
    val crosswind: Double
)

/**
 * Conditions-based adjustment to carry distance.
 *
 * @property carryModifier Multiplier for carry distance (e.g., 0.95 = 5% less carry, 1.05 = 5% more carry)
 * @property reason Human-readable explanation of the adjustment
 */
data class ConditionsAdjustment(
    val carryModifier: Double,
    val reason: String
) {
    init {
        require(carryModifier > 0) { "Carry modifier must be positive" }
    }
}
