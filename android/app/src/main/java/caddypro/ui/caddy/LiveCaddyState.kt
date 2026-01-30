package caddypro.ui.caddy

import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.LiveCaddySettings
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.WeatherData
import caddypro.domain.caddy.usecases.ShotResult
import caddypro.domain.navcaddy.context.RoundState
import com.example.app.domain.navcaddy.models.Club

/**
 * UI state for Live Caddy Mode.
 *
 * Represents the complete state of the Live Caddy HUD including:
 * - Current round context
 * - Real-time weather and readiness
 * - Hole strategy with hazard awareness
 * - Shot logger state
 * - HUD visibility toggles
 *
 * Spec reference: live-caddy-mode.md R1-R7
 * Plan reference: live-caddy-mode-plan.md Task 13
 * Acceptance criteria: A1-A4 (all)
 *
 * @property roundState Current active round state (null if no round active)
 * @property weather Current weather conditions (null if unavailable)
 * @property readiness Current readiness score (affects strategy conservatism)
 * @property holeStrategy Personalized hole strategy with hazards (null if unavailable)
 * @property isLoading True while loading context data
 * @property error Error message if context load fails (null if no error)
 * @property settings User's Live Caddy settings for distraction control
 * @property selectedClub Currently selected club for shot logging (null if no selection)
 * @property isShotLoggerVisible True when shot logger UI is displayed
 * @property lastShotConfirmed True briefly after shot is logged (for haptic/visual feedback)
 * @property isWeatherHudExpanded True when weather HUD is expanded to show details
 * @property isReadinessDetailsVisible True when readiness breakdown is shown
 * @property isStrategyMapVisible True when hole strategy map is displayed
 */
data class LiveCaddyState(
    val roundState: RoundState? = null,
    val weather: WeatherData? = null,
    val readiness: ReadinessScore = ReadinessScore.default(),
    val holeStrategy: HoleStrategy? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val settings: LiveCaddySettings = LiveCaddySettings.default(),

    // Shot logger state (R6: Real-Time Shot Logger)
    val selectedClub: Club? = null,
    val isShotLoggerVisible: Boolean = false,
    val lastShotConfirmed: Boolean = false,

    // HUD visibility (R2, R3, R4: Forecaster HUD, BodyCaddy, PinSeeker AI Map)
    val isWeatherHudExpanded: Boolean = false,
    val isReadinessDetailsVisible: Boolean = false,
    val isStrategyMapVisible: Boolean = false
)

/**
 * User actions for Live Caddy Mode.
 *
 * Sealed interface defining all possible user interactions with the Live Caddy HUD.
 * Each action maps to a specific use case or state mutation.
 *
 * Spec reference: live-caddy-mode.md R1-R7
 * Plan reference: live-caddy-mode-plan.md Task 13
 */
sealed interface LiveCaddyAction {
    /**
     * Load/refresh all live caddy context (weather, readiness, hole strategy).
     *
     * Spec reference: R1, R2, R3, R4
     */
    data object LoadContext : LiveCaddyAction

    /**
     * Manually refresh weather data.
     *
     * Spec reference: R2 (Forecaster HUD)
     */
    data object RefreshWeather : LiveCaddyAction

    /**
     * Manually refresh readiness score.
     *
     * Spec reference: R3 (BodyCaddy)
     */
    data object RefreshReadiness : LiveCaddyAction

    /**
     * Select a club for shot logging.
     *
     * Spec reference: R6 (Real-Time Shot Logger)
     *
     * @property club The club selected for the shot
     */
    data class SelectClub(val club: Club) : LiveCaddyAction

    /**
     * Log a shot with the selected club and result.
     *
     * Spec reference: R6 (Real-Time Shot Logger)
     * Acceptance criteria: A4 (Shot logger speed and persistence)
     *
     * @property result Shot result containing lie and optional miss direction
     */
    data class LogShot(val result: ShotResult) : LiveCaddyAction

    /**
     * Dismiss the shot logger UI without logging.
     *
     * Spec reference: R6 (Real-Time Shot Logger)
     */
    data object DismissShotLogger : LiveCaddyAction

    /**
     * Advance to a different hole.
     *
     * Spec reference: R1 (Live Round Context)
     *
     * @property holeNumber The hole number to advance to (1-18)
     */
    data class AdvanceHole(val holeNumber: Int) : LiveCaddyAction

    /**
     * End the current round.
     *
     * Spec reference: R1 (Live Round Context)
     */
    data object EndRound : LiveCaddyAction

    /**
     * Toggle weather HUD expanded/collapsed state.
     *
     * Spec reference: R2 (Forecaster HUD)
     *
     * @property expanded True to expand, false to collapse
     */
    data class ToggleWeatherHud(val expanded: Boolean) : LiveCaddyAction

    /**
     * Toggle readiness details visibility.
     *
     * Spec reference: R3 (BodyCaddy)
     *
     * @property visible True to show details, false to hide
     */
    data class ToggleReadinessDetails(val visible: Boolean) : LiveCaddyAction

    /**
     * Toggle hole strategy map visibility.
     *
     * Spec reference: R4 (PinSeeker AI Map)
     *
     * @property visible True to show map, false to hide
     */
    data class ToggleStrategyMap(val visible: Boolean) : LiveCaddyAction

    /**
     * Update Live Caddy settings.
     *
     * Spec reference: R7 (Safety and Distraction Controls)
     *
     * @property settings New settings to apply
     */
    data class UpdateSettings(val settings: LiveCaddySettings) : LiveCaddyAction
}

/**
 * Extension function to provide default ReadinessScore.
 *
 * Returns a neutral readiness score (70/100) when wearables are unavailable.
 */
private fun ReadinessScore.Companion.default(): ReadinessScore {
    return ReadinessScore(
        overall = 70,
        breakdown = caddypro.domain.caddy.models.ReadinessBreakdown(
            hrv = null,
            sleepQuality = null,
            stressLevel = null
        ),
        timestamp = System.currentTimeMillis(),
        source = caddypro.domain.caddy.models.ReadinessSource.MANUAL_ENTRY
    )
}

/**
 * Companion object for ReadinessScore to support default() extension.
 */
private val ReadinessScore.Companion: ReadinessScoreCompanion
    get() = ReadinessScoreCompanion

private object ReadinessScoreCompanion
