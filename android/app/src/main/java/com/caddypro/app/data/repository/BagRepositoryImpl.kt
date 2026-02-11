package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.BagDao
import com.caddypro.app.data.local.dao.ClubDao
import com.caddypro.app.data.local.mappers.toDomain
import com.caddypro.app.data.local.mappers.toEntity
import com.caddypro.app.data.sync.SyncManager
import com.caddypro.app.domain.model.Bag
import com.caddypro.app.domain.repository.BagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BagRepository
 *
 * Offline-first: all operations go to Room first, then queue sync.
 * AC6: Default "My Bag" created on first launch
 * AC7: Only one bag active at a time
 * AC8: Deleting active bag promotes next bag
 * AC9: Cannot delete last bag
 * AC10: Real-time updates via Flow
 * AC16: All CRUD operations work with no network
 */
@Singleton
class BagRepositoryImpl @Inject constructor(
    private val bagDao: BagDao,
    private val clubDao: ClubDao,
    private val syncManager: SyncManager
) : BagRepository {

    override fun getBagsByProfileId(profileId: String): Flow<List<Bag>> {
        return bagDao.getBagsByProfileId(profileId).map { entities ->
            entities.map { entity ->
                val clubCount = clubDao.getClubCount(entity.id)
                entity.toDomain(clubCount)
            }
        }
    }

    override suspend fun getBagById(id: String): Bag? {
        return bagDao.getBagById(id)?.let { entity ->
            val clubCount = clubDao.getClubCount(entity.id)
            entity.toDomain(clubCount)
        }
    }

    override fun getActiveBag(profileId: String): Flow<Bag?> {
        return bagDao.getActiveBag(profileId).map { entity ->
            entity?.let {
                val clubCount = clubDao.getClubCount(it.id)
                it.toDomain(clubCount)
            }
        }
    }

    override suspend fun getActiveBagSync(profileId: String): Bag? {
        return bagDao.getActiveBagSync(profileId)?.let { entity ->
            val clubCount = clubDao.getClubCount(entity.id)
            entity.toDomain(clubCount)
        }
    }

    override suspend fun createBag(bag: Bag) {
        val updatedBag = bag.copy(
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
        bagDao.insert(updatedBag.toEntity())
        syncManager.queueSync()
    }

    override suspend fun updateBag(bag: Bag) {
        val updatedBag = bag.copy(
            updatedAt = System.currentTimeMillis(),
            synced = false
        )
        bagDao.update(updatedBag.toEntity())
        syncManager.queueSync()
    }

    override suspend fun setActiveBag(profileId: String, bagId: String) {
        // AC7: Only one bag is active at a time
        bagDao.setActiveBag(profileId, bagId)
        syncManager.queueSync()
    }

    override suspend fun deleteBag(profileId: String, bagId: String): Result<Unit> {
        // AC9: Cannot delete the last bag
        val bagCount = bagDao.getBagCount(profileId)
        if (bagCount <= 1) {
            return Result.failure(Exception("Cannot delete the last bag"))
        }

        val bagToDelete = bagDao.getBagById(bagId)
            ?: return Result.failure(Exception("Bag not found"))

        val wasActive = bagToDelete.isActive

        // Delete the bag (cascades to clubs via foreign key)
        bagDao.delete(bagToDelete)

        // AC8: If deleted bag was active, promote the next bag to active
        if (wasActive) {
            val remainingBags = bagDao.getBagsByProfileIdSync(profileId)
            val nextBag = remainingBags.firstOrNull()
            nextBag?.let {
                setActiveBag(profileId, it.id)
            }
        }

        syncManager.queueSync()
        return Result.success(Unit)
    }

    override suspend fun getBagCount(profileId: String): Int {
        return bagDao.getBagCount(profileId)
    }

    override suspend fun createDefaultBag(profileId: String): Bag {
        // AC6: Create default "My Bag" on first launch
        val defaultBag = Bag(
            profileId = profileId,
            name = Bag.DEFAULT_BAG_NAME,
            isActive = true,
            synced = false
        )
        bagDao.insert(defaultBag.toEntity())
        syncManager.queueSync()
        return defaultBag
    }
}
