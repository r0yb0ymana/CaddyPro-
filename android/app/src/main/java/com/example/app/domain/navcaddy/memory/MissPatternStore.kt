package caddypro.domain.navcaddy.memory

import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main facade for the Miss Pattern Store subsystem.
 *
 * Combines shot recording and pattern aggregation into a unified interface
 * for the NavCaddy memory system.
 *
 * Spec reference: navcaddy-engine.md R5 (Miss Pattern Memory)
 */
@Singleton
class MissPatternStore @Inject constructor(
    private val shotRecorder: ShotRecorder,
    private val patternAggregator: MissPatternAggregator
) {
    // ========================================================================
    // Shot Recording
    // ========================================================================

    /**
     * Record a miss with full context.
     *
     * Primary method for manual miss entry (Q3: manual only for MVP).
     *
     * @param club Club used for the shot
     * @param direction Direction of miss
     * @param lie Ball lie before the shot
     * @param pressure Pressure context for the shot
     * @param holeNumber Hole number if shot was during a round (optional)
     * @param notes Optional user notes about the shot
     */
    suspend fun recordMiss(
        club: Club,
        direction: MissDirection,
        lie: Lie,
        pressure: PressureContext,
        holeNumber: Int? = null,
        notes: String? = null
    ) {
        shotRecorder.recordShot(
            club = club,
            missDirection = direction,
            lie = lie,
            pressureContext = pressure,
            holeNumber = holeNumber,
            notes = notes
        )
    }

    /**
     * Record a miss with simplified parameters.
     *
     * Convenience method for basic miss entry.
     *
     * @param clubId ID of the club used
     * @param clubName Display name of the club
     * @param direction Direction of miss
     * @param lie Ball lie before the shot
     * @param isUserTaggedPressure Whether user marked this as a pressure shot
     */
    suspend fun recordMiss(
        clubId: String,
        clubName: String,
        direction: MissDirection,
        lie: Lie,
        isUserTaggedPressure: Boolean = false
    ) {
        val clubType = inferClubType(clubName)
        val club = Club(
            id = clubId,
            name = clubName,
            type = clubType,
            estimatedCarry = inferDefaultCarry(clubType)
        )

        val pressure = PressureContext(
            isUserTagged = isUserTaggedPressure,
            isInferred = false
        )

        recordMiss(
            club = club,
            direction = direction,
            lie = lie,
            pressure = pressure
        )
    }

    // ========================================================================
    // Pattern Retrieval
    // ========================================================================

    /**
     * Get all current patterns with decay applied.
     *
     * Returns patterns from recent shot history (default 30 days, 50 shots).
     *
     * @param clubId Optional club ID to filter patterns (null = all clubs)
     * @param pressure Optional pressure context to filter patterns (null = all contexts)
     * @return List of miss patterns ordered by confidence descending
     */
    suspend fun getPatterns(
        clubId: String? = null,
        pressure: PressureContext? = null
    ): List<MissPattern> {
        return when {
            // Club + pressure filter
            clubId != null && pressure?.hasPressure == true -> {
                patternAggregator.getPatternsForClubAndPressure(clubId)
            }

            // Club filter only
            clubId != null -> {
                patternAggregator.getPatternsForClub(clubId)
            }

            // Pressure filter only
            pressure?.hasPressure == true -> {
                patternAggregator.getPatternsForPressure()
            }

            // No filters - all patterns
            else -> {
                patternAggregator.aggregatePatterns()
            }
        }
    }

    /**
     * Get patterns as a Flow for reactive updates.
     *
     * Subscribes to pattern changes with real-time decay applied.
     *
     * @return Flow of patterns ordered by decayed confidence
     */
    fun getPatternsFlow(): Flow<List<MissPattern>> {
        return patternAggregator.getPatternsWithDecay()
    }

    /**
     * Get patterns for a specific club.
     *
     * @param clubId The club ID to filter by
     * @return List of club-specific patterns
     */
    suspend fun getPatternsForClub(clubId: String): List<MissPattern> {
        return getPatterns(clubId = clubId)
    }

    /**
     * Get patterns for pressure situations.
     *
     * @return List of pressure-specific patterns
     */
    suspend fun getPatternsForPressure(): List<MissPattern> {
        return getPatterns(
            pressure = PressureContext(isUserTagged = true, isInferred = true)
        )
    }

    /**
     * Get the most dominant pattern (highest confidence).
     *
     * @param clubId Optional club ID to filter by
     * @param pressure Optional pressure context to filter by
     * @return Most dominant pattern or null if no patterns exist
     */
    suspend fun getDominantPattern(
        clubId: String? = null,
        pressure: PressureContext? = null
    ): MissPattern? {
        return getPatterns(clubId, pressure).maxByOrNull { it.confidence }
    }

    // ========================================================================
    // Shot History
    // ========================================================================

    /**
     * Get recent shot history.
     *
     * @param count Maximum number of shots to retrieve
     * @return List of recent shots ordered by timestamp descending
     */
    suspend fun getRecentShots(count: Int): List<caddypro.domain.navcaddy.models.Shot> {
        return shotRecorder.getMostRecentShots(count)
    }

    /**
     * Get shot history for a specific club.
     *
     * @param clubId The club ID to filter by
     * @param limit Maximum number of shots to retrieve
     * @return List of club shots ordered by timestamp descending
     */
    suspend fun getShotsByClub(clubId: String, limit: Int): List<caddypro.domain.navcaddy.models.Shot> {
        return shotRecorder.getShotsByClub(clubId, limit)
    }

    // ========================================================================
    // Memory Management
    // ========================================================================

    /**
     * Clear all miss history and patterns.
     *
     * User-requested memory wipe per C4.
     *
     * Spec reference: navcaddy-engine.md C4 (privacy controls)
     */
    suspend fun clearHistory() {
        shotRecorder.clearHistory()
    }

    /**
     * Enforce retention policy and clean up old data.
     *
     * Deletes shots and patterns outside the 90-day retention window.
     *
     * @return Number of items deleted
     */
    suspend fun enforceRetentionPolicy(): Int {
        val shotsDeleted = shotRecorder.enforceRetentionPolicy()
        val patternsDeleted = patternAggregator.clearStalePatterns()
        return shotsDeleted + patternsDeleted
    }

    /**
     * Refresh patterns from current shot history.
     *
     * Re-aggregates patterns and persists to database.
     * Useful for periodic pattern updates or manual refresh.
     *
     * @return Number of patterns updated
     */
    suspend fun refreshPatterns(): Int {
        val patterns = patternAggregator.aggregatePatterns()
        patternAggregator.savePatterns(patterns)
        return patterns.size
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Infer club type from club name.
     *
     * Simple heuristic for club type inference when creating clubs from strings.
     * Not comprehensive - for production, use a proper club database.
     *
     * @param clubName Display name of the club
     * @return Inferred club type
     */
    private fun inferClubType(clubName: String): caddypro.domain.navcaddy.models.ClubType {
        val name = clubName.lowercase()
        return when {
            "driver" in name || "1w" in name -> caddypro.domain.navcaddy.models.ClubType.DRIVER
            "wood" in name || name.matches(Regex("\\d+w")) -> caddypro.domain.navcaddy.models.ClubType.WOOD
            "hybrid" in name || name.matches(Regex("\\d+h")) -> caddypro.domain.navcaddy.models.ClubType.HYBRID
            "iron" in name || name.matches(Regex("[3-9]i?")) -> caddypro.domain.navcaddy.models.ClubType.IRON
            "wedge" in name || "pw" in name || "sw" in name || "lw" in name || "gw" in name -> caddypro.domain.navcaddy.models.ClubType.WEDGE
            "putter" in name -> caddypro.domain.navcaddy.models.ClubType.PUTTER
            else -> caddypro.domain.navcaddy.models.ClubType.IRON // Default fallback
        }
    }

    /**
     * Infer default carry distance from club type.
     *
     * Provides reasonable default carry distances in meters for each club type.
     * Used when creating clubs from strings without explicit distance info.
     *
     * @param clubType The club type
     * @return Estimated carry distance in meters
     */
    private fun inferDefaultCarry(clubType: caddypro.domain.navcaddy.models.ClubType): Int {
        return when (clubType) {
            caddypro.domain.navcaddy.models.ClubType.DRIVER -> 220
            caddypro.domain.navcaddy.models.ClubType.WOOD -> 190
            caddypro.domain.navcaddy.models.ClubType.HYBRID -> 170
            caddypro.domain.navcaddy.models.ClubType.IRON -> 140
            caddypro.domain.navcaddy.models.ClubType.WEDGE -> 100
            caddypro.domain.navcaddy.models.ClubType.PUTTER -> 0
        }
    }
}
