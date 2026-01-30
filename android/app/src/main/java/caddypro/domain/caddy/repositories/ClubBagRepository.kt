package caddypro.domain.caddy.repositories

import com.example.app.domain.navcaddy.models.BagProfile
import com.example.app.domain.navcaddy.models.Club
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for club bag data access.
 *
 * Provides domain-level access to player's bag profiles and club distances.
 * Used by PinSeeker engine to personalize strategy based on club capabilities.
 *
 * This is a placeholder interface for MVP - implementation will delegate to
 * NavCaddyRepository until bag logic is fully extracted.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 8
 *
 * @see BagProfile
 * @see Club
 * @see PinSeekerEngine
 */
interface ClubBagRepository {

    /**
     * Get the currently active bag profile.
     *
     * Only one bag can be active at a time. Returns null if no bag is active.
     *
     * @return Flow of active bag (null if none)
     */
    fun getActiveBag(): Flow<BagProfile?>

    /**
     * Get all clubs in the active bag.
     *
     * Returns empty list if no bag is active.
     *
     * @return Flow of clubs ordered by position
     */
    fun getActiveBagClubs(): Flow<List<Club>>

    /**
     * Get club distances for the active bag.
     *
     * Returns a map of club name to estimated carry distance in meters.
     * Used by strategy engine for distance-based recommendations.
     *
     * @return Flow of club distances (empty if no active bag)
     */
    fun getClubDistances(): Flow<Map<String, Int>>

    /**
     * Get a specific club by ID from the active bag.
     *
     * @param clubId The club ID to retrieve
     * @return Flow of club (null if not found or no active bag)
     */
    fun getClubById(clubId: String): Flow<Club?>

    /**
     * Get all non-archived bags.
     *
     * @return Flow of all bags
     */
    fun getAllBags(): Flow<List<BagProfile>>

    /**
     * Switch the active bag.
     *
     * Deactivates all bags and activates the specified one.
     *
     * @param bagId The bag ID to activate
     */
    suspend fun switchActiveBag(bagId: String)
}
