package com.caddypro.app.ui.shotlogger

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caddypro.app.data.repository.LocationRepository
import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.Shot
import com.caddypro.app.domain.model.ShotType
import com.caddypro.app.domain.repository.BagRepository
import com.caddypro.app.domain.repository.ClubRepository
import com.caddypro.app.domain.repository.ProfileRepository
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
 * Shot Logger ViewModel
 *
 * AC6: Club quick-select shows all clubs from active bag
 * AC7: Shot type auto-suggested based on shot number
 * AC8: "Log Shot" requires both club and shot type selected
 * AC9: Shot number auto-increments per hole
 * AC10: GPS location captured automatically on shot log
 * AC12: Undo last shot with confirmation
 */
@HiltViewModel
class ShotLoggerViewModel @Inject constructor(
    private val roundRepository: RoundRepository,
    private val shotRepository: ShotRepository,
    private val clubRepository: ClubRepository,
    private val profileRepository: ProfileRepository,
    private val bagRepository: BagRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShotLoggerState())
    val uiState: StateFlow<ShotLoggerState> = _uiState.asStateFlow()

    init {
        loadActiveRound()
    }

    fun onAction(action: ShotLoggerAction) {
        when (action) {
            is ShotLoggerAction.SelectClub -> selectClub(action.club)
            is ShotLoggerAction.SelectShotType -> selectShotType(action.type)
            is ShotLoggerAction.LogShot -> logShot()
            is ShotLoggerAction.NextHole -> nextHole()
            is ShotLoggerAction.ShowUndoConfirmation -> showUndoConfirmation()
            is ShotLoggerAction.ConfirmUndo -> confirmUndo()
            is ShotLoggerAction.DismissUndo -> dismissUndo()
            is ShotLoggerAction.ShowEndRound -> showEndRound()
            is ShotLoggerAction.ConfirmEndRound -> confirmEndRound()
            is ShotLoggerAction.DismissEndRound -> dismissEndRound()
            is ShotLoggerAction.ToggleHoleSummary -> toggleHoleSummary()
        }
    }

    private fun loadActiveRound() {
        viewModelScope.launch {
            val round = roundRepository.getActiveRound()
            if (round == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No active round"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                roundId = round.id,
                courseName = round.courseName,
                maxHoles = round.holesPlayed
            )

            // Load clubs from active bag
            val profile = profileRepository.getProfile().firstOrNull()
            if (profile != null) {
                val activeBag = bagRepository.getActiveBagSync(profile.id)
                if (activeBag != null) {
                    clubRepository.getClubsByBagId(activeBag.id).collect { clubs ->
                        _uiState.value = _uiState.value.copy(clubs = clubs)
                    }
                }
            }

            // Load shots for current hole
            loadCurrentHoleShots()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private fun loadCurrentHoleShots() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.roundId.isEmpty()) return@launch

            shotRepository.getShotsByHole(state.roundId, state.currentHole).collect { shots ->
                val shotCount = shots.size
                val totalShots = shotRepository.getTotalShotCount(state.roundId)
                val suggested = suggestShotType(shotCount + 1)

                _uiState.value = _uiState.value.copy(
                    shotsForCurrentHole = shots,
                    shotCountForHole = shotCount,
                    totalShots = totalShots,
                    suggestedShotType = suggested,
                    selectedShotType = suggested
                )
            }
        }
    }

    private fun selectClub(club: Club) {
        _uiState.value = _uiState.value.copy(selectedClub = club)
    }

    private fun selectShotType(type: ShotType) {
        _uiState.value = _uiState.value.copy(selectedShotType = type)
    }

    private fun logShot() {
        val state = _uiState.value
        val club = state.selectedClub ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            // AC10: GPS location captured automatically
            var location: Location? = null
            if (locationRepository.hasLocationPermission()) {
                location = locationRepository.getLastKnownLocation()
            }

            val shotNumber = state.shotCountForHole + 1
            val shot = Shot(
                roundId = state.roundId,
                holeNumber = state.currentHole,
                shotNumber = shotNumber,
                clubId = club.id,
                clubName = club.name,
                shotType = state.selectedShotType,
                startLatitude = location?.latitude,
                startLongitude = location?.longitude
            )

            val result = shotRepository.logShot(shot)
            result.fold(
                onSuccess = {
                    // Update round shot count
                    val newTotal = state.totalShots + 1
                    roundRepository.updateShotCount(state.roundId, newTotal)

                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        lastSavedClubId = club.id,
                        errorMessage = null
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Failed to log shot: ${e.message}"
                    )
                }
            )
        }
    }

    private fun nextHole() {
        val state = _uiState.value
        if (!state.canAdvanceHole) return

        _uiState.value = _uiState.value.copy(
            currentHole = state.currentHole + 1,
            selectedClub = null,
            showHoleSummary = false
        )
        loadCurrentHoleShots()
    }

    private fun showUndoConfirmation() {
        _uiState.value = _uiState.value.copy(showUndoConfirmation = true)
    }

    private fun confirmUndo() {
        val lastShot = _uiState.value.shotsForCurrentHole.lastOrNull()
        if (lastShot != null) {
            viewModelScope.launch {
                shotRepository.deleteShot(lastShot.id)
                _uiState.value = _uiState.value.copy(showUndoConfirmation = false)
            }
        }
    }

    private fun dismissUndo() {
        _uiState.value = _uiState.value.copy(showUndoConfirmation = false)
    }

    private fun showEndRound() {
        _uiState.value = _uiState.value.copy(showEndRoundConfirmation = true)
    }

    private fun confirmEndRound() {
        viewModelScope.launch {
            roundRepository.endRound(_uiState.value.roundId)
            _uiState.value = _uiState.value.copy(
                showEndRoundConfirmation = false,
                roundEnded = true
            )
        }
    }

    private fun dismissEndRound() {
        _uiState.value = _uiState.value.copy(showEndRoundConfirmation = false)
    }

    private fun toggleHoleSummary() {
        _uiState.value = _uiState.value.copy(
            showHoleSummary = !_uiState.value.showHoleSummary
        )
    }

    /**
     * AC7: Auto-suggest shot type based on shot number
     */
    private fun suggestShotType(shotNumber: Int): ShotType {
        return when {
            shotNumber == 1 -> ShotType.TEE
            shotNumber in 2..3 -> ShotType.FAIRWAY
            else -> ShotType.PUTT
        }
    }
}
