package caddypro.domain.caddy.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for WeatherData model and calculation methods.
 *
 * Verifies:
 * - Wind component decomposition (headwind/crosswind calculation)
 * - Air density proxy calculation
 * - Conditions adjustment integration
 * - Input validation
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 2, Task 7
 * Acceptance criteria: A1 (Weather HUD with conditions adjustment)
 */
class WeatherDataTest {

    private val testLocation = Location(latitude = 37.7749, longitude = -122.4194)

    // ===========================
    // Wind Component Tests
    // ===========================

    @Test
    fun `windComponent with wind from north target north produces tailwind`() {
        // Given: Wind from North (0°), shooting North (0°)
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 0,     // Wind FROM North
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // When
        val component = weather.windComponent(targetBearing = 0)

        // Then: Should be a tailwind (positive headwind)
        assertTrue("Should have tailwind (positive headwind)", component.headwind > 4.9)
        assertEquals("Crosswind should be minimal", 0.0, component.crosswind, 0.1)
    }

    @Test
    fun `windComponent with wind from south target north produces headwind`() {
        // Given: Wind from South (180°), shooting North (0°)
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 180,   // Wind FROM South
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // When
        val component = weather.windComponent(targetBearing = 0)

        // Then: Should be a headwind (negative headwind)
        assertTrue("Should have headwind (negative headwind)", component.headwind < -4.9)
        assertEquals("Crosswind should be minimal", 0.0, component.crosswind, 0.1)
    }

    @Test
    fun `windComponent with wind from east target north produces right to left crosswind`() {
        // Given: Wind from East (90°), shooting North (0°)
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 90,    // Wind FROM East
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // When
        val component = weather.windComponent(targetBearing = 0)

        // Then: Should be a right-to-left crosswind (positive crosswind)
        assertEquals("Headwind should be minimal", 0.0, component.headwind, 0.1)
        assertTrue("Should have right-to-left crosswind", component.crosswind > 4.9)
    }

    @Test
    fun `windComponent with wind from west target north produces left to right crosswind`() {
        // Given: Wind from West (270°), shooting North (0°)
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 270,   // Wind FROM West
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // When
        val component = weather.windComponent(targetBearing = 0)

        // Then: Should be a left-to-right crosswind (negative crosswind)
        assertEquals("Headwind should be minimal", 0.0, component.headwind, 0.1)
        assertTrue("Should have left-to-right crosswind", component.crosswind < -4.9)
    }

