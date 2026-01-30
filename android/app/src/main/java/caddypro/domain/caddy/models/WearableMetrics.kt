package caddypro.domain.caddy.models

/**
 * Domain model representing raw biometric data from wearable devices.
 *
 * Contains unprocessed metrics synced from Apple Health, Google Fit, or other
 * wearable platforms. These metrics are later normalized and combined into a
 * ReadinessScore by the readiness calculation engine.
 *
 * All metric fields are nullable since different wearables provide different
 * data points. The readiness calculator handles missing data gracefully.
 *
 * This is a pure domain model - serialization concerns belong in the data layer DTOs.
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 3
 *
 * @property hrvMs Heart Rate Variability in milliseconds (null if unavailable)
 *                 Typical range: 20-100ms. Higher is generally better.
 * @property sleepMinutes Total sleep duration in minutes (null if unavailable)
 *                        Typical range: 0-720 minutes (0-12 hours)
 * @property sleepQualityScore Platform-provided sleep quality score 0-100 (null if unavailable)
 *                             Higher is better. May be computed differently per platform.
 * @property stressScore Stress level score 0-100 (null if unavailable)
 *                       0 = low stress (good), 100 = high stress (bad)
 * @property timestamp Unix timestamp (epoch milliseconds) when metrics were recorded
 */
data class WearableMetrics(
    val hrvMs: Double?,
    val sleepMinutes: Int?,
    val sleepQualityScore: Double?,
    val stressScore: Double?,
    val timestamp: Long
) {
    init {
        hrvMs?.let { hrv ->
            require(hrv >= 0) { "HRV cannot be negative, got $hrv" }
        }
        sleepMinutes?.let { sleep ->
            require(sleep >= 0) { "Sleep minutes cannot be negative, got $sleep" }
        }
        sleepQualityScore?.let { quality ->
            require(quality in 0.0..100.0) {
                "Sleep quality score must be between 0 and 100, got $quality"
            }
        }
        stressScore?.let { stress ->
            require(stress in 0.0..100.0) {
                "Stress score must be between 0 and 100, got $stress"
            }
        }
        require(timestamp > 0) { "Timestamp must be positive" }
    }
}
