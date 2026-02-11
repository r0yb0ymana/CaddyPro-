package com.caddypro.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.caddypro.app.data.local.entities.PlayerProfileEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PlayerProfile
 */
@Dao
interface PlayerProfileDao {

    @Query("SELECT * FROM player_profiles WHERE supabase_user_id = :userId LIMIT 1")
    fun getProfileByUserId(userId: String): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): PlayerProfileEntity?

    @Query("SELECT * FROM player_profiles LIMIT 1")
    fun getProfile(): Flow<PlayerProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: PlayerProfileEntity)

    @Update
    suspend fun update(profile: PlayerProfileEntity)

    @Delete
    suspend fun delete(profile: PlayerProfileEntity)

    @Query("SELECT COUNT(*) FROM player_profiles")
    suspend fun getProfileCount(): Int

    @Query("SELECT * FROM player_profiles WHERE synced = 0")
    suspend fun getUnsyncedProfiles(): List<PlayerProfileEntity>

    @Query("UPDATE player_profiles SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)
}
