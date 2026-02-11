package caddypro.data.navcaddy.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import caddypro.data.navcaddy.entities.BagClubEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for bag-club association database operations.
 *
 * Spec reference: player-profile-bag-management.md R2
 * Plan reference: player-profile-bag-management-plan.md Task 3
 */
@Dao
interface BagClubDao {
    /**
     * Insert a club into a bag.
     *
     * @param bagClub The club-bag association to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClub(bagClub: BagClubEntity)

    /**
     * Update a club in a bag.
     *
     * @param bagClub The club-bag association to update
     */
    @Update
    suspend fun updateClub(bagClub: BagClubEntity)

    /**
     * Get all clubs for a specific bag.
     *
     * @param bagId The bag ID
     * @return Flow of clubs ordered by position
     */
    @Query("SELECT * FROM bag_clubs WHERE bag_id = :bagId ORDER BY position ASC")
    fun getClubsForBag(bagId: String): Flow<List<BagClubEntity>>

    /**
     * Get a specific club from a bag.
     *
     * @param bagId The bag ID
     * @param clubId The club ID
     * @return Flow of the club (null if not found)
     */
    @Query("SELECT * FROM bag_clubs WHERE bag_id = :bagId AND club_id = :clubId")
    fun getClubFromBag(bagId: String, clubId: String): Flow<BagClubEntity?>

    /**
     * Update the estimated carry distance for a club.
     *
     * @param bagId The bag ID
     * @param clubId The club ID
     * @param estimatedCarry The new estimated carry in meters
     */
    @Query(
        """
        UPDATE bag_clubs
        SET estimated_carry = :estimatedCarry
        WHERE bag_id = :bagId AND club_id = :clubId
        """
    )
    suspend fun updateClubDistance(bagId: String, clubId: String, estimatedCarry: Int)

    /**
     * Update the inferred carry distance and confidence for a club.
     *
     * @param bagId The bag ID
     * @param clubId The club ID
     * @param inferredCarry The inferred carry in meters
     * @param confidence The confidence score (0.0-1.0)
     */
    @Query(
        """
        UPDATE bag_clubs
        SET inferred_carry = :inferredCarry, inferred_confidence = :confidence
        WHERE bag_id = :bagId AND club_id = :clubId
        """
    )
    suspend fun updateInferredDistance(
        bagId: String,
        clubId: String,
        inferredCarry: Int,
        confidence: Double
    )

    /**
     * Update the miss bias for a club.
     *
     * @param bagId The bag ID
     * @param clubId The club ID
     * @param direction The dominant miss direction
     * @param missType The miss type (optional)
     * @param isUserDefined Whether this was manually set
     * @param confidence The confidence score (0.0-1.0)
     * @param lastUpdated The timestamp of the update
     */
    @Query(
        """
        UPDATE bag_clubs
        SET miss_bias_direction = :direction,
            miss_bias_type = :missType,
            miss_bias_is_user_defined = :isUserDefined,
            miss_bias_confidence = :confidence,
            miss_bias_last_updated = :lastUpdated
        WHERE bag_id = :bagId AND club_id = :clubId
        """
    )
    suspend fun updateMissBias(
        bagId: String,
        clubId: String,
        direction: String,
        missType: String?,
        isUserDefined: Boolean,
        confidence: Double,
        lastUpdated: Long
    )

    /**
     * Remove a club from a bag.
     *
     * @param bagId The bag ID
     * @param clubId The club ID
     */
    @Query("DELETE FROM bag_clubs WHERE bag_id = :bagId AND club_id = :clubId")
    suspend fun removeClubFromBag(bagId: String, clubId: String)

    /**
     * Get count of clubs in a bag.
     *
     * @param bagId The bag ID
     * @return Flow of club count
     */
    @Query("SELECT COUNT(*) FROM bag_clubs WHERE bag_id = :bagId")
    fun getClubCountForBag(bagId: String): Flow<Int>

    /**
     * Delete all clubs for a bag (used when bag is deleted).
     *
     * @param bagId The bag ID
     */
    @Query("DELETE FROM bag_clubs WHERE bag_id = :bagId")
    suspend fun deleteAllClubsForBag(bagId: String)

    /**
     * Delete all clubs (for testing).
     */
    @Query("DELETE FROM bag_clubs")
    suspend fun deleteAllClubs()
}
