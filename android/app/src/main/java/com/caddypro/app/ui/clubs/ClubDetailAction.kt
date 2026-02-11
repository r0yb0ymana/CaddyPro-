package com.caddypro.app.ui.clubs

import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias

/**
 * User actions for ClubDetailSheet
 */
sealed class ClubDetailAction {
    data class UpdateName(val name: String) : ClubDetailAction()
    data class UpdateClubType(val type: ClubType) : ClubDetailAction()
    data class UpdateLoft(val loft: String) : ClubDetailAction()
    data class UpdateCarryDistance(val distance: String) : ClubDetailAction()
    data class UpdateTotalDistance(val distance: String) : ClubDetailAction()
    data class UpdateMissBias(val bias: MissBias) : ClubDetailAction()
    data object SaveClub : ClubDetailAction()
    data object ClearError : ClubDetailAction()
}
