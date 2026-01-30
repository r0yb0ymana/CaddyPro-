package caddypro.domain.caddy.models

/**
 * Domain model representing a user's readiness score for golf performance.
 *
 * Combines wearable biometric data (HRV, sleep, stress) into a single readiness score
 * that influences strategy recommendations. Lower readiness scores trigger more
 * conservative club selections and larger safety margins.
 *
 * This is a pure domain model - serialization concerns belong in the data layer DTOs.
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 3
 * Acceptance criteria: A2 (Readiness impacts strategy)
 *
 * Algorithm decision (from Task 1):
 * - Fixed weights: HRV 40%, Sleep 40%, Stress 20%
 * - All metrics normalized to 0-100 scale
 * - User can override with manual entry
 *
 * @property overall Overall readiness score (0-100)
 * @property breakdown Detailed metric breakdown showing individual components
 * @property timestamp Unix timestamp (epoch milliseconds) when score was computed
 * @property source How the readiness data was obtained
 */
data class ReadinessScore(
    val overall: Int,
    val breakdown: ReadinessBreakdown,
    val timestamp: Long,
    val source: ReadinessSource
) {
    init {
        require(overall in 0..100) {
            "Overall readiness score must be between 0 and 100, got $overall"
        }
        require(timestamp > 0) { "Timestamp must be positive" }
    }

    /**
     * Check if readiness is below a threshold.
     *
     * Used to trigger conservative strategy adjustments.
     *
     * @param threshold Readiness threshold (default: 60)
     * @return true if overall score is below threshold
     */
    fun isLow(threshold: Int = 60): Boolean {
        require(threshold in 0..100) { "Threshold must be between 0 and 100" }
        return overall < threshold
    }

    /**
     * Calculate a strategy adjustment factor based on readiness.
     *
     * This factor is used to modify safety margins and risk tolerance in recommendations.
     * Lower readiness produces smaller factors (more conservative play).
     *
     * Algorithm:
     * - Score >= 60: factor = 1.0 (normal risk tolerance)
     * - Score <= 40: factor = 0.5 (maximum conservatism)
     * - Score 41-59: linear interpolation between 0.5 and 1.0
     *
     * @return Adjustment factor between 0.5 (low readiness) and 1.0 (high readiness)
     */
    fun adjustmentFactor(): Double {
        return when {
            overall >= 60 -> 1.0
            overall <= 40 -> 0.5
            else -> {
                // Linear interpolation: 0.5 at 40, 1.0 at 60
                0.5 + ((overall - 40) / 20.0) * 0.5
            }
        }
    }
}

/**
 * Detailed breakdown of readiness metrics.
 *
 * Each metric is optional as not all wearables provide all data points.
 * The overall readiness score gracefully handles missing metrics by
 * redistributing weights among available metrics.
 *
 * @property hrv Heart Rate Variability metric score (null if unavailable)
 * @property sleepQuality Sleep quality metric score (null if unavailable)
 * @property stressLevel Stress level metric score (null if unavailable)
 */
data class ReadinessBreakdown(
    val hrv: MetricScore?,
    val sleepQuality: MetricScore?,
    val stressLevel: MetricScore?
)

/**
 * Individual metric contribution to overall readiness.
 *
 * @property value Normalized metric value (0-100, higher is better)
 * @property weight Contribution weight (0.0-1.0) used in weighted average
 */
data class MetricScore(
    val value: Double,
    val weight: Double
) {
    init {
        require(value in 0.0..100.0) {
            "Metric value must be between 0 and 100, got $value"
        }
        require(weight in 0.0..1.0) {
            "Metric weight must be between 0.0 and 1.0, got $weight"
        }
    }
}

/**
 * Source of readiness data.
 *
 * Distinguishes between automated wearable sync and manual user entry.
 */
enum class ReadinessSource {
    /**
     * Data synced from wearable device (Apple Health, Google Fit, etc.)
     */
    WEARABLE_SYNC,

    /**
     * User manually entered their readiness score
     *
     * Used when wearables are unavailable or user wants to override
     */
    MANUAL_ENTRY
}
