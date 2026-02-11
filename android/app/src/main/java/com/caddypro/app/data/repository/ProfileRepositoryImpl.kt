package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.PlayerProfileDao
import com.caddypro.app.data.local.mappers.toDomain
import com.caddypro.app.data.local.mappers.toEntity
import com.caddypro.app.data.sync.SyncManager
import com.caddypro.app.domain.model.PlayerProfile
import com.caddypro.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ProfileRepository
 *
 * Offline-first: all operations go to Room first, then queue sync.
 * AC3: Profile syncs to Supabase when online
 * AC16: All CRUD operations work with no network
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val playerProfileDao: PlayerProfileDao,
    private val syncManager: SyncManager
) : ProfileRepository {

    override fun getProfile(): Flow<PlayerProfile?> {
        return playerProfileDao.getProfile().map { entity ->
            entity?.toDomain()
        }
    }

    override fun getProfileByUserId(userId: String): Flow<PlayerProfile?> {
        return playerProfileDao.getProfileByUserId(userId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun saveProfile(profile: PlayerProfile) {
        val updatedProfile = profile.copy(
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
        playerProfileDao.insert(updatedProfile.toEntity())
        syncManager.queueSync()
    }

    override suspend fun hasProfile(): Boolean {
        return playerProfileDao.getProfileCount() > 0
    }

    override suspend fun deleteProfile(profile: PlayerProfile) {
        playerProfileDao.delete(profile.toEntity())
        syncManager.queueSync()
    }

    override suspend fun getProfileCount(): Int {
        return playerProfileDao.getProfileCount()
    }
}
