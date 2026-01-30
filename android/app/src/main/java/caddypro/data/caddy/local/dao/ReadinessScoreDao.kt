package caddypro.data.caddy.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import caddypro.data.caddy.local.entities.ReadinessScoreEntity

/**
 * DAO for readiness score database operations.
 *
 * Provides async access to persisted readiness data for offline-first functionality
 * and historical trend analysis.
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 3
 */
@Dao
interface ReadinessScoreDao {
    /**
     * Insert a readiness score into the database.
     *
     * If a score with the same timestamp already exists, it will be replaced.
     * This allows updating scores for the same time period (e.g., manual override).
     *
     * @param score The readiness score to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: ReadinessScoreEntity)

    /**
     * Get the most recent readiness score.
     *
     * Used to display current readiness state and apply strategy adjustments.
     *
     * @return The most recent score (null if no scores exist)
     */
    @Query("SELECT * FROM readiness_scores ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecent(): ReadinessScoreEntity?

    /**
     * Get readiness scores within a time range.
     *
     * Used for trend analysis and historical visualization.
     * Results are ordered chronologically (oldest first).
     *
     * @param start Start timestamp (inclusive) in epoch milliseconds
     * @param end End timestamp (inclusive) in epoch milliseconds
     * @return List of scores in the time range, ordered by timestamp ascending
     */
    @Query("SELECT * FROM readiness_scores WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    suspend fun getScoresInRange(start: Long, end: Long): List<ReadinessScoreEntity>
}
