package caddypro.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import caddypro.domain.navcaddy.navigation.NavCaddyDestination
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NavCaddyNavigatorImpl with Live Caddy routes.
 *
 * Verifies:
 * - LiveCaddy navigation
 * - RoundEndSummary navigation with parameters
 * - Back navigation from Live Caddy
 * - PopUpTo navigation for round flows
 * - Clear back stack navigation
 *
 * Spec reference: live-caddy-mode.md R1, R5, live-caddy-mode-plan.md Task 23
 */
class NavCaddyNavigatorImplTest {

    private lateinit var navController: NavController
    private lateinit var navigator: NavCaddyNavigatorImpl

    @Before
    fun setup() {
        navController = mockk(relaxed = true)
        navigator = NavCaddyNavigatorImpl(navController)
    }

    // LiveCaddy Navigation Tests

    @Test
    fun `navigate to LiveCaddy calls navController with correct route`() {
        val routeSlot = slot<String>()
        every { navController.navigate(capture(routeSlot)) } just Runs

        navigator.navigate(NavCaddyDestination.LiveCaddy)

        verify { navController.navigate(any<String>()) }
        assertEquals("caddy/live_caddy", routeSlot.captured)
    }

    @Test
    fun `navigate to LiveCaddy from multiple sources uses same route`() {
        val routeSlot = slot<String>()
        every { navController.navigate(capture(routeSlot)) } just Runs

        // Navigate from home
        navigator.navigate(NavCaddyDestination.LiveCaddy)
        val route1 = routeSlot.captured

        // Navigate from shot recommendation
        navigator.navigate(NavCaddyDestination.LiveCaddy)
        val route2 = routeSlot.captured

        assertEquals(route1, route2)
        assertEquals("caddy/live_caddy", route1)
    }

    // RoundEndSummary Navigation Tests

    @Test
    fun `navigate to RoundEndSummary includes roundId in route`() {
        val routeSlot = slot<String>()
        every { navController.navigate(capture(routeSlot)) } just Runs

        navigator.navigate(NavCaddyDestination.RoundEndSummary(roundId = 123L))

        verify { navController.navigate(any<String>()) }
        assertEquals("caddy/round_end_summary/123", routeSlot.captured)
    }

    @Test
    fun `navigate to RoundEndSummary with different IDs generates different routes`() {
        val routes = mutableListOf<String>()
        every { navController.navigate(capture(routes)) } just Runs

        navigator.navigate(NavCaddyDestination.RoundEndSummary(roundId = 1L))
        navigator.navigate(NavCaddyDestination.RoundEndSummary(roundId = 999L))

        assertEquals(2, routes.size)
        assertEquals("caddy/round_end_summary/1", routes[0])
        assertEquals("caddy/round_end_summary/999", routes[1])
    }

    // Back Navigation Tests

    @Test
    fun `navigateBack returns true when pop is successful`() {
        every { navController.popBackStack() } returns true

        val result = navigator.navigateBack()

        assertTrue(result)
        verify { navController.popBackStack() }
    }

    @Test
    fun `navigateBack returns false when at root`() {
        every { navController.popBackStack() } returns false

        val result = navigator.navigateBack()

        assertFalse(result)
        verify { navController.popBackStack() }
    }

    // PopUpTo Navigation Tests

    @Test
    fun `navigateAndPopUpTo LiveCaddy clears back to home`() {
        val routeSlot = slot<String>()
        val builderSlot = slot<NavOptionsBuilder.() -> Unit>()
        every {
            navController.navigate(capture(routeSlot), capture(builderSlot))
        } just Runs

        navigator.navigateAndPopUpTo(
            destination = NavCaddyDestination.LiveCaddy,
            popUpTo = "home",
            inclusive = false
        )

        verify { navController.navigate(any<String>(), any<NavOptionsBuilder.() -> Unit>()) }
        assertEquals("caddy/live_caddy", routeSlot.captured)
    }

    @Test
    fun `navigateAndPopUpTo RoundStart to LiveCaddy after round started`() {
        val routeSlot = slot<String>()
        val builderSlot = slot<NavOptionsBuilder.() -> Unit>()
        every {
            navController.navigate(capture(routeSlot), capture(builderSlot))
        } just Runs

        // Simulate: StartRound -> LiveCaddy, clearing StartRound from back stack
        navigator.navigateAndPopUpTo(
            destination = NavCaddyDestination.LiveCaddy,
            popUpTo = "caddy/round_start",
            inclusive = true
        )

        verify { navController.navigate(any<String>(), any<NavOptionsBuilder.() -> Unit>()) }
        assertEquals("caddy/live_caddy", routeSlot.captured)
    }

