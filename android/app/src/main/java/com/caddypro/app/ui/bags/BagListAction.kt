package com.caddypro.app.ui.bags

import com.caddypro.app.domain.model.Bag

/**
 * User actions for BagListScreen
 */
sealed class BagListAction {
    data class SetActiveBag(val bag: Bag) : BagListAction()
    data class ShowDeleteConfirmation(val bag: Bag) : BagListAction()
    data object DismissDeleteConfirmation : BagListAction()
    data object ConfirmDelete : BagListAction()
    data class NavigateToClubEditor(val bagId: String) : BagListAction()
    data object CreateNewBag : BagListAction()
    data object ClearError : BagListAction()
}
