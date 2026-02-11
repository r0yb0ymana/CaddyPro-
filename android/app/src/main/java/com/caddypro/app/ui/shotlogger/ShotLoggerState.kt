package com.caddypro.app.ui.shotlogger

import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.Shot
import com.caddypro.app.domain.model.ShotType

/**
 * Shot Logger screen UI state
 */
data class ShotLoggerState(
    val roundId: String = "",
    val courseName: String = "",
    val currentHole: Int = 1,
    val maxHoles: Int = 18,
    val shotCountForHole: Int = 0,
    val totalShots: Int = 0,
    val clubs: List<Club> = emptyList(),
    val selectedClub: Club? = null,
    val selectedShotType: ShotType = ShotType.TEE,
    val suggestedShotType: ShotType = ShotType.TEE,
    val shotsForCurrentHole: List<Shot> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val showUndoConfirmation: Boolean = false,
    val showEndRoundConfirmation: Boolean = false,
    val showHoleSummary: Boolean = false,
    val roundEnded: Boolean = false,
    val errorMessage: String? = null,
    val lastSavedClubId: String? = null
) {
    val canLogShot: Boolean
        get() = selectedClub != null && !isSaving

    val canAdvanceHole: Boolean
        get() = currentHole < maxHoles

    val canUndo: Boolean
        get() = shotsForCurrentHole.isNotEmpty()
}
