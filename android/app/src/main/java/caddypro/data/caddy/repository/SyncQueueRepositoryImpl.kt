package caddypro.data.caddy.repository

import caddypro.data.caddy.local.dao.SyncQueueDao
import caddypro.data.caddy.local.entities.SyncQueueEntity
import caddypro.domain.caddy.models.SyncOperation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SyncQueueRepository using Room database.
 *
 * Provides offline-first persistence for sync operations with
 * automatic entity-to-domain mapping.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 19
 * Acceptance criteria: A4 (Shot logger persistence)
 *
 * @see SyncQueueRepository
 * @see SyncQueueDao
 */
@Singleton
class SyncQueueRepositoryImpl @Inject constructor(
    private val syncQueueDao: SyncQueueDao
) : SyncQueueRepository {

    override suspend fun enqueue(operation: SyncOperation): Long {
        val entity = operation.toEntity()
        return syncQueueDao.insert(entity)
    }

    override suspend fun getPendingOperations(): List<SyncOperation> {
        return syncQueueDao.getPendingOperations().map { it.toDomain() }
    }

    override suspend fun getOperationsByStatus(status: SyncOperation.SyncStatus): List<SyncOperation> {
        return syncQueueDao.getOperationsByStatus(status.name)
            .map { it.toDomain() }
    }

    override suspend fun updateStatus(
        operationId: Long,
        status: SyncOperation.SyncStatus,
        errorMessage: String?
    ) {
        syncQueueDao.updateStatus(operationId, status.name, errorMessage)
    }

    override suspend fun incrementRetryCount(operationId: Long) {
        val currentTime = System.currentTimeMillis()
        syncQueueDao.incrementRetryCount(operationId, currentTime)
    }

    override suspend fun delete(operationId: Long) {
        syncQueueDao.delete(operationId)
    }

    override suspend fun deleteSynced() {
        syncQueueDao.deleteSynced()
    }

    override fun observePendingCount(): Flow<Int> {
        return syncQueueDao.observePendingCount()
    }

    override suspend fun getPendingCount(): Int {
        return syncQueueDao.getPendingCount()
    }
}

/**
 * Convert domain SyncOperation to Room entity.
 */
private fun SyncOperation.toEntity(): SyncQueueEntity {
    return SyncQueueEntity(
        id = if (this.id == 0L) 0 else this.id, // Let Room auto-generate if 0
        operationType = this.operationType.name,
        payload = this.payload,
        timestamp = this.timestamp,
        status = this.status.name,
        retryCount = this.retryCount,
        lastAttemptTimestamp = this.lastAttemptTimestamp,
        errorMessage = this.errorMessage
    )
}

/**
 * Convert Room entity to domain SyncOperation.
 */
private fun SyncQueueEntity.toDomain(): SyncOperation {
    return SyncOperation(
        id = this.id,
        operationType = SyncOperation.OperationType.valueOf(this.operationType),
        payload = this.payload,
        timestamp = this.timestamp,
        status = SyncOperation.SyncStatus.valueOf(this.status),
        retryCount = this.retryCount,
        lastAttemptTimestamp = this.lastAttemptTimestamp,
        errorMessage = this.errorMessage
    )
}
