package caddypro.domain.navcaddy.navigation

import caddypro.domain.navcaddy.models.ExtractedEntities
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.Module
import caddypro.domain.navcaddy.models.ParsedIntent
import caddypro.domain.navcaddy.models.RoutingTarget
import caddypro.domain.navcaddy.routing.Prerequisite
import caddypro.domain.navcaddy.routing.RoutingResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NavigationExecutor.
 *
 * Verifies:
 * - Correct handling of all RoutingResult types
 * - Navigation execution via navigator
 * - Error handling for invalid targets
 * - NavigationAction results
 */
class NavigationExecutorTest {

    private lateinit var deepLinkBuilder: DeepLinkBuilder
    private lateinit var navigator: NavCaddyNavigator
    private lateinit var executor: NavigationExecutor

    @Before
    fun setup() {
        deepLinkBuilder = DeepLinkBuilder()
        navigator = mockk(relaxed = true)
        executor = NavigationExecutor(deepLinkBuilder, navigator)
    }

    // Navigate Result Tests

    @Test
    fun `execute Navigate result calls navigator and returns Navigated action`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "club_adjustment",
            parameters = mapOf("clubId" to "7i")
        )
        val intent = createTestIntent(IntentType.CLUB_ADJUSTMENT)
        val result = RoutingResult.Navigate(target, intent)

        val action = executor.execute(result)

        verify { navigator.navigate(any()) }
        assertTrue(action is NavigationAction.Navigated)
        assertEquals("caddy/club_adjustment?clubId=7i", (action as NavigationAction.Navigated).route)
    }

    @Test
    fun `execute Navigate with shot recommendation navigates correctly`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "shot_recommendation",
            parameters = mapOf(
                "yardage" to 150,
                "lie" to "fairway"
            )
        )
        val intent = createTestIntent(IntentType.SHOT_RECOMMENDATION)
        val result = RoutingResult.Navigate(target, intent)

        val action = executor.execute(result)

        verify { navigator.navigate(any()) }
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.ShotRecommendation)
        assertEquals("caddy/shot_recommendation?yardage=150&lie=fairway", navigated.route)
    }

    @Test
    fun `execute Navigate with recovery overview navigates correctly`() {
        val target = RoutingTarget(
            module = Module.RECOVERY,
            screen = "overview",
            parameters = emptyMap()
        )
        val intent = createTestIntent(IntentType.RECOVERY_CHECK)
        val result = RoutingResult.Navigate(target, intent)

        val action = executor.execute(result)

        verify { navigator.navigate(any()) }
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.RecoveryOverview)
        assertEquals("recovery/overview", navigated.route)
    }

    @Test
    fun `execute Navigate with drill screen navigates correctly`() {
        val target = RoutingTarget(
            module = Module.COACH,
            screen = "drill",
            parameters = mapOf("focusArea" to "putting")
        )
        val intent = createTestIntent(IntentType.DRILL_REQUEST)
        val result = RoutingResult.Navigate(target, intent)

        val action = executor.execute(result)

        verify { navigator.navigate(any()) }
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.DrillScreen)
    }

    @Test
    fun `execute Navigate with invalid target returns NavigationFailed`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "unknown_screen",
            parameters = emptyMap()
        )
        val intent = createTestIntent(IntentType.HELP_REQUEST)
        val result = RoutingResult.Navigate(target, intent)

        val action = executor.execute(result)

        assertTrue(action is NavigationAction.NavigationFailed)
        val failed = action as NavigationAction.NavigationFailed
        assertTrue(failed.error.contains("Invalid navigation target"))
        assertEquals(target, failed.target)
    }

    @Test
    fun `execute Navigate with missing required parameter returns NavigationFailed`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "course_info",
            parameters = emptyMap() // Missing required courseId
        )
        val intent = createTestIntent(IntentType.COURSE_INFO)
        val result = RoutingResult.Navigate(target, intent)

        val action = executor.execute(result)

        assertTrue(action is NavigationAction.NavigationFailed)
        val failed = action as NavigationAction.NavigationFailed
        assertTrue(failed.error.contains("courseId"))
    }

    // NoNavigation Result Tests

    @Test
    fun `execute NoNavigation returns ShowInlineResponse`() {
        val intent = createTestIntent(IntentType.PATTERN_QUERY)
        val response = "You've been slicing under pressure lately. Focus on alignment."
        val result = RoutingResult.NoNavigation(intent, response)

        val action = executor.execute(result)

        assertTrue(action is NavigationAction.ShowInlineResponse)
        assertEquals(response, (action as NavigationAction.ShowInlineResponse).response)
    }

    @Test
    fun `execute NoNavigation for help request returns correct response`() {
        val intent = createTestIntent(IntentType.HELP_REQUEST)
        val response = "I'm Bones, your caddy. Ask me anything!"
        val result = RoutingResult.NoNavigation(intent, response)

        val action = executor.execute(result)

        assertTrue(action is NavigationAction.ShowInlineResponse)
        assertEquals(response, (action as NavigationAction.ShowInlineResponse).response)
    }

    // PrerequisiteMissing Result Tests

    @Test
    fun `execute PrerequisiteMissing returns PromptPrerequisites`() {
        val intent = createTestIntent(IntentType.RECOVERY_CHECK)
        val message = "I don't have any recovery data yet. Log your sleep first."
        val result = RoutingResult.PrerequisiteMissing(
            intent = intent,
            missing = listOf(Prerequisite.RECOVERY_DATA),
            message = message
        )

        val action = executor.execute(result)

        assertTrue(action is NavigationAction.PromptPrerequisites)
        val prompt = action as NavigationAction.PromptPrerequisites
        assertEquals(message, prompt.message)
        assertEquals(listOf("RECOVERY_DATA"), prompt.missingPrerequisites)
    }

    @Test
    fun `execute PrerequisiteMissing with multiple prerequisites lists all`() {
        val intent = createTestIntent(IntentType.SHOT_RECOMMENDATION)
        val message = "Your bag isn't configured and no round is active."
        val result = RoutingResult.PrerequisiteMissing(
            intent = intent,
            missing = listOf(Prerequisite.BAG_CONFIGURED, Prerequisite.ROUND_ACTIVE),
            message = message
        )

        val action = executor.execute(result)

        assertTrue(action is NavigationAction.PromptPrerequisites)
        val prompt = action as NavigationAction.PromptPrerequisites
        assertEquals(2, prompt.missingPrerequisites.size)
        assertTrue(prompt.missingPrerequisites.contains("BAG_CONFIGURED"))
        assertTrue(prompt.missingPrerequisites.contains("ROUND_ACTIVE"))
    }

    // ConfirmationRequired Result Tests

    @Test
    fun `execute ConfirmationRequired returns RequestConfirmation`() {
        val intent = createTestIntent(IntentType.CLUB_ADJUSTMENT)
        val message = "Did you mean to adjust your 7-iron distance?"
        val result = RoutingResult.ConfirmationRequired(intent, message)

        val action = executor.execute(result)

        assertTrue(action is NavigationAction.RequestConfirmation)
        val confirmation = action as NavigationAction.RequestConfirmation
        assertEquals(message, confirmation.message)
        assertEquals(intent, confirmation.intent)
    }

    // Edge Cases

    @Test
    fun `execute handles settings navigation correctly`() {
        val target = RoutingTarget(
            module = Module.SETTINGS,
            screen = "equipment",
            parameters = emptyMap()
        )
        val intent = createTestIntent(IntentType.EQUIPMENT_INFO)
        val result = RoutingResult.Navigate(target, intent)

        val action = executor.execute(result)

        verify { navigator.navigate(any()) }
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.EquipmentManagement)
        assertEquals("settings/equipment", navigated.route)
    }

    @Test
    fun `execute handles round start with course name`() {
        val target = RoutingTarget(
            module = Module.CADDY,
            screen = "round_start",
            parameters = mapOf("courseName" to "St Andrews")
        )
        val intent = createTestIntent(IntentType.ROUND_START)
        val result = RoutingResult.Navigate(target, intent)

        val action = executor.execute(result)

        verify { navigator.navigate(any()) }
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.RoundStart)
        assertEquals("caddy/round_start?course=St%20Andrews", navigated.route)
    }

    // Helper Functions

    private fun createTestIntent(
        intentType: IntentType,
        confidence: Float = 0.9f
    ): ParsedIntent {
        return ParsedIntent(
            intentId = "test_${System.currentTimeMillis()}",
            intentType = intentType,
            confidence = confidence,
            entities = ExtractedEntities(),
            userGoal = "Test user goal",
            routingTarget = null
        )
    }
}
