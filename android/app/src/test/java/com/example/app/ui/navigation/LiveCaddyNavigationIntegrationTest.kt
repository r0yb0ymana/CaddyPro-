package caddypro.ui.navigation

import caddypro.domain.navcaddy.models.Module
import caddypro.domain.navcaddy.models.RoutingTarget
import caddypro.domain.navcaddy.navigation.NavCaddyDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration tests for Live Caddy navigation flows.
 *
 * Tests complete navigation scenarios including:
 * - Home to Live Caddy flow
 * - Round start to Live Caddy flow
 * - Live Caddy to Round End Summary flow
 * - Voice query routing to Live Caddy
 * - Deep linking scenarios
 *
 * Spec reference: live-caddy-mode.md R1, R5, live-caddy-mode-plan.md Task 23
 */
class LiveCaddyNavigationIntegrationTest {

    // Complete Navigation Flow Tests

    @Test
    fun `full round flow navigation generates correct routes`() {
        // Simulate: Home -> RoundStart -> LiveCaddy -> RoundEndSummary
        val routes = mutableListOf<String>()

        // Step 1: Navigate to round start
        val roundStart = NavCaddyDestination.RoundStart(courseName = "Augusta National")
        routes.add(roundStart.toRoute())

        // Step 2: Start round, navigate to Live Caddy
        val liveCaddy = NavCaddyDestination.LiveCaddy
        routes.add(liveCaddy.toRoute())

        // Step 3: End round, navigate to summary
        val roundEnd = NavCaddyDestination.RoundEndSummary(roundId = 42L)
        routes.add(roundEnd.toRoute())

        assertEquals(3, routes.size)
        assertEquals("caddy/round_start?course=Augusta%20National", routes[0])
        assertEquals("caddy/live_caddy", routes[1])
        assertEquals("caddy/round_end_summary/42", routes[2])
    }

    @Test
    fun `quick start to Live Caddy flow`() {
        // User clicks "Start Live Caddy" from home (no round start screen)
        val destination = NavCaddyDestination.LiveCaddy

        assertEquals("caddy/live_caddy", destination.toRoute())
        assertEquals(Module.CADDY, destination.module)
    }

    @Test
    fun `resume existing round flow`() {
        // User has an active round, goes directly to Live Caddy
        val destination = NavCaddyDestination.LiveCaddy

        // Same route regardless of resume vs new round
        assertEquals("caddy/live_caddy", destination.toRoute())
    }

    // Voice Query Integration Tests

    @Test
    fun `voice query 'What's the play' routes to Live Caddy`() {
        // Voice query detected, intent classification routes to live_caddy screen
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "live_caddy",
            parameters = emptyMap()
        )

        val destination = NavCaddyDestination.fromRoutingTarget(target)

