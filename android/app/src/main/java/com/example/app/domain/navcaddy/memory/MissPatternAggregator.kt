package caddypro.domain.navcaddy.memory

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Shot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates miss patterns from shot history.
 *
 * Analyzes shots within a rolling window and generates patterns with
 * time-based decay applied to confidence scores.
 *
 * Spec reference: navcaddy-engine.md R5
 */
@Singleton
class MissPatternAggregator @Inject constructor(
    private val repository: NavCaddyRepository,
    private val decayCalculator: PatternDecayCalculator
) {
    companion object {
        /** Default rolling window in days for pattern analysis */
        const val DEFAULT_WINDOW_DAYS = 30

        /** Default maximum number of shots to analyze */
        const val DEFAULT_SHOT_LIMIT = 50

        /** Minimum shots required to establish a pattern */
        const val MIN_SHOTS_FOR_PATTERN = 3

        /** Minimum frequency percentage to consider a direction a "pattern" (e.g., 0.3 = 30%) */
        const val MIN_FREQUENCY_THRESHOLD = 0.3f
    }

    /**
     * Aggregate patterns from recent shots.
     *
     * Analyzes shots within the rolling window (default 30 days or 50 shots, whichever is smaller)
     * and generates patterns with decay-adjusted confidence.
     *
     * @param days Rolling window in days (default 30)
     * @param maxShots Maximum number of shots to analyze (default 50)
     * @return List of aggregated patterns ordered by decayed confidence descending
     */
    suspend fun aggregatePatterns(
        days: Int = DEFAULT_WINDOW_DAYS,
        maxShots: Int = DEFAULT_SHOT_LIMIT
    ): List<MissPattern> {
        val shots = repository.getRecentShots(days)
            .first()
            .take(maxShots)

        return aggregatePatterns(shots)
    }

    /**
     * Aggregate patterns from a specific list of shots.
     *
     * Groups shots by miss direction and calculates frequency and confidence.
     * Applies time-based decay to confidence scores.
     *
     * @param shots List of shots to analyze
     * @return List of aggregated patterns ordered by decayed confidence descending
     */
    suspend fun aggregatePatterns(shots: List<Shot>): List<MissPattern> {
        if (shots.size < MIN_SHOTS_FOR_PATTERN) {
            return emptyList()
        }

        val currentTime = System.currentTimeMillis()

        // Filter out straight shots (we only care about misses)
        val missedShots = shots.filter {
            it.missDirection != null && it.missDirection != MissDirection.STRAIGHT
        }

        if (missedShots.isEmpty()) {
            return emptyList()
        }

        // Group by miss direction and calculate patterns
        return missedShots
            .groupBy { it.missDirection!! }
            .mapNotNull { (direction, directionShots) ->
                createPattern(
                    direction = direction,
                    shots = directionShots,
                    totalShots = shots.size,
                    currentTime = currentTime
                )
            }
            .sortedByDescending { it.confidence }
    }

    /**
     * Get patterns for a specific club.
     *
     * Analyzes shots for the club within the rolling window.
     *
     * @param clubId The club ID to filter by
     * @param days Rolling window in days (default 30)
     * @param maxShots Maximum number of shots to analyze (default 50)
     * @return List of club-specific patterns ordered by decayed confidence descending
     */
    suspend fun getPatternsForClub(
        clubId: String,
        days: Int = DEFAULT_WINDOW_DAYS,
        maxShots: Int = DEFAULT_SHOT_LIMIT
    ): List<MissPattern> {
        val shots = repository.getShotsByClub(clubId)
            .first()
            .filter { decayCalculator.isWithinRetentionWindow(it.timestamp, days) }
            .take(maxShots)

        val patterns = aggregatePatterns(shots)

        // Add club information to patterns
        return patterns.map { pattern ->
            if (shots.isNotEmpty()) {
                pattern.copy(club = shots.first().club)
            } else {
                pattern
            }
        }
    }

    /**
     * Get patterns for a specific pressure context.
     *
     * Analyzes pressure shots within the rolling window.
     *
     * @param days Rolling window in days (default 30)
     * @param maxShots Maximum number of shots to analyze (default 50)
     * @return List of pressure-specific patterns ordered by decayed confidence descending
     */
    suspend fun getPatternsForPressure(
        days: Int = DEFAULT_WINDOW_DAYS,
        maxShots: Int = DEFAULT_SHOT_LIMIT
    ): List<MissPattern> {
        val shots = repository.getShotsWithPressure()
            .first()
            .filter { decayCalculator.isWithinRetentionWindow(it.timestamp, days) }
            .take(maxShots)

        val patterns = aggregatePatterns(shots)

        // Mark patterns as pressure-related
        return patterns.map { pattern ->
            pattern.copy(
                pressureContext = PressureContext(
                    isUserTagged = shots.any { it.pressureContext.isUserTagged },
                    isInferred = shots.any { it.pressureContext.isInferred }
                )
            )
        }
    }

    /**
     * Get patterns for a specific club under pressure.
     *
     * Analyzes pressure shots for the club within the rolling window.
     *
     * @param clubId The club ID to filter by
     * @param days Rolling window in days (default 30)
     * @param maxShots Maximum number of shots to analyze (default 50)
     * @return List of club-pressure patterns ordered by decayed confidence descending
     */
    suspend fun getPatternsForClubAndPressure(
        clubId: String,
        days: Int = DEFAULT_WINDOW_DAYS,
        maxShots: Int = DEFAULT_SHOT_LIMIT
    ): List<MissPattern> {
        val shots = repository.getShotsByClub(clubId)
            .first()
            .filter { it.pressureContext.hasPressure }
            .filter { decayCalculator.isWithinRetentionWindow(it.timestamp, days) }
            .take(maxShots)

        val patterns = aggregatePatterns(shots)

        // Add club and pressure information
        return patterns.map { pattern ->
            pattern.copy(
                club = if (shots.isNotEmpty()) shots.first().club else null,
                pressureContext = PressureContext(
                    isUserTagged = shots.any { it.pressureContext.isUserTagged },
                    isInferred = shots.any { it.pressureContext.isInferred }
                )
            )
        }
    }

    /**
     * Get patterns from repository with decay applied.
     *
     * Returns persisted patterns with current decayed confidence values.
     *
     * @return Flow of patterns with decay applied
     */
    fun getPatternsWithDecay(): Flow<List<MissPattern>> {
        val currentTime = System.currentTimeMillis()
        return repository.getMissPatterns().map { patterns ->
            patterns
                .map { pattern ->
                    pattern.copy(
                        confidence = decayCalculator.calculateDecayedConfidence(
                            baseConfidence = pattern.confidence,
                            lastOccurrence = pattern.lastOccurrence,
                            currentTime = currentTime
                        )
                    )
                }
                .filter { it.confidence > 0.01f } // Filter out nearly-decayed patterns
                .sortedByDescending { it.confidence }
        }
    }

    /**
     * Create a miss pattern from a group of shots with the same direction.
     *
     * Calculates frequency, base confidence, and applies time-based decay.
     *
     * @param direction The miss direction for this pattern
     * @param shots Shots with this miss direction
     * @param totalShots Total number of shots in the analysis window
     * @param currentTime Current timestamp for decay calculation
     * @return MissPattern or null if pattern doesn't meet thresholds
     */
    private fun createPattern(
        direction: MissDirection,
        shots: List<Shot>,
        totalShots: Int,
        currentTime: Long
    ): MissPattern? {
        val frequency = shots.size
        val frequencyRatio = frequency.toFloat() / totalShots

        // Filter out patterns below threshold
        if (frequencyRatio < MIN_FREQUENCY_THRESHOLD) {
            return null
        }

        // Calculate base confidence from frequency ratio
        val baseConfidence = frequencyRatio.coerceIn(0f, 1f)

        // Find most recent occurrence
        val lastOccurrence = shots.maxOfOrNull { it.timestamp } ?: currentTime

        // Apply decay to base confidence
        val decayedConfidence = decayCalculator.calculateDecayedConfidence(
            baseConfidence = baseConfidence,
            lastOccurrence = lastOccurrence,
            currentTime = currentTime
        )

        return MissPattern(
            id = UUID.randomUUID().toString(),
            direction = direction,
            club = null, // Club will be added by caller if applicable
            frequency = frequency,
            confidence = decayedConfidence,
            pressureContext = null, // Pressure context will be added by caller if applicable
            lastOccurrence = lastOccurrence
        )
    }

    /**
     * Save aggregated patterns to the database.
     *
     * Persists patterns for retrieval without re-aggregation.
     *
     * @param patterns List of patterns to save
     */
    suspend fun savePatterns(patterns: List<MissPattern>) {
        patterns.forEach { pattern ->
            repository.updatePattern(pattern)
        }
    }

    /**
     * Clear stale patterns from the database.
     *
     * Removes patterns outside the retention window.
     *
     * @return Number of patterns deleted
     */
    suspend fun clearStalePatterns(): Int {
        return repository.deleteStalePatterns()
    }
}
