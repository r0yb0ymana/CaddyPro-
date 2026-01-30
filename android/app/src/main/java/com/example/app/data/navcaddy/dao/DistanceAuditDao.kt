package caddypro.data.navcaddy.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import caddypro.data.navcaddy.entities.DistanceAuditEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for distance audit trail database operations.
 *
 * Spec reference: player-profile-bag-management.md R3
 * Plan reference: player-profile-bag-management-plan.md Task 3
 */
@Dao
interface DistanceAuditDao {
    /**
     * Insert an audit entry.
     *
     * @param audit The audit entry to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: DistanceAuditEntity)

    /**
     * Get audit history for a specific club.
     *
     * @param clubId The club ID
     * @return Flow of audit entries ordered by timestamp descending
     */
    @Query("SELECT * FROM distance_audits WHERE club_id = :clubId ORDER BY timestamp DESC")
    fun getAuditHistory(clubId: String): Flow<List<DistanceAuditEntity>>

    /**
     * Get audit history for a specific club with limit.
     *
     * @param clubId The club ID
     * @param limit Maximum number of entries to return
     * @return Flow of audit entries ordered by timestamp descending
     */
    @Query("SELECT * FROM distance_audits WHERE club_id = :clubId ORDER BY timestamp DESC LIMIT :limit")
    fun getAuditHistoryLimited(clubId: String, limit: Int): Flow<List<DistanceAuditEntity>>

    /**
     * Get all audit entries since a given timestamp.
     *
     * @param sinceTimestamp Minimum timestamp (inclusive)
     * @return Flow of recent audit entries ordered by timestamp descending
     */
    @Query("SELECT * FROM distance_audits WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun getAuditsSince(sinceTimestamp: Long): Flow<List<DistanceAuditEntity>>

    /**
     * Get count of audit entries for a club.
     *
     * @param clubId The club ID
     * @return Flow of audit count
     */
    @Query("SELECT COUNT(*) FROM distance_audits WHERE club_id = :clubId")
    fun getAuditCount(clubId: String): Flow<Int>

    /**
     * Delete audit entries older than a given timestamp.
     *
     * Can be used for data retention policies.
     *
     * @param beforeTimestamp Maximum timestamp (exclusive) for deletion
     * @return Number of entries deleted
     */
    @Query("DELETE FROM distance_audits WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldAudits(beforeTimestamp: Long): Int

    /**
     * Delete all audit entries for a specific club.
     *
     * @param clubId The club ID
     */
    @Query("DELETE FROM distance_audits WHERE club_id = :clubId")
    suspend fun deleteAuditsForClub(clubId: String)

    /**
     * Delete all audit entries (for testing).
     */
    @Query("DELETE FROM distance_audits")
    suspend fun deleteAllAudits()
}
