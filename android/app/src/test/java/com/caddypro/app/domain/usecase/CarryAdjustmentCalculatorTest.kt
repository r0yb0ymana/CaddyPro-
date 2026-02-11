package com.caddypro.app.domain.usecase

import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias
import com.caddypro.app.domain.model.ShotDirection
import com.caddypro.app.domain.model.WeatherData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CarryAdjustmentCalculator
 *
 * Tests each adjustment factor and combined calculations.
 */
class CarryAdjustmentCalculatorTest {

    private lateinit var calculator: CarryAdjustmentCalculator

    private val baseWeather = WeatherData(
        temperatureC = 20.0, // baseline
        windSpeedKmh = 0.0,
        windDirectionDeg = 0,
        humidity = 50, // baseline
        conditionCode = 800,
        conditionDescription = "clear sky",
        feelsLikeC = 20.0,
        pressureHpa = 1013
    )

    private val testClub = Club(
        bagId = "bag1",
        name = "7 Iron",
        type = ClubType.IRON,
        carryDistance = 150,
        totalDistance = 160,
        missBias = MissBias.STRAIGHT
    )

    @Before
    fun setup() {
        calculator = CarryAdjustmentCalculator()
    }

    // Temperature Factor Tests

    @Test
    fun `temperature factor returns 1 at baseline 20C`() {
        val factor = calculator.temperatureFactor(20.0)
        assertEquals(1.0, factor, 0.001)
    }

    @Test
    fun `temperature factor increases with higher temperature`() {
        val factor = calculator.temperatureFactor(30.0) // 10C above baseline
        assertTrue("Factor should be > 1 for warm temps", factor > 1.0)
    }

    @Test
    fun `temperature factor decreases with lower temperature`() {
        val factor = calculator.temperatureFactor(10.0) // 10C below baseline
        assertTrue("Factor should be < 1 for cold temps", factor < 1.0)
    }

    @Test
    fun `temperature factor is approximately 1 yard per 5C per 150yds`() {
        // At 25C (5C above baseline), for 150yd club: ~1yd gain
        val factor = calculator.temperatureFactor(25.0)
        val adjustedCarry = 150 * factor
        val delta = adjustedCarry - 150
        assertTrue("Delta should be ~1yd for 5C above baseline", delta in 0.5..2.0)
    }

    // Altitude Factor Tests

    @Test
    fun `altitude factor returns 1 at sea level`() {
        val factor = calculator.altitudeFactor(0.0)
        assertEquals(1.0, factor, 0.001)
    }

    @Test
    fun `altitude factor returns 1 for negative altitude`() {
        val factor = calculator.altitudeFactor(-50.0)
        assertEquals(1.0, factor, 0.001)
    }

    @Test
    fun `altitude factor increases with elevation`() {
        val factor = calculator.altitudeFactor(300.0) // 300m above sea level
        assertTrue("Factor should be > 1 at altitude", factor > 1.0)
        // ~2% increase per 300m
        assertEquals(1.02, factor, 0.001)
    }

    @Test
    fun `altitude factor is approximately 2 percent per 300m`() {
        val factor600 = calculator.altitudeFactor(600.0)
        assertEquals(1.04, factor600, 0.001)
    }

    // Wind Factor Tests

    @Test
    fun `wind factor returns 1 with no wind`() {
        val factor = calculator.windFactor(0.0, 0, ShotDirection.N)
        assertEquals(1.0, factor, 0.001)
    }

    @Test
    fun `direct headwind reduces carry`() {
        // Wind from N (0 deg) blowing south, shot going N = headwind
        val factor = calculator.windFactor(20.0, 0, ShotDirection.N)
        assertTrue("Headwind should reduce carry (factor < 1)", factor < 1.0)
    }

    @Test
    fun `direct tailwind increases carry`() {
        // Wind from S (180 deg) blowing north, shot going N = tailwind
        val factor = calculator.windFactor(20.0, 180, ShotDirection.N)
        assertTrue("Tailwind should increase carry (factor > 1)", factor > 1.0)
    }

    @Test
    fun `headwind effect is stronger than tailwind`() {
        val headwindFactor = calculator.windFactor(20.0, 0, ShotDirection.N)
        val tailwindFactor = calculator.windFactor(20.0, 180, ShotDirection.N)

        val headwindDelta = Math.abs(1.0 - headwindFactor)
        val tailwindDelta = Math.abs(tailwindFactor - 1.0)

        assertTrue("Headwind effect should be stronger than tailwind", headwindDelta > tailwindDelta)
    }

    @Test
    fun `crosswind has minimal carry effect`() {
        // Wind from E (90 deg), shot going N = pure crosswind
        val factor = calculator.windFactor(20.0, 90, ShotDirection.N)
        // Should be close to 1.0 (minimal carry effect)
        assertEquals("Crosswind should have minimal carry effect", 1.0, factor, 0.02)
    }

