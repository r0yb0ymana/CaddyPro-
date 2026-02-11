package com.caddypro.app.ui.shotlogger

/**
 * Round Start screen UI state
 */
data class RoundStartState(
    val courseName: String = "",
    val holesPlayed: Int = 18,
    val activeBagName: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val courseNameError: String? = null,
    val hasActiveBag: Boolean = false,
    val roundStarted: Boolean = false
)
