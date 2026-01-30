package caddypro.domain.caddy.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SyncOperation domain model.
 *
 * Tests exponential backoff calculation and retry eligibility logic.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 19
 */
class SyncOperationTest {

    @Test
    fun `canRetry returns true when no previous attempt`() {
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = "{}",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.PENDING,
            retryCount = 0,
            lastAttemptTimestamp = null
        )

        val currentTime = 5000L
        assertTrue(operation.canRetry(currentTime))
    }

    @Test
    fun `canRetry returns false when status is SYNCED`() {
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = "{}",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.SYNCED,
            retryCount = 0,
            lastAttemptTimestamp = 2000L
        )

        val currentTime = 100000L
        assertFalse(operation.canRetry(currentTime))
    }

    @Test
    fun `canRetry returns false when backoff delay not elapsed - retry 0`() {
        // Retry 0: 30 second backoff
        val lastAttempt = 1000L
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = "{}",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.FAILED,
            retryCount = 0,
            lastAttemptTimestamp = lastAttempt
        )

        // Only 15 seconds have passed (need 30)
        val currentTime = lastAttempt + 15_000L
        assertFalse(operation.canRetry(currentTime))
    }

    @Test
    fun `canRetry returns true when backoff delay elapsed - retry 0`() {
        // Retry 0: 30 second backoff
        val lastAttempt = 1000L
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = "{}",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.FAILED,
            retryCount = 0,
            lastAttemptTimestamp = lastAttempt
        )

        // 30 seconds have passed
        val currentTime = lastAttempt + 30_000L
        assertTrue(operation.canRetry(currentTime))
    }

    @Test
    fun `canRetry returns false when backoff delay not elapsed - retry 1`() {
        // Retry 1: 60 second backoff
        val lastAttempt = 1000L
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = "{}",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.FAILED,
            retryCount = 1,
            lastAttemptTimestamp = lastAttempt
        )

        // Only 30 seconds have passed (need 60)
        val currentTime = lastAttempt + 30_000L
        assertFalse(operation.canRetry(currentTime))
    }

    @Test
    fun `canRetry returns true when backoff delay elapsed - retry 1`() {
        // Retry 1: 60 second backoff
        val lastAttempt = 1000L
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = "{}",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.FAILED,
            retryCount = 1,
            lastAttemptTimestamp = lastAttempt
        )

        // 60 seconds have passed
        val currentTime = lastAttempt + 60_000L
        assertTrue(operation.canRetry(currentTime))
    }

    @Test
    fun `canRetry returns false when backoff delay not elapsed - retry 2`() {
        // Retry 2: 120 second backoff
        val lastAttempt = 1000L
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = "{}",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.FAILED,
            retryCount = 2,
            lastAttemptTimestamp = lastAttempt
        )

        // Only 60 seconds have passed (need 120)
        val currentTime = lastAttempt + 60_000L
        assertFalse(operation.canRetry(currentTime))
    }

    @Test
    fun `canRetry respects maximum backoff of 30 minutes`() {
        // High retry count should cap at 30 minutes (1,800,000 ms)
        val lastAttempt = 1000L
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = "{}",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.FAILED,
            retryCount = 10, // Very high retry count
            lastAttemptTimestamp = lastAttempt
        )

        // 29 minutes have passed (need 30)
        val currentTime = lastAttempt + (29 * 60 * 1000L)
        assertFalse(operation.canRetry(currentTime))

        // 30 minutes have passed
        val currentTime2 = lastAttempt + (30 * 60 * 1000L)
        assertTrue(operation.canRetry(currentTime2))
    }

    @Test
    fun `exponential backoff progression is correct`() {
        // Verify backoff delays follow exponential pattern
        // Retry 0: 30s, Retry 1: 60s, Retry 2: 120s, Retry 3: 240s, Retry 4: 480s, Retry 5+: 30min

        val testCases = listOf(
            0 to 30_000L,      // 30 seconds
            1 to 60_000L,      // 60 seconds
            2 to 120_000L,     // 120 seconds
            3 to 240_000L,     // 240 seconds (4 minutes)
            4 to 480_000L,     // 480 seconds (8 minutes)
            5 to 960_000L,     // 960 seconds (16 minutes)
            6 to 1_800_000L,   // Capped at 30 minutes
            10 to 1_800_000L   // Still capped at 30 minutes
        )

        val baseTime = 1000L

        for ((retryCount, expectedDelay) in testCases) {
            val operation = SyncOperation(
                id = 1,
                operationType = SyncOperation.OperationType.SHOT_LOG,
                payload = "{}",
                timestamp = baseTime,
                status = SyncOperation.SyncStatus.FAILED,
                retryCount = retryCount,
                lastAttemptTimestamp = baseTime
            )

            // Just before delay elapses - should NOT be ready
            assertFalse(
                "Retry $retryCount: Should not be ready 1ms before delay",
                operation.canRetry(baseTime + expectedDelay - 1)
            )

            // At exact delay - should be ready
            assertTrue(
                "Retry $retryCount: Should be ready at exact delay of ${expectedDelay}ms",
                operation.canRetry(baseTime + expectedDelay)
            )
        }
    }

    @Test
    fun `operation types are correctly defined`() {
        assertEquals(3, SyncOperation.OperationType.values().size)
        assertEquals("SHOT_LOG", SyncOperation.OperationType.SHOT_LOG.name)
        assertEquals("ROUND_SYNC", SyncOperation.OperationType.ROUND_SYNC.name)
        assertEquals("MISS_PATTERN_SYNC", SyncOperation.OperationType.MISS_PATTERN_SYNC.name)
    }

    @Test
    fun `sync statuses are correctly defined`() {
        assertEquals(4, SyncOperation.SyncStatus.values().size)
        assertEquals("PENDING", SyncOperation.SyncStatus.PENDING.name)
        assertEquals("SYNCING", SyncOperation.SyncStatus.SYNCING.name)
        assertEquals("SYNCED", SyncOperation.SyncStatus.SYNCED.name)
        assertEquals("FAILED", SyncOperation.SyncStatus.FAILED.name)
    }
}
