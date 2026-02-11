package com.caddypro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caddypro.app.data.local.entities.ShotEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for shot operations
 */
@Dao
interface ShotDao {

    @Query("SELECT * FROM shots WHERE round_id = :roundId ORDER BY hole_number, shot_number")
    fun getShotsByRoundId(roundId: String): Flow<List<ShotEntity>>

    @Query("SELECT * FROM shots WHERE round_id = :roundId AND hole_number = :holeNumber ORDER BY shot_number")
    fun getShotsByHole(roundId: String, holeNumber: Int): Flow<List<ShotEntity>>

    @Query("SELECT * FROM shots WHERE id = :id")
    suspend fun getShotById(id: String): ShotEntity?

    @Query("SELECT COUNT(*) FROM shots WHERE round_id = :roundId AND hole_number = :holeNumber")
    suspend fun getShotCountForHole(roundId: String, holeNumber: Int): Int

    @Query("SELECT COUNT(*) FROM shots WHERE round_id = :roundId")
    suspend fun getTotalShotCount(roundId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shot: ShotEntity)

    @Update
    suspend fun update(shot: ShotEntity)

    @Delete
    suspend fun delete(shot: ShotEntity)

    @Query("SELECT * FROM shots WHERE round_id = :roundId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastShot(roundId: String): ShotEntity?

    @Query("UPDATE shots SET shot_number = shot_number - 1 WHERE round_id = :roundId AND hole_number = :holeNumber AND shot_number > :deletedShotNumber")
    suspend fun renumberShotsAfterDelete(roundId: String, holeNumber: Int, deletedShotNumber: Int)

    @Query("SELECT * FROM shots WHERE synced = 0")
    suspend fun getUnsyncedShots(): List<ShotEntity>

    @Query("UPDATE shots SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
