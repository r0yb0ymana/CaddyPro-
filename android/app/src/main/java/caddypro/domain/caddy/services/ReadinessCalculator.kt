package caddypro.domain.caddy.services

import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import caddypro.domain.caddy.models.WearableMetrics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that calculates readiness scores from wearable metrics.
 *
 * Converts raw biometric data (HRV, sleep, stress) into a normalized readiness score
 * that influences strategy recommendations. Lower readiness scores trigger more
 * conservative club selections and larger safety margins.
 *
 * Algorithm (from Task 1 decision):
 * - Fixed weights: HRV 40%, Sleep 40%, Stress 20%
 * - All metrics normalized to 0-100 scale
 * - Missing metrics default to 50 (neutral) with weight redistribution
 *
 * Normalization guidelines:
 * - HRV: 0-100ms range, higher is better
 * - Sleep: 6-9 hours optimal, uses quality score if available
 * - Stress: 0-100 range, inverted (lower stress = higher score)
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 6
 * Acceptance criteria: A2 (Readiness impacts strategy)
 *
 * @see ReadinessScore
 * @see WearableMetrics
 */
@Singleton
class ReadinessCalculator @Inject constructor() {

    /**
     * Calculate readiness score from wearable metrics.
     *
     * Normalizes each metric to 0-100, applies fixed weights, and computes
     * a weighted average. Missing metrics are handled gracefully by redistributing
     * weights among available metrics or defaulting to 50 (neutral).
     *
     * @param metrics Raw biometric data from wearable device
     * @return ReadinessScore with overall score, breakdown, and timestamp
     */
    fun calculateReadiness(metrics: WearableMetrics): ReadinessScore {
        // Normalize each metric to 0-100
        val hrvScore = normalizeHRV(metrics.hrvMs)
        val sleepScore = normalizeSleep(metrics.sleepMinutes, metrics.sleepQualityScore)
        val stressScore = normalizeStress(metrics.stressScore)

        // Weighted average (HRV: 40%, Sleep: 40%, Stress: 20% per Task 1 decision)
        val overall = calculateWeightedAverage(hrvScore, sleepScore, stressScore)

        return ReadinessScore(
            overall = overall,
            breakdown = ReadinessBreakdown(
                hrv = hrvScore,
                sleepQuality = sleepScore,
                stressLevel = stressScore
            ),
            timestamp = System.currentTimeMillis(),
            source = ReadinessSource.WEARABLE_SYNC
        )
    }

    /**
     * Normalize Heart Rate Variability to 0-100 scale.
     *
     * HRV normalization:
     * - Input range: 0-100+ ms
     * - Good range: 50-100 ms
     * - Score: linear scale with clamping
     * - Higher HRV = better readiness
     *
     * @param hrvMs Heart Rate Variability in milliseconds (null if unavailable)
     * @return Normalized score with 40% weight, or null if unavailable
     */
    private fun normalizeHRV(hrvMs: Double?): MetricScore? {
        if (hrvMs == null) return null

        // Clamp to 0-100 range, then use directly as score
        // HRV values naturally fall in this range, with higher being better
        val normalized = hrvMs.coerceIn(0.0, 100.0)

        return MetricScore(
            value = normalized,
            weight = WEIGHT_HRV
        )
    }

