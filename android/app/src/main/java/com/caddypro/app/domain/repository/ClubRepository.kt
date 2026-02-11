package com.caddypro.app.domain.repository

import com.caddypro.app.domain.model.Club
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Club operations
 *
 * AC11: Quick Add populates standard 14-club set
 * AC12: Validate carry <= total distance
 * AC15: Maximum 14 clubs per bag
 */
interface ClubRepository {

    /**
     * Get all clubs for a bag, ordered by club type
     * AC13: Club type determines sort order
     */
    fun getClubsByBagId(bagId: String): Flow<List<Club>>

    /**
     * Get a single club by ID
     */
    suspend fun getClubById(id: String): Club?

    /**
     * Create a new club
     * AC15: Enforces maximum 14 clubs per bag
     */
    suspend fun createClub(club: Club): Result<Unit>

    /**
     * Update an existing club
     */
    suspend fun updateClub(club: Club): Result<Unit>

    /**
     * Delete a club
     */
    suspend fun deleteClub(clubId: String): Result<Unit>

    /**
     * Get the count of clubs in a bag
     */
    suspend fun getClubCount(bagId: String): Int

    /**
     * Quick Add: Insert standard 14-club set
     * AC11: Quick Add populates a standard 14-club set with typical distances
     */
    suspend fun quickAddStandardSet(bagId: String): Result<Unit>

    /**
     * Delete all clubs in a bag
     */
    suspend fun deleteAllClubsInBag(bagId: String)
}
