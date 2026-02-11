package com.caddypro.app.ui.shotlogger

/**
 * Round Start screen user actions
 */
sealed class RoundStartAction {
    data class UpdateCourseName(val name: String) : RoundStartAction()
    data class UpdateHolesPlayed(val holes: Int) : RoundStartAction()
    data object StartRound : RoundStartAction()
}
