package caddypro.data.caddy.repository

import caddypro.domain.caddy.models.ReadinessScore

/**
 * Repository interface for readiness score data operations.
 *
 * Provides access to current and historical readiness scores with persistence
 * for offline-first functionality.
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 10
 * Acceptance criteria: A2 (Readiness impacts strategy)
 */
interface ReadinessRepository {

    /**
     * Save a readiness score to persistent storage.
     *
     * If a score with the same timestamp already exists, it will be replaced.
     * This allows manual override of wearable-synced scores.
     *
     * @param score The readiness score to save
     */
    suspend fun saveReadiness(score: ReadinessScore)

    /**
     * Get the most recent readiness score.
     *
     * Used to apply current readiness adjustments to strategy recommendations.
     *
     * @return Most recent ReadinessScore, or null if no scores exist
     */
    suspend fun getMostRecent(): ReadinessScore?

    /**
     * Get readiness scores from the past N days.
     *
     * Used for trend analysis and readiness history visualization.
     *
     * @param days Number of days to look back
     * @return List of readiness scores, ordered by timestamp descending (most recent first)
     */
    suspend fun getHistory(days: Int): List<ReadinessScore>

    /**
     * Delete all readiness scores.
     *
     * Used when user wants to clear their readiness history.
     */
    suspend fun deleteAll()
}
