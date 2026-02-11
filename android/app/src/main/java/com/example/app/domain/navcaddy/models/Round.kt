package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing a golf round.
 *
 * Used for session context and score tracking.
 *
 * Spec reference: navcaddy-engine.md R6
 *
 * @property id Unique identifier for the round
 * @property startTime Unix timestamp when round started
 * @property courseName Name of the course (optional)
 * @property scores Map of hole number to score
 */
data class Round(
    @SerializedName("id")
    val id: String,

    @SerializedName("start_time")
    val startTime: Long,

    @SerializedName("course_name")
    val courseName: String? = null,

    @SerializedName("scores")
    val scores: Map<Int, Int> = emptyMap()
) {
    init {
        scores.keys.forEach { hole ->
            require(hole in 1..18) { "Invalid hole number: $hole" }
        }
    }
}
