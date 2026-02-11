package com.caddypro.app.ui.shotlogger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caddypro.app.domain.model.Round
import com.caddypro.app.domain.repository.BagRepository
import com.caddypro.app.domain.repository.ProfileRepository
import com.caddypro.app.domain.repository.RoundRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Round Start ViewModel
 *
 * AC1: User can start a round with course name and hole count
 * AC2: Active bag auto-selected and shown
 * AC3: Only one active round at a time
 */
@HiltViewModel
class RoundStartViewModel @Inject constructor(
    private val roundRepository: RoundRepository,
    private val profileRepository: ProfileRepository,
    private val bagRepository: BagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoundStartState())
    val uiState: StateFlow<RoundStartState> = _uiState.asStateFlow()

    private var profileId: String? = null

    init {
        loadActiveBag()
    }

    fun onAction(action: RoundStartAction) {
        when (action) {
            is RoundStartAction.UpdateCourseName -> updateCourseName(action.name)
            is RoundStartAction.UpdateHolesPlayed -> updateHolesPlayed(action.holes)
            is RoundStartAction.StartRound -> startRound()
        }
    }

    private fun loadActiveBag() {
        viewModelScope.launch {
            val profile = profileRepository.getProfile().firstOrNull()
            if (profile == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No profile found."
                )
                return@launch
            }

            profileId = profile.id
            val activeBag = bagRepository.getActiveBagSync(profile.id)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                activeBagName = activeBag?.name ?: "No bag",
                hasActiveBag = activeBag != null
            )
        }
    }

    private fun updateCourseName(name: String) {
        _uiState.value = _uiState.value.copy(
            courseName = name,
            courseNameError = null
        )
    }

    private fun updateHolesPlayed(holes: Int) {
        _uiState.value = _uiState.value.copy(holesPlayed = holes)
    }

    private fun startRound() {
        val state = _uiState.value
        if (state.courseName.isBlank()) {
            _uiState.value = state.copy(courseNameError = "Course name is required")
            return
        }
        if (!state.hasActiveBag) {
            _uiState.value = state.copy(errorMessage = "Set up a bag before starting a round")
            return
        }

        val pid = profileId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val round = Round(
                profileId = pid,
                courseName = state.courseName.trim(),
                holesPlayed = state.holesPlayed
            )
            val result = roundRepository.createRound(round)

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        roundStarted = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            )
        }
    }
}
