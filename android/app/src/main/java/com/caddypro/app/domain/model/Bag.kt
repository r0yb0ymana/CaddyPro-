package com.caddypro.app.domain.model

import java.util.UUID

/**
 * Domain model for Bag
 *
 * Represents a collection of clubs. Players can have multiple bags
 * (e.g., tournament vs casual), but only one is active at a time.
 */
data class Bag(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val name: String,
    val isActive: Boolean = false,
    val clubCount: Int = 0, // For display purposes
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    /**
     * Check if bag has valid name
     */
    fun isValid(): Boolean {
        return name.isNotBlank() && name.length <= 50
    }

    companion object {
        const val DEFAULT_BAG_NAME = "My Bag"
    }
}
