package com.caddypro.app.ui.shotlogger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caddypro.app.domain.repository.RoundRepository
import com.caddypro.app.domain.repository.ShotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Round Summary ViewModel
 *
 * AC16: Hole summary shows shots per hole grid
 * AC17: Round summary shows per-hole breakdown
 */
@HiltViewModel
class RoundSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val roundRepository: RoundRepository,
    private val shotRepository: ShotRepository
) : ViewModel() {

    private val roundId: String = savedStateHandle["roundId"] ?: ""

    private val _uiState = MutableStateFlow(RoundSummaryState())
    val uiState: StateFlow<RoundSummaryState> = _uiState.asStateFlow()

    init {
        loadRoundSummary()
    }

    fun onAction(action: RoundSummaryAction) {
        when (action) {
            is RoundSummaryAction.NavigateHome -> { /* handled by screen */ }
        }
    }

    private fun loadRoundSummary() {
        viewModelScope.launch {
            if (roundId.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No round ID provided"
                )
                return@launch
            }

            val round = roundRepository.getRoundById(roundId)
            if (round == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Round not found"
                )
                return@launch
            }

            // Load all shots for the round
            val shots = shotRepository.getShotsByRoundId(roundId).firstOrNull() ?: emptyList()

            // Per-hole shot counts
            val holeShotCounts = shots.groupBy { it.holeNumber }
                .mapValues { it.value.size }

            // Shot type breakdown
            val shotTypeBreakdown = shots.groupBy { it.shotType }
                .mapValues { it.value.size }

            // Club usage stats
            val clubUsage = shots.groupBy { it.clubName }
                .map { (name, shotList) -> ClubUsageStat(name, shotList.size) }
                .sortedByDescending { it.count }

            _uiState.value = RoundSummaryState(
                roundId = round.id,
                courseName = round.courseName,
                holesPlayed = round.holesPlayed,
                totalShots = round.totalShots,
                startedAt = round.startedAt,
                endedAt = round.endedAt,
                holeShotCounts = holeShotCounts,
                shotTypeBreakdown = shotTypeBreakdown,
                clubUsage = clubUsage,
                allShots = shots,
                isLoading = false
            )
        }
    }
}
