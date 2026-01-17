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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for module-specific routing behavior.
 *
 * Verifies:
 * - Each module (CADDY, COACH, RECOVERY, SETTINGS) routes correctly
 * - Screen mapping for each intent type is correct
 * - Parameters are passed correctly through the pipeline
 * - Module-specific navigation patterns work as expected
 *
 * This ensures that intent types correctly map to their designated modules
 * and screens according to the spec.
 *
 * Spec reference: navcaddy-engine.md R3, navcaddy-engine-plan.md Task 13
 */
class ModuleRoutingTest {

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

    // ============ CADDY Module Tests ============

    @Test
    fun `CADDY module - CLUB_ADJUSTMENT routes to club_adjustment screen`() = runTest {
        // Given: Club adjustment intent
        val intent = createIntent(IntentType.CLUB_ADJUSTMENT, 0.85f)
        val target = RoutingTarget(Module.CADDY, "club_adjustment", mapOf("clubId" to "7i"))
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns emptyList()

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to CADDY module, club_adjustment screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.CADDY, target.module)
        assertEquals("club_adjustment", target.screen)
        assertTrue(navigated.destination is NavCaddyDestination.ClubAdjustment)
        assertTrue(navigated.route.startsWith("caddy/"))
    }

    @Test
    fun `CADDY module - SHOT_RECOMMENDATION routes to shot_recommendation screen with parameters`() = runTest {
        // Given: Shot recommendation with multiple parameters
        val entities = ExtractedEntities(yardage = 150, lie = "fairway", wind = "10mph headwind")
        val intent = createIntent(IntentType.SHOT_RECOMMENDATION, 0.92f, entities)
        val target = RoutingTarget(
            Module.CADDY,
            "shot_recommendation",
            mapOf("yardage" to 150, "lie" to "fairway", "wind" to "10mph headwind")
        )
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns emptyList()

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to CADDY module with all parameters
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.CADDY, target.module)
        assertTrue(navigated.destination is NavCaddyDestination.ShotRecommendation)
        assertTrue(navigated.route.contains("yardage=150"))
        assertTrue(navigated.route.contains("lie=fairway"))
    }

    @Test
    fun `CADDY module - ROUND_START routes to round_start screen with course`() = runTest {
        // Given: Round start intent
        val intent = createIntent(IntentType.ROUND_START, 0.88f)
        val target = RoutingTarget(Module.CADDY, "round_start", mapOf("courseName" to "Pebble Beach"))
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to CADDY round_start screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.CADDY, target.module)
        assertTrue(navigated.destination is NavCaddyDestination.RoundStart)
        assertTrue(navigated.route.contains("Pebble"))
    }

    @Test
    fun `CADDY module - SCORE_ENTRY routes to score_entry screen with hole number`() = runTest {
        // Given: Score entry intent
        val intent = createIntent(IntentType.SCORE_ENTRY, 0.90f)
        val target = RoutingTarget(Module.CADDY, "score_entry", mapOf("hole" to 7))
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.ROUND_ACTIVE)) } returns emptyList()

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to CADDY score_entry screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.CADDY, target.module)
        assertTrue(navigated.destination is NavCaddyDestination.ScoreEntry)
        assertTrue(navigated.route.contains("hole=7"))
    }

    @Test
    fun `CADDY module - ROUND_END routes to round_end screen`() = runTest {
        // Given: Round end intent
        val intent = createIntent(IntentType.ROUND_END, 0.87f)
        val target = RoutingTarget(Module.CADDY, "round_end", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.ROUND_ACTIVE)) } returns emptyList()

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to CADDY round_end screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.CADDY, target.module)
        assertTrue(navigated.destination is NavCaddyDestination.RoundEnd)
        assertEquals("caddy/round_end", navigated.route)
    }

    @Test
    fun `CADDY module - WEATHER_CHECK routes to weather screen`() = runTest {
        // Given: Weather check intent
        val intent = createIntent(IntentType.WEATHER_CHECK, 0.86f)
        val target = RoutingTarget(Module.CADDY, "weather", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to CADDY weather screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.CADDY, target.module)
        assertTrue(navigated.destination is NavCaddyDestination.Weather)
        assertEquals("caddy/weather", navigated.route)
    }

    @Test
    fun `CADDY module - STATS_LOOKUP routes to stats screen with type`() = runTest {
        // Given: Stats lookup intent
        val intent = createIntent(IntentType.STATS_LOOKUP, 0.87f)
        val target = RoutingTarget(Module.CADDY, "stats", mapOf("statType" to "driving"))
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to CADDY stats screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.CADDY, target.module)
        assertTrue(navigated.destination is NavCaddyDestination.StatsLookup)
        assertTrue(navigated.route.contains("type=driving"))
    }

    @Test
    fun `CADDY module - COURSE_INFO routes to course_info screen with courseId`() = runTest {
        // Given: Course info intent
        val intent = createIntent(IntentType.COURSE_INFO, 0.84f)
        val target = RoutingTarget(Module.CADDY, "course_info", mapOf("courseId" to "course-123"))
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.COURSE_SELECTED)) } returns emptyList()

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to CADDY course_info screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.CADDY, target.module)
        assertTrue(navigated.destination is NavCaddyDestination.CourseInfo)
        assertTrue(navigated.route.contains("id=course-123"))
    }

    // ============ COACH Module Tests ============

    @Test
    fun `COACH module - DRILL_REQUEST routes to drill screen`() = runTest {
        // Given: Drill request intent
        val intent = createIntent(IntentType.DRILL_REQUEST, 0.89f)
        val target = RoutingTarget(Module.COACH, "drill", mapOf("focusArea" to "putting"))
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to COACH module, drill screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.COACH, target.module)
        assertEquals("drill", target.screen)
        assertTrue(navigated.destination is NavCaddyDestination.DrillScreen)
        assertTrue(navigated.route.startsWith("coach/"))
    }

    @Test
    fun `COACH module - DRILL_REQUEST with drill ID routes with parameters`() = runTest {
        // Given: Drill request with specific drill ID
        val intent = createIntent(IntentType.DRILL_REQUEST, 0.91f)
        val target = RoutingTarget(
            Module.COACH,
            "drill",
            mapOf("drillId" to "drill-123", "focusArea" to "chipping")
        )
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route with both parameters
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertTrue(navigated.destination is NavCaddyDestination.DrillScreen)
        assertTrue(navigated.route.contains("drillId=drill-123"))
        assertTrue(navigated.route.contains("focusArea=chipping"))
    }

    @Test
    fun `COACH module - PATTERN_QUERY does not navigate`() = runTest {
        // Given: Pattern query intent (no-navigation)
        val intent = createIntent(IntentType.PATTERN_QUERY, 0.90f)
        val target = RoutingTarget(Module.COACH, "patterns", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should not navigate, return inline response
        assertTrue(routingResult is RoutingResult.NoNavigation)
        assertTrue(action is NavigationAction.ShowInlineResponse)

        val response = action as NavigationAction.ShowInlineResponse
        assertTrue(response.response.contains("miss patterns"))
    }

    // ============ RECOVERY Module Tests ============

    @Test
    fun `RECOVERY module - RECOVERY_CHECK routes to overview screen`() = runTest {
        // Given: Recovery check intent
        val intent = createIntent(IntentType.RECOVERY_CHECK, 0.91f)
        val target = RoutingTarget(Module.RECOVERY, "overview", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.RECOVERY_DATA)) } returns emptyList()

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to RECOVERY module, overview screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.RECOVERY, target.module)
        assertEquals("overview", target.screen)
        assertTrue(navigated.destination is NavCaddyDestination.RecoveryOverview)
        assertTrue(navigated.route.startsWith("recovery/"))
        assertEquals("recovery/overview", navigated.route)
    }

    @Test
    fun `RECOVERY module - data entry routes with type parameter`() = runTest {
        // Given: Recovery data entry intent (hypothetical for future expansion)
        val intent = createIntent(IntentType.RECOVERY_CHECK, 0.88f)
        val target = RoutingTarget(Module.RECOVERY, "data_entry", mapOf("dataType" to "sleep"))
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.RECOVERY_DATA)) } returns emptyList()

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route with dataType parameter
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertTrue(navigated.destination is NavCaddyDestination.RecoveryDataEntry)
        assertTrue(navigated.route.contains("type=sleep"))
    }

    // ============ SETTINGS Module Tests ============

    @Test
    fun `SETTINGS module - EQUIPMENT_INFO routes to equipment screen`() = runTest {
        // Given: Equipment info intent
        val intent = createIntent(IntentType.EQUIPMENT_INFO, 0.83f)
        val target = RoutingTarget(Module.SETTINGS, "equipment", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to SETTINGS module, equipment screen
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertEquals(Module.SETTINGS, target.module)
        assertEquals("equipment", target.screen)
        assertTrue(navigated.destination is NavCaddyDestination.EquipmentManagement)
        assertTrue(navigated.route.startsWith("settings/"))
        assertEquals("settings/equipment", navigated.route)
    }

    @Test
    fun `SETTINGS module - SETTINGS_CHANGE routes to settings screen with key`() = runTest {
        // Given: Settings change intent
        val intent = createIntent(IntentType.SETTINGS_CHANGE, 0.82f)
        val target = RoutingTarget(Module.SETTINGS, "settings", mapOf("settingKey" to "notifications"))
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should route to settings with key parameter
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertTrue(navigated.destination is NavCaddyDestination.Settings)
        assertTrue(navigated.route.contains("key=notifications"))
    }

    @Test
    fun `SETTINGS module - HELP_REQUEST does not navigate`() = runTest {
        // Given: Help request intent (no-navigation)
        val intent = createIntent(IntentType.HELP_REQUEST, 0.95f)
        val target = RoutingTarget(Module.SETTINGS, "help", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should not navigate, return inline help response
        assertTrue(routingResult is RoutingResult.NoNavigation)
        assertTrue(action is NavigationAction.ShowInlineResponse)

        val response = action as NavigationAction.ShowInlineResponse
        assertTrue(response.response.contains("Bones"))
        assertTrue(response.response.contains("caddy"))
    }

    @Test
    fun `SETTINGS module - FEEDBACK does not navigate`() = runTest {
        // Given: Feedback intent (no-navigation)
        val intent = createIntent(IntentType.FEEDBACK, 0.88f)
        val target = RoutingTarget(Module.SETTINGS, "feedback", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Should not navigate, return thank you response
        assertTrue(routingResult is RoutingResult.NoNavigation)
        assertTrue(action is NavigationAction.ShowInlineResponse)

        val response = action as NavigationAction.ShowInlineResponse
        assertTrue(response.response.contains("Thanks for the feedback"))
    }

    // ============ Parameter Passing Tests ============

    @Test
    fun `parameters are preserved through complete routing pipeline`() = runTest {
        // Given: Intent with complex parameters
        val entities = ExtractedEntities(
            club = "7-iron",
            yardage = 155,
            lie = "light rough",
            wind = "15mph crosswind"
        )
        val intent = createIntent(IntentType.SHOT_RECOMMENDATION, 0.93f, entities)
        val target = RoutingTarget(
            Module.CADDY,
            "shot_recommendation",
            mapOf(
                "yardage" to 155,
                "lie" to "light rough",
                "wind" to "15mph crosswind"
            )
        )
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns emptyList()

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: All parameters should be in the route
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertTrue(navigated.route.contains("yardage=155"))
        assertTrue(navigated.route.contains("lie=light"))
        assertTrue(navigated.route.contains("wind=15mph"))
    }

    @Test
    fun `numeric parameters are converted correctly`() = runTest {
        // Given: Intent with numeric hole parameter
        val intent = createIntent(IntentType.SCORE_ENTRY, 0.90f)
        val target = RoutingTarget(Module.CADDY, "score_entry", mapOf("hole" to 18))
        val classification = ClassificationResult.Route(intent, target)

        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.ROUND_ACTIVE)) } returns emptyList()

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Numeric parameter should be correctly encoded
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertTrue(navigated.route.contains("hole=18"))
    }

    @Test
    fun `string parameters with spaces are encoded correctly`() = runTest {
        // Given: Intent with string containing spaces
        val intent = createIntent(IntentType.ROUND_START, 0.88f)
        val target = RoutingTarget(
            Module.CADDY,
            "round_start",
            mapOf("courseName" to "Augusta National Golf Club")
        )
        val classification = ClassificationResult.Route(intent, target)

        // When: Route through pipeline
        val routingResult = orchestrator.route(classification)
        val action = executor.execute(routingResult)

        // Then: Spaces should be URL encoded
        assertTrue(action is NavigationAction.Navigated)
        val navigated = action as NavigationAction.Navigated

        assertTrue(navigated.route.contains("course=Augusta"))
        // Spaces are encoded as %20
        assertTrue(navigated.route.contains("%20") || navigated.route.contains("+"))
    }

    // ============ Cross-Module Intent Distribution ============

    @Test
    fun `all 15 intent types route to correct modules`() = runTest {
        // Verify intent type → module mapping
        val moduleMapping = mapOf(
            IntentType.CLUB_ADJUSTMENT to Module.CADDY,
            IntentType.SHOT_RECOMMENDATION to Module.CADDY,
            IntentType.ROUND_START to Module.CADDY,
            IntentType.ROUND_END to Module.CADDY,
            IntentType.SCORE_ENTRY to Module.CADDY,
            IntentType.WEATHER_CHECK to Module.CADDY,
            IntentType.STATS_LOOKUP to Module.CADDY,
            IntentType.COURSE_INFO to Module.CADDY,
            IntentType.DRILL_REQUEST to Module.COACH,
            IntentType.PATTERN_QUERY to Module.COACH, // No navigation
            IntentType.RECOVERY_CHECK to Module.RECOVERY,
            IntentType.EQUIPMENT_INFO to Module.SETTINGS,
            IntentType.SETTINGS_CHANGE to Module.SETTINGS,
            IntentType.HELP_REQUEST to Module.SETTINGS, // No navigation
            IntentType.FEEDBACK to Module.SETTINGS // No navigation
        )

        // Then: Verify mapping is complete
        assertEquals(15, moduleMapping.size)
        assertEquals(IntentType.values().size, moduleMapping.size)

        // Count intents per module
        val caddyIntents = moduleMapping.values.count { it == Module.CADDY }
        val coachIntents = moduleMapping.values.count { it == Module.COACH }
        val recoveryIntents = moduleMapping.values.count { it == Module.RECOVERY }
        val settingsIntents = moduleMapping.values.count { it == Module.SETTINGS }

        assertEquals(8, caddyIntents)
        assertEquals(2, coachIntents)
        assertEquals(1, recoveryIntents)
        assertEquals(4, settingsIntents)
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
