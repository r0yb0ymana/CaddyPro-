package caddypro.domain.navcaddy.routing

import caddypro.domain.navcaddy.classifier.ClassificationResult
import caddypro.domain.navcaddy.models.ExtractedEntities
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.Module
import caddypro.domain.navcaddy.models.ParsedIntent
import caddypro.domain.navcaddy.models.RoutingTarget
import caddypro.domain.navcaddy.navigation.DeepLinkBuilder
import caddypro.domain.navcaddy.navigation.NavCaddyNavigator
import caddypro.domain.navcaddy.navigation.NavigationAction
import caddypro.domain.navcaddy.navigation.NavigationExecutor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for routing determinism.
 *
 * Verifies that routing decisions are reproducible from the same inputs:
 * - Same ParsedIntent + context → same RoutingResult (R3, C3)
 * - Same RoutingTarget → same NavCaddyDestination
 * - Same RoutingResult → same NavigationAction
 * - Multiple executions produce identical results
 *
 * This is critical for:
 * - QA build testing and debugging
 * - Consistent user experience
 * - Predictable behavior for automated testing
 *
 * Spec reference: navcaddy-engine.md C3 (Deterministic routing)
 */
class RoutingDeterminismTest {

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

    // ============ Same Intent → Same Routing Result ============

    @Test
    fun `same ParsedIntent produces same RoutingResult on multiple invocations`() = runTest {
        // Given: Identical intent and classification
        val intent = createIntent(IntentType.CLUB_ADJUSTMENT, 0.85f)
        val target = RoutingTarget(Module.CADDY, "club_adjustment", mapOf("clubId" to "7i"))
        val classification = ClassificationResult.Route(intent, target)

        // Bag configured (same prerequisite state)
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns emptyList()

        // When: Route multiple times
        val result1 = orchestrator.route(classification)
        val result2 = orchestrator.route(classification)
        val result3 = orchestrator.route(classification)

        // Then: All results should be identical Navigate results
        assertTrue(result1 is RoutingResult.Navigate)
        assertTrue(result2 is RoutingResult.Navigate)
        assertTrue(result3 is RoutingResult.Navigate)

        val nav1 = result1 as RoutingResult.Navigate
        val nav2 = result2 as RoutingResult.Navigate
        val nav3 = result3 as RoutingResult.Navigate

        assertEquals(nav1.target.module, nav2.target.module)
        assertEquals(nav1.target.module, nav3.target.module)
        assertEquals(nav1.target.screen, nav2.target.screen)
        assertEquals(nav1.target.screen, nav3.target.screen)
        assertEquals(nav1.target.parameters, nav2.target.parameters)
        assertEquals(nav1.target.parameters, nav3.target.parameters)
    }

