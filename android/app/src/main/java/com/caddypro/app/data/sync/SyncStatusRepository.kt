package com.caddypro.app.data.sync

import com.caddypro.app.data.local.dao.BagDao
import com.caddypro.app.data.local.dao.ClubDao
import com.caddypro.app.data.local.dao.PlayerProfileDao
import com.caddypro.app.data.remote.ConnectivityObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides observable sync status for the UI
 *
 * AC17: Unsynced changes show subtle indicator
 */
@Singleton
class SyncStatusRepository @Inject constructor(
    private val playerProfileDao: PlayerProfileDao,
    private val bagDao: BagDao,
    private val clubDao: ClubDao,
    private val connectivityObserver: ConnectivityObserver
) {

    /**
     * Whether there are any unsynced local changes
     */
    suspend fun hasUnsyncedChanges(): Boolean {
        return playerProfileDao.getUnsyncedProfiles().isNotEmpty() ||
                bagDao.getUnsyncedBags().isNotEmpty() ||
                clubDao.getUnsyncedClubs().isNotEmpty()
    }

    /**
     * Observable connectivity state
     */
    val isConnected: Flow<Boolean> = connectivityObserver.isConnected
}
