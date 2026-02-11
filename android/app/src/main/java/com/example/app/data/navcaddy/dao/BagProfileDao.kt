package caddypro.data.navcaddy.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import caddypro.data.navcaddy.entities.BagProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for bag profile database operations.
 *
 * Spec reference: player-profile-bag-management.md R1
 * Plan reference: player-profile-bag-management-plan.md Task 3
 */
@Dao
interface BagProfileDao {
    /**
     * Insert a bag profile into the database.
     *
     * @param bag The bag profile to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBag(bag: BagProfileEntity)

    /**
     * Update a bag profile in the database.
     *
     * @param bag The bag profile to update
     */
    @Update
    suspend fun updateBag(bag: BagProfileEntity)

    /**
     * Get the currently active bag.
     *
     * @return Flow of the active bag (null if none)
     */
    @Query("SELECT * FROM bag_profiles WHERE is_active = 1 AND is_archived = 0 LIMIT 1")
    fun getActiveBag(): Flow<BagProfileEntity?>

    /**
     * Get all non-archived bags.
     *
     * @return Flow of all bags ordered by creation date descending
     */
    @Query("SELECT * FROM bag_profiles WHERE is_archived = 0 ORDER BY created_at DESC")
    fun getAllBags(): Flow<List<BagProfileEntity>>

    /**
     * Get a specific bag by ID.
     *
     * @param bagId The bag ID
     * @return Flow of the bag (null if not found)
     */
    @Query("SELECT * FROM bag_profiles WHERE id = :bagId")
    fun getBagById(bagId: String): Flow<BagProfileEntity?>

    /**
     * Switch the active bag.
     *
     * This is a transaction that deactivates all bags and activates the specified one.
     *
     * @param bagId The bag to activate
     */
    @Transaction
    suspend fun switchActiveBag(bagId: String) {
        deactivateAllBags()
        activateBag(bagId)
    }

    /**
     * Deactivate all bags.
     */
    @Query("UPDATE bag_profiles SET is_active = 0")
    suspend fun deactivateAllBags()

    /**
     * Activate a specific bag.
     *
     * @param bagId The bag to activate
     */
    @Query("UPDATE bag_profiles SET is_active = 1, updated_at = :timestamp WHERE id = :bagId")
    suspend fun activateBag(bagId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Archive a bag (soft delete).
     *
     * @param bagId The bag to archive
     */
    @Query("UPDATE bag_profiles SET is_archived = 1, is_active = 0, updated_at = :timestamp WHERE id = :bagId")
    suspend fun archiveBag(bagId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Get count of non-archived bags.
     *
     * @return Flow of bag count
     */
    @Query("SELECT COUNT(*) FROM bag_profiles WHERE is_archived = 0")
    fun getBagCount(): Flow<Int>

    /**
     * Delete all bags (for testing).
     */
    @Query("DELETE FROM bag_profiles")
    suspend fun deleteAllBags()
}
