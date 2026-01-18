package caddypro.domain.navcaddy.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for PatternDecayCalculator.
 *
 * Tests decay calculation, confidence adjustment, and retention window checks.
 */
class PatternDecayCalculatorTest {

    private lateinit var calculator: PatternDecayCalculator

    private val currentTime = 1704067200000L // 2024-01-01 00:00:00 UTC
    private val oneDayMillis = 24 * 60 * 60 * 1000L

    @Before
    fun setup() {
        calculator = PatternDecayCalculator()
    }

    // ========================================================================
    // Decay Calculation Tests
    // ========================================================================

    @Test
    fun `calculateDecay returns 1_0 for current time`() {
        val decay = calculator.calculateDecay(
            shotTimestamp = currentTime,
            currentTime = currentTime
        )
        assertEquals(1.0f, decay, 0.001f)
    }

    @Test
    fun `calculateDecay returns 0_5 after one half-life`() {
        val halfLife = 14 // days
        val shotTime = currentTime - (halfLife * oneDayMillis)

        val decay = calculator.calculateDecay(
            shotTimestamp = shotTime,
            currentTime = currentTime
        )

        assertEquals(0.5f, decay, 0.01f)
    }

    @Test
    fun `calculateDecay returns 0_25 after two half-lives`() {
        val halfLife = 14 // days
        val shotTime = currentTime - (2 * halfLife * oneDayMillis)

        val decay = calculator.calculateDecay(
            shotTimestamp = shotTime,
            currentTime = currentTime
        )

        assertEquals(0.25f, decay, 0.01f)
    }

    @Test
    fun `calculateDecay returns near-zero after six half-lives`() {
        val halfLife = 14 // days
        val shotTime = currentTime - (6 * halfLife * oneDayMillis)

        val decay = calculator.calculateDecay(
            shotTimestamp = shotTime,
            currentTime = currentTime
        )

        assertTrue("Decay should be less than 0.02", decay < 0.02f)
    }

