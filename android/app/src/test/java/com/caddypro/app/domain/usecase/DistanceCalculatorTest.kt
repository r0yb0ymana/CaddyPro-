package com.caddypro.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AC15: Haversine formula used for distance calculation
 */
class DistanceCalculatorTest {

    private lateinit var calculator: DistanceCalculator

    @Before
    fun setup() {
        calculator = DistanceCalculator()
    }

    @Test
    fun `same point returns zero distance`() {
        val meters = calculator.distanceMeters(-37.8136, 144.9631, -37.8136, 144.9631)
        assertEquals(0.0, meters, 0.01)
    }

    @Test
    fun `same point returns zero yards`() {
        val yards = calculator.distanceYards(-37.8136, 144.9631, -37.8136, 144.9631)
        assertEquals(0, yards)
    }

    @Test
    fun `known distance Melbourne CBD to St Kilda approximately 5km`() {
        // Melbourne CBD: -37.8136, 144.9631
        // St Kilda: -37.8599, 144.9764
        val meters = calculator.distanceMeters(-37.8136, 144.9631, -37.8599, 144.9764)
        // Should be approximately 5200m
        assertTrue("Distance should be around 5200m, was $meters", meters > 4800 && meters < 5600)
    }

    @Test
    fun `distance in yards is approximately 1x09 meters`() {
        val lat1 = -37.8136
        val lon1 = 144.9631
        val lat2 = -37.8200
        val lon2 = 144.9700

        val meters = calculator.distanceMeters(lat1, lon1, lat2, lon2)
        val yards = calculator.distanceYards(lat1, lon1, lat2, lon2)
        val expectedYards = (meters * 1.09361).toInt()

        // Allow +-1 yard for rounding
        assertTrue("Yards ($yards) should be close to $expectedYards",
            kotlin.math.abs(yards - expectedYards) <= 1)
    }

    @Test
    fun `distanceMetersRounded returns integer`() {
        val rounded = calculator.distanceMetersRounded(-37.8136, 144.9631, -37.8200, 144.9700)
        assertTrue("Should be a positive integer", rounded > 0)
    }

    @Test
    fun `typical golf distance 150m green shot`() {
        // Approximate: 150m is about 0.00135 degrees latitude
        val lat1 = -37.8136
        val lon1 = 144.9631
        val lat2 = lat1 + 0.00135
        val lon2 = lon1

        val meters = calculator.distanceMetersRounded(lat1, lon1, lat2, lon2)
        // Should be approximately 150m
        assertTrue("Distance should be ~150m, was $meters", meters in 140..160)
    }

    @Test
    fun `typical golf distance 200 yards`() {
        // 200 yards ≈ 183m ≈ 0.00165 degrees latitude
        val lat1 = -37.8136
        val lon1 = 144.9631
        val lat2 = lat1 + 0.00165
        val lon2 = lon1

        val yards = calculator.distanceYards(lat1, lon1, lat2, lon2)
        assertTrue("Distance should be ~200yds, was $yards", yards in 190..210)
    }

    @Test
    fun `distance is symmetric`() {
        val lat1 = -37.8136
        val lon1 = 144.9631
        val lat2 = -37.8200
        val lon2 = 144.9700

        val forward = calculator.distanceMeters(lat1, lon1, lat2, lon2)
        val reverse = calculator.distanceMeters(lat2, lon2, lat1, lon1)
        assertEquals(forward, reverse, 0.01)
    }
}
