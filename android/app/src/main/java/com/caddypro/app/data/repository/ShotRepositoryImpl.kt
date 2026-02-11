package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.ShotDao
import com.caddypro.app.data.local.mappers.toDomain
import com.caddypro.app.data.local.mappers.toEntity
import com.caddypro.app.data.sync.SyncManager
import com.caddypro.app.domain.model.Shot
import com.caddypro.app.domain.repository.ShotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shot repository implementation
 *
 * AC9: Shot number auto-increments per hole
 * AC20: Shots deletable with shot renumbering
 * AC21: All shot logging works with no network
 */
@Singleton
class ShotRepositoryImpl @Inject constructor(
    private val shotDao: ShotDao,
    private val syncManager: SyncManager
) : ShotRepository {

    override fun getShotsByRoundId(roundId: String): Flow<List<Shot>> {
        return shotDao.getShotsByRoundId(roundId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getShotsByHole(roundId: String, holeNumber: Int): Flow<List<Shot>> {
        return shotDao.getShotsByHole(roundId, holeNumber).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getShotCountForHole(roundId: String, holeNumber: Int): Int {
        return shotDao.getShotCountForHole(roundId, holeNumber)
    }

    override suspend fun getTotalShotCount(roundId: String): Int {
        return shotDao.getTotalShotCount(roundId)
    }

    override suspend fun logShot(shot: Shot): Result<Unit> {
        return try {
            shotDao.insert(shot.toEntity())
            syncManager.queueSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateShot(shot: Shot): Result<Unit> {
        return try {
            shotDao.update(shot.copy(updatedAt = System.currentTimeMillis(), synced = false).toEntity())
            syncManager.queueSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteShot(shotId: String): Result<Unit> {
        val shot = shotDao.getShotById(shotId)
            ?: return Result.failure(IllegalStateException("Shot not found"))

        shotDao.delete(shot)
        // AC20: Renumber shots after delete
        shotDao.renumberShotsAfterDelete(shot.roundId, shot.holeNumber, shot.shotNumber)
        syncManager.queueSync()
        return Result.success(Unit)
    }

    override suspend fun getLastShot(roundId: String): Shot? {
        return shotDao.getLastShot(roundId)?.toDomain()
    }
}
