package caddypro.data.caddy.repository

import caddypro.data.caddy.local.dao.ReadinessScoreDao
import caddypro.data.caddy.local.entities.ReadinessScoreEntity
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ReadinessRepository using Room database.
 *
 * Provides offline-first persistence for readiness scores with
 * automatic entity-to-domain mapping.
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 10
 * Acceptance criteria: A2 (Readiness impacts strategy)
 *
 * @see ReadinessRepository
 * @see ReadinessScoreDao
 */
@Singleton
class ReadinessRepositoryImpl @Inject constructor(
    private val readinessScoreDao: ReadinessScoreDao
) : ReadinessRepository {

    override suspend fun saveReadiness(score: ReadinessScore) {
        val entity = score.toEntity()
        readinessScoreDao.insert(entity)
    }

    override suspend fun getMostRecent(): ReadinessScore? {
        return readinessScoreDao.getMostRecent()?.toDomain()
    }

    override suspend fun getHistory(days: Int): List<ReadinessScore> {
        require(days > 0) { "Days must be positive" }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days * 24 * 60 * 60 * 1000L)

        return readinessScoreDao.getScoresInRange(startTime, endTime)
            .map { it.toDomain() }
            .reversed()  // DAO returns ascending, we want descending
    }

    override suspend fun deleteAll() {
        throw UnsupportedOperationException("deleteAll() not supported in MVP - add to ReadinessScoreDao if needed")
    }
}

/**
 * Convert domain ReadinessScore to Room entity.
 */
private fun ReadinessScore.toEntity(): ReadinessScoreEntity {
    return ReadinessScoreEntity(
        timestamp = this.timestamp,
        overall = this.overall,
        hrvScore = this.breakdown.hrv?.value,
        sleepScore = this.breakdown.sleepQuality?.value,
        stressScore = this.breakdown.stressLevel?.value,
        source = this.source.name
    )
}

/**
 * Convert Room entity to domain ReadinessScore.
 *
 * Reconstructs MetricScore objects with appropriate weights from Task 1 decisions:
 * - HRV: 40% (0.4)
 * - Sleep: 40% (0.4)
 * - Stress: 20% (0.2)
 */
private fun ReadinessScoreEntity.toDomain(): ReadinessScore {
    // Fixed weights from spec (Task 1 decisions)
    val hrvWeight = 0.4
    val sleepWeight = 0.4
    val stressWeight = 0.2

    val breakdown = ReadinessBreakdown(
        hrv = hrvScore?.let { MetricScore(value = it, weight = hrvWeight) },
        sleepQuality = sleepScore?.let { MetricScore(value = it, weight = sleepWeight) },
        stressLevel = stressScore?.let { MetricScore(value = it, weight = stressWeight) }
    )

    return ReadinessScore(
        overall = this.overall,
        breakdown = breakdown,
        timestamp = this.timestamp,
        source = ReadinessSource.valueOf(this.source)
    )
}
