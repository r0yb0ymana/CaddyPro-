package caddypro.domain.navcaddy.memory

import kotlin.math.pow

/**
 * Calculator for time-based pattern confidence decay.
 *
 * Implements exponential decay to prevent stale patterns from dominating advice.
 * Uses a 14-day half-life per spec resolution Q5.
 *
 * Spec reference: navcaddy-engine.md R5, Q5 (14-day decay half-life)
 */
class PatternDecayCalculator(
    private val decayHalfLifeDays: Double = DEFAULT_HALF_LIFE_DAYS
) {
    companion object {
        /** Default decay half-life in days (spec Q5) */
        const val DEFAULT_HALF_LIFE_DAYS = 14.0

        /** Milliseconds per day for time conversions */
        private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

        /** Maximum age in days before confidence reaches near-zero (6 half-lives = ~1.5%) */
        private const val MAX_AGE_DAYS = 84.0 // 6 half-lives
    }

    /**
     * Calculate the decay factor for a shot based on time elapsed.
     *
     * Uses exponential decay formula: decay = 0.5^(age / half_life)
     *
     * @param shotTimestamp Timestamp when the shot was recorded (milliseconds)
     * @param currentTime Current timestamp (milliseconds)
     * @return Decay factor between 0.0 (very old) and 1.0 (recent)
     */
    fun calculateDecay(shotTimestamp: Long, currentTime: Long = System.currentTimeMillis()): Float {
        require(shotTimestamp <= currentTime) {
            "Shot timestamp ($shotTimestamp) cannot be in the future"
        }

        val ageMillis = currentTime - shotTimestamp
        val ageDays = ageMillis.toDouble() / MILLIS_PER_DAY

        // If age exceeds max, return minimum decay
        if (ageDays >= MAX_AGE_DAYS) {
            return 0.0f
        }

        // Exponential decay: 0.5^(age / half_life)
        val decay = 0.5.pow(ageDays / decayHalfLifeDays)
        return decay.toFloat()
    }

    /**
     * Calculate decayed confidence for a pattern.
     *
     * Applies decay to the base confidence to reduce influence of old patterns.
     *
     * @param baseConfidence Original confidence value (0-1)
     * @param lastOccurrence Timestamp of most recent occurrence (milliseconds)
     * @param currentTime Current timestamp (milliseconds)
     * @return Decayed confidence between 0.0 and 1.0
     */
    fun calculateDecayedConfidence(
        baseConfidence: Float,
        lastOccurrence: Long,
        currentTime: Long = System.currentTimeMillis()
    ): Float {
        require(baseConfidence in 0f..1f) {
            "Base confidence must be between 0 and 1, was: $baseConfidence"
        }

        val decayFactor = calculateDecay(lastOccurrence, currentTime)
        return baseConfidence * decayFactor
    }

    /**
     * Calculate the age in days for a timestamp.
     *
     * @param timestamp Timestamp to calculate age for (milliseconds)
     * @param currentTime Current timestamp (milliseconds)
     * @return Age in days
     */
    fun calculateAgeDays(timestamp: Long, currentTime: Long = System.currentTimeMillis()): Double {
        val ageMillis = currentTime - timestamp
        return ageMillis.toDouble() / MILLIS_PER_DAY
    }

    /**
     * Check if a timestamp is within the retention window.
     *
     * @param timestamp Timestamp to check (milliseconds)
     * @param retentionDays Retention window in days (default 90 per Q5)
     * @param currentTime Current timestamp (milliseconds)
     * @return True if within retention window
     */
    fun isWithinRetentionWindow(
        timestamp: Long,
        retentionDays: Int = 90,
        currentTime: Long = System.currentTimeMillis()
    ): Boolean {
        val ageDays = calculateAgeDays(timestamp, currentTime)
        return ageDays < retentionDays
    }
}
