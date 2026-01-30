package caddypro.ui.caddy

import caddypro.domain.caddy.models.LiveCaddySettings
import caddypro.domain.caddy.usecases.ShotResult
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.models.Lie
import com.example.app.domain.navcaddy.models.Club
import com.example.app.domain.navcaddy.models.ClubType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for LiveCaddyState data class.
 *
 * Validates:
 * - Default state initialization
 * - State immutability and copy operations
 * - Action type checking
 *
 * Spec reference: live-caddy-mode.md R1-R7
 * Plan reference: live-caddy-mode-plan.md Task 13
 */
class LiveCaddyStateTest {

    @Test
    fun `default state has correct initial values`() {
        // When: Creating default state
        val state = LiveCaddyState()

        // Then: State has expected defaults
        assertNull(state.roundState)
        assertNull(state.weather)
        assertNull(state.holeStrategy)
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertEquals(LiveCaddySettings.default(), state.settings)

        // Shot logger defaults
        assertNull(state.selectedClub)
        assertFalse(state.isShotLoggerVisible)
        assertFalse(state.lastShotConfirmed)

        // HUD visibility defaults
        assertFalse(state.isWeatherHudExpanded)
        assertFalse(state.isReadinessDetailsVisible)
        assertFalse(state.isStrategyMapVisible)
    }

    @Test
    fun `state copy updates only specified fields`() {
        // Given: Initial state
        val initialState = LiveCaddyState()

        // When: Copying with isLoading change
        val loadedState = initialState.copy(isLoading = false)

        // Then: Only isLoading changed
        assertFalse(loadedState.isLoading)
        assertNull(loadedState.roundState)
        assertEquals(initialState.settings, loadedState.settings)
    }

    @Test
    fun `state copy with round state updates correctly`() {
        // Given: Initial state
        val initialState = LiveCaddyState()

        // When: Adding round state
        val roundState = RoundState(
            roundId = "round-1",
            courseName = "Pebble Beach",
            currentHole = 1,
            currentPar = 4,
            totalScore = 0,
            holesCompleted = 0
        )
        val stateWithRound = initialState.copy(roundState = roundState)

        // Then: Round state is set
        assertEquals(roundState, stateWithRound.roundState)
        assertEquals("round-1", stateWithRound.roundState?.roundId)
        assertEquals("Pebble Beach", stateWithRound.roundState?.courseName)
    }

    @Test
    fun `state copy with shot logger state updates correctly`() {
        // Given: Initial state
        val initialState = LiveCaddyState()

        // When: Opening shot logger with club
        val club = Club(
            id = "club-1",
            name = "7-iron",
            type = ClubType.IRON,
            estimatedCarry = 150
        )
        val stateWithLogger = initialState.copy(
            selectedClub = club,
            isShotLoggerVisible = true
        )

        // Then: Shot logger state is updated
        assertEquals(club, stateWithLogger.selectedClub)
        assertTrue(stateWithLogger.isShotLoggerVisible)
        assertFalse(stateWithLogger.lastShotConfirmed)
    }

    @Test
    fun `state copy with HUD visibility updates correctly`() {
        // Given: Initial state
        val initialState = LiveCaddyState()

        // When: Expanding weather HUD
        val stateWithWeather = initialState.copy(isWeatherHudExpanded = true)

        // Then: Weather HUD is expanded
        assertTrue(stateWithWeather.isWeatherHudExpanded)
        assertFalse(stateWithWeather.isReadinessDetailsVisible)
        assertFalse(stateWithWeather.isStrategyMapVisible)

        // When: Showing all HUDs
        val stateWithAllHuds = initialState.copy(
            isWeatherHudExpanded = true,
            isReadinessDetailsVisible = true,
            isStrategyMapVisible = true
        )

        // Then: All HUDs are visible
        assertTrue(stateWithAllHuds.isWeatherHudExpanded)
        assertTrue(stateWithAllHuds.isReadinessDetailsVisible)
        assertTrue(stateWithAllHuds.isStrategyMapVisible)
    }

    @Test
    fun `state immutability - copy does not affect original`() {
        // Given: Initial state
        val originalState = LiveCaddyState()

        // When: Creating a modified copy
        val modifiedState = originalState.copy(
            isLoading = false,
            isWeatherHudExpanded = true
        )

        // Then: Original state is unchanged
        assertTrue(originalState.isLoading)
        assertFalse(originalState.isWeatherHudExpanded)

        // And: Modified state has changes
        assertFalse(modifiedState.isLoading)
        assertTrue(modifiedState.isWeatherHudExpanded)
    }

    // ========== Action Tests ==========

    @Test
    fun `LoadContext action is singleton`() {
        // When: Creating multiple instances
        val action1 = LiveCaddyAction.LoadContext
        val action2 = LiveCaddyAction.LoadContext

        // Then: They are the same instance
        assertEquals(action1, action2)
    }

    @Test
    fun `SelectClub action contains club data`() {
        // Given: A club
        val club = Club(
            id = "club-1",
            name = "Driver",
            type = ClubType.DRIVER,
            estimatedCarry = 230
        )

        // When: Creating SelectClub action
        val action = LiveCaddyAction.SelectClub(club)

        // Then: Action contains club
        assertEquals(club, action.club)
        assertEquals("Driver", action.club.name)
    }

    @Test
    fun `LogShot action contains shot result data`() {
        // Given: A shot result
        val shotResult = ShotResult(
            lie = Lie.FAIRWAY,
            missDirection = null
        )

        // When: Creating LogShot action
        val action = LiveCaddyAction.LogShot(shotResult)

        // Then: Action contains shot result
        assertEquals(shotResult, action.result)
        assertEquals(Lie.FAIRWAY, action.result.lie)
    }

    @Test
    fun `AdvanceHole action contains hole number`() {
        // When: Creating AdvanceHole action
        val action = LiveCaddyAction.AdvanceHole(holeNumber = 5)

        // Then: Action contains hole number
        assertEquals(5, action.holeNumber)
    }

    @Test
    fun `ToggleWeatherHud action contains expanded state`() {
        // When: Creating toggle actions
        val expandAction = LiveCaddyAction.ToggleWeatherHud(expanded = true)
        val collapseAction = LiveCaddyAction.ToggleWeatherHud(expanded = false)

        // Then: Actions contain correct state
        assertTrue(expandAction.expanded)
        assertFalse(collapseAction.expanded)
    }

    @Test
    fun `UpdateSettings action contains settings data`() {
        // Given: New settings
        val settings = LiveCaddySettings.outdoor()

        // When: Creating UpdateSettings action
        val action = LiveCaddyAction.UpdateSettings(settings)

        // Then: Action contains settings
        assertEquals(settings, action.settings)
        assertTrue(action.settings.lowDistractionMode)
    }

    @Test
    fun `all action types are distinct`() {
        // When: Creating different action types
        val loadContext = LiveCaddyAction.LoadContext
        val refreshWeather = LiveCaddyAction.RefreshWeather
        val refreshReadiness = LiveCaddyAction.RefreshReadiness
        val dismissLogger = LiveCaddyAction.DismissShotLogger
        val endRound = LiveCaddyAction.EndRound

        // Then: All singleton actions are equal to themselves
        assertEquals(loadContext, LiveCaddyAction.LoadContext)
        assertEquals(refreshWeather, LiveCaddyAction.RefreshWeather)
        assertEquals(refreshReadiness, LiveCaddyAction.RefreshReadiness)
        assertEquals(dismissLogger, LiveCaddyAction.DismissShotLogger)
        assertEquals(endRound, LiveCaddyAction.EndRound)
    }
}