    // Humidity Factor Tests

    @Test
    fun `humidity factor returns 1 at baseline 50 percent`() {
        val factor = calculator.humidityFactor(50)
        assertEquals(1.0, factor, 0.001)
    }

    @Test
    fun `high humidity slightly increases carry`() {
        val factor = calculator.humidityFactor(100)
        assertTrue("High humidity should slightly increase carry", factor > 1.0)
        // Effect should be very small
        assertTrue("Humidity effect should be negligible", factor < 1.01)
    }

    @Test
    fun `low humidity slightly decreases carry`() {
        val factor = calculator.humidityFactor(0)
        assertTrue("Low humidity should slightly decrease carry", factor < 1.0)
    }

    // AC9: Putter Excluded

    @Test
    fun `putter returns zero adjustment`() {
        val putter = Club(
            bagId = "bag1",
            name = "Putter",
            type = ClubType.PUTTER,
            carryDistance = 0,
            totalDistance = 0
        )
        val hotWindyWeather = baseWeather.copy(
            temperatureC = 35.0,
            windSpeedKmh = 30.0,
            windDirectionDeg = 0,
            humidity = 90
        )
        val result = calculator.calculateAdjustment(putter, hotWindyWeather, 500.0, ShotDirection.N)

        assertEquals("Putter should have no adjustment", 0, result.delta)
        assertEquals("Putter adjusted should equal base", result.baseCarry, result.adjustedCarry)
    }

    // Combined Calculation Tests

    @Test
    fun `calculate adjustment with all baseline values returns no change`() {
        val result = calculator.calculateAdjustment(
            club = testClub,
            weather = baseWeather,
            altitudeM = 0.0,
            shotDirection = ShotDirection.N
        )

        assertEquals("Base carry should be 150", 150, result.baseCarry)
        assertEquals("No adjustment at baseline", 0, result.delta)
        assertEquals("Adjusted should equal base", 150, result.adjustedCarry)
    }

    @Test
    fun `hot weather increases adjusted carry`() {
        val hotWeather = baseWeather.copy(temperatureC = 35.0)
        val result = calculator.calculateAdjustment(
            club = testClub,
            weather = hotWeather,
            altitudeM = 0.0,
            shotDirection = ShotDirection.N
        )

        assertTrue("Hot weather should increase carry", result.adjustedCarry > 150)
        assertTrue("Temperature effect should be positive", result.temperatureEffect > 0)
    }

    @Test
    fun `cold weather with headwind decreases adjusted carry`() {
        val coldHeadwindWeather = baseWeather.copy(
            temperatureC = 5.0,
            windSpeedKmh = 25.0,
            windDirectionDeg = 0 // Wind from N = headwind if hitting N
        )
        val result = calculator.calculateAdjustment(
            club = testClub,
            weather = coldHeadwindWeather,
            altitudeM = 0.0,
            shotDirection = ShotDirection.N
        )

        assertTrue("Cold + headwind should decrease carry", result.adjustedCarry < 150)
        assertTrue("Delta should be negative", result.delta < 0)
    }

    @Test
    fun `calculateAdjustments processes all clubs`() {
        val clubs = listOf(
            testClub,
            testClub.copy(name = "8 Iron", carryDistance = 135, totalDistance = 145),
            testClub.copy(name = "Putter", type = ClubType.PUTTER, carryDistance = 0, totalDistance = 0)
        )
        val results = calculator.calculateAdjustments(clubs, baseWeather, 0.0, ShotDirection.N)

        assertEquals("Should return adjustment for each club", 3, results.size)
        assertEquals("7 Iron", results[0].clubName)
        assertEquals("8 Iron", results[1].clubName)
        assertEquals("Putter", results[2].clubName)
    }

    @Test
    fun `club with zero carry returns zero adjustment`() {
        val zeroCarryClub = testClub.copy(carryDistance = 0)
        val result = calculator.calculateAdjustment(
            club = zeroCarryClub,
            weather = baseWeather.copy(temperatureC = 35.0),
            altitudeM = 500.0,
            shotDirection = ShotDirection.N
        )

        assertEquals("Zero carry should have no adjustment", 0, result.delta)
    }

    @Test
    fun `high altitude Melbourne course has small effect`() {
        // Melbourne courses: ~30-80m elevation
        val result = calculator.calculateAdjustment(
            club = testClub,
            weather = baseWeather,
            altitudeM = 50.0, // Typical Melbourne elevation
            shotDirection = ShotDirection.N
        )

        // At 50m, effect should be very small (< 1 yard for 150yd club)
        assertTrue("Melbourne altitude effect should be minimal", Math.abs(result.delta) <= 1)
    }
}