    @Test
    fun `navigateAndPopUpTo RoundEndSummary clears LiveCaddy from stack`() {
        val routeSlot = slot<String>()
        every {
            navController.navigate(capture(routeSlot), any<NavOptionsBuilder.() -> Unit>())
        } just Runs

        navigator.navigateAndPopUpTo(
            destination = NavCaddyDestination.RoundEndSummary(roundId = 42L),
            popUpTo = "caddy/live_caddy",
            inclusive = true
        )

        verify { navController.navigate(any<String>(), any<NavOptionsBuilder.() -> Unit>()) }
        assertEquals("caddy/round_end_summary/42", routeSlot.captured)
    }

    // Clear Back Stack Tests

    @Test
    fun `navigateAndClearBackStack to LiveCaddy clears all history`() {
        val startDestinationId = 12345
        every { navController.graph.startDestinationId } returns startDestinationId

        val routeSlot = slot<String>()
        every {
            navController.navigate(capture(routeSlot), any<NavOptionsBuilder.() -> Unit>())
        } just Runs

        navigator.navigateAndClearBackStack(NavCaddyDestination.LiveCaddy)

        verify { navController.navigate(any<String>(), any<NavOptionsBuilder.() -> Unit>()) }
        verify { navController.graph.startDestinationId }
        assertEquals("caddy/live_caddy", routeSlot.captured)
    }

    @Test
    fun `navigateAndClearBackStack to RoundEndSummary after logout`() {
        val startDestinationId = 99999
        every { navController.graph.startDestinationId } returns startDestinationId

        val routeSlot = slot<String>()
        every {
            navController.navigate(capture(routeSlot), any<NavOptionsBuilder.() -> Unit>())
        } just Runs

        navigator.navigateAndClearBackStack(
            NavCaddyDestination.RoundEndSummary(roundId = 100L)
        )

        verify { navController.navigate(any<String>(), any<NavOptionsBuilder.() -> Unit>()) }
        assertEquals("caddy/round_end_summary/100", routeSlot.captured)
    }

    // Integration Tests

    @Test
    fun `navigate to existing destinations still works`() {
        val routeSlot = slot<String>()
        every { navController.navigate(capture(routeSlot)) } just Runs

        navigator.navigate(NavCaddyDestination.WeatherCheck)

        verify { navController.navigate(any<String>()) }
        assertEquals("caddy/weather", routeSlot.captured)
    }

    @Test
    fun `navigate to ShotRecommendation with parameters works`() {
        val routeSlot = slot<String>()
        every { navController.navigate(capture(routeSlot)) } just Runs

        navigator.navigate(
            NavCaddyDestination.ShotRecommendation(
                yardage = 175,
                lie = "rough",
                wind = "10mph_headwind"
            )
        )

        verify { navController.navigate(any<String>()) }
        assertEquals(
            "caddy/shot_recommendation?yardage=175&lie=rough&wind=10mph_headwind",
            routeSlot.captured
        )
    }

    @Test
    fun `multiple sequential navigations all succeed`() {
        val routes = mutableListOf<String>()
        every { navController.navigate(capture(routes)) } just Runs

        // Simulate navigation flow: Home -> RoundStart -> LiveCaddy -> RoundEndSummary
        navigator.navigate(NavCaddyDestination.RoundStart())
        navigator.navigate(NavCaddyDestination.LiveCaddy)
        navigator.navigate(NavCaddyDestination.RoundEndSummary(roundId = 1L))

        assertEquals(3, routes.size)
        assertEquals("caddy/round_start", routes[0])
        assertEquals("caddy/live_caddy", routes[1])
        assertEquals("caddy/round_end_summary/1", routes[2])
    }

    // Voice-first Navigation Tests (for Task 24 integration)

    @Test
    fun `voice query routing to LiveCaddy works`() {
        val routeSlot = slot<String>()
        every { navController.navigate(capture(routeSlot)) } just Runs

        // Simulate: "What's the play?" -> routes to LiveCaddy
        navigator.navigate(NavCaddyDestination.LiveCaddy)

        verify { navController.navigate(any<String>()) }
        assertEquals("caddy/live_caddy", routeSlot.captured)
    }

    @Test
    fun `voice query routing to ShotRecommendation within LiveCaddy`() {
        val routeSlot = slot<String>()
        every { navController.navigate(capture(routeSlot)) } just Runs

        // Simulate: "Where should I aim?" -> might navigate to ShotRecommendation
        // or expand strategy map within LiveCaddy
        navigator.navigate(
            NavCaddyDestination.ShotRecommendation(yardage = 150)
        )

        verify { navController.navigate(any<String>()) }
        assertTrue(routeSlot.captured.contains("shot_recommendation"))
        assertTrue(routeSlot.captured.contains("yardage=150"))
    }
}
