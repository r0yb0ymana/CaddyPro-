package caddypro.ui.caddy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import caddypro.analytics.NavCaddyAnalytics
import caddypro.data.caddy.local.LiveCaddySettingsDataStore
import caddypro.domain.caddy.repositories.ClubBagRepository
import caddypro.domain.caddy.usecases.EndRoundUseCase
import caddypro.domain.caddy.usecases.GetLiveCaddyContextUseCase
import caddypro.domain.caddy.usecases.LogShotUseCase
import caddypro.domain.caddy.usecases.UpdateHoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Live Caddy Mode.
 *
 * Manages state and orchestrates use cases for the Live Caddy HUD:
 * - Loading round context (weather, readiness, hole strategy)
 * - Real-time shot logging
 * - Round management (advance hole, end round)
 * - HUD visibility toggles
 * - Settings persistence
 *
 * All use cases support offline-first operation with local caching.
 *
 * Spec reference: live-caddy-mode.md R1-R7
 * Plan reference: live-caddy-mode-plan.md Task 13
 * Acceptance criteria: A1-A4 (all)
 *
 * @property getLiveCaddyContext Use case to fetch all live caddy context
 * @property logShot Use case to log shots during the round
 * @property endRound Use case to finalize and save the round
 * @property updateHole Use case to advance to a different hole
 * @property clubBagRepository Repository for accessing active bag clubs
 * @property settingsDataStore DataStore for persisting settings
 * @property analytics Analytics logger for tracking events
 */
