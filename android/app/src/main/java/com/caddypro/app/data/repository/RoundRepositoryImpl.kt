package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.RoundDao
import com.caddypro.app.data.local.dao.ShotDao
import com.caddypro.app.data.local.mappers.toDomain
import com.caddypro.app.data.local.mappers.toEntity
import com.caddypro.app.data.sync.SyncManager
import com.caddypro.app.domain.model.Round
import com.caddypro.app.domain.repository.RoundRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Round repository implementation
 *
 * AC3: Only one active round at a time
 * AC21: All shot logging works with no network
 * AC24: Active round survives app restart
 */
@Singleton
class RoundRepositoryImpl @Inject constructor(
    private val roundDao: RoundDao,
    private val shotDao: ShotDao,
    private val syncManager: SyncManager
) : RoundRepository {

    override fun getRoundsByProfileId(profileId: String): Flow<List<Round>> {
        return roundDao.getRoundsByProfileId(profileId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getRoundById(id: String): Round? {
        return roundDao.getRoundById(id)?.toDomain()
    }

    override suspend fun getActiveRound(): Round? {
        return roundDao.getActiveRound()?.toDomain()
    }

    override fun getActiveRoundFlow(): Flow<Round?> {
        return roundDao.getActiveRoundFlow().map { it?.toDomain() }
    }

    override suspend fun createRound(round: Round): Result<Unit> {
        // AC3: Only one active round at a time
        val existingActive = roundDao.getActiveRound()
        if (existingActive != null) {
            return Result.failure(IllegalStateException("An active round already exists. End it before starting a new one."))
        }

        roundDao.insert(round.toEntity())
        syncManager.queueSync()
        return Result.success(Unit)
    }

    override suspend fun endRound(roundId: String): Result<Unit> {
        roundDao.getRoundById(roundId)
            ?: return Result.failure(IllegalStateException("Round not found"))

        val totalShots = shotDao.getTotalShotCount(roundId)
        roundDao.endRound(
            roundId = roundId,
            endedAt = System.currentTimeMillis(),
            totalShots = totalShots,
            updatedAt = System.currentTimeMillis()
        )
        syncManager.queueSync()
        return Result.success(Unit)
    }

    override suspend fun updateShotCount(roundId: String, totalShots: Int) {
        roundDao.updateShotCount(roundId, totalShots, System.currentTimeMillis())
    }
}
