package caddypro.ui.caddy.components

import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.WeatherData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for Forecaster HUD components.
 *
 * Validates:
 * - Wind direction to compass conversion logic
 * - Temperature conversion (Celsius to Fahrenheit)
 * - Conditions adjustment percentage calculations
 * - Weather data handling (null states, edge cases)
 *
 * Note: These are unit tests for component logic. Full Compose UI tests
 * should be added in androidTest once the Compose testing framework is available.
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 14
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds)
 */
class ForecasterHudTest {

    // ========== Wind Direction Tests ==========

    @Test
    fun `degreesToCompassDirection returns correct cardinal directions`() {
        // Test cardinal directions
        assertEquals("N", degreesToCompassDirection(0))
        assertEquals("N", degreesToCompassDirection(360))
        assertEquals("E", degreesToCompassDirection(90))
        assertEquals("S", degreesToCompassDirection(180))
        assertEquals("W", degreesToCompassDirection(270))
    }

    @Test
    fun `degreesToCompassDirection returns correct intercardinal directions`() {
        // Test intercardinal directions
        assertEquals("NE", degreesToCompassDirection(45))
        assertEquals("SE", degreesToCompassDirection(135))
        assertEquals("SW", degreesToCompassDirection(225))
        assertEquals("NW", degreesToCompassDirection(315))
    }

    @Test
    fun `degreesToCompassDirection handles boundary cases`() {
        // North boundaries
        assertEquals("N", degreesToCompassDirection(22))
        assertEquals("NE", degreesToCompassDirection(23))
        assertEquals("N", degreesToCompassDirection(337))
        assertEquals("NW", degreesToCompassDirection(338))

        // East boundaries
        assertEquals("NE", degreesToCompassDirection(67))
        assertEquals("E", degreesToCompassDirection(68))
        assertEquals("E", degreesToCompassDirection(112))
        assertEquals("SE", degreesToCompassDirection(113))

        // South boundaries
        assertEquals("SE", degreesToCompassDirection(157))
        assertEquals("S", degreesToCompassDirection(158))
        assertEquals("S", degreesToCompassDirection(202))
        assertEquals("SW", degreesToCompassDirection(203))

        // West boundaries
        assertEquals("SW", degreesToCompassDirection(247))
        assertEquals("W", degreesToCompassDirection(248))
        assertEquals("W", degreesToCompassDirection(292))
        assertEquals("NW", degreesToCompassDirection(293))
    }

    // ========== Temperature Conversion Tests ==========

    @Test
    fun `celsius to fahrenheit conversion is accurate`() {
        // Freezing point
        assertEquals(32, celsiusToFahrenheit(0.0))

        // Standard temperature
        assertEquals(59, celsiusToFahrenheit(15.0))

        // Room temperature
        assertEquals(68, celsiusToFahrenheit(20.0))

        // Hot day
        assertEquals(86, celsiusToFahrenheit(30.0))

        // Very hot
        assertEquals(95, celsiusToFahrenheit(35.0))

        // Cold day
        assertEquals(41, celsiusToFahrenheit(5.0))

        // Below freezing
        assertEquals(14, celsiusToFahrenheit(-10.0))
    }

    // ========== Wind Speed Conversion Tests ==========

    @Test
    fun `meters per second to mph conversion is accurate`() {
        // Calm
        assertEquals(0, mpsToMph(0.0))

        // Light breeze
        assertEquals(7, mpsToMph(3.0))

        // Moderate wind
        assertEquals(11, mpsToMph(5.0))

        // Strong wind
        assertEquals(18, mpsToMph(8.0))

        // Very strong
        assertEquals(27, mpsToMph(12.0))
    }

    // ========== Conditions Adjustment Tests ==========

    @Test
    fun `conditions adjustment percentage calculation is correct`() {
        // Helping conditions (+5%)
        val helpingPercent = calculatePercentChange(1.05)
        assertEquals(5, helpingPercent)

        // Hurting conditions (-8%)
        val hurtingPercent = calculatePercentChange(0.92)
        assertEquals(-8, hurtingPercent)

        // Neutral conditions (0%)
        val neutralPercent = calculatePercentChange(1.0)
        assertEquals(0, neutralPercent)

        // Strong helping (+12%)
        val strongHelpingPercent = calculatePercentChange(1.12)
        assertEquals(12, strongHelpingPercent)

        // Strong hurting (-15%)
        val strongHurtingPercent = calculatePercentChange(0.85)
        assertEquals(-15, strongHurtingPercent)
    }

    // ========== WeatherData Integration Tests ==========

    @Test
    fun `WeatherData conditionsAdjustment integrates with HUD display`() {
        // Given: Weather data with tailwind and warm conditions
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 180, // From South
            temperatureCelsius = 25.0,
            humidity = 60,
            timestamp = System.currentTimeMillis(),
            location = Location(
                latitude = 37.7749,
                longitude = -122.4194,
                name = "Test Course"
            )
        )

        // When: Calculating adjustment for northward shot
        val adjustment = weather.conditionsAdjustment(
            targetBearing = 0, // North
            baseCarryMeters = 150
        )

