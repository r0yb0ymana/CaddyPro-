package caddypro.data.caddy.repository

import app.cash.turbine.test
import caddypro.data.caddy.local.dao.SyncQueueDao
import caddypro.data.caddy.local.entities.SyncQueueEntity
import caddypro.domain.caddy.models.SyncOperation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SyncQueueRepositoryImpl.
 *
 * Tests repository operations and entity-domain mapping.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 19
 */
class SyncQueueRepositoryImplTest {

    private lateinit var dao: SyncQueueDao
    private lateinit var repository: SyncQueueRepositoryImpl

    @Before
    fun setup() {
        dao = mockk()
        repository = SyncQueueRepositoryImpl(dao)
    }

    @Test
    fun `enqueue inserts operation and returns generated id`() = runTest {
        val operation = SyncOperation(
            id = 0, // 0 means auto-generate
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = """{"club":"7i","lie":"fairway"}""",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.PENDING
        )

        coEvery { dao.insert(any()) } returns 42L

        val result = repository.enqueue(operation)

        assertEquals(42L, result)
        coVerify {
            dao.insert(
                withArg { entity ->
                    assertEquals("SHOT_LOG", entity.operationType)
                    assertEquals("""{"club":"7i","lie":"fairway"}""", entity.payload)
                    assertEquals(1000L, entity.timestamp)
                    assertEquals("PENDING", entity.status)
                    assertEquals(0, entity.retryCount)
                    assertEquals(null, entity.lastAttemptTimestamp)
                    assertEquals(null, entity.errorMessage)
                }
            )
        }
    }

    @Test
    fun `getPendingOperations returns mapped domain models`() = runTest {
        val entities = listOf(
            SyncQueueEntity(
                id = 1,
                operationType = "SHOT_LOG",
                payload = """{"club":"7i"}""",
                timestamp = 1000L,
                status = "PENDING",
                retryCount = 0
            ),
            SyncQueueEntity(
                id = 2,
                operationType = "ROUND_SYNC",
                payload = """{"roundId":"abc"}""",
                timestamp = 2000L,
                status = "PENDING",
                retryCount = 1,
                lastAttemptTimestamp = 1500L
            )
        )

        coEvery { dao.getPendingOperations() } returns entities

        val result = repository.getPendingOperations()

        assertEquals(2, result.size)

        // First operation
        assertEquals(1L, result[0].id)
        assertEquals(SyncOperation.OperationType.SHOT_LOG, result[0].operationType)
        assertEquals("""{"club":"7i"}""", result[0].payload)
        assertEquals(1000L, result[0].timestamp)
        assertEquals(SyncOperation.SyncStatus.PENDING, result[0].status)
        assertEquals(0, result[0].retryCount)
        assertEquals(null, result[0].lastAttemptTimestamp)

        // Second operation
        assertEquals(2L, result[1].id)
        assertEquals(SyncOperation.OperationType.ROUND_SYNC, result[1].operationType)
        assertEquals("""{"roundId":"abc"}""", result[1].payload)
        assertEquals(2000L, result[1].timestamp)
        assertEquals(SyncOperation.SyncStatus.PENDING, result[1].status)
        assertEquals(1, result[1].retryCount)
        assertEquals(1500L, result[1].lastAttemptTimestamp)
    }

    @Test
    fun `getOperationsByStatus filters by status correctly`() = runTest {
        val entities = listOf(
            SyncQueueEntity(
                id = 1,
                operationType = "SHOT_LOG",
                payload = "{}",
                timestamp = 1000L,
                status = "FAILED",
                retryCount = 3,
                errorMessage = "Network error"
            )
        )

        coEvery { dao.getOperationsByStatus("FAILED") } returns entities

        val result = repository.getOperationsByStatus(SyncOperation.SyncStatus.FAILED)

        assertEquals(1, result.size)
        assertEquals(SyncOperation.SyncStatus.FAILED, result[0].status)
        assertEquals(3, result[0].retryCount)
        assertEquals("Network error", result[0].errorMessage)

        coVerify { dao.getOperationsByStatus("FAILED") }
    }