    @Test
    fun `same prerequisite missing state produces same RoutingResult on multiple invocations`() = runTest {
        // Given: Intent with missing prerequisite
        val intent = createIntent(IntentType.RECOVERY_CHECK, 0.88f)
        val target = RoutingTarget(Module.RECOVERY, "overview", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // Recovery data missing (same prerequisite state)
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.RECOVERY_DATA)) } returns
            listOf(Prerequisite.RECOVERY_DATA)

        // When: Route multiple times
        val result1 = orchestrator.route(classification)
        val result2 = orchestrator.route(classification)
        val result3 = orchestrator.route(classification)

        // Then: All results should be identical PrerequisiteMissing results
        assertTrue(result1 is RoutingResult.PrerequisiteMissing)
        assertTrue(result2 is RoutingResult.PrerequisiteMissing)
        assertTrue(result3 is RoutingResult.PrerequisiteMissing)

        val missing1 = result1 as RoutingResult.PrerequisiteMissing
        val missing2 = result2 as RoutingResult.PrerequisiteMissing
        val missing3 = result3 as RoutingResult.PrerequisiteMissing

        assertEquals(missing1.missing, missing2.missing)
        assertEquals(missing1.missing, missing3.missing)
        assertEquals(missing1.message, missing2.message)
        assertEquals(missing1.message, missing3.message)
    }

    @Test
    fun `same no-navigation intent produces same response on multiple invocations`() = runTest {
        // Given: No-navigation intent
        val intent = createIntent(IntentType.PATTERN_QUERY, 0.90f)
        val target = RoutingTarget(Module.COACH, "patterns", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route multiple times
        val result1 = orchestrator.route(classification)
        val result2 = orchestrator.route(classification)
        val result3 = orchestrator.route(classification)

        // Then: All results should be identical NoNavigation results
        assertTrue(result1 is RoutingResult.NoNavigation)
        assertTrue(result2 is RoutingResult.NoNavigation)
        assertTrue(result3 is RoutingResult.NoNavigation)

        val noNav1 = result1 as RoutingResult.NoNavigation
        val noNav2 = result2 as RoutingResult.NoNavigation
        val noNav3 = result3 as RoutingResult.NoNavigation

        assertEquals(noNav1.response, noNav2.response)
        assertEquals(noNav1.response, noNav3.response)
    }

    @Test
    fun `same confirmation required produces same result on multiple invocations`() = runTest {
        // Given: Mid-confidence classification
        val intent = createIntent(IntentType.WEATHER_CHECK, 0.65f)
        val confirmMessage = "Did you want to check the weather forecast?"
        val classification = ClassificationResult.Confirm(intent, confirmMessage)

        // When: Route multiple times
        val result1 = orchestrator.route(classification)
        val result2 = orchestrator.route(classification)
        val result3 = orchestrator.route(classification)

        // Then: All results should be identical ConfirmationRequired results
        assertTrue(result1 is RoutingResult.ConfirmationRequired)
        assertTrue(result2 is RoutingResult.ConfirmationRequired)
        assertTrue(result3 is RoutingResult.ConfirmationRequired)

        val confirm1 = result1 as RoutingResult.ConfirmationRequired
        val confirm2 = result2 as RoutingResult.ConfirmationRequired
        val confirm3 = result3 as RoutingResult.ConfirmationRequired

        assertEquals(confirm1.message, confirm2.message)
        assertEquals(confirm1.message, confirm3.message)
        assertEquals(confirm1.intent.intentType, confirm2.intent.intentType)
        assertEquals(confirm1.intent.intentType, confirm3.intent.intentType)
    }

    // ============ Same RoutingTarget → Same Destination ============

    @Test
    fun `same RoutingTarget produces identical destination on multiple builds`() {
        // Given: Identical routing target
        val target = RoutingTarget(
            Module.CADDY,
            "club_adjustment",
            mapOf("clubId" to "7i")
        )

        // When: Build destination multiple times
        val destination1 = deepLinkBuilder.buildDestination(target)
        val destination2 = deepLinkBuilder.buildDestination(target)
        val destination3 = deepLinkBuilder.buildDestination(target)

        // Then: All destinations should be identical
        assertEquals(destination1, destination2)
        assertEquals(destination1, destination3)
        assertEquals(destination1.toRoute(), destination2.toRoute())
        assertEquals(destination1.toRoute(), destination3.toRoute())
    }

    @Test
    fun `same RoutingTarget with complex parameters produces identical destination`() {
        // Given: Target with multiple parameters
        val target = RoutingTarget(
            Module.CADDY,
            "shot_recommendation",
            mapOf(
                "yardage" to 150,
                "lie" to "fairway",
                "wind" to "10mph headwind"
            )
        )

        // When: Build destination multiple times
        val destination1 = deepLinkBuilder.buildDestination(target)
        val destination2 = deepLinkBuilder.buildDestination(target)
        val destination3 = deepLinkBuilder.buildDestination(target)

        // Then: All destinations should be identical
        assertEquals(destination1, destination2)
        assertEquals(destination1, destination3)
        assertEquals(destination1.toRoute(), destination2.toRoute())
        assertEquals(destination1.toRoute(), destination3.toRoute())
    }

    @Test
    fun `same RoutingTarget produces identical route string on multiple builds`() {
        // Given: Routing target
        val target = RoutingTarget(
            Module.RECOVERY,
            "overview",
            emptyMap()
        )

        // When: Build route string multiple times
        val route1 = deepLinkBuilder.buildRoute(target)
        val route2 = deepLinkBuilder.buildRoute(target)
        val route3 = deepLinkBuilder.buildRoute(target)

        // Then: All route strings should be identical
        assertEquals(route1, route2)
        assertEquals(route1, route3)
    }

    // ============ Same RoutingResult → Same NavigationAction ============

    @Test
    fun `same Navigate result produces identical NavigationAction on multiple executions`() {
        // Given: Navigate routing result
        val intent = createIntent(IntentType.CLUB_ADJUSTMENT, 0.85f)
        val target = RoutingTarget(Module.CADDY, "club_adjustment", mapOf("clubId" to "7i"))
        val routingResult = RoutingResult.Navigate(target, intent)

        // When: Execute navigation multiple times
        val action1 = executor.execute(routingResult)
        val action2 = executor.execute(routingResult)
        val action3 = executor.execute(routingResult)

        // Then: All actions should be identical Navigated actions
        assertTrue(action1 is NavigationAction.Navigated)
        assertTrue(action2 is NavigationAction.Navigated)
        assertTrue(action3 is NavigationAction.Navigated)

        val nav1 = action1 as NavigationAction.Navigated
        val nav2 = action2 as NavigationAction.Navigated
        val nav3 = action3 as NavigationAction.Navigated

        assertEquals(nav1.route, nav2.route)
        assertEquals(nav1.route, nav3.route)
        assertEquals(nav1.destination, nav2.destination)
        assertEquals(nav1.destination, nav3.destination)
    }

    @Test
    fun `same NoNavigation result produces identical action on multiple executions`() {
        // Given: NoNavigation routing result
        val intent = createIntent(IntentType.PATTERN_QUERY, 0.90f)
        val response = "You've been slicing under pressure lately."
        val routingResult = RoutingResult.NoNavigation(intent, response)

        // When: Execute multiple times
        val action1 = executor.execute(routingResult)
        val action2 = executor.execute(routingResult)
        val action3 = executor.execute(routingResult)

        // Then: All actions should be identical ShowInlineResponse actions
        assertTrue(action1 is NavigationAction.ShowInlineResponse)
        assertTrue(action2 is NavigationAction.ShowInlineResponse)
        assertTrue(action3 is NavigationAction.ShowInlineResponse)

        assertEquals(
            (action1 as NavigationAction.ShowInlineResponse).response,
            (action2 as NavigationAction.ShowInlineResponse).response
        )
        assertEquals(
            (action1 as NavigationAction.ShowInlineResponse).response,
            (action3 as NavigationAction.ShowInlineResponse).response
        )
    }

    @Test
    fun `same PrerequisiteMissing result produces identical action on multiple executions`() {
        // Given: PrerequisiteMissing routing result
        val intent = createIntent(IntentType.RECOVERY_CHECK, 0.87f)
        val message = "I don't have any recovery data yet."
        val routingResult = RoutingResult.PrerequisiteMissing(
            intent,
            listOf(Prerequisite.RECOVERY_DATA),
            message
        )

        // When: Execute multiple times
        val action1 = executor.execute(routingResult)
        val action2 = executor.execute(routingResult)
        val action3 = executor.execute(routingResult)

        // Then: All actions should be identical PromptPrerequisites actions
        assertTrue(action1 is NavigationAction.PromptPrerequisites)
        assertTrue(action2 is NavigationAction.PromptPrerequisites)
        assertTrue(action3 is NavigationAction.PromptPrerequisites)

        val prompt1 = action1 as NavigationAction.PromptPrerequisites
        val prompt2 = action2 as NavigationAction.PromptPrerequisites
        val prompt3 = action3 as NavigationAction.PromptPrerequisites

        assertEquals(prompt1.message, prompt2.message)
        assertEquals(prompt1.message, prompt3.message)
        assertEquals(prompt1.missingPrerequisites, prompt2.missingPrerequisites)
        assertEquals(prompt1.missingPrerequisites, prompt3.missingPrerequisites)
    }

    // ============ Complete Pipeline Determinism ============

    @Test
    fun `complete pipeline produces identical results across 10 invocations`() = runTest {
        // Given: Complete classification and prerequisites
        val intent = createIntent(IntentType.SHOT_RECOMMENDATION, 0.92f)
        val target = RoutingTarget(
            Module.CADDY,
            "shot_recommendation",
            mapOf("yardage" to 150, "lie" to "fairway")
        )
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns emptyList()

        // When: Execute complete pipeline 10 times
        val results = mutableListOf<String>()
        repeat(10) {
            val routingResult = orchestrator.route(classification)
            val navigationAction = executor.execute(routingResult)

            if (navigationAction is NavigationAction.Navigated) {
                results.add(navigationAction.route)
            }
        }

        // Then: All 10 route strings should be identical
        assertEquals(10, results.size)
        assertTrue(results.all { it == results[0] })
        assertEquals("caddy/shot_recommendation?yardage=150&lie=fairway", results[0])
    }

    @Test
    fun `different intents with same structure produce consistent results`() = runTest {
        // Given: Two different intents with similar structure
        val intent1 = createIntent(IntentType.CLUB_ADJUSTMENT, 0.85f)
        val target1 = RoutingTarget(Module.CADDY, "club_adjustment", mapOf("clubId" to "7i"))
        val classification1 = ClassificationResult.Route(intent1, target1)

        val intent2 = createIntent(IntentType.CLUB_ADJUSTMENT, 0.85f)
        val target2 = RoutingTarget(Module.CADDY, "club_adjustment", mapOf("clubId" to "7i"))
        val classification2 = ClassificationResult.Route(intent2, target2)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns emptyList()

        // When: Route both intents multiple times
        val results1 = mutableListOf<String>()
        val results2 = mutableListOf<String>()

        repeat(5) {
            val routing1 = orchestrator.route(classification1)
            val action1 = executor.execute(routing1)
            if (action1 is NavigationAction.Navigated) {
                results1.add(action1.route)
            }

            val routing2 = orchestrator.route(classification2)
            val action2 = executor.execute(routing2)
            if (action2 is NavigationAction.Navigated) {
                results2.add(action2.route)
            }
        }

        // Then: Both should produce identical sequences
        assertEquals(5, results1.size)
        assertEquals(5, results2.size)
        assertEquals(results1, results2)
    }

    @Test
    fun `parameter order does not affect route determinism`() {
        // Given: Two targets with same parameters in different order
        val target1 = RoutingTarget(
            Module.CADDY,
            "shot_recommendation",
            mapOf("yardage" to 150, "lie" to "fairway", "wind" to "10mph")
        )

        val target2 = RoutingTarget(
            Module.CADDY,
            "shot_recommendation",
            mapOf("wind" to "10mph", "lie" to "fairway", "yardage" to 150)
        )

        // When: Build routes multiple times
        val route1a = deepLinkBuilder.buildRoute(target1)
        val route1b = deepLinkBuilder.buildRoute(target1)
        val route2a = deepLinkBuilder.buildRoute(target2)
        val route2b = deepLinkBuilder.buildRoute(target2)

        // Then: Each target should produce consistent routes
        assertEquals(route1a, route1b)
        assertEquals(route2a, route2b)
        // Note: Route strings for target1 and target2 may differ in parameter order,
        // but each is internally consistent
    }

    // ============ Edge Cases ============

    @Test
    fun `empty parameters produce deterministic results`() {
        // Given: Target with empty parameters
        val target = RoutingTarget(Module.CADDY, "weather", emptyMap())

        // When: Build route multiple times
        val route1 = deepLinkBuilder.buildRoute(target)
        val route2 = deepLinkBuilder.buildRoute(target)
        val route3 = deepLinkBuilder.buildRoute(target)

        // Then: All routes should be identical
        assertEquals(route1, route2)
        assertEquals(route1, route3)
        assertEquals("caddy/weather", route1)
    }

    @Test
    fun `special characters in parameters are handled deterministically`() {
        // Given: Target with special characters
        val target = RoutingTarget(
            Module.CADDY,
            "round_start",
            mapOf("courseName" to "St. Andrews (Old Course)")
        )

        // When: Build route multiple times
        val route1 = deepLinkBuilder.buildRoute(target)
        val route2 = deepLinkBuilder.buildRoute(target)
        val route3 = deepLinkBuilder.buildRoute(target)

        // Then: All routes should be identical with proper encoding
        assertEquals(route1, route2)
        assertEquals(route1, route3)
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
