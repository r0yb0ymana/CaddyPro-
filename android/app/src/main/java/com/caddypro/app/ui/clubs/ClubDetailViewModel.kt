package com.caddypro.app.ui.clubs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias
import com.caddypro.app.domain.repository.ClubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ClubDetailSheet
 *
 * AC12: Carry distance must be <= total distance
 * AC14: Miss bias visual selector
 */
@HiltViewModel
class ClubDetailViewModel @Inject constructor(
    private val clubRepository: ClubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClubDetailState())
    val uiState: StateFlow<ClubDetailState> = _uiState.asStateFlow()

    /**
     * Initialize with a club for editing
     */
    fun initForEdit(club: Club) {
        _uiState.update {
            ClubDetailState(
                clubId = club.id,
                bagId = club.bagId,
                name = club.name,
                clubType = club.type,
                loft = club.loft?.toString() ?: "",
                carryDistance = club.carryDistance.toString(),
                totalDistance = club.totalDistance.toString(),
                missBias = club.missBias
            )
        }
    }

    /**
     * Initialize for adding a new club
     */
    fun initForAdd(bagId: String) {
        _uiState.update {
            ClubDetailState(bagId = bagId)
        }
    }

    /**
     * Handle user actions
     */
    fun onAction(action: ClubDetailAction) {
        when (action) {
            is ClubDetailAction.UpdateName -> updateName(action.name)
            is ClubDetailAction.UpdateClubType -> updateClubType(action.type)
            is ClubDetailAction.UpdateLoft -> updateLoft(action.loft)
            is ClubDetailAction.UpdateCarryDistance -> updateCarryDistance(action.distance)
            is ClubDetailAction.UpdateTotalDistance -> updateTotalDistance(action.distance)
            is ClubDetailAction.UpdateMissBias -> updateMissBias(action.bias)
            is ClubDetailAction.SaveClub -> saveClub()
            is ClubDetailAction.ClearError -> clearError()
        }
    }

    private fun updateName(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = validateName(name)
            )
        }
    }

    private fun updateClubType(type: ClubType) {
        _uiState.update { it.copy(clubType = type) }
    }

    private fun updateLoft(loft: String) {
        _uiState.update {
            it.copy(
                loft = loft,
                loftError = validateLoft(loft)
            )
        }
    }

    private fun updateCarryDistance(distance: String) {
        val state = _uiState.value
        val basicError = validateCarryDistance(distance)
        val relationshipError = validateDistanceRelationship(distance, state.totalDistance)
        _uiState.update {
            it.copy(
                carryDistance = distance,
                carryDistanceError = basicError ?: relationshipError,
                totalDistanceError = null
            )
        }
    }

    private fun updateTotalDistance(distance: String) {
        val state = _uiState.value
        val basicError = validateTotalDistance(distance)
        val relationshipError = validateDistanceRelationship(state.carryDistance, distance)
        _uiState.update {
            it.copy(
                totalDistance = distance,
                totalDistanceError = basicError,
                carryDistanceError = relationshipError
            )
        }
    }

    private fun updateMissBias(bias: MissBias) {
        _uiState.update { it.copy(missBias = bias) }
    }

    /**
     * Validate club name
     */
    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Club name is required"
            name.length < 2 -> "Club name must be at least 2 characters"
            name.length > 30 -> "Club name must be less than 30 characters"
            else -> null
        }
    }

    /**
     * Validate loft (optional field)
     */
    private fun validateLoft(loft: String): String? {
        if (loft.isBlank()) return null // Optional field

        val loftFloat = loft.toFloatOrNull()
            ?: return "Loft must be a valid number"

        return when {
            loftFloat < 0 -> "Loft cannot be negative"
            loftFloat > 90 -> "Loft cannot exceed 90 degrees"
            else -> null
        }
    }

    /**
     * Validate carry distance
     */
    private fun validateCarryDistance(distance: String): String? {
        if (distance.isBlank()) return "Carry distance is required"

        val distanceInt = distance.toIntOrNull()
            ?: return "Carry distance must be a valid number"

        return when {
            distanceInt < 0 -> "Carry distance cannot be negative"
            distanceInt > 500 -> "Carry distance seems unrealistic (max 500 yards)"
            else -> null
        }
    }

    /**
     * Validate total distance
     */
    private fun validateTotalDistance(distance: String): String? {
        if (distance.isBlank()) return "Total distance is required"

        val distanceInt = distance.toIntOrNull()
            ?: return "Total distance must be a valid number"

        return when {
            distanceInt < 0 -> "Total distance cannot be negative"
            distanceInt > 500 -> "Total distance seems unrealistic (max 500 yards)"
            else -> null
        }
    }

    /**
     * AC12: Validate carry <= total distance
     */
    private fun validateDistanceRelationship(carry: String, total: String): String? {
        val carryInt = carry.toIntOrNull() ?: return null
        val totalInt = total.toIntOrNull() ?: return null

        return if (carryInt > totalInt) {
            "Carry distance cannot exceed total distance"
        } else {
            null
        }
    }

    /**
     * Save club
     * AC12: Validates carry <= total distance before saving
     */
    private fun saveClub() {
        val state = _uiState.value

        // Validate all fields
        val nameError = validateName(state.name)
        val loftError = validateLoft(state.loft)
        val carryError = validateCarryDistance(state.carryDistance)
        val totalError = validateTotalDistance(state.totalDistance)
        val relationshipError = validateDistanceRelationship(
            state.carryDistance,
            state.totalDistance
        )

        if (nameError != null || loftError != null || carryError != null ||
            totalError != null || relationshipError != null
        ) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    loftError = loftError,
                    carryDistanceError = carryError ?: relationshipError,
                    totalDistanceError = totalError,
                    errorMessage = "Please fix the errors before saving"
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val club = Club(
                    id = state.clubId ?: "",
                    bagId = state.bagId,
                    name = state.name.trim(),
                    type = state.clubType,
                    loft = state.loft.toFloatOrNull(),
                    carryDistance = state.carryDistance.toInt(),
                    totalDistance = state.totalDistance.toInt(),
                    missBias = state.missBias
                )

                val result = if (state.clubId != null) {
                    clubRepository.updateClub(club)
                } else {
                    clubRepository.createClub(club)
                }

                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isSaved = true
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Failed to save club"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save club: ${e.message}"
                    )
                }
            }
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
