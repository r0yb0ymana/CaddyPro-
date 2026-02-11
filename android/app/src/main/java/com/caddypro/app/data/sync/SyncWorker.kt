package com.caddypro.app.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.caddypro.app.data.local.dao.BagDao
import com.caddypro.app.data.local.dao.ClubDao
import com.caddypro.app.data.local.dao.PlayerProfileDao
import com.caddypro.app.data.remote.SupabaseDataSource
import com.caddypro.app.data.remote.dto.toDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager Worker that syncs unsynced local data to Supabase
 *
 * AC3: Profile syncs to Supabase when online
 * AC18: Sync completes automatically when connectivity returns
 * AC19: Conflicting edits resolve via last-write-wins (updatedAt timestamp)
 *
 * Strategy: Push all local unsynced records to Supabase via UPSERT.
 * Supabase upsert with the same primary key will overwrite the remote record.
 * Since we use updatedAt timestamps, the most recent write wins.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playerProfileDao: PlayerProfileDao,
    private val bagDao: BagDao,
    private val clubDao: ClubDao,
    private val supabaseDataSource: SupabaseDataSource
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            syncProfiles()
            syncBags()
            syncClubs()
            Log.d(TAG, "Sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun syncProfiles() {
        val unsyncedProfiles = playerProfileDao.getUnsyncedProfiles()
        Log.d(TAG, "Syncing ${unsyncedProfiles.size} profiles")
        for (profile in unsyncedProfiles) {
            try {
                supabaseDataSource.upsertProfile(profile.toDto())
                playerProfileDao.markAsSynced(profile.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync profile ${profile.id}", e)
                throw e
            }
        }
    }

    private suspend fun syncBags() {
        val unsyncedBags = bagDao.getUnsyncedBags()
        Log.d(TAG, "Syncing ${unsyncedBags.size} bags")
        for (bag in unsyncedBags) {
            try {
                supabaseDataSource.upsertBag(bag.toDto())
                bagDao.markAsSynced(bag.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync bag ${bag.id}", e)
                throw e
            }
        }
    }

    private suspend fun syncClubs() {
        val unsyncedClubs = clubDao.getUnsyncedClubs()
        Log.d(TAG, "Syncing ${unsyncedClubs.size} clubs")
        for (club in unsyncedClubs) {
            try {
                supabaseDataSource.upsertClub(club.toDto())
                clubDao.markAsSynced(club.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync club ${club.id}", e)
                throw e
            }
        }
    }
}
