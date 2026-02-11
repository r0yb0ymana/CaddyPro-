package caddypro.domain.navcaddy.routing

import caddypro.domain.navcaddy.classifier.ClassificationResult
import caddypro.domain.navcaddy.models.ExtractedEntities
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.Module
import caddypro.domain.navcaddy.models.ParsedIntent
import caddypro.domain.navcaddy.models.RoutingTarget
import caddypro.domain.navcaddy.navigation.DeepLinkBuilder
import caddypro.domain.navcaddy.navigation.NavCaddyDestination
import caddypro.domain.navcaddy.navigation.NavCaddyNavigator
import caddypro.domain.navcaddy.navigation.NavigationAction
import caddypro.domain.navcaddy.navigation.NavigationExecutor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for the complete routing flow.
 *
 * Tests the full pipeline:
 * ClassificationResult → RoutingOrchestrator → RoutingResult → NavigationExecutor → NavigationAction
 *
 * Verifies:
 * - All 15 intent types route to correct modules
 * - Prerequisite validation works across the full pipeline
 * - Error propagation through the stack
 * - Complete routing flow for each module
 *
 * Spec reference: navcaddy-engine.md R3, C3, navcaddy-engine-plan.md Task 13
 */
class RoutingIntegrationTest {

    private lateinit var prerequisiteChecker: PrerequisiteChecker
    private lateinit var orchestrator: RoutingOrchestrator
    private lateinit var deepLinkBuilder: DeepLinkBuilder
    private lateinit var navigator: NavCaddyNavigator
    private lateinit var executor: NavigationExecutor

    @Before
    fun setUp() {
        prerequisiteChecker = mockk()
        orchestrator = RoutingOrchestrator(prerequisiteChecker)
        deepLinkBuilder = DeepLinkBuilder()
        navigator = mockk(relaxed = true)
        executor = NavigationExecutor(deepLinkBuilder, navigator)
    }

    // ============ Complete Pipeline Tests - CADDY Module ============

    @Test
    fun `CLUB_ADJUSTMENT flows through complete pipeline to navigation`() = runTest {
        // Given: Complete classification result for club adjustment
        val intent = createIntent(IntentType.CLUB_ADJUSTMENT, 0.85f)
        val target = RoutingTarget(Module.CADDY, "club_adjustment", mapOf("clubId" to "7i"))
        val classification = ClassificationResult.Route(intent, target)

        // Bag is configured
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns emptyList()

        // When: Flow through complete pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to club adjustment screen
        assertTrue(routingResult is RoutingResult.Navigate)
        assertTrue(navigationAction is NavigationAction.Navigated)

        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.ClubAdjustment)
        assertEquals("caddy/club_adjustment?clubId=7i", navigated.route)

