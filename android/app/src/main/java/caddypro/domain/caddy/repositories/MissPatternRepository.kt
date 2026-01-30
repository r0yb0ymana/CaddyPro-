package caddypro.domain.caddy.repositories

import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for miss pattern data access.
 *
 * Provides domain-level access to player's miss patterns with temporal decay.
 * Used by PinSeeker engine to personalize strategy recommendations.
 *
 * This is a placeholder interface for MVP - implementation will delegate to
 * NavCaddyRepository until miss pattern logic is fully extracted.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 8
 *
 * @see MissPattern
 * @see PinSeekerEngine
 */
interface MissPatternRepository {

    /**
     * Get all miss patterns with temporal decay applied.
     *
     * Patterns are ordered by decayed confidence (highest first).
     * Decay follows 14-day half-life per navcaddy-engine.md Q5.
     *
     * @return Flow of miss patterns ordered by confidence
     */
    fun getMissPatterns(): Flow<List<MissPattern>>

    /**
     * Get the dominant miss pattern for the player.
     *
     * Returns the pattern with highest decayed confidence, or null if no patterns exist.
     *
     * @return Flow of dominant miss direction (null if no patterns)
     */
    fun getDominantMiss(): Flow<MissDirection?>

    /**
     * Get miss patterns for a specific club.
     *
     * Useful for club-specific strategy adjustments.
     *
     * @param clubId The club ID to filter by
     * @return Flow of club-specific patterns
     */
    fun getPatternsByClub(clubId: String): Flow<List<MissPattern>>

    /**
     * Update or insert a miss pattern.
     *
     * Used when new shot data produces updated pattern analysis.
     *
     * @param pattern The pattern to update
     */
    suspend fun updatePattern(pattern: MissPattern)

    /**
     * Delete stale patterns older than retention window.
     *
     * @return Number of patterns deleted
     */
    suspend fun deleteStalePatterns(): Int
}
