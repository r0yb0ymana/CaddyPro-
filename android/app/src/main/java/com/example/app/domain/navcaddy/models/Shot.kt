package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing a recorded golf shot.
 *
 * Shots are the atomic unit for miss pattern analysis and contextual memory.
 *
 * Spec reference: navcaddy-engine.md R5
 *
 * @property id Unique identifier for the shot
 * @property timestamp Unix timestamp when shot was recorded
 * @property club Club used for the shot
 * @property missDirection Direction/type of miss (null if straight)
 * @property lie Ball lie before the shot
 * @property pressureContext Pressure context for the shot
 * @property holeNumber Hole number if shot was during a round
 * @property notes Optional user notes about the shot
 */
data class Shot(
    @SerializedName("id")
    val id: String,

    @SerializedName("timestamp")
    val timestamp: Long,

    @SerializedName("club")
    val club: Club,

    @SerializedName("miss_direction")
    val missDirection: MissDirection? = null,

    @SerializedName("lie")
    val lie: Lie,

    @SerializedName("pressure_context")
    val pressureContext: PressureContext,

    @SerializedName("hole_number")
    val holeNumber: Int? = null,

    @SerializedName("notes")
    val notes: String? = null
)
