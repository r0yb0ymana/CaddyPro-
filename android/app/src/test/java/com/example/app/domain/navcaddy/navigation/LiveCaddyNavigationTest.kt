package caddypro.domain.navcaddy.navigation

import caddypro.domain.navcaddy.models.Module
import caddypro.domain.navcaddy.models.RoutingTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Live Caddy navigation destinations.
 *
 * Verifies:
 * - LiveCaddy route generation
 * - RoundEndSummary route generation with parameters
 * - Route patterns for navigation graph
 * - RoutingTarget conversion for Live Caddy destinations
 *
 * Spec reference: live-caddy-mode.md R1, R5, live-caddy-mode-plan.md Task 23
 */
class LiveCaddyNavigationTest {

    // LiveCaddy Destination Tests

    @Test
    fun `LiveCaddy generates correct route`() {
        val destination = NavCaddyDestination.LiveCaddy

        assertEquals("caddy/live_caddy", destination.toRoute())
        assertEquals(Module.CADDY, destination.module)
        assertEquals("live_caddy", destination.screen)
    }

    @Test
    fun `LiveCaddy is a data object singleton`() {
        val destination1 = NavCaddyDestination.LiveCaddy
        val destination2 = NavCaddyDestination.LiveCaddy

        assertTrue(destination1 === destination2)
    }

    // RoundEndSummary Destination Tests

    @Test
    fun `RoundEndSummary generates route with roundId`() {
        val destination = NavCaddyDestination.RoundEndSummary(roundId = 123L)

        assertEquals("caddy/round_end_summary/123", destination.toRoute())
        assertEquals(Module.CADDY, destination.module)
        assertEquals("round_end_summary", destination.screen)
        assertEquals(123L, destination.roundId)
    }

    @Test
    fun `RoundEndSummary with different IDs generate different routes`() {
        val destination1 = NavCaddyDestination.RoundEndSummary(roundId = 1L)
        val destination2 = NavCaddyDestination.RoundEndSummary(roundId = 999L)

        assertEquals("caddy/round_end_summary/1", destination1.toRoute())
        assertEquals("caddy/round_end_summary/999", destination2.toRoute())
    }

    @Test
    fun `RoundEndSummary ROUTE_PATTERN is correct for NavHost`() {
        assertEquals(
            "caddy/round_end_summary/{roundId}",
            NavCaddyDestination.RoundEndSummary.ROUTE_PATTERN
        )
    }

    @Test
    fun `RoundEndSummary ARG_ROUND_ID constant matches parameter name`() {
        assertEquals("roundId", NavCaddyDestination.RoundEndSummary.ARG_ROUND_ID)
    }

    // RoutingTarget Conversion Tests

    @Test
    fun `fromRoutingTarget converts live_caddy screen correctly`() {
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
    fun `fromRoutingTarget converts round_end_summary with roundId`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "round_end_summary",
            parameters = mapOf("roundId" to 456L)
        )

        val destination = NavCaddyDestination.fromRoutingTarget(target)

        assertTrue(destination is NavCaddyDestination.RoundEndSummary)
        assertEquals("caddy/round_end_summary/456", destination.toRoute())
        assertEquals(456L, (destination as NavCaddyDestination.RoundEndSummary).roundId)
    }

    @Test
    fun `fromRoutingTarget converts round_end_summary with Integer roundId`() {
        // Test that Number types are properly converted to Long
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "round_end_summary",
            parameters = mapOf("roundId" to 789)
        )

        val destination = NavCaddyDestination.fromRoutingTarget(target)

        assertTrue(destination is NavCaddyDestination.RoundEndSummary)
        assertEquals(789L, (destination as NavCaddyDestination.RoundEndSummary).roundId)
    }

    @Test(expected = IllegalStateException::class)
    fun `fromRoutingTarget throws when round_end_summary missing roundId`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "round_end_summary",
            parameters = emptyMap()
        )

        NavCaddyDestination.fromRoutingTarget(target)
    }

    @Test(expected = IllegalStateException::class)
    fun `fromRoutingTarget throws when round_end_summary has null roundId`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "round_end_summary",
            parameters = mapOf("roundId" to null)
        )

        NavCaddyDestination.fromRoutingTarget(target)
    }

    // Integration with Existing Routes Tests

    @Test
    fun `RoundStart route generation still works`() {
        val destination = NavCaddyDestination.RoundStart(courseName = "Pebble Beach")

        assertEquals("caddy/round_start?course=Pebble%20Beach", destination.toRoute())
    }

    @Test
    fun `RoundStart without course generates base route`() {
        val destination = NavCaddyDestination.RoundStart()

        assertEquals("caddy/round_start", destination.toRoute())
    }

    @Test
    fun `LiveCaddy and RoundStart have different routes`() {
        val liveCaddy = NavCaddyDestination.LiveCaddy
        val roundStart = NavCaddyDestination.RoundStart()

        assertEquals("caddy/live_caddy", liveCaddy.toRoute())
        assertEquals("caddy/round_start", roundStart.toRoute())
        assertTrue(liveCaddy.toRoute() != roundStart.toRoute())
    }

    // Voice-first Integration Tests (for Task 24)

    @Test
    fun `LiveCaddy destination can be created from voice query routing`() {
        // Simulate voice query "What's the play?" routing to Live Caddy
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "live_caddy",
            parameters = emptyMap()
        )

        val destination = NavCaddyDestination.fromRoutingTarget(target)

        assertTrue(destination is NavCaddyDestination.LiveCaddy)
    }

    @Test
    fun `ShotRecommendation can route to strategy details within Live Caddy`() {
        // Voice query "Where should I aim?" could expand strategy map
        val destination = NavCaddyDestination.ShotRecommendation(
            yardage = 150,
            lie = "fairway"
        )

        assertEquals("caddy/shot_recommendation?yardage=150&lie=fairway", destination.toRoute())
        assertEquals(Module.CADDY, destination.module)
    }

    // Deprecated RoundEnd Tests

    @Test
    fun `RoundEnd still generates correct route for backwards compatibility`() {
        @Suppress("DEPRECATION")
        val destination = NavCaddyDestination.RoundEnd

        assertEquals("caddy/round_end", destination.toRoute())
    }

    @Test
    fun `RoundEnd and RoundEndSummary have different routes`() {
        @Suppress("DEPRECATION")
        val roundEnd = NavCaddyDestination.RoundEnd
        val roundEndSummary = NavCaddyDestination.RoundEndSummary(roundId = 1L)

        assertEquals("caddy/round_end", roundEnd.toRoute())
        assertEquals("caddy/round_end_summary/1", roundEndSummary.toRoute())
        assertTrue(roundEnd.toRoute() != roundEndSummary.toRoute())
    }
}
