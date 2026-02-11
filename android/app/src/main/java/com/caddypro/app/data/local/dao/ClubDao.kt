package com.caddypro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caddypro.app.data.local.entities.ClubEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Club
 */
@Dao
interface ClubDao {

    @Query("SELECT * FROM clubs WHERE bag_id = :bagId ORDER BY sort_order ASC")
    fun getClubsByBagId(bagId: String): Flow<List<ClubEntity>>

    @Query("SELECT * FROM clubs WHERE id = :id")
    suspend fun getClubById(id: String): ClubEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(club: ClubEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clubs: List<ClubEntity>)

    @Update
    suspend fun update(club: ClubEntity)

    @Delete
    suspend fun delete(club: ClubEntity)

    @Query("DELETE FROM clubs WHERE bag_id = :bagId")
    suspend fun deleteAllClubsInBag(bagId: String)

    @Query("SELECT COUNT(*) FROM clubs WHERE bag_id = :bagId")
    suspend fun getClubCount(bagId: String): Int

    @Query("SELECT MAX(sort_order) FROM clubs WHERE bag_id = :bagId")
    suspend fun getMaxSortOrder(bagId: String): Int?

    @Query("SELECT * FROM clubs WHERE synced = 0")
    suspend fun getUnsyncedClubs(): List<ClubEntity>

    @Query("UPDATE clubs SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
