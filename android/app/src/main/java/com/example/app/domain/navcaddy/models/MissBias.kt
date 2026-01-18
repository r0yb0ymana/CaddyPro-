package com.example.app.domain.navcaddy.models

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing a club's miss bias characteristics.
 *
 * Miss bias captures a club's tendency to miss in a particular direction,
 * either user-defined or inferred from shot history. This information
 * feeds into strategy recommendations.
 *
 * Spec reference: player-profile-bag-management.md R4
 * Plan reference: player-profile-bag-management-plan.md Task 2
 *
 * @property dominantDirection Primary direction of misses (LEFT, STRAIGHT, RIGHT)
 * @property missType Optional detailed miss pattern (e.g., SLICE, HOOK)
 * @property isUserDefined True if manually set by user, false if inferred from data
 * @property confidence Confidence in this bias assessment (0.0-1.0)
 * @property lastUpdated Unix timestamp when this bias was last updated
 */
data class MissBias(
    @SerializedName("dominant_direction")
    val dominantDirection: MissBiasDirection,

    @SerializedName("miss_type")
    val missType: MissType? = null,

    @SerializedName("is_user_defined")
    val isUserDefined: Boolean,

    @SerializedName("confidence")
    val confidence: Double,

    @SerializedName("last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
) {
    init {
        require(confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
    }
}
