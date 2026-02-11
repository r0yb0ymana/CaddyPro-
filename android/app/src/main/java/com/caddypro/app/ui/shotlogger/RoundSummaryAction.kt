package com.caddypro.app.ui.shotlogger

/**
 * Round Summary screen user actions
 */
sealed class RoundSummaryAction {
    data object NavigateHome : RoundSummaryAction()
}
