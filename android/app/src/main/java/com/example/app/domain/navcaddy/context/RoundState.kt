package caddypro.domain.navcaddy.context

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing the current round state.
 *
 * Extends the basic Round model with real-time state tracking for session context.
 *
 * Spec reference: navcaddy-engine.md R6 (Session Context)
 *
 * @property roundId Unique identifier for the round
 * @property courseName Name of the golf course
 * @property currentHole Current hole number being played (1-18)
 * @property currentPar Par for the current hole
 * @property totalScore Total score for holes completed
 * @property holesCompleted Number of holes completed so far
 * @property conditions Optional course conditions (weather, wind)
 */
data class RoundState(
    @SerializedName("round_id")
    val roundId: String,

    @SerializedName("course_name")
    val courseName: String,

    @SerializedName("current_hole")
    val currentHole: Int,

    @SerializedName("current_par")
    val currentPar: Int,

    @SerializedName("total_score")
    val totalScore: Int = 0,

    @SerializedName("holes_completed")
    val holesCompleted: Int = 0,

    @SerializedName("conditions")
    val conditions: CourseConditions? = null
) {
    init {
        require(currentHole in 1..18) { "Current hole must be between 1 and 18" }
        require(currentPar in 3..5) { "Par must be between 3 and 5" }
        require(holesCompleted in 0..18) { "Holes completed must be between 0 and 18" }
        require(totalScore >= 0) { "Total score must be non-negative" }
    }
}

/**
 * Course conditions that may affect shot selection.
 *
 * @property weather General weather description (e.g., "sunny", "cloudy", "rain")
 * @property windSpeed Wind speed in mph
 * @property windDirection Wind direction (e.g., "NW", "headwind", "crosswind")
 * @property temperature Temperature in Fahrenheit
 */
data class CourseConditions(
    @SerializedName("weather")
    val weather: String? = null,

    @SerializedName("wind_speed")
    val windSpeed: Int? = null,

    @SerializedName("wind_direction")
    val windDirection: String? = null,

    @SerializedName("temperature")
    val temperature: Int? = null
) {
    init {
        windSpeed?.let {
            require(it >= 0) { "Wind speed must be non-negative" }
        }
    }

    /**
     * Returns a human-readable description of conditions.
     */
    fun toDescription(): String {
        val parts = mutableListOf<String>()
        weather?.let { parts.add(it) }
        windSpeed?.let { speed ->
            windDirection?.let { direction ->
                parts.add("$speed mph $direction wind")
            } ?: parts.add("$speed mph wind")
        }
        temperature?.let { parts.add("${it}°F") }
        return parts.joinToString(", ")
    }
}
