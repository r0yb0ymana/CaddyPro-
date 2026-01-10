package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName
import java.util.UUID
import kotlin.math.pow

/**
 * Domain model representing an aggregated miss pattern.
 *
 * Patterns are derived from multiple shots and decay over time to prevent
 * stale tendencies from dominating advice.
 *
 * Spec reference: navcaddy-engine.md R5, Q5
 *
 * @property id Unique identifier for this pattern
 * @property direction The miss direction for this pattern
 * @property club Specific club associated with pattern (null if pattern spans clubs)
 * @property frequency Number of occurrences in the analysis window
 * @property confidence Pattern confidence (0-1, decays over time)
 * @property pressureContext Pressure context if pattern is pressure-specific
 * @property lastOccurrence Timestamp of most recent occurrence
 */
data class MissPattern(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),

    @SerializedName("direction")
    val direction: MissDirection,

    @SerializedName("club")
    val club: Club? = null,

    @SerializedName("frequency")
    val frequency: Int,

    @SerializedName("confidence")
    val confidence: Float,

    @SerializedName("pressure_context")
    val pressureContext: PressureContext? = null,

    @SerializedName("last_occurrence")
    val lastOccurrence: Long
) {
    init {
        require(confidence in 0f..1f) { "Confidence must be between 0 and 1" }
        require(frequency > 0) { "Frequency must be positive" }
    }

    companion object {
        /** Decay half-life in days (Q5: 14-day decay) */
        const val DECAY_HALF_LIFE_DAYS = 14.0
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }

    /**
     * Calculate the decayed confidence based on time elapsed.
     *
     * Confidence decays with a 14-day half-life per Q5 resolution.
     *
     * @param currentTime Current timestamp in milliseconds
     * @return Decayed confidence value (0-1)
     */
    fun decayedConfidence(currentTime: Long): Float {
        val daysSinceOccurrence = (currentTime - lastOccurrence).toDouble() / MILLIS_PER_DAY
        return (confidence * 0.5.pow(daysSinceOccurrence / DECAY_HALF_LIFE_DAYS)).toFloat()
    }
}
