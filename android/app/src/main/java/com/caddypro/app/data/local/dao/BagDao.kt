package com.caddypro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.caddypro.app.data.local.entities.BagEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Bag
 */
@Dao
interface BagDao {

    @Query("SELECT * FROM bags WHERE profile_id = :profileId ORDER BY created_at DESC")
    fun getBagsByProfileId(profileId: String): Flow<List<BagEntity>>

    @Query("SELECT * FROM bags WHERE profile_id = :profileId ORDER BY created_at DESC")
    suspend fun getBagsByProfileIdSync(profileId: String): List<BagEntity>

    @Query("SELECT * FROM bags WHERE id = :id")
    suspend fun getBagById(id: String): BagEntity?

    @Query("SELECT * FROM bags WHERE profile_id = :profileId AND is_active = 1 LIMIT 1")
    fun getActiveBag(profileId: String): Flow<BagEntity?>

    @Query("SELECT * FROM bags WHERE profile_id = :profileId AND is_active = 1 LIMIT 1")
    suspend fun getActiveBagSync(profileId: String): BagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bag: BagEntity)

    @Update
    suspend fun update(bag: BagEntity)

    @Delete
    suspend fun delete(bag: BagEntity)

    @Query("UPDATE bags SET is_active = 0 WHERE profile_id = :profileId")
    suspend fun deactivateAllBags(profileId: String)

    @Transaction
    suspend fun setActiveBag(profileId: String, bagId: String) {
        deactivateAllBags(profileId)
        getBagById(bagId)?.let {
            update(it.copy(isActive = true, updatedAt = System.currentTimeMillis()))
        }
    }

    @Query("SELECT COUNT(*) FROM bags WHERE profile_id = :profileId")
    suspend fun getBagCount(profileId: String): Int

    @Query("SELECT * FROM bags WHERE synced = 0")
    suspend fun getUnsyncedBags(): List<BagEntity>

    @Query("UPDATE bags SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
