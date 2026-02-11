package com.caddypro.app.ui.clubs

import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias

/**
 * UI State for ClubDetailSheet
 *
 * AC12: Carry <= total distance validation
 * AC14: Miss bias visual selector
 */
data class ClubDetailState(
    val clubId: String? = null,
    val bagId: String = "",
    val name: String = "",
    val clubType: ClubType = ClubType.IRON,
    val loft: String = "",
    val carryDistance: String = "",
    val totalDistance: String = "",
    val missBias: MissBias = MissBias.STRAIGHT,
    val nameError: String? = null,
    val loftError: String? = null,
    val carryDistanceError: String? = null,
    val totalDistanceError: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * Check if form is valid
     */
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                nameError == null &&
                carryDistanceError == null &&
                totalDistanceError == null &&
                carryDistance.toIntOrNull() != null &&
                totalDistance.toIntOrNull() != null
    }
}
