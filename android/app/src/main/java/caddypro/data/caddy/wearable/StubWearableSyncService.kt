package caddypro.data.caddy.wearable

import caddypro.domain.caddy.models.WearableMetrics
import caddypro.domain.caddy.services.WearableSyncService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub implementation of WearableSyncService for MVP testing.
 *
 * Returns realistic placeholder metrics instead of integrating with actual wearable devices.
 * This allows the readiness calculation and strategy adjustment features to be tested
 * without requiring physical wearable hardware or SDK integration.
 *
 * The placeholder metrics represent a moderately well-rested user:
 * - HRV: 45ms (moderate recovery)
 * - Sleep: 7 hours with good quality (75/100)
 * - Stress: 30/100 (low stress)
 * - Expected readiness: ~70-75/100 (normal risk tolerance)
 *
 * TODO: Replace with real implementation that integrates with:
 * - Android: Google Fit Health Connect API
 *   - Requires: implementation("androidx.health.connect:connect-client:1.0.0")
 *   - Permissions: ACTIVITY_RECOGNITION, ACCESS_FINE_LOCATION
 * - iOS: Apple HealthKit
 *   - Requires: HealthKit framework
 *   - Permissions: Health data read access
 * - Third-party wearables: Garmin, Whoop, Oura Ring SDKs
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 6
 *
 * @see WearableSyncService
 * @see WearableMetrics
 */
@Singleton
class StubWearableSyncService @Inject constructor() : WearableSyncService {

    /**
     * Return placeholder wearable metrics for MVP testing.
     *
     * Always succeeds with realistic moderate-readiness values.
     * In production, this would sync from actual wearable devices.
     *
     * Placeholder values:
     * - HRV: 45ms (moderate, typical for active adults)
     * - Sleep: 420 minutes (7 hours, within optimal range)
     * - Sleep quality: 75/100 (good sleep)
     * - Stress: 30/100 (low stress, good recovery)
     *
     * These values should produce a readiness score around 70-75,
     * resulting in normal (1.0) risk tolerance per ReadinessScore.adjustmentFactor().
     *
     * @return Result.success with placeholder WearableMetrics
     */
    override suspend fun syncMetrics(): Result<WearableMetrics> {
        // Return placeholder metrics for MVP testing
        return Result.success(
            WearableMetrics(
                hrvMs = 45.0,              // Moderate HRV (typical range: 20-100ms)
                sleepMinutes = 420,         // 7 hours (optimal range: 6-9 hours)
                sleepQualityScore = 75.0,   // Good sleep quality (0-100 scale)
                stressScore = 30.0,         // Low stress (0-100, lower is better)
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * Return false to indicate wearable is not available in MVP.
     *
     * This forces the UI to show manual readiness entry instead of
     * attempting automatic wearable sync.
     *
     * In production, this would:
     * - Check if device is paired and connected
     * - Verify health data permissions are granted
     * - Confirm recent data is available (< 24 hours old)
     *
     * @return false (always unavailable in stub implementation)
     */
    override suspend fun isWearableAvailable() = false  // MVP: always manual entry
}
