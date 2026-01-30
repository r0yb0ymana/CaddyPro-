package caddypro.data.caddy.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import caddypro.data.caddy.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for sync queue operations.
 *
 * Provides CRUD operations for offline sync queue with efficient
 * queries for background sync processing.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 19
 * Acceptance criteria: A4 (Shot logger persistence)
 */
@Dao
interface SyncQueueDao {

    /**
     * Insert a new sync operation.
     *
     * Returns the generated ID for the operation.
     *
     * @param operation The sync operation to insert
     * @return Generated operation ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncQueueEntity): Long

    /**
     * Get all pending operations ordered by timestamp (oldest first).
     *
     * Used by background sync worker to process operations in FIFO order.
     *
     * @return List of pending operations
     */
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingOperations(): List<SyncQueueEntity>

    /**
     * Get operations by status.
     *
     * @param status The sync status to filter by
     * @return List of operations with specified status
     */
    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY timestamp ASC")
    suspend fun getOperationsByStatus(status: String): List<SyncQueueEntity>

    /**
     * Update operation status.
     *
     * @param operationId The operation ID to update
     * @param status New status value
     * @param errorMessage Optional error message (set to null to clear)
     */
    @Query("UPDATE sync_queue SET status = :status, error_message = :errorMessage WHERE id = :operationId")
    suspend fun updateStatus(operationId: Long, status: String, errorMessage: String?)

    /**
     * Increment retry count and update last attempt timestamp.
     *
     * @param operationId The operation ID to update
     * @param currentTime Current timestamp in millis
     */
    @Query(
        """
        UPDATE sync_queue
        SET retry_count = retry_count + 1,
            last_attempt_timestamp = :currentTime
        WHERE id = :operationId
        """
    )
    suspend fun incrementRetryCount(operationId: Long, currentTime: Long)

    /**
     * Delete an operation.
     *
     * @param operationId The operation ID to delete
     */
    @Query("DELETE FROM sync_queue WHERE id = :operationId")
    suspend fun delete(operationId: Long)

    /**
     * Delete all synced operations.
     *
     * Cleanup method to prevent unbounded queue growth.
     */
    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteSynced()

    /**
     * Observe count of pending operations.
     *
     * Emits whenever pending count changes, used for UI sync status display.
     *
     * @return Flow of pending operation count
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    /**
     * Get count of pending operations.
     *
     * @return Number of pending operations
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int
}