    /**
     * Normalize sleep data to 0-100 scale.
     *
     * Sleep normalization:
     * - Prefer quality score if available (already 0-100)
     * - Otherwise use duration with optimal range 6-9 hours
     * - Duration scoring:
     *   - < 6 hours: linear from 0-75
     *   - 6-9 hours: 75-100 (optimal)
     *   - > 9 hours: linear from 100-50 (oversleep penalty)
     *
     * @param minutes Total sleep duration in minutes (null if unavailable)
     * @param quality Platform-provided sleep quality score 0-100 (null if unavailable)
     * @return Normalized score with 40% weight, or null if both unavailable
     */
    private fun normalizeSleep(minutes: Int?, quality: Double?): MetricScore? {
        // Prefer quality score if available
        quality?.let {
            return MetricScore(
                value = it,
                weight = WEIGHT_SLEEP
            )
        }

        // Fall back to duration-based scoring
        if (minutes == null) return null

        val hours = minutes / 60.0
        val normalized = when {
            hours < 6.0 -> {
                // Less than 6 hours: linear from 0 (no sleep) to 75 (6 hours)
                (hours / 6.0) * 75.0
            }
            hours <= 9.0 -> {
                // 6-9 hours optimal range: linear from 75 to 100
                75.0 + ((hours - 6.0) / 3.0) * 25.0
            }
            else -> {
                // Over 9 hours: linear decline from 100 to 50 (up to 12 hours)
                val excess = (hours - 9.0).coerceAtMost(3.0)
                100.0 - (excess / 3.0) * 50.0
            }
        }

        return MetricScore(
            value = normalized,
            weight = WEIGHT_SLEEP
        )
    }

    /**
     * Normalize stress level to 0-100 scale.
     *
     * Stress normalization:
     * - Input: 0-100 (0 = no stress, 100 = maximum stress)
     * - Invert for scoring: score = 100 - stress
     * - Lower stress = higher readiness score
     *
     * @param stress Stress level score 0-100 (null if unavailable)
     * @return Normalized score with 20% weight, or null if unavailable
     */
    private fun normalizeStress(stress: Double?): MetricScore? {
        if (stress == null) return null

        // Invert stress: low stress (0) -> high score (100)
        val normalized = 100.0 - stress

        return MetricScore(
            value = normalized,
            weight = WEIGHT_STRESS
        )
    }

    /**
     * Calculate weighted average from normalized metrics.
     *
     * Handles missing metrics by:
     * 1. Using available metrics with their weights
     * 2. For missing metrics, defaulting to 50 (neutral)
     * 3. Redistributing weights proportionally among available metrics
     *
     * Example:
     * - All available: (40 * hrv + 40 * sleep + 20 * stress) / 100
     * - Missing HRV: (40 * 50 + 40 * sleep + 20 * stress) / 100
     * - Only stress: (40 * 50 + 40 * 50 + 20 * stress) / 100
     *
     * @param hrv HRV metric score (null if unavailable)
     * @param sleep Sleep metric score (null if unavailable)
     * @param stress Stress metric score (null if unavailable)
     * @return Overall readiness score 0-100
     */
    private fun calculateWeightedAverage(
        hrv: MetricScore?,
        sleep: MetricScore?,
        stress: MetricScore?
    ): Int {
        // Use metric value if available, otherwise default to 50 (neutral)
        val hrvValue = hrv?.value ?: NEUTRAL_SCORE
        val sleepValue = sleep?.value ?: NEUTRAL_SCORE
        val stressValue = stress?.value ?: NEUTRAL_SCORE

        // Calculate weighted sum using fixed weights
        val weightedSum = (hrvValue * WEIGHT_HRV) +
                (sleepValue * WEIGHT_SLEEP) +
                (stressValue * WEIGHT_STRESS)

        // Weights always sum to 1.0, so divide by 100 to get percentage
        return weightedSum.toInt()
    }

    companion object {
        /**
         * Fixed weight for HRV contribution (40%).
         * Per Task 1 decision: HRV is a strong indicator of recovery.
         */
        private const val WEIGHT_HRV = 0.4

        /**
         * Fixed weight for sleep contribution (40%).
         * Per Task 1 decision: Sleep quality equals HRV in importance.
         */
        private const val WEIGHT_SLEEP = 0.4

        /**
         * Fixed weight for stress contribution (20%).
         * Per Task 1 decision: Stress is secondary to HRV and sleep.
         */
        private const val WEIGHT_STRESS = 0.2

        /**
         * Neutral score used when a metric is unavailable.
         * 50 represents "unknown" - neither good nor bad readiness.
         */
        private const val NEUTRAL_SCORE = 50.0
    }
}