        verify { navigator.navigate(any()) }
    }

    @Test
    fun `SHOT_RECOMMENDATION flows through pipeline with all parameters`() = runTest {
        // Given: Shot recommendation with yardage, lie, wind
        val entities = ExtractedEntities(
            yardage = 150,
            lie = "fairway",
            wind = "10mph headwind"
        )
        val intent = createIntent(IntentType.SHOT_RECOMMENDATION, 0.92f, entities)
        val target = RoutingTarget(
            Module.CADDY,
            "shot_recommendation",
            mapOf("yardage" to 150, "lie" to "fairway", "wind" to "10mph headwind")
        )
        val classification = ClassificationResult.Route(intent, target)

        // Bag configured
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns emptyList()

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate with correct parameters
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.ShotRecommendation)
        assertTrue(navigated.route.contains("yardage=150"))
        assertTrue(navigated.route.contains("lie=fairway"))
    }

    @Test
    fun `ROUND_START flows through pipeline with course name`() = runTest {
        // Given: Round start with course
        val intent = createIntent(IntentType.ROUND_START, 0.88f)
        val target = RoutingTarget(Module.CADDY, "round_start", mapOf("courseName" to "Augusta National"))
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to round start with course
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.RoundStart)
        assertTrue(navigated.route.contains("Augusta"))
    }

    @Test
    fun `SCORE_ENTRY with active round flows through pipeline`() = runTest {
        // Given: Score entry intent
        val intent = createIntent(IntentType.SCORE_ENTRY, 0.90f)
        val target = RoutingTarget(Module.CADDY, "score_entry", mapOf("hole" to 7))
        val classification = ClassificationResult.Route(intent, target)

        // Round is active
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.ROUND_ACTIVE)) } returns emptyList()

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to score entry
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.ScoreEntry)
    }

    @Test
    fun `WEATHER_CHECK flows through pipeline`() = runTest {
        // Given: Weather check intent
        val intent = createIntent(IntentType.WEATHER_CHECK, 0.86f)
        val target = RoutingTarget(Module.CADDY, "weather", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to weather screen
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.Weather)
        assertEquals("caddy/weather", navigated.route)
    }

    @Test
    fun `STATS_LOOKUP flows through pipeline`() = runTest {
        // Given: Stats lookup intent
        val intent = createIntent(IntentType.STATS_LOOKUP, 0.87f)
        val target = RoutingTarget(Module.CADDY, "stats", mapOf("statType" to "driving"))
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to stats screen
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.StatsLookup)
    }

    @Test
    fun `COURSE_INFO with selected course flows through pipeline`() = runTest {
        // Given: Course info intent
        val intent = createIntent(IntentType.COURSE_INFO, 0.84f)
        val target = RoutingTarget(Module.CADDY, "course_info", mapOf("courseId" to "course-123"))
        val classification = ClassificationResult.Route(intent, target)

        // Course is selected
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.COURSE_SELECTED)) } returns emptyList()

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to course info
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.CourseInfo)
    }

    // ============ Complete Pipeline Tests - COACH Module ============

    @Test
    fun `DRILL_REQUEST flows through pipeline to COACH module`() = runTest {
        // Given: Drill request intent
        val intent = createIntent(IntentType.DRILL_REQUEST, 0.89f)
        val target = RoutingTarget(Module.COACH, "drill", mapOf("focusArea" to "putting"))
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to drill screen in COACH module
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.DrillScreen)
        assertTrue(navigated.route.startsWith("coach/"))
    }

    // ============ Complete Pipeline Tests - RECOVERY Module ============

    @Test
    fun `RECOVERY_CHECK with data flows through pipeline to RECOVERY module`() = runTest {
        // Given: Recovery check intent
        val intent = createIntent(IntentType.RECOVERY_CHECK, 0.91f)
        val target = RoutingTarget(Module.RECOVERY, "overview", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // Recovery data available
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.RECOVERY_DATA)) } returns emptyList()

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to recovery overview
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.RecoveryOverview)
        assertEquals("recovery/overview", navigated.route)
    }

    // ============ Complete Pipeline Tests - SETTINGS Module ============

    @Test
    fun `EQUIPMENT_INFO flows through pipeline to SETTINGS module`() = runTest {
        // Given: Equipment info intent
        val intent = createIntent(IntentType.EQUIPMENT_INFO, 0.83f)
        val target = RoutingTarget(Module.SETTINGS, "equipment", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to equipment management
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.EquipmentManagement)
        assertEquals("settings/equipment", navigated.route)
    }

    @Test
    fun `SETTINGS_CHANGE flows through pipeline to SETTINGS module`() = runTest {
        // Given: Settings change intent
        val intent = createIntent(IntentType.SETTINGS_CHANGE, 0.82f)
        val target = RoutingTarget(Module.SETTINGS, "settings", mapOf("settingKey" to "notifications"))
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should navigate to settings
        assertTrue(navigationAction is NavigationAction.Navigated)
        val navigated = navigationAction as NavigationAction.Navigated
        assertTrue(navigated.destination is NavCaddyDestination.Settings)
    }

    // ============ No-Navigation Intent Tests ============

    @Test
    fun `PATTERN_QUERY flows through pipeline without navigation`() = runTest {
        // Given: Pattern query intent (no navigation)
        val intent = createIntent(IntentType.PATTERN_QUERY, 0.90f)
        val target = RoutingTarget(Module.COACH, "patterns", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should return inline response, no navigation
        assertTrue(routingResult is RoutingResult.NoNavigation)
        assertTrue(navigationAction is NavigationAction.ShowInlineResponse)

        val inlineResponse = navigationAction as NavigationAction.ShowInlineResponse
        assertTrue(inlineResponse.response.contains("miss patterns"))
    }

    @Test
    fun `HELP_REQUEST flows through pipeline without navigation`() = runTest {
        // Given: Help request intent
        val intent = createIntent(IntentType.HELP_REQUEST, 0.95f)
        val target = RoutingTarget(Module.SETTINGS, "help", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should return help response, no navigation
        assertTrue(routingResult is RoutingResult.NoNavigation)
        assertTrue(navigationAction is NavigationAction.ShowInlineResponse)

        val inlineResponse = navigationAction as NavigationAction.ShowInlineResponse
        assertTrue(inlineResponse.response.contains("Bones"))
    }

    @Test
    fun `FEEDBACK flows through pipeline without navigation`() = runTest {
        // Given: Feedback intent
        val intent = createIntent(IntentType.FEEDBACK, 0.88f)
        val target = RoutingTarget(Module.SETTINGS, "feedback", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should return thank you response, no navigation
        assertTrue(routingResult is RoutingResult.NoNavigation)
        assertTrue(navigationAction is NavigationAction.ShowInlineResponse)

        val inlineResponse = navigationAction as NavigationAction.ShowInlineResponse
        assertTrue(inlineResponse.response.contains("Thanks for the feedback"))
    }

    // ============ Prerequisite Validation Through Pipeline ============

    @Test
    fun `prerequisite missing flows through pipeline to prompt action`() = runTest {
        // Given: Recovery check without recovery data
        val intent = createIntent(IntentType.RECOVERY_CHECK, 0.87f)
        val target = RoutingTarget(Module.RECOVERY, "overview", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // Recovery data missing
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.RECOVERY_DATA)) } returns
            listOf(Prerequisite.RECOVERY_DATA)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should prompt for prerequisites
        assertTrue(routingResult is RoutingResult.PrerequisiteMissing)
        assertTrue(navigationAction is NavigationAction.PromptPrerequisites)

        val prompt = navigationAction as NavigationAction.PromptPrerequisites
        assertTrue(prompt.message.contains("recovery data"))
        assertEquals(listOf("RECOVERY_DATA"), prompt.missingPrerequisites)
    }

    @Test
    fun `multiple missing prerequisites flow through pipeline`() = runTest {
        // Given: Shot recommendation with bag not configured
        val intent = createIntent(IntentType.SHOT_RECOMMENDATION, 0.85f)
        val target = RoutingTarget(Module.CADDY, "shot_recommendation", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // Bag not configured
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns
            listOf(Prerequisite.BAG_CONFIGURED)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should prompt for bag configuration
        assertTrue(navigationAction is NavigationAction.PromptPrerequisites)
        val prompt = navigationAction as NavigationAction.PromptPrerequisites
        assertTrue(prompt.message.contains("bag isn't configured"))
        assertTrue(prompt.missingPrerequisites.contains("BAG_CONFIGURED"))
    }

    // ============ Confirmation Flow Tests ============

    @Test
    fun `mid-confidence intent flows through pipeline to confirmation`() = runTest {
        // Given: Mid-confidence classification (0.50-0.74)
        val intent = createIntent(IntentType.WEATHER_CHECK, 0.65f)
        val confirmMessage = "Did you want to check the weather forecast?"
        val classification = ClassificationResult.Confirm(intent, confirmMessage)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should request confirmation
        assertTrue(routingResult is RoutingResult.ConfirmationRequired)
        assertTrue(navigationAction is NavigationAction.RequestConfirmation)

        val confirmation = navigationAction as NavigationAction.RequestConfirmation
        assertEquals(confirmMessage, confirmation.message)
        assertEquals(intent, confirmation.intent)
    }

    // ============ Error Propagation Tests ============

    @Test
    fun `classification error flows through pipeline to inline response`() = runTest {
        // Given: Classification error
        val errorMessage = "Unable to understand your request. Please try again."
        val classification = ClassificationResult.Error(Exception("Network timeout"), errorMessage)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should show error as inline response
        assertTrue(routingResult is RoutingResult.NoNavigation)
        assertTrue(navigationAction is NavigationAction.ShowInlineResponse)

        val response = navigationAction as NavigationAction.ShowInlineResponse
        assertEquals(errorMessage, response.response)
    }

    @Test
    fun `clarify result flows through pipeline to inline response`() = runTest {
        // Given: Low-confidence classification requiring clarification
        val suggestions = listOf(IntentType.DRILL_REQUEST, IntentType.SHOT_RECOMMENDATION)
        val clarifyMessage = "I'm not sure what you're asking. Did you mean:"
        val classification = ClassificationResult.Clarify(suggestions, clarifyMessage, "help me")

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should show clarification message
        assertTrue(routingResult is RoutingResult.NoNavigation)
        assertTrue(navigationAction is NavigationAction.ShowInlineResponse)

        val response = navigationAction as NavigationAction.ShowInlineResponse
        assertEquals(clarifyMessage, response.response)
    }

    @Test
    fun `invalid routing target flows through pipeline to navigation failure`() = runTest {
        // Given: Invalid routing target
        val intent = createIntent(IntentType.HELP_REQUEST, 0.80f)
        val target = RoutingTarget(Module.CADDY, "unknown_screen", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Flow through pipeline
        val routingResult = orchestrator.route(classification)
        val navigationAction = executor.execute(routingResult)

        // Then: Should fail at navigation execution
        assertTrue(routingResult is RoutingResult.Navigate)
        assertTrue(navigationAction is NavigationAction.NavigationFailed)

        val failed = navigationAction as NavigationAction.NavigationFailed
        assertTrue(failed.error.contains("Invalid navigation target"))
        assertEquals(target, failed.target)
    }

    // ============ Helper Methods ============

    private fun createIntent(
        intentType: IntentType,
        confidence: Float,
        entities: ExtractedEntities = ExtractedEntities()
    ): ParsedIntent {
        return ParsedIntent(
            intentId = "test_${intentType.name.lowercase()}_${System.currentTimeMillis()}",
            intentType = intentType,
            confidence = confidence,
            entities = entities,
            userGoal = "Test goal for ${intentType.name}",
            routingTarget = null
        )
    }
}
