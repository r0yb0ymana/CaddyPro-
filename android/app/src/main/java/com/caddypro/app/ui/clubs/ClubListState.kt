package com.caddypro.app.ui.clubs

import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ClubType

/**
 * UI State for ClubListScreen
 *
 * AC15: Shows warning at 14 clubs, blocks at 15
 */
data class ClubListState(
    val clubs: List<Club> = emptyList(),
    val clubsByType: Map<ClubType, List<Club>> = emptyMap(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val bagId: String = "",
    val bagName: String = "",
    val clubCount: Int = 0,
    val showMaxClubsWarning: Boolean = false,  // AC15: Warning at 14 clubs
    val showClubDetail: Boolean = false,
    val clubToEdit: Club? = null
) {
    /**
     * AC15: Check if we're at maximum club limit
     */
    fun isAtMaxClubs(): Boolean = clubCount >= 14

    /**
     * AC15: Check if we should show warning (at 14 clubs)
     */
    fun shouldShowWarning(): Boolean = clubCount == 14
}
