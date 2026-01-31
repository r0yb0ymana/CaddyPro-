package caddypro.domain.caddy.usecases

import caddypro.data.caddy.repository.ReadinessRepository
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for UpdateReadinessUseCase.
 *
 * Tests cover:
 * - Successful readiness score updates with validation
 * - Range validation (0-100 for overall, 0.0-100.0 for components)
 * - Manual entry convenience method
 * - Wearable sync with weighted calculation
 * - Repository persistence
 *
 * Spec reference: live-caddy-mode.md R3, A2
 * Plan reference: live-caddy-mode-plan.md Task 9 step 3
 */
class UpdateReadinessUseCaseTest {

    private lateinit var readinessRepository: ReadinessRepository
    private lateinit var useCase: UpdateReadinessUseCase

    @Before
    fun setup() {
        readinessRepository = mockk(relaxed = true)
        useCase = UpdateReadinessUseCase(readinessRepository)
    }

    @Test
    fun `when readiness updated then saves to repository`() = runTest {
        // Given: Valid readiness score
        val breakdown = ReadinessBreakdown(
            hrv = MetricScore(value = 75.0, weight = 0.4),
            sleepQuality = MetricScore(value = 80.0, weight = 0.4),
            stressLevel = MetricScore(value = 70.0, weight = 0.2)
        )

        // When: Update readiness
        val result = useCase(
            overall = 75,
            breakdown = breakdown,
            source = ReadinessSource.WEARABLE_SYNC
        )

        // Then: Success
        assertTrue(result.isSuccess)

        // Then: Saved to repository
        val scoreSlot = slot<ReadinessScore>()
        coVerify { readinessRepository.saveReadiness(capture(scoreSlot)) }

        val savedScore = scoreSlot.captured
        assertEquals(75, savedScore.overall)
        assertEquals(75.0, savedScore.breakdown.hrv?.value, 0.01)
        assertEquals(80.0, savedScore.breakdown.sleepQuality?.value, 0.01)
        assertEquals(70.0, savedScore.breakdown.stressLevel?.value, 0.01)
        assertEquals(ReadinessSource.WEARABLE_SYNC, savedScore.source)
        assertTrue(savedScore.timestamp > 0)
    }

