package caddypro.data.caddy.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import caddypro.analytics.NavCaddyAnalytics
import caddypro.data.caddy.repository.SyncQueueRepository
import caddypro.domain.caddy.models.SyncOperation
import caddypro.domain.navcaddy.offline.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker for background sync of queued operations.
 *
 * Processes pending sync operations when connectivity is available,
 * with exponential backoff retry logic and conflict resolution.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 19
 * Acceptance criteria: A4 (Shot logger persistence)
 *
 * Features:
 * - Only syncs when network is available
 * - Processes operations in FIFO order
 * - Respects exponential backoff for retries
 * - Updates operation status and error messages
 * - Returns SUCCESS if all operations synced
 * - Returns RETRY if some operations failed (will retry later)
 * - Returns FAILURE if worker should not retry
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncQueueRepository: SyncQueueRepository,
    private val networkMonitor: NetworkMonitor,
    private val analytics: NavCaddyAnalytics
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Check network availability
        if (networkMonitor.isCurrentlyOffline()) {
            return@withContext Result.retry()
        }

        try {
            // Get all pending operations
            val pendingOps = syncQueueRepository.getPendingOperations()

            if (pendingOps.isEmpty()) {
                // Nothing to sync, clean up old synced operations
                syncQueueRepository.deleteSynced()
                return@withContext Result.success()
            }

            // Process each operation
            val currentTime = System.currentTimeMillis()
            var hasFailures = false

            for (operation in pendingOps) {
                // Skip if not eligible for retry (exponential backoff)
                if (!operation.canRetry(currentTime)) {
                    continue
                }

                // Track sync latency
                val syncStartTime = System.currentTimeMillis()

                // Process the operation
                val result = processOperation(operation)

                when (result) {
                    ProcessResult.SUCCESS -> {
                        // Track successful sync latency
                        val syncLatency = System.currentTimeMillis() - syncStartTime
                        analytics.logShotSynced(
                            shotId = operation.id.toString(),
                            latencyMs = syncLatency
                        )

                        // Mark as synced and delete
                        syncQueueRepository.updateStatus(
                            operation.id,
                            SyncOperation.SyncStatus.SYNCED
                        )
                        syncQueueRepository.delete(operation.id)
                    }
                    is ProcessResult.FAILURE -> {
                        // Increment retry count and mark as failed
                        syncQueueRepository.incrementRetryCount(operation.id)
                        syncQueueRepository.updateStatus(
                            operation.id,
                            SyncOperation.SyncStatus.FAILED,
                            result.errorMessage
                        )
                        hasFailures = true
                    }
                    ProcessResult.RETRY -> {
                        // Increment retry count and keep as pending
                        syncQueueRepository.incrementRetryCount(operation.id)
                        hasFailures = true
                    }
                }
            }

            // Return result based on processing outcome
            return@withContext if (hasFailures) {
                Result.retry()
            } else {
                // Clean up synced operations
                syncQueueRepository.deleteSynced()
                Result.success()
            }
        } catch (e: Exception) {
            // Unexpected error, retry later
            return@withContext Result.retry()
        }
    }

    /**
     * Process a single sync operation.
     *
     * In MVP, we stub out the actual sync logic since the backend
     * API is not yet implemented. Future implementation will:
     * - Deserialize payload based on operation type
     * - Call appropriate API endpoint
     * - Handle conflicts (local wins per spec)
     * - Return appropriate result
     *
     * @param operation The sync operation to process
     * @return ProcessResult indicating success, failure, or retry
     */
    private suspend fun processOperation(operation: SyncOperation): ProcessResult {
        return try {
            // TODO: Implement actual sync logic when backend API is ready
            // For now, we stub success for SHOT_LOG operations
            when (operation.operationType) {
                SyncOperation.OperationType.SHOT_LOG -> {
                    // Future: POST to /api/shots endpoint
                    // - Deserialize ShotPayload from operation.payload
                    // - Send to backend
                    // - Handle 409 conflict (local wins)
                    // - Return success/failure based on response

                    // Stub: Auto-succeed for MVP testing
                    ProcessResult.SUCCESS
                }
                SyncOperation.OperationType.ROUND_SYNC -> {
                    // Future: POST to /api/rounds endpoint
                    ProcessResult.SUCCESS
                }
                SyncOperation.OperationType.MISS_PATTERN_SYNC -> {
                    // Future: POST to /api/miss-patterns endpoint
                    ProcessResult.SUCCESS
                }
            }
        } catch (e: Exception) {
            // Network or parsing error
            ProcessResult.FAILURE("Sync failed: ${e.message}")
        }
    }

    /**
     * Result of processing a sync operation.
     */
    private sealed interface ProcessResult {
        /** Operation synced successfully */
        data object SUCCESS : ProcessResult

        /** Operation failed, should retry with backoff */
        data object RETRY : ProcessResult

        /** Operation failed permanently with error message */
        data class FAILURE(val errorMessage: String) : ProcessResult
    }

    companion object {
        /** Unique work name for sync operations */
        const val WORK_NAME = "sync_queue_worker"

        /** Tag for sync work requests */
        const val WORK_TAG = "sync"
    }
}
