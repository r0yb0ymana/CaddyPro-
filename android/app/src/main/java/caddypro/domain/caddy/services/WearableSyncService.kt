package caddypro.domain.caddy.services

import caddypro.domain.caddy.models.WearableMetrics

/**
 * Service interface for wearable device integration.
 *
 * Abstracts platform-specific wearable SDK integrations (Apple Health, Google Fit, etc.)
 * to sync biometric metrics like HRV, sleep, and stress data.
 *
 * For MVP, a stub implementation returns placeholder data. Future implementations
 * will integrate with:
 * - Android: Google Fit Health Connect API
 * - iOS: Apple HealthKit
 * - Third-party: Garmin, Whoop, Oura Ring SDKs
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 6
 *
 * @see WearableMetrics
 * @see StubWearableSyncService
 */
interface WearableSyncService {

    /**
     * Sync the latest biometric metrics from connected wearable device.
     *
     * Retrieves HRV, sleep, and stress data from the last 24 hours.
     * Returns a Result to handle cases where:
     * - Device is disconnected
     * - Permissions are denied
     * - No recent data is available
     *
     * TODO: Real implementation will need to:
     * - Request runtime permissions for health data
     * - Handle OAuth flows for third-party devices
     * - Cache data locally to avoid repeated API calls
     * - Support background sync
     *
     * @return Result containing WearableMetrics if successful, or error if sync failed
     */
    suspend fun syncMetrics(): Result<WearableMetrics>

    /**
     * Check if a wearable device is connected and authorized.
     *
     * Used to determine whether to show wearable sync UI or manual entry.
     *
     * TODO: Real implementation should check:
     * - Device paired/connected status
     * - Health data permissions granted
     * - Recent data availability (last sync < 24h)
     *
     * @return true if wearable is available and can provide metrics
     */
    suspend fun isWearableAvailable(): Boolean
}