@HiltViewModel
class LiveCaddyViewModel @Inject constructor(
    private val getLiveCaddyContext: GetLiveCaddyContextUseCase,
    private val logShot: LogShotUseCase,
    private val endRound: EndRoundUseCase,
    private val updateHole: UpdateHoleUseCase,
    private val clubBagRepository: ClubBagRepository,
    private val settingsDataStore: LiveCaddySettingsDataStore,
    private val analytics: NavCaddyAnalytics
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveCaddyState())
    val uiState: StateFlow<LiveCaddyState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadContext()
        loadClubs()
    }

    /**
     * Handle user actions.
     *
     * Routes actions to appropriate handlers with error handling.
     *
     * @param action The user action to process
     */
    fun onAction(action: LiveCaddyAction) {
        when (action) {
            LiveCaddyAction.LoadContext -> loadContext()
            LiveCaddyAction.RefreshWeather -> refreshWeather()
            LiveCaddyAction.RefreshReadiness -> refreshReadiness()
            LiveCaddyAction.ShowShotLogger -> showShotLogger()
            is LiveCaddyAction.SelectClub -> selectClub(action.club)
            is LiveCaddyAction.LogShot -> logShotAction(action.result)
            LiveCaddyAction.DismissShotLogger -> dismissShotLogger()
            is LiveCaddyAction.AdvanceHole -> advanceHole(action.holeNumber)
            LiveCaddyAction.EndRound -> endRoundAction()
            is LiveCaddyAction.ToggleWeatherHud -> toggleWeatherHud(action.expanded)
            is LiveCaddyAction.ToggleReadinessDetails -> toggleReadinessDetails(action.visible)
            is LiveCaddyAction.ToggleStrategyMap -> toggleStrategyMap(action.visible)
            is LiveCaddyAction.UpdateSettings -> updateSettings(action.settings)
        }
    }

    /**
     * Load settings from DataStore.
     *
     * Collects settings flow and updates UI state reactively.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.getSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    /**
     * Load clubs from active bag.
     *
     * Collects clubs from repository and updates UI state.
     * Used by shot logger for club selection.
     *
     * Spec reference: R6 (Real-Time Shot Logger)
     */
    private fun loadClubs() {
        viewModelScope.launch {
            clubBagRepository.getActiveBagClubs().collect { clubs ->
                _uiState.update { it.copy(clubs = clubs) }
            }
        }
    }

    /**
     * Load all live caddy context.
     *
     * Fetches:
     * - Current round state
     * - Real-time weather
     * - Readiness score
     * - Hole strategy
     *
     * Spec reference: R1, R2, R3, R4
     * Acceptance criteria: A1 (Weather renders within 2 seconds)
     */
    private fun loadContext() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = getLiveCaddyContext()

            result.fold(
                onSuccess = { context ->
                    _uiState.update {
                        it.copy(
                            roundState = context.roundState,
                            weather = context.weather,
                            readiness = context.readiness,
                            holeStrategy = context.holeStrategy,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load live caddy context"
                        )
                    }
                    analytics.logError(
                        errorType = caddypro.analytics.AnalyticsEvent.ErrorOccurred.ErrorType.OPERATION_FAILED,
                        message = "Live caddy context load failed: ${error.message}",
                        isRecoverable = true,
                        throwable = error
                    )
                }
            )
        }
    }

    /**
     * Refresh weather data.
     *
     * Re-fetches weather from the weather repository.
     *
     * Spec reference: R2 (Forecaster HUD)
     * Acceptance criteria: A1 (Weather renders within 2 seconds)
     */
    private fun refreshWeather() {
        // Weather refresh is included in loadContext
        // For now, just reload entire context
        // TODO: Add dedicated weather refresh method when weatherRepository is injectable
        loadContext()
    }

    /**
     * Refresh readiness score.
     *
     * Re-fetches readiness from the readiness repository.
     *
     * Spec reference: R3 (BodyCaddy)
     * Acceptance criteria: A2 (Readiness impacts strategy)
     */
    private fun refreshReadiness() {
        // Readiness refresh is included in loadContext
        // For now, just reload entire context
        // TODO: Add dedicated readiness refresh method when readinessRepository is injectable
        loadContext()
    }

    /**
     * Show the shot logger UI.
     *
     * Opens the shot logger panel for logging a shot.
     *
     * Spec reference: R6 (Real-Time Shot Logger)
     */
    private fun showShotLogger() {
        _uiState.update {
            it.copy(
                isShotLoggerVisible = true,
                selectedClub = null,
                lastShotConfirmed = false
            )
        }
    }

    /**
     * Select a club for shot logging.
     *
     * Updates the selected club in the shot logger.
     *
     * Spec reference: R6 (Real-Time Shot Logger)
     *
     * @param club The club to select
     */
    private fun selectClub(club: com.example.app.domain.navcaddy.models.Club) {
        _uiState.update {
            it.copy(selectedClub = club)
        }
    }

    /**
     * Log a shot with the selected club and result.
     *
     * Persists the shot to the repository and provides haptic confirmation.
     * Supports offline-first operation.
     *
     * Spec reference: R6 (Real-Time Shot Logger)
     * Acceptance criteria: A4 (Shot logger speed and persistence)
     *
     * @param result The shot result (lie and optional miss direction)
     */
    private fun logShotAction(result: caddypro.domain.caddy.usecases.ShotResult) {
        viewModelScope.launch {
            val club = _uiState.value.selectedClub
            if (club == null) {
                _uiState.update {
                    it.copy(error = "No club selected for shot logging")
                }
                return@launch
            }

            val logResult = logShot(club, result)

            logResult.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            lastShotConfirmed = true,
                            isShotLoggerVisible = false,
                            selectedClub = null,
                            error = null
                        )
                    }

                    // Reset confirmation state after brief delay
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(1000)
                        _uiState.update { it.copy(lastShotConfirmed = false) }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to log shot"
                        )
                    }
                    analytics.logError(
                        errorType = caddypro.analytics.AnalyticsEvent.ErrorOccurred.ErrorType.OPERATION_FAILED,
                        message = "Shot logging failed: ${error.message}",
                        isRecoverable = true,
                        throwable = error
                    )
                }
            )
        }
    }

    /**
     * Dismiss the shot logger without logging.
     *
     * Spec reference: R6 (Real-Time Shot Logger)
     */
    private fun dismissShotLogger() {
        _uiState.update {
            it.copy(
                isShotLoggerVisible = false,
                selectedClub = null
            )
        }
    }

    /**
     * Advance to a different hole.
     *
     * Updates the round state and reloads hole strategy.
     * Uses default par of 4 for the new hole (can be adjusted by UI if needed).
     *
     * Spec reference: R1 (Live Round Context)
     *
     * @param holeNumber The hole number to advance to (1-18)
     */
    private fun advanceHole(holeNumber: Int) {
        viewModelScope.launch {
            require(holeNumber in 1..18) { "Hole number must be between 1 and 18" }

            // Use default par of 4 (typical par for most holes)
            // In production, this should be fetched from course data
            val par = 4

            val result = updateHole(holeNumber, par)

            result.fold(
                onSuccess = {
                    // Reload context to get new hole strategy
                    loadContext()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to advance hole"
                        )
                    }
                    analytics.logError(
                        errorType = caddypro.analytics.AnalyticsEvent.ErrorOccurred.ErrorType.OPERATION_FAILED,
                        message = "Advance hole failed: ${error.message}",
                        isRecoverable = true,
                        throwable = error
                    )
                }
            )
        }
    }

    /**
     * End the current round.
     *
     * Finalizes the round and clears the active session.
     *
     * Spec reference: R1 (Live Round Context)
     */
    private fun endRoundAction() {
        viewModelScope.launch {
            val result = endRound()

            result.fold(
                onSuccess = { summary ->
                    _uiState.update {
                        it.copy(
                            roundState = null,
                            weather = null,
                            holeStrategy = null,
                            error = null
                        )
                    }
                    // TODO: Navigate to round summary screen
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            error = error.message ?: "Failed to end round"
                        )
                    }
                    analytics.logError(
                        errorType = caddypro.analytics.AnalyticsEvent.ErrorOccurred.ErrorType.OPERATION_FAILED,
                        message = "End round failed: ${error.message}",
                        isRecoverable = true,
                        throwable = error
                    )
                }
            )
        }
    }

    /**
     * Toggle weather HUD expanded/collapsed state.
     *
     * Spec reference: R2 (Forecaster HUD)
     *
     * @param expanded True to expand, false to collapse
     */
    private fun toggleWeatherHud(expanded: Boolean) {
        _uiState.update {
            it.copy(isWeatherHudExpanded = expanded)
        }
    }

    /**
     * Toggle readiness details visibility.
     *
     * Spec reference: R3 (BodyCaddy)
     *
     * @param visible True to show details, false to hide
     */
    private fun toggleReadinessDetails(visible: Boolean) {
        _uiState.update {
            it.copy(isReadinessDetailsVisible = visible)
        }
    }

    /**
     * Toggle hole strategy map visibility.
     *
     * Spec reference: R4 (PinSeeker AI Map)
     *
     * @param visible True to show map, false to hide
     */
    private fun toggleStrategyMap(visible: Boolean) {
        _uiState.update {
            it.copy(isStrategyMapVisible = visible)
        }
    }

    /**
     * Update Live Caddy settings.
     *
     * Persists settings to DataStore for permanent storage.
     *
     * Spec reference: R7 (Safety and Distraction Controls)
     *
     * @param settings New settings to apply
     */
    private fun updateSettings(settings: caddypro.domain.caddy.models.LiveCaddySettings) {
        viewModelScope.launch {
            try {
                settingsDataStore.saveSettings(settings)
                // State is updated automatically via settings flow
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to save settings"
                    )
                }
                analytics.logError(
                    errorType = caddypro.analytics.AnalyticsEvent.ErrorOccurred.ErrorType.OPERATION_FAILED,
                    message = "Settings update failed: ${e.message}",
                    isRecoverable = true,
                    throwable = e
                )
            }
        }
    }
}
