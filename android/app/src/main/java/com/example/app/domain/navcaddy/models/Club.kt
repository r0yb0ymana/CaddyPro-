package com.example.app.domain.navcaddy.models

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Domain model representing a golf club with its specifications.
 *
 * Extended to support bag management with distance calibration and miss bias tracking.
 * All distances are in meters (metric units).
 *
 * Spec reference: player-profile-bag-management.md R2
 * Plan reference: player-profile-bag-management-plan.md Task 2
 *
 * @property id Unique identifier for this club
 * @property name Display name (e.g., "7-iron", "Driver", "PW")
 * @property type Club type category
 * @property loft Loft angle in degrees (optional)
 * @property distance Typical distance (optional) - DEPRECATED: use estimatedCarry
 * @property estimatedCarry User's estimated carry distance in meters
 * @property inferredCarry Carry distance inferred from shot data in meters (nullable until calculated)
 * @property inferredConfidence Confidence in the inferred value (0.0-1.0, nullable until calculated)
 * @property missBias Miss bias characteristics for this club (nullable until set)
 * @property shaft Shaft model/brand (optional)
 * @property flex Shaft flex (e.g., "Stiff", "Regular", "Senior")
 * @property notes User notes about this club
 */
data class Club(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: ClubType,

    @SerializedName("loft")
    val loft: Double? = null,

    @SerializedName("distance")
    val distance: Int? = null,

    // New fields for bag management (player-profile-bag-management.md R2, R3, R4)
    @SerializedName("estimated_carry")
    val estimatedCarry: Int,

    @SerializedName("inferred_carry")
    val inferredCarry: Int? = null,

    @SerializedName("inferred_confidence")
    val inferredConfidence: Double? = null,

    @SerializedName("miss_bias")
    val missBias: MissBias? = null,

    @SerializedName("shaft")
    val shaft: String? = null,

    @SerializedName("flex")
    val flex: String? = null,

    @SerializedName("notes")
    val notes: String? = null
) {
    init {
        require(estimatedCarry > 0) { "Estimated carry must be positive" }
        inferredConfidence?.let {
            require(it in 0.0..1.0) { "Inferred confidence must be between 0 and 1" }
        }
    }
}
