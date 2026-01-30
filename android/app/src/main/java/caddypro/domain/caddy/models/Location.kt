package caddypro.domain.caddy.models

/**
 * Domain model representing a geographic location.
 *
 * Used for weather queries and round context.
 *
 * This is a pure domain model - serialization concerns belong in the data layer DTOs.
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 2
 *
 * @property latitude Latitude in decimal degrees (-90 to 90)
 * @property longitude Longitude in decimal degrees (-180 to 180)
 */
data class Location(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) {
            "Invalid latitude: $latitude. Must be between -90 and 90 degrees"
        }
        require(longitude in -180.0..180.0) {
            "Invalid longitude: $longitude. Must be between -180 and 180 degrees"
        }
    }
}
