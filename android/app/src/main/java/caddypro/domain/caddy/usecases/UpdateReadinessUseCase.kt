package caddypro.domain.caddy.usecases

import caddypro.data.caddy.repository.ReadinessRepository
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to update the user's readiness score.
 *
 * Validates readiness score range (0-100) and saves to the repository.
 * Supports both wearable-synced scores and manual entry.
 *
 * Readiness scores influence strategy recommendations by adjusting
 * conservatism (lower readiness = bigger safety margins).
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 9 step 3
 * Acceptance criteria: A2 (Readiness impacts strategy)
 *
 * @see ReadinessRepository
 * @see ReadinessScore
 */
@Singleton
class UpdateReadinessUseCase @Inject constructor(
    private val readinessRepository: ReadinessRepository
) {

    /**
     * Update readiness score with breakdown from wearable sync.
     *
     * Validates that overall score is in valid range (0-100).
     * MetricScore validation happens in its own constructor.
     *
     * @param overall Overall readiness score (0-100)
     * @param breakdown Detailed breakdown of contributing factors
     * @param source Source of the readiness data (wearable or manual)
     * @return Result containing the saved ReadinessScore on success
     */
    suspend operator fun invoke(
        overall: Int,
        breakdown: ReadinessBreakdown,
        source: ReadinessSource = ReadinessSource.MANUAL_ENTRY
    ): Result<ReadinessScore> {
        return try {
            // Validate overall score range
            require(overall in 0..100) {
                "Overall readiness score must be between 0 and 100, got: $overall"
            }

            // Create readiness score with current timestamp
            // MetricScore validation happens in ReadinessScore init block
            val readinessScore = ReadinessScore(
                overall = overall,
                breakdown = breakdown,
                timestamp = System.currentTimeMillis(),
                source = source
            )

            // Save to repository
            readinessRepository.saveReadiness(readinessScore)

            Result.success(readinessScore)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update readiness with a simple overall score (manual entry).
     *
     * Convenience method for manual entry without detailed breakdown.
     * Used when wearable is unavailable or user wants to override.
     *
     * @param overall Overall readiness score (0-100)
     * @return Result containing the saved ReadinessScore on success
     */
    suspend fun updateManual(overall: Int): Result<ReadinessScore> {
        return invoke(
            overall = overall,
            breakdown = ReadinessBreakdown(
                hrv = null,
                sleepQuality = null,
                stressLevel = null
            ),
            source = ReadinessSource.MANUAL_ENTRY
        )
    }

    /**
     * Update readiness from wearable metrics.
     *
     * Computes overall score using fixed weights (MVP algorithm):
     * - HRV: 40%
     * - Sleep Quality: 40%
     * - Stress Level: 20%
     *
     * All component scores should be normalized to 0-100 scale by the caller.
     *
     * Spec reference: live-caddy-mode-plan.md Task 1 Q3 decision
     *
     * @param hrv Heart Rate Variability score (0-100)
     * @param sleepQuality Sleep quality score (0-100)
     * @param stressLevel Stress level score (0-100, where 0=high stress, 100=low stress)
     * @param source Wearable source (defaults to WEARABLE_SYNC)
     * @return Result containing the saved ReadinessScore on success
     */
    suspend fun updateFromWearable(
        hrv: Double,
        sleepQuality: Double,
        stressLevel: Double,
        source: ReadinessSource = ReadinessSource.WEARABLE_SYNC
    ): Result<ReadinessScore> {
        // Fixed weights for MVP (per Task 1 Q3 decision)
        val HRV_WEIGHT = 0.40
        val SLEEP_WEIGHT = 0.40
        val STRESS_WEIGHT = 0.20

        // Calculate overall score with weighted average
        val overall = (
            (hrv * HRV_WEIGHT) +
            (sleepQuality * SLEEP_WEIGHT) +
            (stressLevel * STRESS_WEIGHT)
        ).toInt().coerceIn(0, 100)

        // Create MetricScore instances with weights
        return invoke(
            overall = overall,
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(value = hrv, weight = HRV_WEIGHT),
                sleepQuality = MetricScore(value = sleepQuality, weight = SLEEP_WEIGHT),
                stressLevel = MetricScore(value = stressLevel, weight = STRESS_WEIGHT)
            ),
            source = source
        )
    }
}
