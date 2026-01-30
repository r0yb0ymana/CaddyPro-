package caddypro.data.caddy.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a queued sync operation.
 *
 * Maps to domain [SyncOperation] model via extension functions.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 19
 * Acceptance criteria: A4 (Shot logger persistence)
 *
 * Indexes:
 * - status: for efficient pending operation queries
 * - timestamp: for FIFO ordering of operations
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["timestamp"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "operation_type")
    val operationType: String,

    @ColumnInfo(name = "payload")
    val payload: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "last_attempt_timestamp")
    val lastAttemptTimestamp: Long? = null,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null
)