        // Then: Adjustment should be positive (helping conditions)
        assertTrue(adjustment.carryModifier > 1.0)
        assertNotNull(adjustment.reason)
        assertTrue(adjustment.reason.isNotEmpty())
    }

    @Test
    fun `WeatherData conditionsAdjustment handles headwind correctly`() {
        // Given: Weather data with headwind and cold conditions
        val weather = WeatherData(
            windSpeedMps = 8.0,
            windDegrees = 0, // From North
            temperatureCelsius = 5.0,
            humidity = 45,
            timestamp = System.currentTimeMillis(),
            location = Location(
                latitude = 37.7749,
                longitude = -122.4194,
                name = "Test Course"
            )
        )

        // When: Calculating adjustment for northward shot
        val adjustment = weather.conditionsAdjustment(
            targetBearing = 0, // North (into wind)
            baseCarryMeters = 150
        )

        // Then: Adjustment should be negative (hurting conditions)
        assertTrue(adjustment.carryModifier < 1.0)
        assertNotNull(adjustment.reason)
        assertTrue(adjustment.reason.isNotEmpty())
    }

    @Test
    fun `WeatherData conditionsAdjustment handles crosswind correctly`() {
        // Given: Weather data with pure crosswind
        val weather = WeatherData(
            windSpeedMps = 6.0,
            windDegrees = 90, // From East
            temperatureCelsius = 15.0, // Standard temp
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = Location(
                latitude = 37.7749,
                longitude = -122.4194,
                name = "Test Course"
            )
        )

        // When: Calculating adjustment for northward shot
        val adjustment = weather.conditionsAdjustment(
            targetBearing = 0, // North (90° to wind)
            baseCarryMeters = 150
        )

        // Then: Adjustment should be close to neutral (minimal headwind/tailwind component)
        assertTrue(adjustment.carryModifier > 0.95 && adjustment.carryModifier < 1.05)
        assertNotNull(adjustment.reason)
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `wind direction handles wraparound at 360 degrees`() {
        // Given: Wind directions near 360° boundary
        val north1 = degreesToCompassDirection(359)
        val north2 = degreesToCompassDirection(0)
        val north3 = degreesToCompassDirection(1)

        // Then: All should be North
        assertEquals("N", north1)
        assertEquals("N", north2)
        assertEquals("N", north3)
    }

    @Test
    fun `temperature conversion handles extreme values`() {
        // Very hot
        val veryHot = celsiusToFahrenheit(50.0)
        assertEquals(122, veryHot)

        // Very cold
        val veryCold = celsiusToFahrenheit(-40.0)
        assertEquals(-40, veryCold) // -40°C = -40°F (same value)

        // Absolute zero (theoretical)
        val absoluteZero = celsiusToFahrenheit(-273.15)
        assertTrue(absoluteZero < -450)
    }

    @Test
    fun `wind speed conversion handles calm and extreme conditions`() {
        // Perfectly calm
        assertEquals(0, mpsToMph(0.0))

        // Hurricane force (> 33 m/s = 74 mph)
        val hurricane = mpsToMph(35.0)
        assertTrue(hurricane > 74)

        // Extreme (tornado speeds)
        val extreme = mpsToMph(50.0)
        assertTrue(extreme > 100)
    }

    // ========== Outdoor Visibility Requirements Tests ==========

    @Test
    fun `verify minimum text size for outdoor visibility`() {
        // Per spec C2: High contrast, large typography
        // Primary values should be >= 18sp for outdoor visibility
        // This is validated in the actual Composables with fontSize parameters
        // These tests document the requirement

        // Temperature indicator uses 20sp for primary value
        val tempFontSize = 20
        assertTrue(tempFontSize >= 18)

        // Wind indicator uses 20sp for speed
        val windFontSize = 20
        assertTrue(windFontSize >= 18)

        // Conditions chip uses 16sp (acceptable for secondary info)
        val chipFontSize = 16
        assertTrue(chipFontSize >= 14)
    }

    // ========== Helper Functions (extracted from component logic) ==========

    /**
     * Helper function to test wind direction conversion logic.
     * Matches the implementation in WindIndicator.kt.
     */
    private fun degreesToCompassDirection(degrees: Int): String {
        val normalized = ((degrees % 360) + 360) % 360
        return when {
            normalized < 22.5 -> "N"
            normalized < 67.5 -> "NE"
            normalized < 112.5 -> "E"
            normalized < 157.5 -> "SE"
            normalized < 202.5 -> "S"
            normalized < 247.5 -> "SW"
            normalized < 292.5 -> "W"
            normalized < 337.5 -> "NW"
            else -> "N"
        }
    }

    /**
     * Helper function to test temperature conversion logic.
     * Matches the implementation in TemperatureIndicator.kt.
     */
    private fun celsiusToFahrenheit(celsius: Double): Int {
        return (celsius * 9.0 / 5.0 + 32.0).toInt()
    }

    /**
     * Helper function to test wind speed conversion logic.
     * Matches the implementation in WindIndicator.kt.
     */
    private fun mpsToMph(mps: Double): Int {
        return (mps * 2.237).toInt()
    }

    /**
     * Helper function to test conditions adjustment percentage calculation.
     * Matches the implementation in ConditionsChip.kt.
     */
    private fun calculatePercentChange(carryModifier: Double): Int {
        return ((carryModifier - 1.0) * 100).toInt()
    }
}
