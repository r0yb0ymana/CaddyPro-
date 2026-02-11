package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.ClubDao
import com.caddypro.app.data.local.mappers.toDomain
import com.caddypro.app.data.local.mappers.toEntity
import com.caddypro.app.data.sync.SyncManager
import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.repository.ClubRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of ClubRepository
 *
 * Offline-first: all operations go to Room first, then queue sync.
 * AC11: Quick Add populates standard 14-club set
 * AC12: Validate carry <= total distance
 * AC15: Maximum 14 clubs per bag (tournament rules)
 * AC16: All CRUD operations work with no network
 */
class ClubRepositoryImpl @Inject constructor(
    private val clubDao: ClubDao,
    private val syncManager: SyncManager
) : ClubRepository {

    override fun getClubsByBagId(bagId: String): Flow<List<Club>> {
        return clubDao.getClubsByBagId(bagId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getClubById(id: String): Club? {
        return clubDao.getClubById(id)?.toDomain()
    }

    /**
     * AC15: Maximum 14 clubs per bag
     * AC12: Validate carry <= total distance
     */
    override suspend fun createClub(club: Club): Result<Unit> {
        return try {
            // AC15: Check maximum 14 clubs
            val currentCount = clubDao.getClubCount(club.bagId)
            if (currentCount >= 14) {
                return Result.failure(Exception("Maximum 14 clubs allowed per bag"))
            }

            // AC12: Validate distances
            val validationError = club.validateDistances()
            if (validationError != null) {
                return Result.failure(Exception(validationError))
            }

            if (!club.isValid()) {
                return Result.failure(Exception("Invalid club data"))
            }

            // Set sort order based on club type
            val sortOrder = club.type.sortOrder
            val clubToInsert = club.copy(
                sortOrder = sortOrder,
                synced = false,
                updatedAt = System.currentTimeMillis()
            )

            clubDao.insert(clubToInsert.toEntity())
            syncManager.queueSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * AC12: Validate carry <= total distance
     */
    override suspend fun updateClub(club: Club): Result<Unit> {
        return try {
            // AC12: Validate distances
            val validationError = club.validateDistances()
            if (validationError != null) {
                return Result.failure(Exception(validationError))
            }

            if (!club.isValid()) {
                return Result.failure(Exception("Invalid club data"))
            }

            val clubToUpdate = club.copy(
                synced = false,
                updatedAt = System.currentTimeMillis()
            )

            clubDao.update(clubToUpdate.toEntity())
            syncManager.queueSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteClub(clubId: String): Result<Unit> {
        return try {
            val club = clubDao.getClubById(clubId)
                ?: return Result.failure(Exception("Club not found"))

            clubDao.delete(club)
            syncManager.queueSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getClubCount(bagId: String): Int {
        return clubDao.getClubCount(bagId)
    }

    /**
     * AC11: Quick Add populates a standard 14-club set with typical distances
     */
    override suspend fun quickAddStandardSet(bagId: String): Result<Unit> {
        return try {
            // Check if bag already has clubs
            val currentCount = clubDao.getClubCount(bagId)
            if (currentCount > 0) {
                return Result.failure(Exception("Bag already contains clubs. Remove existing clubs before using Quick Add."))
            }

            val standardClubs = Club.standardSet(bagId)
            val entities = standardClubs.map { it.toEntity() }

            clubDao.insertAll(entities)
            syncManager.queueSync()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAllClubsInBag(bagId: String) {
        clubDao.deleteAllClubsInBag(bagId)
        syncManager.queueSync()
    }
}
