package caddypro.data.navcaddy.repository

import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.SessionContext
import caddypro.domain.navcaddy.models.Shot
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for NavCaddy engine persistence.
 *
 * Provides domain-level access to shots, miss patterns, and session context.
 * Implementations handle entity-to-domain mapping and retention policies.
 *
 * Spec reference: navcaddy-engine.md R5, R6, C4, C5
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
}
