package caddypro.data.navcaddy.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import caddypro.data.navcaddy.entities.MissPatternEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for miss pattern database operations.
 *
 * Spec reference: navcaddy-engine.md R5
 */
@Dao
interface MissPatternDao {
    /**
     * Insert or update a miss pattern.
     *
     * Uses REPLACE strategy to update existing patterns with same ID.
     *
     * @param pattern The pattern to upsert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPattern(pattern: MissPatternEntity)

    /**
     * Get all miss patterns.
     *
     * @return Flow of all patterns ordered by last occurrence descending
     */
    @Query("SELECT * FROM miss_patterns ORDER BY last_occurrence DESC")
    fun getAllPatterns(): Flow<List<MissPatternEntity>>

    /**
     * Get patterns for a specific miss direction.
     *
     * @param direction The miss direction to filter by
     * @return Flow of patterns with the given direction
     */
    @Query("SELECT * FROM miss_patterns WHERE direction = :direction ORDER BY last_occurrence DESC")
    fun getPatternsByDirection(direction: String): Flow<List<MissPatternEntity>>

    /**
     * Get patterns for a specific club.
     *
     * @param clubId The club ID to filter by (null matches patterns without specific club)
     * @return Flow of club-specific patterns
     */
    @Query("SELECT * FROM miss_patterns WHERE club_id = :clubId ORDER BY last_occurrence DESC")
    fun getPatternsByClub(clubId: String): Flow<List<MissPatternEntity>>

    /**
     * Get patterns with pressure context.
     *
     * @return Flow of pressure-related patterns ordered by last occurrence descending
     */
    @Query(
        """
        SELECT * FROM miss_patterns
        WHERE is_user_tagged_pressure = 1 OR is_inferred_pressure = 1
        ORDER BY last_occurrence DESC
        """
    )
    fun getPatternsWithPressure(): Flow<List<MissPatternEntity>>

    /**
     * Get patterns with pressure context for a specific club.
     *
     * @param clubId The club ID to filter by
     * @return Flow of pressure patterns for the club
     */
    @Query(
        """
        SELECT * FROM miss_patterns
        WHERE club_id = :clubId
        AND (is_user_tagged_pressure = 1 OR is_inferred_pressure = 1)
        ORDER BY last_occurrence DESC
        """
    )
    fun getPressurePatternsForClub(clubId: String): Flow<List<MissPatternEntity>>

    /**
     * Delete patterns older than a given timestamp.
     *
     * Used for enforcing retention policy and removing stale patterns.
     *
     * @param beforeTimestamp Maximum last occurrence timestamp (exclusive) for deletion
     * @return Number of patterns deleted
     */
    @Query("DELETE FROM miss_patterns WHERE last_occurrence < :beforeTimestamp")
    suspend fun deleteStalePatterns(beforeTimestamp: Long): Int

    /**
     * Delete all patterns from the database.
     *
     * Used for user-requested memory wipe (C4).
     */
    @Query("DELETE FROM miss_patterns")
    suspend fun deleteAllPatterns()

    /**
     * Delete a specific pattern by ID.
     *
     * @param id The pattern ID to delete
     */
    @Query("DELETE FROM miss_patterns WHERE id = :id")
    suspend fun deletePattern(id: String)
}
