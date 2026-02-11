package com.caddypro.app.domain.repository

import com.caddypro.app.domain.model.Bag
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Bag operations
 *
 * Follows offline-first pattern with business rules:
 * - Only one bag can be active at a time
 * - At least one bag must exist
 * - Deleting active bag promotes next bag to active
 */
interface BagRepository {

    /**
     * Get all bags for a profile as a Flow
     */
    fun getBagsByProfileId(profileId: String): Flow<List<Bag>>

    /**
     * Get a specific bag by ID
     */
    suspend fun getBagById(id: String): Bag?

    /**
     * Get the active bag for a profile
     */
    fun getActiveBag(profileId: String): Flow<Bag?>

    /**
     * Get the active bag synchronously
     */
    suspend fun getActiveBagSync(profileId: String): Bag?

    /**
     * Create a new bag
     */
    suspend fun createBag(bag: Bag)

    /**
     * Update an existing bag
     */
    suspend fun updateBag(bag: Bag)

    /**
     * Set a bag as active (deactivates all others)
     * AC7: Only one bag is active at a time
     */
    suspend fun setActiveBag(profileId: String, bagId: String)

    /**
     * Delete a bag
     * AC8: Deleting active bag promotes next bag to active
     * AC9: Cannot delete the last bag
     */
    suspend fun deleteBag(profileId: String, bagId: String): Result<Unit>

    /**
     * Get count of bags for a profile
     */
    suspend fun getBagCount(profileId: String): Int

    /**
     * Create default "My Bag" for a profile
     * AC6: Default bag created on first launch
     */
    suspend fun createDefaultBag(profileId: String): Bag
}