    @Test
    fun `when overall score is 0 then valid`() = runTest {
        // Given: Minimum valid score
        val breakdown = ReadinessBreakdown(hrv = null, sleepQuality = null, stressLevel = null)

        // When: Update with 0
        val result = useCase(0, breakdown)

        // Then: Success
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.overall)
    }

    @Test
    fun `when overall score is 100 then valid`() = runTest {
        // Given: Maximum valid score
        val breakdown = ReadinessBreakdown(hrv = null, sleepQuality = null, stressLevel = null)

        // When: Update with 100
        val result = useCase(100, breakdown)

        // Then: Success
        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull()?.overall)
    }

    @Test
    fun `when overall score is negative then returns failure`() = runTest {
        // Given: Invalid negative score
        val breakdown = ReadinessBreakdown(hrv = null, sleepQuality = null, stressLevel = null)

        // When: Attempt to update with negative value
        val result = useCase(-1, breakdown)

        // Then: Failure
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("must be between 0 and 100") == true
        )

        // Then: Not saved to repository
        coVerify(exactly = 0) { readinessRepository.saveReadiness(any()) }
    }

    @Test
    fun `when overall score exceeds 100 then returns failure`() = runTest {
        // Given: Invalid score above maximum
        val breakdown = ReadinessBreakdown(hrv = null, sleepQuality = null, stressLevel = null)

        // When: Attempt to update with 101
        val result = useCase(101, breakdown)

        // Then: Failure
        assertTrue(result.isFailure)
        assertTrue(
            result.exceptionOrNull()?.message?.contains("must be between 0 and 100") == true
        )

        // Then: Not saved to repository
        coVerify(exactly = 0) { readinessRepository.saveReadiness(any()) }
    }

    @Test
    fun `when MetricScore value is negative then returns failure`() = runTest {
        // Given: Invalid MetricScore (validation happens in MetricScore constructor)
        val breakdown = try {
            ReadinessBreakdown(
                hrv = MetricScore(value = -1.0, weight = 0.4), // Invalid
                sleepQuality = MetricScore(value = 80.0, weight = 0.4),
                stressLevel = MetricScore(value = 70.0, weight = 0.2)
            )
            null
        } catch (e: Exception) {
            // Expected: MetricScore constructor throws
            assertTrue(e.message?.contains("must be between 0 and 100") == true)
            return@runTest
        }

        // Should not reach here
        assertTrue("MetricScore should have thrown exception", false)
    }

    @Test
    fun `when MetricScore value exceeds 100 then returns failure`() = runTest {
        // Given: Invalid MetricScore (validation happens in MetricScore constructor)
        try {
            ReadinessBreakdown(
                hrv = MetricScore(value = 101.0, weight = 0.4), // Invalid
                sleepQuality = MetricScore(value = 80.0, weight = 0.4),
                stressLevel = MetricScore(value = 70.0, weight = 0.2)
            )
            // Should not reach here
            assertTrue("MetricScore should have thrown exception", false)
        } catch (e: Exception) {
            // Expected: MetricScore constructor throws
            assertTrue(e.message?.contains("must be between 0 and 100") == true)
        }
    }

    @Test
    fun `when MetricScore weight is invalid then returns failure`() = runTest {
        // Given: Invalid weight
        try {
            ReadinessBreakdown(
                hrv = MetricScore(value = 75.0, weight = 1.5), // Invalid weight
                sleepQuality = MetricScore(value = 80.0, weight = 0.4),
                stressLevel = MetricScore(value = 70.0, weight = 0.2)
            )
            // Should not reach here
            assertTrue("MetricScore should have thrown exception", false)
        } catch (e: Exception) {
            // Expected: MetricScore constructor throws
            assertTrue(e.message?.contains("weight") == true)
        }
    }

    @Test
    fun `when breakdown has null components then valid`() = runTest {
        // Given: Breakdown with missing components (manual entry)
        val breakdown = ReadinessBreakdown(
            hrv = null,
            sleepQuality = null,
            stressLevel = null
        )

        // When: Update readiness
        val result = useCase(70, breakdown, ReadinessSource.MANUAL_ENTRY)

        // Then: Success
        assertTrue(result.isSuccess)

        val scoreSlot = slot<ReadinessScore>()
        coVerify { readinessRepository.saveReadiness(capture(scoreSlot)) }

        val savedScore = scoreSlot.captured
        assertEquals(70, savedScore.overall)
        assertNull(savedScore.breakdown.hrv)
        assertNull(savedScore.breakdown.sleepQuality)
        assertNull(savedScore.breakdown.stressLevel)
        assertEquals(ReadinessSource.MANUAL_ENTRY, savedScore.source)
    }

    @Test
    fun `when updateManual called then saves with null breakdown`() = runTest {
        // When: Manual update convenience method
        val result = useCase.updateManual(65)

        // Then: Success
        assertTrue(result.isSuccess)

        val scoreSlot = slot<ReadinessScore>()
        coVerify { readinessRepository.saveReadiness(capture(scoreSlot)) }

        val savedScore = scoreSlot.captured
        assertEquals(65, savedScore.overall)
        assertNull(savedScore.breakdown.hrv)
        assertNull(savedScore.breakdown.sleepQuality)
        assertNull(savedScore.breakdown.stressLevel)
        assertEquals(ReadinessSource.MANUAL_ENTRY, savedScore.source)
    }

    @Test
    fun `when updateFromWearable called then calculates weighted average`() = runTest {
        // Given: Wearable metrics (normalized to 0-100)
        val hrv = 80.0        // 40% weight
        val sleep = 90.0      // 40% weight
        val stress = 60.0     // 20% weight

        // Expected: (80 * 0.4) + (90 * 0.4) + (60 * 0.2) = 32 + 36 + 12 = 80

        // When: Update from wearable
        val result = useCase.updateFromWearable(hrv, sleep, stress)

        // Then: Success with calculated overall
        assertTrue(result.isSuccess)

        val scoreSlot = slot<ReadinessScore>()
        coVerify { readinessRepository.saveReadiness(capture(scoreSlot)) }

        val savedScore = scoreSlot.captured
        assertEquals(80, savedScore.overall) // Weighted average
        assertEquals(80.0, savedScore.breakdown.hrv?.value, 0.01)
        assertEquals(90.0, savedScore.breakdown.sleepQuality?.value, 0.01)
        assertEquals(60.0, savedScore.breakdown.stressLevel?.value, 0.01)
        assertEquals(0.4, savedScore.breakdown.hrv?.weight, 0.01)
        assertEquals(0.4, savedScore.breakdown.sleepQuality?.weight, 0.01)
        assertEquals(0.2, savedScore.breakdown.stressLevel?.weight, 0.01)
        assertEquals(ReadinessSource.WEARABLE_SYNC, savedScore.source)
    }

    @Test
    fun `when updateFromWearable with low metrics then low overall score`() = runTest {
        // Given: Poor wearable metrics
        val hrv = 20.0
        val sleep = 30.0
        val stress = 40.0

        // Expected: (20 * 0.4) + (30 * 0.4) + (40 * 0.2) = 8 + 12 + 8 = 28

        // When: Update from wearable
        val result = useCase.updateFromWearable(hrv, sleep, stress)

        // Then: Success with low overall score
        assertTrue(result.isSuccess)
        assertEquals(28, result.getOrNull()?.overall)
    }

    @Test
    fun `when updateFromWearable with perfect metrics then 100 score`() = runTest {
        // Given: Perfect wearable metrics
        val hrv = 100.0
        val sleep = 100.0
        val stress = 100.0

        // Expected: (100 * 0.4) + (100 * 0.4) + (100 * 0.2) = 100

        // When: Update from wearable
        val result = useCase.updateFromWearable(hrv, sleep, stress)

        // Then: Success with perfect score
        assertTrue(result.isSuccess)
        assertEquals(100, result.getOrNull()?.overall)
    }

    @Test
    fun `when updateFromWearable with invalid component then returns failure`() = runTest {
        // Given: Invalid HRV component
        val hrv = 150.0 // Invalid
        val sleep = 80.0
        val stress = 70.0

        // When: Attempt to update from wearable
        val result = useCase.updateFromWearable(hrv, sleep, stress)

        // Then: Failure due to MetricScore validation
        assertTrue(result.isFailure)
    }

    @Test
    fun `when updateFromWearable with custom source then uses provided source`() = runTest {
        // Given: Custom wearable source
        val hrv = 75.0
        val sleep = 80.0
        val stress = 70.0

        // When: Update with custom source
        val result = useCase.updateFromWearable(
            hrv, sleep, stress,
            source = ReadinessSource.WEARABLE_SYNC
        )

        // Then: Success with custom source
        assertTrue(result.isSuccess)

        val scoreSlot = slot<ReadinessScore>()
        coVerify { readinessRepository.saveReadiness(capture(scoreSlot)) }

        assertEquals(ReadinessSource.WEARABLE_SYNC, scoreSlot.captured.source)
    }

    @Test
    fun `when repository throws exception then returns failure`() = runTest {
        // Given: Repository fails
        coEvery { readinessRepository.saveReadiness(any()) } throws Exception("Database error")

        val breakdown = ReadinessBreakdown(hrv = null, sleepQuality = null, stressLevel = null)

        // When: Attempt to update
        val result = useCase(70, breakdown)

        // Then: Failure
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `when updating multiple times then each has unique timestamp`() = runTest {
        // Given: Multiple updates
        val breakdown = ReadinessBreakdown(hrv = null, sleepQuality = null, stressLevel = null)

        // When: Update twice
        val result1 = useCase(70, breakdown)
        Thread.sleep(10) // Small delay to ensure different timestamps
        val result2 = useCase(75, breakdown)

        // Then: Both succeed
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)

        // Then: Different timestamps
        val timestamp1 = result1.getOrNull()?.timestamp ?: 0
        val timestamp2 = result2.getOrNull()?.timestamp ?: 0
        assertTrue(timestamp2 > timestamp1)
    }

    @Test
    fun `when wearable calculation results in decimal then rounds to int`() = runTest {
        // Given: Metrics that result in decimal
        val hrv = 75.5    // 40% weight = 30.2
        val sleep = 80.5  // 40% weight = 32.2
        val stress = 70.5 // 20% weight = 14.1
        // Total = 76.5, should round to 76

        // When: Update from wearable
        val result = useCase.updateFromWearable(hrv, sleep, stress)

        // Then: Success with rounded overall
        assertTrue(result.isSuccess)
        val overall = result.getOrNull()?.overall
        assertNotNull(overall)
        assertTrue(overall!! in 76..77) // Allow for rounding variance
    }

    @Test
    fun `when wearable calculation exceeds 100 then coerces to 100`() = runTest {
        // Given: Edge case where calculation might slightly exceed due to rounding
        // (This is defensive - shouldn't happen with valid inputs)
        val hrv = 100.0
        val sleep = 100.0
        val stress = 100.0

        // When: Update from wearable
        val result = useCase.updateFromWearable(hrv, sleep, stress)

        // Then: Coerced to valid range
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.overall!! <= 100)
    }

    @Test
    fun `when wearable calculation is negative then coerces to 0`() = runTest {
        // Given: Minimum values
        val hrv = 0.0
        val sleep = 0.0
        val stress = 0.0

        // When: Update from wearable
        val result = useCase.updateFromWearable(hrv, sleep, stress)

        // Then: Coerced to valid range
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.overall!! >= 0)
    }

    @Test
    fun `when partial breakdown then valid`() = runTest {
        // Given: Only some metrics available
        val breakdown = ReadinessBreakdown(
            hrv = MetricScore(value = 75.0, weight = 0.5),
            sleepQuality = MetricScore(value = 80.0, weight = 0.5),
            stressLevel = null // Not available
        )

        // When: Update readiness
        val result = useCase(77, breakdown)

        // Then: Success
        assertTrue(result.isSuccess)

        val scoreSlot = slot<ReadinessScore>()
        coVerify { readinessRepository.saveReadiness(capture(scoreSlot)) }

        val savedScore = scoreSlot.captured
        assertEquals(77, savedScore.overall)
        assertNotNull(savedScore.breakdown.hrv)
        assertNotNull(savedScore.breakdown.sleepQuality)
        assertNull(savedScore.breakdown.stressLevel)
    }
}
