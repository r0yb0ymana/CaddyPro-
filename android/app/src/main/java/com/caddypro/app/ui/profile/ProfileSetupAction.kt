package com.caddypro.app.ui.profile

import com.caddypro.app.data.local.entities.PreferredUnits

/**
 * User actions for ProfileSetupScreen
 */
sealed class ProfileSetupAction {
    data class UpdateDisplayName(val name: String) : ProfileSetupAction()
    data class UpdateHandicapIndex(val handicap: String) : ProfileSetupAction()
    data class UpdatePreferredUnits(val units: PreferredUnits) : ProfileSetupAction()
    data class UpdateHomeCourse(val course: String) : ProfileSetupAction()
    data object SaveProfile : ProfileSetupAction()
    data object ClearError : ProfileSetupAction()
}
