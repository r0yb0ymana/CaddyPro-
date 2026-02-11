package com.caddypro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caddypro.app.data.local.entities.RoundEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for round operations
 */
@Dao
interface RoundDao {

    @Query("SELECT * FROM rounds WHERE profile_id = :profileId ORDER BY started_at DESC")
    fun getRoundsByProfileId(profileId: String): Flow<List<RoundEntity>>

    @Query("SELECT * FROM rounds WHERE id = :id")
    suspend fun getRoundById(id: String): RoundEntity?

    @Query("SELECT * FROM rounds WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveRound(): RoundEntity?

    @Query("SELECT * FROM rounds WHERE is_active = 1 LIMIT 1")
    fun getActiveRoundFlow(): Flow<RoundEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(round: RoundEntity)

    @Update
    suspend fun update(round: RoundEntity)

    @Query("UPDATE rounds SET is_active = 0, ended_at = :endedAt, total_shots = :totalShots, updated_at = :updatedAt, synced = 0 WHERE id = :roundId")
    suspend fun endRound(roundId: String, endedAt: Long, totalShots: Int, updatedAt: Long)

    @Query("UPDATE rounds SET total_shots = :totalShots, updated_at = :updatedAt, synced = 0 WHERE id = :roundId")
    suspend fun updateShotCount(roundId: String, totalShots: Int, updatedAt: Long)

    @Query("SELECT * FROM rounds WHERE synced = 0")
    suspend fun getUnsyncedRounds(): List<RoundEntity>

    @Query("UPDATE rounds SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
