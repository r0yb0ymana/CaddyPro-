package caddypro.domain.caddy.services

import caddypro.data.caddy.wearable.StubWearableSyncService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration test for wearable sync and readiness calculation.
 *
 * Verifies the complete flow:
 * 1. Sync metrics from wearable (stub)
 * 2. Calculate readiness score
 * 3. Verify adjustment factor mapping
 *
 * This simulates the real usage pattern where the UI layer will:
 * - Check if wearable is available
 * - Sync metrics if available
 * - Calculate readiness from synced metrics
 * - Use readiness to adjust strategy recommendations
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 6
 * Acceptance criteria: A2 (Readiness impacts strategy)
 */
class WearableSyncIntegrationTest {

    private lateinit var syncService: WearableSyncService
    private lateinit var calculator: ReadinessCalculator

    @Before
    fun setup() {
        syncService = StubWearableSyncService()
        calculator = ReadinessCalculator()
    }

    @Test
    fun `stub wearable sync is always unavailable in MVP`() = runTest {
        // When
        val isAvailable = syncService.isWearableAvailable()

        // Then
        assertFalse("Stub service should report wearable as unavailable", isAvailable)
    }

    @Test
    fun `stub wearable sync returns placeholder metrics`() = runTest {
        // When
        val result = syncService.syncMetrics()

        // Then
        assertTrue("Sync should succeed", result.isSuccess)

        val metrics = result.getOrNull()
        assertNotNull("Metrics should not be null", metrics)

        // Verify placeholder values match StubWearableSyncService
        assertEquals(45.0, metrics?.hrvMs, 0.1)
        assertEquals(420, metrics?.sleepMinutes)
        assertEquals(75.0, metrics?.sleepQualityScore, 0.1)
        assertEquals(30.0, metrics?.stressScore, 0.1)
    }

    @Test
    fun `complete flow from sync to readiness calculation`() = runTest {
        // Given: Sync metrics from wearable
        val syncResult = syncService.syncMetrics()
        assertTrue("Sync should succeed", syncResult.isSuccess)

        val metrics = syncResult.getOrThrow()

        // When: Calculate readiness
        val readiness = calculator.calculateReadiness(metrics)

        // Then: Verify readiness score
        assertTrue("Overall score should be 0-100", readiness.overall in 0..100)
        assertTrue("Stub metrics should produce moderate score 60-75", readiness.overall in 60..75)

        // Verify breakdown components
        assertNotNull("HRV should be present", readiness.breakdown.hrv)
        assertNotNull("Sleep should be present", readiness.breakdown.sleepQuality)
        assertNotNull("Stress should be present", readiness.breakdown.stressLevel)

        // Verify weights sum correctly
        val hrvWeight = readiness.breakdown.hrv?.weight ?: 0.0
        val sleepWeight = readiness.breakdown.sleepQuality?.weight ?: 0.0
        val stressWeight = readiness.breakdown.stressLevel?.weight ?: 0.0
        assertEquals("Weights should sum to 1.0", 1.0, hrvWeight + sleepWeight + stressWeight, 0.01)
    }

    @Test
    fun `readiness score affects strategy through adjustment factor`() = runTest {
        // Given: Sync and calculate readiness
        val metrics = syncService.syncMetrics().getOrThrow()
        val readiness = calculator.calculateReadiness(metrics)

        // When: Get adjustment factor for strategy
        val factor = readiness.adjustmentFactor()

        // Then: Factor should be in valid range
        assertTrue("Adjustment factor should be 0.5-1.0", factor in 0.5..1.0)

        // With stub metrics (~62), factor should be between 0.5 and 1.0
        // Score 62 is above 60, so should get factor = 1.0
        assertEquals("Score >= 60 should get normal risk tolerance (1.0)", 1.0, factor, 0.01)
    }

    @Test
    fun `low readiness triggers conservative strategy`() = runTest {
        // Given: Manually create low readiness scenario
        val lowMetrics = caddypro.domain.caddy.models.WearableMetrics(
            hrvMs = 20.0,
            sleepMinutes = 300,
            sleepQualityScore = 40.0,
            stressScore = 80.0,
            timestamp = System.currentTimeMillis()
        )

        // When: Calculate readiness
        val readiness = calculator.calculateReadiness(lowMetrics)

        // Then: Verify conservative adjustment
        assertTrue("Low metrics should produce low readiness", readiness.isLow())
        assertTrue("Low readiness should be < 60", readiness.overall < 60)

        val factor = readiness.adjustmentFactor()
        assertTrue("Low readiness factor should be < 1.0", factor < 1.0)

        // This factor would be used by strategy engine to:
        // - Increase safety margins
        // - Recommend more conservative club selections
        // - Avoid hero lines and risky plays
    }

    @Test
    fun `high readiness allows normal risk tolerance`() = runTest {
        // Given: High readiness scenario
        val highMetrics = caddypro.domain.caddy.models.WearableMetrics(
            hrvMs = 80.0,
            sleepMinutes = 480,
            sleepQualityScore = 90.0,
            stressScore = 10.0,
            timestamp = System.currentTimeMillis()
        )

        // When: Calculate readiness
        val readiness = calculator.calculateReadiness(highMetrics)

        // Then: Verify normal risk tolerance
        assertFalse("High metrics should not be low readiness", readiness.isLow())
        assertTrue("High readiness should be >= 60", readiness.overall >= 60)

        val factor = readiness.adjustmentFactor()
        assertEquals("High readiness factor should be 1.0", 1.0, factor, 0.01)

        // This factor allows strategy engine to:
        // - Use normal safety margins
        // - Consider aggressive lines when appropriate
        // - Trust player's ability to execute
    }
}
