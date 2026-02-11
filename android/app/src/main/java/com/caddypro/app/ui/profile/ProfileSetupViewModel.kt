package com.caddypro.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caddypro.app.data.local.entities.PreferredUnits
import com.caddypro.app.domain.model.PlayerProfile
import com.caddypro.app.domain.repository.ProfileRepository
import com.caddypro.app.domain.usecase.CreateDefaultBagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for ProfileSetupScreen
 *
 * Handles profile creation with validation:
 * - Display Name: Required, non-blank
 * - Handicap Index: Optional, 0.0-54.0 with one decimal place
 * - Preferred Units: Required, defaults to METRIC (Australia)
 * - Home Course: Optional, text field (placeholder for now)
 * - AC6: Creates default "My Bag" after profile creation
 */
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val createDefaultBagUseCase: CreateDefaultBagUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupState())
    val uiState: StateFlow<ProfileSetupState> = _uiState.asStateFlow()

    /**
     * Handle user actions
     */
    fun onAction(action: ProfileSetupAction) {
        when (action) {
            is ProfileSetupAction.UpdateDisplayName -> updateDisplayName(action.name)
            is ProfileSetupAction.UpdateHandicapIndex -> updateHandicapIndex(action.handicap)
            is ProfileSetupAction.UpdatePreferredUnits -> updatePreferredUnits(action.units)
            is ProfileSetupAction.UpdateHomeCourse -> updateHomeCourse(action.course)
            is ProfileSetupAction.SaveProfile -> saveProfile()
            is ProfileSetupAction.ClearError -> clearError()
        }
    }

    private fun updateDisplayName(name: String) {
        _uiState.update { state ->
            state.copy(
                displayName = name,
                displayNameError = validateDisplayName(name)
            )
        }
    }

    private fun updateHandicapIndex(handicap: String) {
        _uiState.update { state ->
            state.copy(
                handicapIndex = handicap,
                handicapError = validateHandicap(handicap)
            )
        }
    }

    private fun updatePreferredUnits(units: PreferredUnits) {
        _uiState.update { state ->
            state.copy(preferredUnits = units)
        }
    }

    private fun updateHomeCourse(course: String) {
        _uiState.update { state ->
            state.copy(homeCourse = course)
        }
    }

    private fun clearError() {
        _uiState.update { state ->
            state.copy(errorMessage = null)
        }
    }

    /**
     * Validate display name
     * Must not be blank
     */
    private fun validateDisplayName(name: String): String? {
        return when {
            name.isBlank() -> "Display name is required"
            name.length < 2 -> "Display name must be at least 2 characters"
            name.length > 50 -> "Display name must be less than 50 characters"
            else -> null
        }
    }

    /**
     * Validate handicap index
     * Must be between 0.0 and 54.0 with one decimal place
     * Empty/blank is valid (optional field)
     */
    fun validateHandicap(handicap: String): String? {
        if (handicap.isBlank()) return null // Optional field

        val handicapFloat = handicap.toFloatOrNull()
            ?: return "Handicap must be a valid number"

        return when {
            handicapFloat < 0.0f -> "Handicap cannot be negative"
            handicapFloat > 54.0f -> "Handicap cannot exceed 54.0"
            !isOneDecimalPlace(handicap) -> "Handicap must have at most one decimal place"
            else -> null
        }
    }

    /**
     * Check if a string represents a number with at most one decimal place
     */
    private fun isOneDecimalPlace(value: String): Boolean {
        val parts = value.split(".")
        return when {
            parts.size == 1 -> true // No decimal point
            parts.size == 2 -> parts[1].length <= 1 // One decimal place or less
            else -> false // More than one decimal point
        }
    }

    /**
     * Save profile to repository
     */
    private fun saveProfile() {
        val state = _uiState.value

        // Validate all fields before saving
        val displayNameError = validateDisplayName(state.displayName)
        val handicapError = validateHandicap(state.handicapIndex)

        if (displayNameError != null || handicapError != null) {
            _uiState.update {
                it.copy(
                    displayNameError = displayNameError,
                    handicapError = handicapError,
                    errorMessage = "Please fix the errors before saving"
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val profile = PlayerProfile(
                    supabaseUserId = "", // TODO: Get from auth in Task 5
                    displayName = state.displayName.trim(),
                    handicapIndex = state.handicapIndex.toFloatOrNull(),
                    preferredUnits = state.preferredUnits,
                    homeCourseId = if (state.homeCourse.isNotBlank()) state.homeCourse else null
                )

                profileRepository.saveProfile(profile)

                // AC6: Create default "My Bag" for the new profile
                createDefaultBagUseCase(profile.id)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save profile: ${e.message}"
                    )
                }
            }
        }
    }
}