    @Test
    fun `windComponent with 45 degree angle produces mixed components`() {
        // Given: Wind from Northeast (45°), shooting North (0°)
        val weather = WeatherData(
            windSpeedMps = 10.0,
            windDegrees = 45,
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // When
        val component = weather.windComponent(targetBearing = 0)

        // Then: Should have both headwind and crosswind components
        // At 45°, cos(45°) ≈ sin(45°) ≈ 0.707
        assertTrue("Should have positive headwind component", component.headwind > 0)
        assertTrue("Should have positive crosswind component", component.crosswind > 0)
        assertEquals("Headwind should be ~7.1 m/s", 7.1, component.headwind, 0.5)
        assertEquals("Crosswind should be ~7.1 m/s", 7.1, component.crosswind, 0.5)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `windComponent with invalid target bearing throws exception`() {
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 0,
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        weather.windComponent(targetBearing = 361) // Invalid
    }

    // ===========================
    // Air Density Proxy Tests
    // ===========================

    @Test
    fun `airDensityProxy at standard temp returns 1point0`() {
        // Given: Standard temperature (15°C)
        val weather = WeatherData(
            windSpeedMps = 0.0,
            windDegrees = 0,
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // When
        val density = weather.airDensityProxy()

        // Then
        assertEquals("Standard temp should give density 1.0", 1.0, density, 0.01)
    }

    @Test
    fun `airDensityProxy at cold temp returns greater than 1point0`() {
        // Given: Cold temperature (0°C)
        val weather = WeatherData(
            windSpeedMps = 0.0,
            windDegrees = 0,
            temperatureCelsius = 0.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // When
        val density = weather.airDensityProxy()

        // Then: 1.0 + ((15 - 0) * 0.002) = 1.03
        assertEquals("Cold temp should give density > 1.0", 1.03, density, 0.01)
    }

    @Test
    fun `airDensityProxy at warm temp returns less than 1point0`() {
        // Given: Warm temperature (30°C)
        val weather = WeatherData(
            windSpeedMps = 0.0,
            windDegrees = 0,
            temperatureCelsius = 30.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // When
        val density = weather.airDensityProxy()

        // Then: 1.0 + ((15 - 30) * 0.002) = 0.97
        assertEquals("Warm temp should give density < 1.0", 0.97, density, 0.01)
    }

    // ===========================
    // Conditions Adjustment Tests
    // ===========================

    @Test
    fun `conditionsAdjustment integrates with ConditionsCalculator`() {
        // Given: Weather with headwind and cold temp
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 0,
            temperatureCelsius = 0.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // When
        val adjustment = weather.conditionsAdjustment(
            targetBearing = 0,
            baseCarryMeters = 100
        )

        // Then: Should reduce carry
        assertTrue("Headwind + cold should reduce carry", adjustment.carryModifier < 1.0)
        assertTrue("Should have a reason", adjustment.reason.isNotEmpty())
        assertTrue("Reason should mention conditions", adjustment.reason.contains("m"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `conditionsAdjustment with invalid bearing throws exception`() {
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 0,
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        weather.conditionsAdjustment(
            targetBearing = 361,
            baseCarryMeters = 100
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `conditionsAdjustment with invalid carry throws exception`() {
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 0,
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        weather.conditionsAdjustment(
            targetBearing = 0,
            baseCarryMeters = -50
        )
    }

    // ===========================
    // Input Validation Tests
    // ===========================

    @Test(expected = IllegalArgumentException::class)
    fun `WeatherData with negative wind speed throws exception`() {
        WeatherData(
            windSpeedMps = -5.0,
            windDegrees = 0,
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WeatherData with invalid wind direction throws exception`() {
        WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 360,  // Must be 0-359
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WeatherData with invalid humidity throws exception`() {
        WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 0,
            temperatureCelsius = 15.0,
            humidity = 101,  // Must be 0-100
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WeatherData with invalid timestamp throws exception`() {
        WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 0,
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = -1,  // Must be positive
            location = testLocation
        )
    }

    @Test
    fun `WeatherData accepts valid extreme temperatures`() {
        // Cold
        val cold = WeatherData(
            windSpeedMps = 0.0,
            windDegrees = 0,
            temperatureCelsius = -20.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )
        assertEquals(-20.0, cold.temperatureCelsius, 0.01)

        // Hot
        val hot = WeatherData(
            windSpeedMps = 0.0,
            windDegrees = 0,
            temperatureCelsius = 45.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )
        assertEquals(45.0, hot.temperatureCelsius, 0.01)
    }

    @Test
    fun `WeatherData accepts valid edge case values`() {
        val weather = WeatherData(
            windSpeedMps = 0.0,          // Minimum wind
            windDegrees = 0,              // Minimum direction
            temperatureCelsius = 0.0,     // Freezing
            humidity = 0,                 // Minimum humidity
            timestamp = 1,                // Minimum timestamp
            location = testLocation
        )

        assertEquals(0.0, weather.windSpeedMps, 0.01)
        assertEquals(0, weather.windDegrees)
        assertEquals(0.0, weather.temperatureCelsius, 0.01)
        assertEquals(0, weather.humidity)
    }

    // ===========================
    // Integration Tests
    // ===========================

    @Test
    fun `full workflow from weather to adjustment works correctly`() {
        // Scenario: Perfect tailwind conditions (warm air, strong tailwind)
        val weather = WeatherData(
            windSpeedMps = 8.0,
            windDegrees = 180,         // Wind from South
            temperatureCelsius = 25.0, // Warm
            humidity = 40,
            timestamp = System.currentTimeMillis(),
            location = testLocation
        )

        // Step 1: Decompose wind
        val wind = weather.windComponent(targetBearing = 0)
        assertTrue("Should have strong tailwind", wind.headwind > 7.9)

        // Step 2: Check air density
        val density = weather.airDensityProxy()
        assertTrue("Warm air should be less dense", density < 1.0)

        // Step 3: Calculate adjustment
        val adjustment = weather.conditionsAdjustment(
            targetBearing = 0,
            baseCarryMeters = 150
        )

        // Then: Should significantly increase carry
        assertTrue("Warm tailwind should increase carry", adjustment.carryModifier > 1.05)
        assertTrue("Reason should mention warm", adjustment.reason.contains("warm"))
        assertTrue("Reason should mention tailwind", adjustment.reason.contains("tailwind"))
    }
}
