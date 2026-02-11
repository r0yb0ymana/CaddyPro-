package com.caddypro.app.ui.shotlogger

import com.caddypro.app.domain.model.Shot
import com.caddypro.app.domain.model.ShotType

/**
 * Round Summary screen UI state
 */
data class RoundSummaryState(
    val roundId: String = "",
    val courseName: String = "",
    val holesPlayed: Int = 18,
    val totalShots: Int = 0,
    val startedAt: Long = 0L,
    val endedAt: Long? = null,
    val holeShotCounts: Map<Int, Int> = emptyMap(),
    val shotTypeBreakdown: Map<ShotType, Int> = emptyMap(),
    val clubUsage: List<ClubUsageStat> = emptyList(),
    val allShots: List<Shot> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val durationMinutes: Long
        get() {
            val end = endedAt ?: return 0L
            return (end - startedAt) / 60_000
        }
}

data class ClubUsageStat(
    val clubName: String,
    val count: Int
)
