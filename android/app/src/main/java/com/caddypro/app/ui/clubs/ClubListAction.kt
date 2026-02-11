package com.caddypro.app.ui.clubs

import com.caddypro.app.domain.model.Club

/**
 * User actions for ClubListScreen
 */
sealed class ClubListAction {
    data object ShowAddClub : ClubListAction()
    data class EditClub(val club: Club) : ClubListAction()
    data object DismissClubDetail : ClubListAction()
    data class DeleteClub(val clubId: String) : ClubListAction()
    data object QuickAddStandardSet : ClubListAction()  // AC11
    data object ClearError : ClubListAction()
    data object DismissMaxClubsWarning : ClubListAction()
}
