package caddypro.data.navcaddy.repository

import caddypro.data.navcaddy.NavCaddyDatabase
import caddypro.data.navcaddy.toDomain
import caddypro.data.navcaddy.toEntity
import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.SessionContext
import caddypro.domain.navcaddy.models.Shot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NavCaddyRepository.
 *
 * Handles entity-to-domain mapping and implements retention policies.
 *
 * Spec reference: navcaddy-engine.md R5, R6, C4, C5, Q5
 */
@Singleton
class NavCaddyRepositoryImpl @Inject constructor(
    private val database: NavCaddyDatabase
) : NavCaddyRepository {

    private val shotDao = database.shotDao()
    private val missPatternDao = database.missPatternDao()
    private val sessionDao = database.sessionDao()

    companion object {
        /** 90-day retention window in milliseconds */
        private const val RETENTION_WINDOW_MS = 90L * 24 * 60 * 60 * 1000

        /** Maximum conversation history size */
        private const val MAX_CONVERSATION_HISTORY = 10
    }

    // ========================================================================
    // Shot Operations
    // ========================================================================

    override suspend fun recordShot(shot: Shot) {
        shotDao.insertShot(shot.toEntity())
    }

    override fun getRecentShots(days: Int): Flow<List<Shot>> {
        val sinceTimestamp = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return shotDao.getRecentShots(sinceTimestamp).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getShotsByClub(clubId: String): Flow<List<Shot>> {
        return shotDao.getShotsByClub(clubId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getShotsWithPressure(): Flow<List<Shot>> {
        return shotDao.getShotsWithPressure().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun enforceRetentionPolicy(): Int {
        val cutoffTimestamp = System.currentTimeMillis() - RETENTION_WINDOW_MS
        return shotDao.deleteOldShots(cutoffTimestamp)
    }

    // ========================================================================
    // Miss Pattern Operations
    // ========================================================================

    override fun getMissPatterns(): Flow<List<MissPattern>> {
        return missPatternDao.getAllPatterns().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPatternsByClub(clubId: String): Flow<List<MissPattern>> {
        return missPatternDao.getPatternsByClub(clubId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPatternsWithPressure(): Flow<List<MissPattern>> {
        return missPatternDao.getPatternsWithPressure().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updatePattern(pattern: MissPattern) {
        missPatternDao.upsertPattern(pattern.toEntity())
    }

    override suspend fun deletePattern(patternId: String) {
        missPatternDao.deletePattern(patternId)
    }

    override suspend fun deleteStalePatterns(): Int {
        val cutoffTimestamp = System.currentTimeMillis() - RETENTION_WINDOW_MS
        return missPatternDao.deleteStalePatterns(cutoffTimestamp)
    }

    // ========================================================================
    // Session Operations
    // ========================================================================

    override fun getSession(): Flow<SessionContext> {
        return combine(
            sessionDao.getCurrentSession(),
            sessionDao.getConversationHistory(limit = MAX_CONVERSATION_HISTORY)
        ) { sessionEntity, conversationEntities ->
            val conversationTurns = conversationEntities.map { it.toDomain() }.reversed()
            sessionEntity?.toDomain(conversationTurns) ?: SessionContext.empty
        }
    }

    override suspend fun saveSession(context: SessionContext) {
        sessionDao.saveSession(context.toEntity())
    }

    override suspend fun addConversationTurn(turn: ConversationTurn) {
        sessionDao.insertConversationTurn(turn.toEntity())
        // Trim history to maintain max size
        sessionDao.trimConversationHistory(keepCount = MAX_CONVERSATION_HISTORY)
    }

    override suspend fun clearConversationHistory() {
        sessionDao.deleteConversationHistory()
    }

    // ========================================================================
    // Memory Management (C4)
    // ========================================================================

    override suspend fun clearMemory() {
        shotDao.deleteAllShots()
        missPatternDao.deleteAllPatterns()
        sessionDao.deleteCurrentSession()
        sessionDao.deleteAllConversationTurns()
    }
}
