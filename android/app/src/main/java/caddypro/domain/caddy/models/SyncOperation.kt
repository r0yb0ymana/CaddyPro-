package caddypro.domain.caddy.models

/**
 * Domain model representing a queued sync operation.
 *
 * Supports offline-first shot logging with automatic background sync
 * when connectivity returns. Operations are persisted locally and retried
 * with exponential backoff on failure.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 19
 * Acceptance criteria: A4 (Shot logger persistence)
 *
 * @property id Unique identifier for the sync operation
 * @property operationType Type of operation (SHOT_LOG, etc.)
 * @property payload Serialized operation data
 * @property timestamp When the operation was created (unix millis)
 * @property status Current sync status
 * @property retryCount Number of sync attempts
 * @property lastAttemptTimestamp Last sync attempt timestamp (null if never attempted)
 * @property errorMessage Error message from last failed attempt
 */
data class SyncOperation(
    val id: Long,
    val operationType: OperationType,
    val payload: String,
    val timestamp: Long,
    val status: SyncStatus,
    val retryCount: Int = 0,
    val lastAttemptTimestamp: Long? = null,
    val errorMessage: String? = null
) {
    /**
     * Type of sync operation.
     */
    enum class OperationType {
        /** Shot logging operation */
        SHOT_LOG,

        /** Round data sync */
        ROUND_SYNC,

        /** Miss pattern update */
        MISS_PATTERN_SYNC
    }

    /**
     * Current status of the sync operation.
     */
    enum class SyncStatus {
        /** Pending sync (not yet attempted) */
        PENDING,

        /** Currently being synced */
        SYNCING,

        /** Successfully synced */
        SYNCED,

        /** Failed after retry attempts */
        FAILED
    }

    /**
     * Check if operation is eligible for retry.
     *
     * Uses exponential backoff to avoid overwhelming the server.
     * Base delay: 30 seconds, max delay: 30 minutes.
     *
     * @param currentTime Current timestamp in millis
     * @return true if operation can be retried now
     */
    fun canRetry(currentTime: Long): Boolean {
        if (status == SyncStatus.SYNCED) return false
        if (lastAttemptTimestamp == null) return true

        val backoffDelayMs = calculateBackoffDelay()
        val timeSinceLastAttempt = currentTime - lastAttemptTimestamp

        return timeSinceLastAttempt >= backoffDelayMs
    }

    /**
     * Calculate exponential backoff delay in milliseconds.
     *
     * Formula: min(30_000 * 2^retryCount, 1_800_000)
     * - Retry 0: 30 seconds
     * - Retry 1: 60 seconds
     * - Retry 2: 120 seconds
     * - Retry 3: 240 seconds
     * - Retry 4: 480 seconds
     * - Retry 5+: 30 minutes (capped)
     *
     * @return Delay in milliseconds before next retry
     */
    private fun calculateBackoffDelay(): Long {
        val baseDelayMs = 30_000L // 30 seconds
        val maxDelayMs = 1_800_000L // 30 minutes

        val exponentialDelay = baseDelayMs * (1 shl retryCount)
        return minOf(exponentialDelay, maxDelayMs)
    }
}
