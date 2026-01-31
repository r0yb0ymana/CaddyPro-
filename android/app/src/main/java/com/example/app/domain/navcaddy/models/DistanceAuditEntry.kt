package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Domain model representing an audit trail entry for distance changes.
 *
 * Tracks all changes to club distances (both manual edits and accepted suggestions)
 * to provide transparency and allow users to review their calibration history.
 *
 * All distances are in meters (metric units).
 *
 * Spec reference: player-profile-bag-management.md R3
 * Plan reference: player-profile-bag-management-plan.md Task 2
 *
 * @property id Unique identifier for this audit entry
 * @property clubId ID of the club that was modified
 * @property oldEstimated Previous estimated carry distance in meters
 * @property newEstimated New estimated carry distance in meters
 * @property inferredValue The inferred value from shot data (if applicable)
 * @property confidence Confidence score of the inference (if applicable, 0.0-1.0)
 * @property reason Human-readable reason for the change
 * @property timestamp Unix timestamp when the change was made
 * @property wasAccepted True if this was an accepted suggestion, false if manual edit
 */
data class DistanceAuditEntry(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("club_id")
    val clubId: String,

    @SerializedName("old_estimated")
    val oldEstimated: Int,

    @SerializedName("new_estimated")
    val newEstimated: Int,

    @SerializedName("inferred_value")
    val inferredValue: Int? = null,

    @SerializedName("confidence")
    val confidence: Double? = null,

    @SerializedName("reason")
    val reason: String,

    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @SerializedName("was_accepted")
    val wasAccepted: Boolean
) {
    init {
        require(oldEstimated > 0) { "Old estimated distance must be positive" }
        require(newEstimated > 0) { "New estimated distance must be positive" }
        confidence?.let {
            require(it in 0.0..1.0) { "Confidence must be between 0 and 1" }
        }
        require(reason.isNotBlank()) { "Reason cannot be blank" }
    }
}