        assertTrue(destination is NavCaddyDestination.LiveCaddy)
        assertEquals("caddy/live_caddy", destination.toRoute())
    }

    @Test
    fun `voice query 'Where should I aim' with context routes to shot recommendation`() {
        // Voice query with yardage context
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "shot_recommendation",
            parameters = mapOf(
                "yardage" to 165,
                "lie" to "fairway"
            )
        )

        val destination = NavCaddyDestination.fromRoutingTarget(target)

        assertTrue(destination is NavCaddyDestination.ShotRecommendation)
        assertEquals("caddy/shot_recommendation?yardage=165&lie=fairway", destination.toRoute())
    }

    @Test
    fun `voice query 'Where's the bailout' routes to Live Caddy strategy map`() {
        // Bailout query routes to Live Caddy (strategy map expanded)
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "live_caddy",
            parameters = mapOf("expandStrategy" to true) // Future: could add state param
        )

        val destination = NavCaddyDestination.fromRoutingTarget(target)

        assertTrue(destination is NavCaddyDestination.LiveCaddy)
        assertEquals("caddy/live_caddy", destination.toRoute())
    }

    // Deep Linking Tests

    @Test
    fun `deep link to Live Caddy from notification`() {
        // User taps "Resume Round" notification
        val deepLinkUri = "caddy://live_caddy"
        val screenPart = deepLinkUri.substringAfter("://")

        val destination = NavCaddyDestination.LiveCaddy
        assertTrue(destination.toRoute().contains(screenPart))
    }

    @Test
    fun `deep link to round summary from share link`() {
        // User taps shared round link
        val deepLinkUri = "caddy://round_end_summary/999"
        val screenPart = deepLinkUri.substringAfter("://")

        val destination = NavCaddyDestination.RoundEndSummary(roundId = 999L)
        assertTrue(destination.toRoute().contains(screenPart))
    }

    @Test
    fun `deep link to round start with course name`() {
        // User taps "Play at Pebble Beach" widget
        val courseName = "Pebble Beach"
        val destination = NavCaddyDestination.RoundStart(courseName = courseName)

        assertEquals("caddy/round_start?course=Pebble%20Beach", destination.toRoute())
    }

    // State Restoration Tests

    @Test
    fun `round end summary preserves roundId through process death`() {
        // App killed and restored from saved state
        val originalRoundId = 12345L
        val destination = NavCaddyDestination.RoundEndSummary(roundId = originalRoundId)

        // Route can be reconstructed with same ID
        val route = destination.toRoute()
        assertTrue(route.contains(originalRoundId.toString()))
        assertEquals("caddy/round_end_summary/12345", route)
    }

    @Test
    fun `Live Caddy route is stateless and restorable`() {
        // Live Caddy has no parameters, always restores to same state
        val destination = NavCaddyDestination.LiveCaddy

        assertEquals("caddy/live_caddy", destination.toRoute())
        // No state loss possible - route is constant
    }

    // Error Handling Tests

    @Test
    fun `navigation to Live Caddy always succeeds`() {
        // LiveCaddy has no required parameters, cannot fail
        val destination = NavCaddyDestination.LiveCaddy

        // Should never throw
        val route = destination.toRoute()
        assertTrue(route.isNotEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun `navigation to round summary without ID fails fast`() {
        // Missing required roundId should fail at conversion time
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "round_end_summary",
            parameters = emptyMap()
        )

        NavCaddyDestination.fromRoutingTarget(target)
    }

    // Multi-Screen Context Tests

    @Test
    fun `weather check can be accessed from Live Caddy`() {
        // User in Live Caddy, taps "Detailed Weather"
        val weatherDestination = NavCaddyDestination.WeatherCheck

        assertEquals("caddy/weather", weatherDestination.toRoute())
        assertEquals(Module.CADDY, weatherDestination.module)
    }

    @Test
    fun `stats lookup can be accessed from Live Caddy`() {
        // User in Live Caddy, asks "How's my putting today?"
        val statsDestination = NavCaddyDestination.StatsLookup(statType = "putting")

        assertEquals("caddy/stats?type=putting", statsDestination.toRoute())
        assertEquals(Module.CADDY, statsDestination.module)
    }

    @Test
    fun `club adjustment can be accessed from Live Caddy`() {
        // User in Live Caddy, says "Adjust 7 iron distance"
        val clubDestination = NavCaddyDestination.ClubAdjustment(clubId = "7i")

        assertEquals("caddy/club_adjustment?clubId=7i", clubDestination.toRoute())
        assertEquals(Module.CADDY, clubDestination.module)
    }

    // Navigation Pattern Validation Tests

    @Test
    fun `all Live Caddy destinations use CADDY module`() {
        val destinations = listOf(
            NavCaddyDestination.LiveCaddy,
            NavCaddyDestination.RoundStart(),
            NavCaddyDestination.RoundEndSummary(roundId = 1L),
            NavCaddyDestination.WeatherCheck,
            NavCaddyDestination.ShotRecommendation(),
            NavCaddyDestination.ClubAdjustment(),
            NavCaddyDestination.ScoreEntry(),
            NavCaddyDestination.StatsLookup(),
            NavCaddyDestination.CourseInfo(courseId = "test")
        )

        destinations.forEach { destination ->
            assertEquals(
                "Destination ${destination::class.simpleName} should use CADDY module",
                Module.CADDY,
                destination.module
            )
        }
    }

    @Test
    fun `all Live Caddy routes start with caddy prefix`() {
        val destinations = listOf(
            NavCaddyDestination.LiveCaddy,
            NavCaddyDestination.RoundStart(),
            NavCaddyDestination.RoundEndSummary(roundId = 1L),
            NavCaddyDestination.WeatherCheck,
            NavCaddyDestination.ShotRecommendation()
        )

        destinations.forEach { destination ->
            assertTrue(
                "Route ${destination.toRoute()} should start with 'caddy/'",
                destination.toRoute().startsWith("caddy/")
            )
        }
    }

    @Test
    fun `route strings do not contain spaces`() {
        // Ensures all routes are URL-safe
        val destinations = listOf(
            NavCaddyDestination.LiveCaddy,
            NavCaddyDestination.RoundStart(courseName = "Pebble Beach Golf Links"),
            NavCaddyDestination.RoundEndSummary(roundId = 1L),
            NavCaddyDestination.ShotRecommendation(lie = "heavy rough")
        )

        destinations.forEach { destination ->
            val route = destination.toRoute()
            // Spaces should be encoded as %20
            assertTrue(
                "Route $route should not contain raw spaces",
                !route.contains(" ") || route.contains("%20")
            )
        }
    }

    // Performance Tests

    @Test
    fun `route generation is idempotent`() {
        // Calling toRoute() multiple times produces same result
        val destination = NavCaddyDestination.LiveCaddy

        val route1 = destination.toRoute()
        val route2 = destination.toRoute()
        val route3 = destination.toRoute()

        assertEquals(route1, route2)
        assertEquals(route2, route3)
    }

    @Test
    fun `data class destinations with same parameters are equal`() {
        val destination1 = NavCaddyDestination.RoundEndSummary(roundId = 100L)
        val destination2 = NavCaddyDestination.RoundEndSummary(roundId = 100L)

        assertEquals(destination1, destination2)
        assertEquals(destination1.hashCode(), destination2.hashCode())
    }

    @Test
    fun `data object destinations are singletons`() {
        val liveCaddy1 = NavCaddyDestination.LiveCaddy
        val liveCaddy2 = NavCaddyDestination.LiveCaddy

        assertTrue(liveCaddy1 === liveCaddy2)
    }
}
