package caddypro.domain.navcaddy.navigation

import caddypro.domain.navcaddy.models.Module
import caddypro.domain.navcaddy.models.RoutingTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.Assert.fail

/**
 * Unit tests for DeepLinkBuilder.
 *
 * Verifies:
 * - Correct destination building from RoutingTarget
 * - Parameter extraction and type conversion
 * - Validation and error handling
 * - Route string generation
 */
class DeepLinkBuilderTest {

    private lateinit var builder: DeepLinkBuilder

    @Before
    fun setup() {
        builder = DeepLinkBuilder()
    }

    // CADDY Module Tests

    @Test
    fun `buildRoute creates correct route for club adjustment without clubId`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "club_adjustment",
            parameters = emptyMap()
        )

        val route = builder.buildRoute(target)

        assertEquals("caddy/club_adjustment", route)
    }

    @Test
    fun `buildRoute creates correct route for club adjustment with clubId`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "club_adjustment",
            parameters = mapOf("clubId" to "7i")
        )

        val route = builder.buildRoute(target)

        assertEquals("caddy/club_adjustment?clubId=7i", route)
    }

    @Test
    fun `buildDestination creates ClubAdjustment with correct parameters`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "club_adjustment",
            parameters = mapOf("clubId" to "PW")
        )

        val destination = builder.buildDestination(target)

        assertTrue(destination is NavCaddyDestination.ClubAdjustment)
        assertEquals("PW", (destination as NavCaddyDestination.ClubAdjustment).clubId)
    }

    @Test
    fun `buildRoute creates correct route for shot recommendation with all parameters`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "shot_recommendation",
            parameters = mapOf(
                "yardage" to 150,
                "lie" to "fairway",
                "wind" to "10mph"
            )
        )

        val route = builder.buildRoute(target)

        assertEquals("caddy/shot_recommendation?yardage=150&lie=fairway&wind=10mph", route)
    }

    @Test
    fun `buildDestination converts Number types to Int for yardage`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "shot_recommendation",
            parameters = mapOf("yardage" to 175.0) // Double instead of Int
        )

        val destination = builder.buildDestination(target)

        assertTrue(destination is NavCaddyDestination.ShotRecommendation)
        assertEquals(175, (destination as NavCaddyDestination.ShotRecommendation).yardage)
    }

    @Test
    fun `buildRoute creates correct route for round start with course name`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "round_start",
            parameters = mapOf("courseName" to "Augusta National")
        )

        val route = builder.buildRoute(target)

        assertEquals("caddy/round_start?course=Augusta%20National", route)
    }

    @Test
    fun `buildRoute creates correct route for score entry with hole`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "score_entry",
            parameters = mapOf("hole" to 7)
        )

        val route = builder.buildRoute(target)

        assertEquals("caddy/score_entry?hole=7", route)
    }

    @Test
    fun `buildRoute creates correct route for round end`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "round_end",
            parameters = emptyMap()
        )

        val route = builder.buildRoute(target)

        assertEquals("caddy/round_end", route)
    }

    @Test
    fun `buildRoute creates correct route for weather check`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "weather",
            parameters = emptyMap()
        )

        val route = builder.buildRoute(target)

        assertEquals("caddy/weather", route)
    }

    @Test
    fun `buildRoute creates correct route for stats lookup with type`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "stats",
            parameters = mapOf("statType" to "driving")
        )

        val route = builder.buildRoute(target)

        assertEquals("caddy/stats?type=driving", route)
    }

    @Test
    fun `buildRoute creates correct route for course info with required courseId`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "course_info",
            parameters = mapOf("courseId" to "course-123")
        )

        val route = builder.buildRoute(target)

        assertEquals("caddy/course_info?id=course-123", route)
    }

    @Test
    fun `buildRoute throws when course info missing required courseId`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "course_info",
            parameters = emptyMap()
        )

        try {
            builder.buildRoute(target)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("courseId"))
        }
    }

    // COACH Module Tests

    @Test
    fun `buildRoute creates correct route for drill with both parameters`() {
        val target = RoutingTarget(
            module = Module.COACH,
            screen = "drill",
            parameters = mapOf(
                "drillId" to "drill-123",
                "focusArea" to "putting"
            )
        )

        val route = builder.buildRoute(target)

        assertEquals("coach/drill?drillId=drill-123&focusArea=putting", route)
    }

    @Test
    fun `buildRoute creates correct route for practice session`() {
        val target = RoutingTarget(
            module = Module.COACH,
            screen = "practice",
            parameters = emptyMap()
        )

        val route = builder.buildRoute(target)

        assertEquals("coach/practice", route)
    }

    // RECOVERY Module Tests

    @Test
    fun `buildRoute creates correct route for recovery overview`() {
        val target = RoutingTarget(
            module = Module.RECOVERY,
            screen = "overview",
            parameters = emptyMap()
        )

        val route = builder.buildRoute(target)

        assertEquals("recovery/overview", route)
    }

    @Test
    fun `buildRoute creates correct route for recovery data entry with type`() {
        val target = RoutingTarget(
            module = Module.RECOVERY,
            screen = "data_entry",
            parameters = mapOf("dataType" to "sleep")
        )

        val route = builder.buildRoute(target)

        assertEquals("recovery/data_entry?type=sleep", route)
    }

    // SETTINGS Module Tests

    @Test
    fun `buildRoute creates correct route for equipment management`() {
        val target = RoutingTarget(
            module = Module.SETTINGS,
            screen = "equipment",
            parameters = emptyMap()
        )

        val route = builder.buildRoute(target)

        assertEquals("settings/equipment", route)
    }

    @Test
    fun `buildRoute creates correct route for settings with key`() {
        val target = RoutingTarget(
            module = Module.SETTINGS,
            screen = "settings",
            parameters = mapOf("settingKey" to "notifications")
        )

        val route = builder.buildRoute(target)

        assertEquals("settings/settings?key=notifications", route)
    }

    @Test
    fun `buildRoute creates correct route for help screen`() {
        val target = RoutingTarget(
            module = Module.SETTINGS,
            screen = "help",
            parameters = emptyMap()
        )

        val route = builder.buildRoute(target)

        assertEquals("settings/help", route)
    }

    // Validation Tests

    @Test
    fun `buildRoute throws for unknown CADDY screen`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "unknown_screen",
            parameters = emptyMap()
        )

        try {
            builder.buildRoute(target)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unknown CADDY screen"))
        }
    }

    @Test
    fun `buildRoute throws for unknown COACH screen`() {
        val target = RoutingTarget(
            module = Module.COACH,
            screen = "invalid",
            parameters = emptyMap()
        )

        try {
            builder.buildRoute(target)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unknown COACH screen"))
        }
    }

    @Test
    fun `buildRoute throws for unknown RECOVERY screen`() {
        val target = RoutingTarget(
            module = Module.RECOVERY,
            screen = "invalid",
            parameters = emptyMap()
        )

        try {
            builder.buildRoute(target)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unknown RECOVERY screen"))
        }
    }

    @Test
    fun `buildRoute throws for unknown SETTINGS screen`() {
        val target = RoutingTarget(
            module = Module.SETTINGS,
            screen = "invalid",
            parameters = emptyMap()
        )

        try {
            builder.buildRoute(target)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unknown SETTINGS screen"))
        }
    }

    @Test
    fun `buildRoute throws for blank screen name`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "",
            parameters = emptyMap()
        )

        try {
            builder.buildRoute(target)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("non-blank screen name"))
        }
    }

    // Companion Function Tests

    @Test
    fun `canBuild returns true for valid target`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "club_adjustment",
            parameters = mapOf("clubId" to "7i")
        )

        assertTrue(DeepLinkBuilder.canBuild(target))
    }

    @Test
    fun `canBuild returns false for invalid target`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "unknown_screen",
            parameters = emptyMap()
        )

        assertFalse(DeepLinkBuilder.canBuild(target))
    }

    @Test
    fun `canBuild returns false for missing required parameter`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "course_info",
            parameters = emptyMap() // Missing required courseId
        )

        assertFalse(DeepLinkBuilder.canBuild(target))
    }
}
