package caddypro.data.caddy.repository

import caddypro.domain.caddy.models.SyncOperation
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for offline sync queue operations.
 *
 * Provides persistence for queued sync operations that execute when
 * connectivity returns. Supports offline-first shot logging and data sync.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 19
 * Acceptance criteria: A4 (Shot logger persistence)
 */
interface SyncQueueRepository {

    /**
     * Add a new operation to the sync queue.
     *
     * Operation is persisted immediately and will be processed when online.
     *
     * @param operation The sync operation to queue
     * @return ID of the queued operation
     */
    suspend fun enqueue(operation: SyncOperation): Long

    /**
     * Get all pending operations.
     *
     * Returns operations that need to be synced, ordered by timestamp
     * (oldest first) to maintain FIFO ordering.
     *
     * @return List of pending sync operations
     */
    suspend fun getPendingOperations(): List<SyncOperation>

    /**
     * Get all operations with a specific status.
     *
     * @param status The sync status to filter by
     * @return List of operations with the specified status
     */
    suspend fun getOperationsByStatus(status: SyncOperation.SyncStatus): List<SyncOperation>

    /**
     * Update operation status.
     *
     * Used to mark operations as syncing, synced, or failed during
     * background sync processing.
     *
     * @param operationId The operation ID to update
     * @param status New sync status
     * @param errorMessage Optional error message if status is FAILED
     */
    suspend fun updateStatus(
        operationId: Long,
        status: SyncOperation.SyncStatus,
        errorMessage: String? = null
    )

    /**
     * Increment retry count for an operation.
     *
     * Updates retry count and last attempt timestamp for exponential
     * backoff calculations.
     *
     * @param operationId The operation ID to update
     */
    suspend fun incrementRetryCount(operationId: Long)

    /**
     * Delete a successfully synced operation.
     *
     * Removes operation from queue after successful sync to prevent
     * unbounded growth.
     *
     * @param operationId The operation ID to delete
     */
    suspend fun delete(operationId: Long)

    /**
     * Delete all synced operations.
     *
     * Cleanup method to remove successfully synced operations in batch.
     */
    suspend fun deleteSynced()

    /**
     * Observe pending operation count.
     *
     * Flow that emits the count of pending operations whenever it changes.
     * Used to display sync status in UI.
     *
     * @return Flow of pending operation count
     */
    fun observePendingCount(): Flow<Int>

    /**
     * Get count of pending operations.
     *
     * Synchronous count for one-off checks.
     *
     * @return Number of pending operations
     */
    suspend fun getPendingCount(): Int
}