    @Test
    fun `calculateDecay returns 0 for shots beyond max age`() {
        val maxAge = 84 // days (6 half-lives)
        val shotTime = currentTime - ((maxAge + 1) * oneDayMillis)

        val decay = calculator.calculateDecay(
            shotTimestamp = shotTime,
            currentTime = currentTime
        )

        assertEquals(0.0f, decay, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateDecay throws for future timestamps`() {
        calculator.calculateDecay(
            shotTimestamp = currentTime + oneDayMillis,
            currentTime = currentTime
        )
    }

    // ========================================================================
    // Decayed Confidence Tests
    // ========================================================================

    @Test
    fun `calculateDecayedConfidence preserves confidence for recent shots`() {
        val baseConfidence = 0.8f
        val decayedConfidence = calculator.calculateDecayedConfidence(
            baseConfidence = baseConfidence,
            lastOccurrence = currentTime,
            currentTime = currentTime
        )

        assertEquals(baseConfidence, decayedConfidence, 0.001f)
    }

    @Test
    fun `calculateDecayedConfidence halves confidence after one half-life`() {
        val baseConfidence = 0.8f
        val halfLife = 14 // days
        val lastOccurrence = currentTime - (halfLife * oneDayMillis)

        val decayedConfidence = calculator.calculateDecayedConfidence(
            baseConfidence = baseConfidence,
            lastOccurrence = lastOccurrence,
            currentTime = currentTime
        )

        assertEquals(0.4f, decayedConfidence, 0.01f)
    }

    @Test
    fun `calculateDecayedConfidence reduces confidence to near-zero for old shots`() {
        val baseConfidence = 0.9f
        val maxAge = 90 // days
        val lastOccurrence = currentTime - (maxAge * oneDayMillis)

        val decayedConfidence = calculator.calculateDecayedConfidence(
            baseConfidence = baseConfidence,
            lastOccurrence = lastOccurrence,
            currentTime = currentTime
        )

        assertTrue("Decayed confidence should be less than 0.01", decayedConfidence < 0.01f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateDecayedConfidence throws for confidence above 1`() {
        calculator.calculateDecayedConfidence(
            baseConfidence = 1.5f,
            lastOccurrence = currentTime,
            currentTime = currentTime
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calculateDecayedConfidence throws for negative confidence`() {
        calculator.calculateDecayedConfidence(
            baseConfidence = -0.1f,
            lastOccurrence = currentTime,
            currentTime = currentTime
        )
    }

    // ========================================================================
    // Age Calculation Tests
    // ========================================================================

    @Test
    fun `calculateAgeDays returns 0 for current time`() {
        val age = calculator.calculateAgeDays(
            timestamp = currentTime,
            currentTime = currentTime
        )
        assertEquals(0.0, age, 0.001)
    }

    @Test
    fun `calculateAgeDays returns 1 for one day ago`() {
        val timestamp = currentTime - oneDayMillis
        val age = calculator.calculateAgeDays(
            timestamp = timestamp,
            currentTime = currentTime
        )
        assertEquals(1.0, age, 0.001)
    }

    @Test
    fun `calculateAgeDays returns 30 for thirty days ago`() {
        val timestamp = currentTime - (30 * oneDayMillis)
        val age = calculator.calculateAgeDays(
            timestamp = timestamp,
            currentTime = currentTime
        )
        assertEquals(30.0, age, 0.001)
    }

    @Test
    fun `calculateAgeDays handles partial days`() {
        val halfDay = oneDayMillis / 2
        val timestamp = currentTime - halfDay
        val age = calculator.calculateAgeDays(
            timestamp = timestamp,
            currentTime = currentTime
        )
        assertEquals(0.5, age, 0.001)
    }

    // ========================================================================
    // Retention Window Tests
    // ========================================================================

    @Test
    fun `isWithinRetentionWindow returns true for recent timestamp`() {
        val timestamp = currentTime - (10 * oneDayMillis)
        val isWithin = calculator.isWithinRetentionWindow(
            timestamp = timestamp,
            retentionDays = 90,
            currentTime = currentTime
        )
        assertTrue(isWithin)
    }

    @Test
    fun `isWithinRetentionWindow returns true for timestamp at boundary`() {
        val timestamp = currentTime - (89 * oneDayMillis)
        val isWithin = calculator.isWithinRetentionWindow(
            timestamp = timestamp,
            retentionDays = 90,
            currentTime = currentTime
        )
        assertTrue(isWithin)
    }

    @Test
    fun `isWithinRetentionWindow returns false for old timestamp`() {
        val timestamp = currentTime - (91 * oneDayMillis)
        val isWithin = calculator.isWithinRetentionWindow(
            timestamp = timestamp,
            retentionDays = 90,
            currentTime = currentTime
        )
        assertFalse(isWithin)
    }

    @Test
    fun `isWithinRetentionWindow uses default 90-day window`() {
        val timestamp = currentTime - (89 * oneDayMillis)
        val isWithin = calculator.isWithinRetentionWindow(
            timestamp = timestamp,
            currentTime = currentTime
        )
        assertTrue(isWithin)
    }

    // ========================================================================
    // Custom Half-Life Tests
    // ========================================================================

    @Test
    fun `custom half-life affects decay calculation`() {
        val customCalculator = PatternDecayCalculator(decayHalfLifeDays = 7.0)
        val shotTime = currentTime - (7 * oneDayMillis)

        val decay = customCalculator.calculateDecay(
            shotTimestamp = shotTime,
            currentTime = currentTime
        )

        assertEquals(0.5f, decay, 0.01f)
    }

    @Test
    fun `custom half-life affects decayed confidence`() {
        val customCalculator = PatternDecayCalculator(decayHalfLifeDays = 7.0)
        val baseConfidence = 0.8f
        val lastOccurrence = currentTime - (7 * oneDayMillis)

        val decayedConfidence = customCalculator.calculateDecayedConfidence(
            baseConfidence = baseConfidence,
            lastOccurrence = lastOccurrence,
            currentTime = currentTime
        )

        assertEquals(0.4f, decayedConfidence, 0.01f)
    }
}
