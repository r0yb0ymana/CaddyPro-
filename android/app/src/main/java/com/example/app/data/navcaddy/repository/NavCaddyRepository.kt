package caddypro.data.navcaddy.repository

import caddypro.domain.navcaddy.models.BagProfile
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.DistanceAuditEntry
import caddypro.domain.navcaddy.models.MissBias
import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.SessionContext
import caddypro.domain.navcaddy.models.Shot
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for NavCaddy engine persistence.
 *
 * Provides domain-level access to shots, miss patterns, session context,
 * bag profiles, clubs, and distance audit trail.
 * Implementations handle entity-to-domain mapping and retention policies.
 *
 * Spec reference: navcaddy-engine.md R5, R6, C4, C5
 *                 player-profile-bag-management.md R1, R2, R3, R4
 */
interface NavCaddyRepository {
    // ========================================================================
    // Shot Operations
    // ========================================================================

    /**
     * Record a new shot in the database.
     *
     * @param shot The shot to record
     */
    suspend fun recordShot(shot: Shot)

    /**
     * Get recent shots within the specified time window.
     *
     * @param days Number of days to look back (default 90 per Q5)
     * @return Flow of shots within the time window
     */
    fun getRecentShots(days: Int = 90): Flow<List<Shot>>

    /**
     * Get shots for a specific club.
     *
     * @param clubId The club ID to filter by
     * @return Flow of shots for the club
     */
    fun getShotsByClub(clubId: String): Flow<List<Shot>>

    /**
     * Get shots with pressure context.
     *
     * @return Flow of pressure shots
     */
    fun getShotsWithPressure(): Flow<List<Shot>>

    /**
     * Enforce 90-day retention policy by deleting old shots.
     *
     * Spec reference: navcaddy-engine.md Q5
     *
     * @return Number of shots deleted
     */
    suspend fun enforceRetentionPolicy(): Int

    // ========================================================================
    // Miss Pattern Operations
    // ========================================================================

    /**
     * Get all miss patterns.
     *
     * @return Flow of all patterns
     */
    fun getMissPatterns(): Flow<List<MissPattern>>

    /**
     * Get patterns for a specific club.
     *
     * @param clubId The club ID to filter by
     * @return Flow of club-specific patterns
     */
    fun getPatternsByClub(clubId: String): Flow<List<MissPattern>>

    /**
     * Get patterns with pressure context.
     *
     * @return Flow of pressure patterns
     */
    fun getPatternsWithPressure(): Flow<List<MissPattern>>

    /**
     * Update or insert a miss pattern.
     *
     * @param pattern The pattern to update
     */
    suspend fun updatePattern(pattern: MissPattern)

    /**
     * Delete a specific pattern.
     *
     * @param patternId The pattern ID to delete
     */
    suspend fun deletePattern(patternId: String)

    /**
     * Delete stale patterns older than retention window.
     *
     * @return Number of patterns deleted
     */
    suspend fun deleteStalePatterns(): Int

    // ========================================================================
    // Session Operations
    // ========================================================================

    /**
     * Get the current session context.
     *
     * @return Flow of session context
     */
    fun getSession(): Flow<SessionContext>

    /**
     * Save the current session context.
     *
     * @param context The session context to save
     */
    suspend fun saveSession(context: SessionContext)

    /**
     * Add a conversation turn to the session history.
     *
     * Automatically trims history to max size per R6.
     *
     * @param turn The conversation turn to add
     */
    suspend fun addConversationTurn(turn: ConversationTurn)

    /**
     * Clear conversation history for the current session.
     */
    suspend fun clearConversationHistory()

    // ========================================================================
    // Memory Management (C4)
    // ========================================================================

    /**
     * Clear all memory (shots, patterns, session, conversation history).
     *
     * Used for user-requested memory wipe per C4.
     *
     * Spec reference: navcaddy-engine.md C4
     */
    suspend fun clearMemory()

    // ========================================================================
    // Bag Profile Operations (player-profile-bag-management.md R1, R5)
    // ========================================================================

    /**
     * Create a new bag profile.
     *
     * @param profile The bag profile to create
     * @return The created bag profile
     */
    suspend fun createBag(profile: BagProfile): BagProfile

    /**
     * Update an existing bag profile.
     *
     * @param profile The bag profile to update
     */
    suspend fun updateBag(profile: BagProfile)

    /**
     * Archive a bag (soft delete).
     *
     * @param bagId The bag ID to archive
     */
    suspend fun archiveBag(bagId: String)

    /**
     * Duplicate a bag with a new name.
     *
     * Creates a copy of the bag and all its clubs with a new ID.
     *
     * @param bagId The bag ID to duplicate
     * @param newName The name for the duplicated bag
     * @return The newly created bag profile
     */
    suspend fun duplicateBag(bagId: String, newName: String): BagProfile

    /**
     * Get the currently active bag.
     *
     * @return Flow of the active bag (null if none)
     */
    fun getActiveBag(): Flow<BagProfile?>

    /**
     * Get all non-archived bags.
     *
     * @return Flow of all bags
     */
    fun getAllBags(): Flow<List<BagProfile>>

    /**
     * Switch the active bag.
     *
     * Deactivates all bags and activates the specified one.
     *
     * @param bagId The bag ID to activate
     */
    suspend fun switchActiveBag(bagId: String)

    // ========================================================================
    // Club Operations - bag-scoped (player-profile-bag-management.md R2)
    // ========================================================================

    /**
     * Get all clubs for a specific bag.
     *
     * @param bagId The bag ID
     * @return Flow of clubs ordered by position
     */
    fun getClubsForBag(bagId: String): Flow<List<Club>>

    /**
     * Update the estimated carry distance for a club in a bag.
     *
     * @param bagId The bag ID
     * @param clubId The club ID
     * @param estimated The new estimated carry distance in meters
     */
    suspend fun updateClubDistance(bagId: String, clubId: String, estimated: Int)

    /**
     * Update the miss bias for a club in a bag.
     *
     * @param bagId The bag ID
     * @param clubId The club ID
     * @param bias The miss bias to set
     */
    suspend fun updateClubMissBias(bagId: String, clubId: String, bias: MissBias)

    /**
     * Add a club to a bag at a specific position.
     *
     * @param bagId The bag ID
     * @param club The club to add
     * @param position The position in the bag (0-based)
     */
    suspend fun addClubToBag(bagId: String, club: Club, position: Int)

    /**
     * Remove a club from a bag.
     *
     * @param bagId The bag ID
     * @param clubId The club ID to remove
     */
    suspend fun removeClubFromBag(bagId: String, clubId: String)

    // ========================================================================
    // Audit Trail (player-profile-bag-management.md R3)
    // ========================================================================

    /**
     * Record a distance audit entry.
     *
     * @param entry The audit entry to record
     */
    suspend fun recordDistanceAudit(entry: DistanceAuditEntry)

    /**
     * Get audit history for a specific club.
     *
     * @param clubId The club ID
     * @return Flow of audit entries ordered by timestamp descending
     */
    fun getAuditHistory(clubId: String): Flow<List<DistanceAuditEntry>>
}
