package com.caddypro.app.ui.holemap

import com.caddypro.app.domain.model.GreenData
import com.caddypro.app.domain.model.HazardData

/**
 * Hole Map screen UI state
 */
data class HoleMapState(
    val playerLatitude: Double? = null,
    val playerLongitude: Double? = null,
    val isLoading: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null,
    val mapReady: Boolean = false,
    val greens: List<GreenData> = emptyList(),
    val hazards: List<HazardData> = emptyList(),
    val selectedGreenIndex: Int = 0,
    val hasCourseData: Boolean = false,
    val isFetchingCourseData: Boolean = false,
    val courseName: String? = null,
    val distanceToGreenYards: Int? = null,
    val distanceToGreenMeters: Int? = null,
    val useMetric: Boolean = false,
    val showFlagDialog: Boolean = false,
    val flagSubmitted: Boolean = false
) {
    val selectedGreen: GreenData?
        get() = greens.getOrNull(selectedGreenIndex)

    val displayHoleNumber: String
        get() {
            val green = selectedGreen
            return when {
                green?.holeNumber != null -> "Hole ${green.holeNumber}"
                greens.isNotEmpty() -> "Green ${selectedGreenIndex + 1}"
                else -> "Hole Map"
            }
        }

    val canGoNext: Boolean
        get() = selectedGreenIndex < greens.size - 1

    val canGoPrev: Boolean
        get() = selectedGreenIndex > 0

    val displayDistance: String?
        get() = if (useMetric) {
            distanceToGreenMeters?.let { "${it}m" }
        } else {
            distanceToGreenYards?.let { "${it}" }
        }

    val distanceUnit: String
        get() = if (useMetric) "meters" else "yards"
}
