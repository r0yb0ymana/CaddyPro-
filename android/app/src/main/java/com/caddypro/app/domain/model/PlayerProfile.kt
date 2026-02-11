package com.caddypro.app.domain.model

import com.caddypro.app.data.local.entities.PreferredUnits
import java.util.UUID

/**
 * Domain model for PlayerProfile
 *
 * Clean domain representation separate from database entity.
 */
data class PlayerProfile(
    val id: String = UUID.randomUUID().toString(),
    val supabaseUserId: String = "",
    val displayName: String,
    val handicapIndex: Float? = null,
    val preferredUnits: PreferredUnits = PreferredUnits.METRIC,
    val homeCourseId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
) {
    /**
     * Validation for handicap index
     * Must be between 0.0 and 54.0 with one decimal place
     */
    fun isHandicapValid(): Boolean {
        return handicapIndex == null || (handicapIndex in 0.0..54.0)
    }

    /**
     * Check if profile has minimum required data
     */
    fun isValid(): Boolean {
        return displayName.isNotBlank() && isHandicapValid()
    }
}
