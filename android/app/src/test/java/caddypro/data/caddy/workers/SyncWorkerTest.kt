package caddypro.data.caddy.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import caddypro.data.caddy.repository.SyncQueueRepository
import caddypro.domain.caddy.models.SyncOperation
import caddypro.domain.navcaddy.offline.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Unit tests for SyncWorker.
 *
 * Tests background sync logic, retry behavior, and exponential backoff.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 19
 */
@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {

    private lateinit var context: Context
    private lateinit var syncQueueRepository: SyncQueueRepository
    private lateinit var networkMonitor: NetworkMonitor

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        syncQueueRepository = mockk(relaxed = true)
        networkMonitor = mockk()
    }

    private fun createWorker(): SyncWorker {
        return TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker {
                    return SyncWorker(
                        appContext,
                        workerParameters,
                        syncQueueRepository,
                        networkMonitor
                    )
                }
            })
            .build()
    }

    @Test
    fun `worker retries when offline`() = runTest {
        every { networkMonitor.isCurrentlyOffline() } returns true

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        coVerify(exactly = 0) { syncQueueRepository.getPendingOperations() }
    }

    @Test
    fun `worker succeeds when no pending operations`() = runTest {
        every { networkMonitor.isCurrentlyOffline() } returns false
        coEvery { syncQueueRepository.getPendingOperations() } returns emptyList()
        coEvery { syncQueueRepository.deleteSynced() } returns Unit

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { syncQueueRepository.deleteSynced() }
    }

    @Test
    fun `worker syncs eligible operation successfully`() = runTest {
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = """{"club":"7i"}""",
            timestamp = 1000L,
            status = SyncOperation.SyncStatus.PENDING,
            retryCount = 0,
            lastAttemptTimestamp = null
        )

        every { networkMonitor.isCurrentlyOffline() } returns false
        coEvery { syncQueueRepository.getPendingOperations() } returns listOf(operation)
        coEvery { syncQueueRepository.updateStatus(any(), any(), any()) } returns Unit
        coEvery { syncQueueRepository.delete(any()) } returns Unit
        coEvery { syncQueueRepository.deleteSynced() } returns Unit

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        // Verify operation was marked as synced
        coVerify {
            syncQueueRepository.updateStatus(
                1L,
                SyncOperation.SyncStatus.SYNCED
            )
        }

        // Verify operation was deleted
        coVerify { syncQueueRepository.delete(1L) }

        // Verify cleanup
        coVerify { syncQueueRepository.deleteSynced() }
    }

    @Test
    fun `worker skips operation not eligible for retry`() = runTest {
        val currentTime = System.currentTimeMillis()
        val operation = SyncOperation(
            id = 1,
            operationType = SyncOperation.OperationType.SHOT_LOG,
            payload = """{"club":"7i"}""",
            timestamp = currentTime,
            status = SyncOperation.SyncStatus.PENDING,
            retryCount = 2,
            lastAttemptTimestamp = currentTime - 60_000L // 1 minute ago (need 120s for retry 2)
        )

        every { networkMonitor.isCurrentlyOffline() } returns false
        coEvery { syncQueueRepository.getPendingOperations() } returns listOf(operation)
        coEvery { syncQueueRepository.deleteSynced() } returns Unit

        val worker = createWorker()
        val result = worker.doWork()

        // Should succeed because no eligible operations to process
        assertEquals(ListenableWorker.Result.success(), result)

        // Operation should not be updated (skipped)
        coVerify(exactly = 0) {
            syncQueueRepository.updateStatus(any(), any(), any())
        }
        coVerify(exactly = 0) {
            syncQueueRepository.delete(any())
        }
        coVerify(exactly = 0) {
            syncQueueRepository.incrementRetryCount(any())
        }
    }

    @Test
    fun `worker processes multiple operations`() = runTest {
        val operations = listOf(
            SyncOperation(
                id = 1,
                operationType = SyncOperation.OperationType.SHOT_LOG,
                payload = """{"club":"7i"}""",
                timestamp = 1000L,
                status = SyncOperation.SyncStatus.PENDING,
                retryCount = 0,
                lastAttemptTimestamp = null
            ),
            SyncOperation(
                id = 2,
                operationType = SyncOperation.OperationType.ROUND_SYNC,
                payload = """{"roundId":"abc"}""",
                timestamp = 2000L,
                status = SyncOperation.SyncStatus.PENDING,
                retryCount = 0,
                lastAttemptTimestamp = null
            )
        )

        every { networkMonitor.isCurrentlyOffline() } returns false
        coEvery { syncQueueRepository.getPendingOperations() } returns operations
        coEvery { syncQueueRepository.updateStatus(any(), any(), any()) } returns Unit
        coEvery { syncQueueRepository.delete(any()) } returns Unit
        coEvery { syncQueueRepository.deleteSynced() } returns Unit

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)

        // Verify both operations were processed
        coVerify { syncQueueRepository.updateStatus(1L, SyncOperation.SyncStatus.SYNCED) }
        coVerify { syncQueueRepository.updateStatus(2L, SyncOperation.SyncStatus.SYNCED) }
        coVerify { syncQueueRepository.delete(1L) }
        coVerify { syncQueueRepository.delete(2L) }
    }

    @Test
    fun `worker handles exceptions gracefully`() = runTest {
        every { networkMonitor.isCurrentlyOffline() } returns false
        coEvery { syncQueueRepository.getPendingOperations() } throws RuntimeException("Database error")

        val worker = createWorker()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `worker constants are correctly defined`() {
        assertEquals("sync_queue_worker", SyncWorker.WORK_NAME)
        assertEquals("sync", SyncWorker.WORK_TAG)
    }
}
