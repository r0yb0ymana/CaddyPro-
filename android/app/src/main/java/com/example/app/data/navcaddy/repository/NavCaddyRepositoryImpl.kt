package caddypro.data.navcaddy.repository

import caddypro.domain.navcaddy.models.BagProfile
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.DistanceAuditEntry
import caddypro.domain.navcaddy.models.MissBias
import caddypro.data.navcaddy.NavCaddyDatabase
import caddypro.data.navcaddy.toDomain
import caddypro.data.navcaddy.toEntity
import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.SessionContext
import caddypro.domain.navcaddy.models.Shot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NavCaddyRepository.
 *
 * Handles entity-to-domain mapping and implements retention policies.
 *
 * Spec reference: navcaddy-engine.md R5, R6, C4, C5, Q5
 *                 player-profile-bag-management.md R1, R2, R3, R4, R5
 */
@Singleton
class NavCaddyRepositoryImpl @Inject constructor(
    private val database: NavCaddyDatabase
) : NavCaddyRepository {

    private val shotDao = database.shotDao()
    private val missPatternDao = database.missPatternDao()
    private val sessionDao = database.sessionDao()
    private val bagProfileDao = database.bagProfileDao()
    private val bagClubDao = database.bagClubDao()
    private val distanceAuditDao = database.distanceAuditDao()

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

    // ========================================================================
    // Bag Profile Operations (player-profile-bag-management.md R1, R5)
    // ========================================================================

    override suspend fun createBag(profile: BagProfile): BagProfile {
        bagProfileDao.insertBag(profile.toEntity())
        return profile
    }

    override suspend fun updateBag(profile: BagProfile) {
        val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
        bagProfileDao.updateBag(updatedProfile.toEntity())
    }

    override suspend fun archiveBag(bagId: String) {
        bagProfileDao.archiveBag(bagId)
    }

    override suspend fun duplicateBag(bagId: String, newName: String): BagProfile {
        // Get the source bag to duplicate
        val sourceBag = bagProfileDao.getBagById(bagId).first()
            ?: throw IllegalArgumentException("Bag not found: $bagId")

        // Create a new bag with a new ID and the specified name
        val newBagId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val newBag = sourceBag.copy(
            id = newBagId,
            name = newName,
            isActive = false,
            isArchived = false,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        bagProfileDao.insertBag(newBag)

        // Copy all clubs from the source bag to the new bag
        val sourceClubs = bagClubDao.getClubsForBag(bagId).first()
        sourceClubs.forEach { sourceClub ->
            val newClub = sourceClub.copy(
                id = UUID.randomUUID().toString(),
                bagId = newBagId
            )
            bagClubDao.insertClub(newClub)
        }

        return newBag.toDomain()
    }

    override fun getActiveBag(): Flow<BagProfile?> {
        return bagProfileDao.getActiveBag().map { entity ->
            entity?.toDomain()
        }
    }

    override fun getAllBags(): Flow<List<BagProfile>> {
        return bagProfileDao.getAllBags().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun switchActiveBag(bagId: String) {
        bagProfileDao.switchActiveBag(bagId)
    }

    // ========================================================================
    // Club Operations - bag-scoped (player-profile-bag-management.md R2)
    // ========================================================================

    override fun getClubsForBag(bagId: String): Flow<List<Club>> {
        return bagClubDao.getClubsForBag(bagId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateClubDistance(bagId: String, clubId: String, estimated: Int) {
        bagClubDao.updateClubDistance(bagId, clubId, estimated)
    }

    override suspend fun updateClubMissBias(bagId: String, clubId: String, bias: MissBias) {
        bagClubDao.updateMissBias(
            bagId = bagId,
            clubId = clubId,
            direction = bias.dominantDirection.name,
            missType = bias.missType?.name,
            isUserDefined = bias.isUserDefined,
            confidence = bias.confidence,
            lastUpdated = bias.lastUpdated
        )
    }

    override suspend fun addClubToBag(bagId: String, club: Club, position: Int) {
        bagClubDao.insertClub(club.toEntity(bagId, position))
    }

    override suspend fun removeClubFromBag(bagId: String, clubId: String) {
        bagClubDao.removeClubFromBag(bagId, clubId)
    }

    // ========================================================================
    // Audit Trail (player-profile-bag-management.md R3)
    // ========================================================================

    override suspend fun recordDistanceAudit(entry: DistanceAuditEntry) {
        distanceAuditDao.insertAudit(entry.toEntity())
    }

    override fun getAuditHistory(clubId: String): Flow<List<DistanceAuditEntry>> {
        return distanceAuditDao.getAuditHistory(clubId).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
