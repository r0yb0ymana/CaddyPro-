package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Domain model representing a golf club with its specifications.
 *
 * @property id Unique identifier for this club
 * @property name Display name (e.g., "7-iron", "Driver", "PW")
 * @property type Club type category
 * @property loft Loft angle in degrees (optional)
 * @property distance Typical distance in yards (optional, user-calibrated)
 */
data class Club(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: ClubType,

    @SerializedName("loft")
    val loft: Float? = null,

    @SerializedName("distance")
    val distance: Int? = null
)
