package caddypro.data.navcaddy.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import caddypro.data.navcaddy.entities.ShotEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for shot database operations.
 *
 * Spec reference: navcaddy-engine.md R5, Q5 (90-day retention)
 */
@Dao
interface ShotDao {
    /**
     * Insert a shot into the database.
     *
     * @param shot The shot to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShot(shot: ShotEntity)

    /**
     * Get all shots for a specific club.
     *
     * @param clubId The club ID to filter by
     * @return Flow of shots for the club
     */
    @Query("SELECT * FROM shots WHERE club_id = :clubId ORDER BY timestamp DESC")
    fun getShotsByClub(clubId: String): Flow<List<ShotEntity>>

    /**
     * Get shots recorded since a given timestamp.
     *
     * Used for pattern analysis within the retention window.
     *
     * @param sinceTimestamp Minimum timestamp (inclusive)
     * @return Flow of recent shots ordered by timestamp descending
     */
    @Query("SELECT * FROM shots WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getRecentShots(sinceTimestamp: Long): Flow<List<ShotEntity>>

    /**
     * Get shots with pressure context (user tagged or inferred).
     *
     * @return Flow of pressure shots ordered by timestamp descending
     */
    @Query(
        """
        SELECT * FROM shots
        WHERE is_user_tagged_pressure = 1 OR is_inferred_pressure = 1
        ORDER BY timestamp DESC
        """
    )
    fun getShotsWithPressure(): Flow<List<ShotEntity>>

    /**
     * Get shots recorded since a given timestamp with pressure context.
     *
     * @param sinceTimestamp Minimum timestamp (inclusive)
     * @return Flow of recent pressure shots
     */
    @Query(
        """
        SELECT * FROM shots
        WHERE timestamp >= :sinceTimestamp
        AND (is_user_tagged_pressure = 1 OR is_inferred_pressure = 1)
        ORDER BY timestamp DESC
        """
    )
    fun getRecentPressureShots(sinceTimestamp: Long): Flow<List<ShotEntity>>

    /**
     * Delete shots older than a given timestamp.
     *
     * Used for enforcing 90-day retention policy.
     *
     * @param beforeTimestamp Maximum timestamp (exclusive) for deletion
     * @return Number of shots deleted
     */
    @Query("DELETE FROM shots WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldShots(beforeTimestamp: Long): Int

    /**
     * Delete all shots from the database.
     *
     * Used for user-requested memory wipe (C4).
     */
    @Query("DELETE FROM shots")
    suspend fun deleteAllShots()

    /**
     * Get total count of shots in database.
     *
     * @return Flow of shot count
     */
    @Query("SELECT COUNT(*) FROM shots")
    fun getShotCount(): Flow<Int>
}
