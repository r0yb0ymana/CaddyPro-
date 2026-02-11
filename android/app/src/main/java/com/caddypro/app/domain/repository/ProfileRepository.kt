package com.caddypro.app.domain.repository

import com.caddypro.app.domain.model.PlayerProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for PlayerProfile operations
 *
 * Follows offline-first pattern: all reads from local Room database,
 * writes go to Room first then sync to Supabase via WorkManager.
 */
interface ProfileRepository {

    /**
     * Get the current user's profile as a Flow
     * Returns null if no profile exists
     */
    fun getProfile(): Flow<PlayerProfile?>

    /**
     * Get profile by user ID
     */
    fun getProfileByUserId(userId: String): Flow<PlayerProfile?>

    /**
     * Create or update a profile
     * Saves to Room immediately, queues Supabase sync
     */
    suspend fun saveProfile(profile: PlayerProfile)

    /**
     * Check if a profile exists for the current user
     */
    suspend fun hasProfile(): Boolean

    /**
     * Delete a profile
     */
    suspend fun deleteProfile(profile: PlayerProfile)

    /**
     * Get count of profiles (for first-launch detection)
     */
    suspend fun getProfileCount(): Int
}
