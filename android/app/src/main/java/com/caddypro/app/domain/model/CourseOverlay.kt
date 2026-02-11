package com.caddypro.app.domain.model

/**
 * Green center data from OSM
 */
data class GreenData(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val holeNumber: Int? = null
)

/**
 * Hazard polygon data from OSM
 */
data class HazardData(
    val id: String,
    val type: HazardType,
    val name: String? = null,
    val coordinates: List<LatLngPoint>
)

/**
 * Simple lat/lon coordinate
 */
data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * Bundle of all course overlay data for a location
 */
data class CourseOverlayBundle(
    val greens: List<GreenData> = emptyList(),
    val hazards: List<HazardData> = emptyList(),
    val courseName: String? = null,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    val hasCourseData: Boolean
        get() = greens.isNotEmpty() || hazards.isNotEmpty()
}