    @Test
    fun `updateStatus calls DAO with correct parameters`() = runTest {
        coEvery { dao.updateStatus(any(), any(), any()) } returns Unit

        repository.updateStatus(
            operationId = 42L,
            status = SyncOperation.SyncStatus.SYNCED,
            errorMessage = null
        )

        coVerify { dao.updateStatus(42L, "SYNCED", null) }
    }

    @Test
    fun `updateStatus with error message calls DAO correctly`() = runTest {
        coEvery { dao.updateStatus(any(), any(), any()) } returns Unit

        repository.updateStatus(
            operationId = 42L,
            status = SyncOperation.SyncStatus.FAILED,
            errorMessage = "Connection timeout"
        )

        coVerify { dao.updateStatus(42L, "FAILED", "Connection timeout") }
    }

    @Test
    fun `incrementRetryCount calls DAO with operation id`() = runTest {
        coEvery { dao.incrementRetryCount(any(), any()) } returns Unit

        repository.incrementRetryCount(42L)

        coVerify { dao.incrementRetryCount(eq(42L), any()) }
    }

    @Test
    fun `delete calls DAO with operation id`() = runTest {
        coEvery { dao.delete(any()) } returns Unit

        repository.delete(42L)

        coVerify { dao.delete(42L) }
    }

    @Test
    fun `deleteSynced calls DAO deleteSynced`() = runTest {
        coEvery { dao.deleteSynced() } returns Unit

        repository.deleteSynced()

        coVerify { dao.deleteSynced() }
    }

    @Test
    fun `observePendingCount returns flow from DAO`() = runTest {
        every { dao.observePendingCount() } returns flowOf(5, 3, 0)

        repository.observePendingCount().test {
            assertEquals(5, awaitItem())
            assertEquals(3, awaitItem())
            assertEquals(0, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getPendingCount returns count from DAO`() = runTest {
        coEvery { dao.getPendingCount() } returns 7

        val result = repository.getPendingCount()

        assertEquals(7, result)
        coVerify { dao.getPendingCount() }
    }

    @Test
    fun `entity to domain mapping preserves all fields`() = runTest {
        val entity = SyncQueueEntity(
            id = 123,
            operationType = "MISS_PATTERN_SYNC",
            payload = """{"pattern":"slice"}""",
            timestamp = 9999L,
            status = "SYNCING",
            retryCount = 2,
            lastAttemptTimestamp = 8888L,
            errorMessage = "Previous error"
        )

        coEvery { dao.getOperationsByStatus("SYNCING") } returns listOf(entity)

        val result = repository.getOperationsByStatus(SyncOperation.SyncStatus.SYNCING)

        assertEquals(1, result.size)
        val operation = result[0]
        assertEquals(123L, operation.id)
        assertEquals(SyncOperation.OperationType.MISS_PATTERN_SYNC, operation.operationType)
        assertEquals("""{"pattern":"slice"}""", operation.payload)
        assertEquals(9999L, operation.timestamp)
        assertEquals(SyncOperation.SyncStatus.SYNCING, operation.status)
        assertEquals(2, operation.retryCount)
        assertEquals(8888L, operation.lastAttemptTimestamp)
        assertEquals("Previous error", operation.errorMessage)
    }

    @Test
    fun `domain to entity mapping preserves all fields`() = runTest {
        val operation = SyncOperation(
            id = 456,
            operationType = SyncOperation.OperationType.ROUND_SYNC,
            payload = """{"roundData":"..."}""",
            timestamp = 7777L,
            status = SyncOperation.SyncStatus.FAILED,
            retryCount = 5,
            lastAttemptTimestamp = 6666L,
            errorMessage = "Failed to sync"
        )

        coEvery { dao.insert(any()) } returns 456L

        repository.enqueue(operation)

        coVerify {
            dao.insert(
                withArg { entity ->
                    assertEquals(456L, entity.id)
                    assertEquals("ROUND_SYNC", entity.operationType)
                    assertEquals("""{"roundData":"..."}""", entity.payload)
                    assertEquals(7777L, entity.timestamp)
                    assertEquals("FAILED", entity.status)
                    assertEquals(5, entity.retryCount)
                    assertEquals(6666L, entity.lastAttemptTimestamp)
                    assertEquals("Failed to sync", entity.errorMessage)
                }
            )
        }
    }
}
