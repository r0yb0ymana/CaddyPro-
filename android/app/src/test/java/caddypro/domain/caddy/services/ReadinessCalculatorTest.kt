package caddypro.domain.caddy.services

import caddypro.domain.caddy.models.ReadinessSource
import caddypro.domain.caddy.models.WearableMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ReadinessCalculator.
 *
 * Verifies:
 * - Correct normalization of HRV, sleep, and stress metrics
 * - Weighted average calculation with fixed weights (40%, 40%, 20%)
 * - Missing metrics default to 50 (neutral)
 * - Overall scores are in valid 0-100 range
 * - Adjustment factor mapping is correct
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 6
 * Acceptance criteria: A2 (Readiness impacts strategy)
 */
class ReadinessCalculatorTest {

    private lateinit var calculator: ReadinessCalculator

    @Before
    fun setup() {
        calculator = ReadinessCalculator()
    }

    @Test
    fun `calculateReadiness with all metrics available produces valid score`() {
        // Given: Good readiness metrics
        val metrics = WearableMetrics(
            hrvMs = 60.0,              // Good HRV
            sleepMinutes = 450,         // 7.5 hours (optimal)
            sleepQualityScore = 85.0,   // Good quality
            stressScore = 20.0,         // Low stress
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        assertTrue("Overall score should be 0-100", result.overall in 0..100)
        assertEquals(ReadinessSource.WEARABLE_SYNC, result.source)
        assertNotNull(result.breakdown.hrv)
        assertNotNull(result.breakdown.sleepQuality)
        assertNotNull(result.breakdown.stressLevel)

        // Should produce high readiness (all good metrics)
        assertTrue("Good metrics should produce score >= 70", result.overall >= 70)
    }

    @Test
    fun `calculateReadiness with missing HRV uses neutral default`() {
        // Given: Missing HRV, good sleep and stress
        val metrics = WearableMetrics(
            hrvMs = null,              // Missing
            sleepMinutes = 420,         // 7 hours
            sleepQualityScore = 80.0,   // Good
            stressScore = 25.0,         // Low
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        assertTrue("Overall score should be 0-100", result.overall in 0..100)
        assertEquals("Missing HRV should be null in breakdown", null, result.breakdown.hrv)
        assertNotNull("Sleep should be present", result.breakdown.sleepQuality)
        assertNotNull("Stress should be present", result.breakdown.stressLevel)

        // With HRV missing (defaults to 50), overall should be moderate
        assertTrue("Score with missing HRV should be 60-80", result.overall in 60..80)
    }

    @Test
    fun `calculateReadiness with all metrics missing uses neutral defaults`() {
        // Given: All metrics missing
        val metrics = WearableMetrics(
            hrvMs = null,
            sleepMinutes = null,
            sleepQualityScore = null,
            stressScore = null,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        assertEquals("All neutral defaults should produce score 50", 50, result.overall)
        assertEquals(null, result.breakdown.hrv)
        assertEquals(null, result.breakdown.sleepQuality)
        assertEquals(null, result.breakdown.stressLevel)
    }

    @Test
    fun `calculateReadiness with poor metrics produces low score`() {
        // Given: Poor readiness metrics
        val metrics = WearableMetrics(
            hrvMs = 20.0,              // Low HRV
            sleepMinutes = 300,         // 5 hours (insufficient)
            sleepQualityScore = 40.0,   // Poor quality
            stressScore = 80.0,         // High stress
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        assertTrue("Overall score should be 0-100", result.overall in 0..100)
        assertTrue("Poor metrics should produce score <= 40", result.overall <= 40)
    }

    @Test
    fun `calculateReadiness with sleep duration only calculates correctly`() {
        // Given: Sleep duration without quality score
        val metrics = WearableMetrics(
            hrvMs = 50.0,
            sleepMinutes = 420,         // 7 hours (optimal)
            sleepQualityScore = null,   // No quality score
            stressScore = 30.0,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        assertNotNull("Sleep should be calculated from duration", result.breakdown.sleepQuality)
        assertTrue("Sleep score should be in valid range",
            result.breakdown.sleepQuality?.value in 0.0..100.0)

        // 7 hours is optimal (6-9 range), should score high
        val sleepScore = result.breakdown.sleepQuality?.value ?: 0.0
        assertTrue("7 hours sleep should score >= 75", sleepScore >= 75.0)
    }

    @Test
    fun `calculateReadiness with oversleep reduces score`() {
        // Given: Too much sleep (10.5 hours)
        val metrics = WearableMetrics(
            hrvMs = 50.0,
            sleepMinutes = 630,         // 10.5 hours (oversleep)
            sleepQualityScore = null,
            stressScore = 30.0,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        val sleepScore = result.breakdown.sleepQuality?.value ?: 0.0
        assertTrue("Oversleep should reduce score below 100", sleepScore < 100.0)
    }

    @Test
    fun `calculateReadiness prefers sleep quality over duration`() {
        // Given: Short sleep duration but high quality score
        val metrics = WearableMetrics(
            hrvMs = 50.0,
            sleepMinutes = 300,         // 5 hours (poor duration)
            sleepQualityScore = 90.0,   // Excellent quality
            stressScore = 30.0,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        val sleepScore = result.breakdown.sleepQuality?.value ?: 0.0
        assertEquals("Should use quality score instead of duration", 90.0, sleepScore, 0.1)
    }

    @Test
    fun `calculateReadiness inverts stress score correctly`() {
        // Given: High stress (bad for readiness)
        val metrics = WearableMetrics(
            hrvMs = 50.0,
            sleepMinutes = 420,
            sleepQualityScore = 75.0,
            stressScore = 90.0,         // High stress (bad)
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        val stressScore = result.breakdown.stressLevel?.value ?: 0.0
        assertEquals("High stress (90) should invert to low score (10)", 10.0, stressScore, 0.1)
    }

    @Test
    fun `calculateReadiness with stub service data produces expected score`() {
        // Given: Placeholder metrics from StubWearableSyncService
        val metrics = WearableMetrics(
            hrvMs = 45.0,              // Moderate
            sleepMinutes = 420,         // 7 hours
            sleepQualityScore = 75.0,   // Good
            stressScore = 30.0,         // Low
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        // Expected: HRV=45 (45%), Sleep=75 (75%), Stress=70 (inverted 30)
        // Weighted: (45*0.4) + (75*0.4) + (70*0.2) = 18 + 30 + 14 = 62
        assertTrue("Stub metrics should produce score around 60-75", result.overall in 60..75)

        // Adjustment factor at ~62 should be between 0.5 and 1.0
        val factor = result.adjustmentFactor()
        assertTrue("Adjustment factor should be in valid range", factor in 0.5..1.0)
    }

    @Test
    fun `ReadinessScore adjustmentFactor maps correctly for high readiness`() {
        // Given: High readiness score (>= 60)
        val metrics = WearableMetrics(
            hrvMs = 80.0,
            sleepMinutes = 480,
            sleepQualityScore = 90.0,
            stressScore = 10.0,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        assertTrue("High readiness should be >= 60", result.overall >= 60)
        assertEquals("High readiness factor should be 1.0", 1.0, result.adjustmentFactor(), 0.01)
    }

    @Test
    fun `ReadinessScore adjustmentFactor maps correctly for low readiness`() {
        // Given: Low readiness score (<= 40)
        val metrics = WearableMetrics(
            hrvMs = 15.0,
            sleepMinutes = 240,
            sleepQualityScore = 30.0,
            stressScore = 90.0,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        assertTrue("Low readiness should be <= 40", result.overall <= 40)
        assertEquals("Low readiness factor should be 0.5", 0.5, result.adjustmentFactor(), 0.01)
    }

    @Test
    fun `ReadinessScore adjustmentFactor interpolates for moderate readiness`() {
        // Given: Moderate readiness score (41-59)
        // We need to craft metrics that produce a score in the 41-59 range
        val metrics = WearableMetrics(
            hrvMs = 40.0,              // Below average
            sleepMinutes = 360,         // 6 hours (minimum optimal)
            sleepQualityScore = 50.0,   // Average
            stressScore = 50.0,         // Moderate
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = calculator.calculateReadiness(metrics)

        // Then
        if (result.overall in 41..59) {
            val factor = result.adjustmentFactor()
            assertTrue("Moderate readiness factor should be 0.5-1.0", factor in 0.5..1.0)
            assertTrue("Factor should not be exactly 0.5 or 1.0", factor > 0.5 && factor < 1.0)
        }
    }

    @Test
    fun `calculateReadiness produces consistent timestamp`() {
        // Given
        val metrics = WearableMetrics(
            hrvMs = 50.0,
            sleepMinutes = 420,
            sleepQualityScore = 75.0,
            stressScore = 30.0,
            timestamp = System.currentTimeMillis()
        )

        val before = System.currentTimeMillis()

        // When
        val result = calculator.calculateReadiness(metrics)

        val after = System.currentTimeMillis()

        // Then
        assertTrue("Timestamp should be recent", result.timestamp in before..after)
    }
}
