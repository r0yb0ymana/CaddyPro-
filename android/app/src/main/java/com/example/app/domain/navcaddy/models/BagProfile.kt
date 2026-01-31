package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Domain model representing a bag profile (collection of clubs).
 *
 * Each user can have multiple bag profiles (e.g., "Summer Bag", "Tournament Bag")
 * and switch between them. Only one bag can be active at a time.
 *
 * Spec reference: player-profile-bag-management.md R1
 * Plan reference: player-profile-bag-management-plan.md Task 2
 *
 * @property id Unique identifier for this bag profile
 * @property name Display name (e.g., "Tournament Bag", "Practice Bag")
 * @property isActive Whether this bag is currently active
 * @property isArchived Whether this bag has been archived (soft delete)
 * @property createdAt Unix timestamp when bag was created
 * @property updatedAt Unix timestamp when bag was last modified
 */
data class BagProfile(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("name")
    val name: String,

    @SerializedName("is_active")
    val isActive: Boolean = false,

    @SerializedName("is_archived")
    val isArchived: Boolean = false,

    @SerializedName("created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @SerializedName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(name.isNotBlank()) { "Bag name cannot be blank" }
    }
}
