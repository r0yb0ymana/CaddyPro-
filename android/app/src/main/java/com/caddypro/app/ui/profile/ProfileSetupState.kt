package com.caddypro.app.ui.profile

import com.caddypro.app.data.local.entities.PreferredUnits

/**
 * UI State for ProfileSetupScreen
 */
data class ProfileSetupState(
    val displayName: String = "",
    val handicapIndex: String = "",
    val preferredUnits: PreferredUnits = PreferredUnits.METRIC,
    val homeCourse: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val displayNameError: String? = null,
    val handicapError: String? = null
) {
    /**
     * Check if form is valid and can be submitted
     */
    fun isValid(): Boolean {
        return displayName.isNotBlank() &&
                displayNameError == null &&
                handicapError == null &&
                (handicapIndex.isBlank() || handicapIndex.toFloatOrNull() != null)
    }
}
