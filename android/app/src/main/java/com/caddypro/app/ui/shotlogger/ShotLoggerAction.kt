package com.caddypro.app.ui.shotlogger

import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ShotType

/**
 * Shot Logger screen user actions
 */
sealed class ShotLoggerAction {
    data class SelectClub(val club: Club) : ShotLoggerAction()
    data class SelectShotType(val type: ShotType) : ShotLoggerAction()
    data object LogShot : ShotLoggerAction()
    data object NextHole : ShotLoggerAction()
    data object ShowUndoConfirmation : ShotLoggerAction()
    data object ConfirmUndo : ShotLoggerAction()
    data object DismissUndo : ShotLoggerAction()
    data object ShowEndRound : ShotLoggerAction()
    data object ConfirmEndRound : ShotLoggerAction()
    data object DismissEndRound : ShotLoggerAction()
    data object ToggleHoleSummary : ShotLoggerAction()
}
