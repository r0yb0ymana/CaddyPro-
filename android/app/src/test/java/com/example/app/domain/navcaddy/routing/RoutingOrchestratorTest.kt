package caddypro.domain.navcaddy.routing

import caddypro.domain.navcaddy.classifier.ClassificationResult
import caddypro.domain.navcaddy.models.ExtractedEntities
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.Module
import caddypro.domain.navcaddy.models.ParsedIntent
import caddypro.domain.navcaddy.models.RoutingTarget
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RoutingOrchestrator.
 *
 * Verifies:
 * - High-confidence intents route correctly
 * - Prerequisites are validated
 * - No-navigation intents handled correctly
 * - Confirmation flow works
 *
 * Spec reference: navcaddy-engine.md R3, navcaddy-engine-plan.md Task 10
 */
class RoutingOrchestratorTest {

    private lateinit var prerequisiteChecker: PrerequisiteChecker
    private lateinit var orchestrator: RoutingOrchestrator

    @Before
    fun setUp() {
        prerequisiteChecker = mockk()
        orchestrator = RoutingOrchestrator(prerequisiteChecker)
    }

    // ============ High-Confidence Route Tests ============

    @Test
    fun `route with high confidence and no prerequisites navigates to target`() = runTest {
        // Given: High confidence classification for drill request (no prerequisites)
        val intent = createIntent(IntentType.DRILL_REQUEST, confidence = 0.85f)
        val target = RoutingTarget(Module.COACH, "drills", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should navigate to target
        assertTrue(result is RoutingResult.Navigate)
        val navigateResult = result as RoutingResult.Navigate
        assertEquals(target, navigateResult.target)
        assertEquals(intent, navigateResult.intent)
    }

    @Test
    fun `route with club adjustment and bag configured navigates`() = runTest {
        // Given: Club adjustment intent with prerequisite satisfied
        val intent = createIntent(IntentType.CLUB_ADJUSTMENT, confidence = 0.90f)
        val target = RoutingTarget(Module.CADDY, "club_adjustment", mapOf("club" to "7-iron"))
        val classification = ClassificationResult.Route(intent, target)

        // Bag is configured
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns emptyList()

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should navigate to target
        assertTrue(result is RoutingResult.Navigate)
        coVerify { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) }
    }

    // ============ Prerequisite Validation Tests ============

    @Test
    fun `route with recovery check and no recovery data returns prerequisite missing`() = runTest {
        // Given: Recovery check intent without recovery data
        val intent = createIntent(IntentType.RECOVERY_CHECK, confidence = 0.88f)
        val target = RoutingTarget(Module.RECOVERY, "overview", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // Recovery data missing
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.RECOVERY_DATA)) } returns
            listOf(Prerequisite.RECOVERY_DATA)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return prerequisite missing
        assertTrue(result is RoutingResult.PrerequisiteMissing)
        val missingResult = result as RoutingResult.PrerequisiteMissing
        assertEquals(listOf(Prerequisite.RECOVERY_DATA), missingResult.missing)
        assertTrue(missingResult.message.contains("recovery data"))
        coVerify { prerequisiteChecker.checkAll(listOf(Prerequisite.RECOVERY_DATA)) }
    }

    @Test
    fun `route with score entry and no active round returns prerequisite missing`() = runTest {
        // Given: Score entry intent without active round
        val intent = createIntent(IntentType.SCORE_ENTRY, confidence = 0.92f)
        val target = RoutingTarget(Module.CADDY, "score_entry", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // No active round
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.ROUND_ACTIVE)) } returns
            listOf(Prerequisite.ROUND_ACTIVE)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return prerequisite missing
        assertTrue(result is RoutingResult.PrerequisiteMissing)
        val missingResult = result as RoutingResult.PrerequisiteMissing
        assertEquals(listOf(Prerequisite.ROUND_ACTIVE), missingResult.missing)
        assertTrue(missingResult.message.contains("start a round"))
        coVerify { prerequisiteChecker.checkAll(listOf(Prerequisite.ROUND_ACTIVE)) }
    }

    @Test
    fun `route with shot recommendation and no bag configured returns prerequisite missing`() = runTest {
        // Given: Shot recommendation intent without configured bag
        val intent = createIntent(IntentType.SHOT_RECOMMENDATION, confidence = 0.87f)
        val target = RoutingTarget(Module.CADDY, "shot_recommendation", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // Bag not configured
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.BAG_CONFIGURED)) } returns
            listOf(Prerequisite.BAG_CONFIGURED)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return prerequisite missing
        assertTrue(result is RoutingResult.PrerequisiteMissing)
        val missingResult = result as RoutingResult.PrerequisiteMissing
        assertEquals(listOf(Prerequisite.BAG_CONFIGURED), missingResult.missing)
        assertTrue(missingResult.message.contains("bag isn't configured"))
    }

    @Test
    fun `route with course info and no course selected returns prerequisite missing`() = runTest {
        // Given: Course info intent without course selection
        val intent = createIntent(IntentType.COURSE_INFO, confidence = 0.83f)
        val target = RoutingTarget(Module.CADDY, "course_info", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // Course not selected
        coEvery { prerequisiteChecker.checkAll(listOf(Prerequisite.COURSE_SELECTED)) } returns
            listOf(Prerequisite.COURSE_SELECTED)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return prerequisite missing
        assertTrue(result is RoutingResult.PrerequisiteMissing)
        val missingResult = result as RoutingResult.PrerequisiteMissing
        assertEquals(listOf(Prerequisite.COURSE_SELECTED), missingResult.missing)
        assertTrue(missingResult.message.contains("Which course"))
    }

    // ============ No-Navigation Intent Tests ============

    @Test
    fun `route with pattern query returns no navigation with response`() = runTest {
        // Given: Pattern query intent (no navigation)
        val intent = createIntent(IntentType.PATTERN_QUERY, confidence = 0.91f)
        val target = RoutingTarget(Module.COACH, "patterns", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return no navigation with response
        assertTrue(result is RoutingResult.NoNavigation)
        val noNavResult = result as RoutingResult.NoNavigation
        assertEquals(intent, noNavResult.intent)
        assertTrue(noNavResult.response.contains("miss patterns"))
    }

    @Test
    fun `route with help request returns no navigation with response`() = runTest {
        // Given: Help request intent (no navigation)
        val intent = createIntent(IntentType.HELP_REQUEST, confidence = 0.95f)
        val target = RoutingTarget(Module.SETTINGS, "help", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return no navigation with response
        assertTrue(result is RoutingResult.NoNavigation)
        val noNavResult = result as RoutingResult.NoNavigation
        assertEquals(intent, noNavResult.intent)
        assertTrue(noNavResult.response.contains("Bones"))
        assertTrue(noNavResult.response.contains("caddy"))
    }

    @Test
    fun `route with feedback returns no navigation with response`() = runTest {
        // Given: Feedback intent (no navigation)
        val intent = createIntent(IntentType.FEEDBACK, confidence = 0.89f)
        val target = RoutingTarget(Module.SETTINGS, "feedback", emptyMap())
        val classification = ClassificationResult.Route(intent, target)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return no navigation with response
        assertTrue(result is RoutingResult.NoNavigation)
        val noNavResult = result as RoutingResult.NoNavigation
        assertEquals(intent, noNavResult.intent)
        assertTrue(noNavResult.response.contains("Thanks for the feedback"))
    }

    // ============ Confirmation Flow Tests ============

    @Test
    fun `mid-confidence classification returns confirmation required`() = runTest {
        // Given: Mid-confidence classification (0.50-0.74)
        val intent = createIntent(IntentType.WEATHER_CHECK, confidence = 0.65f)
        val confirmMessage = "Did you want to check the weather forecast?"
        val classification = ClassificationResult.Confirm(intent, confirmMessage)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return confirmation required
        assertTrue(result is RoutingResult.ConfirmationRequired)
        val confirmResult = result as RoutingResult.ConfirmationRequired
        assertEquals(intent, confirmResult.intent)
        assertEquals(confirmMessage, confirmResult.message)
    }

    // ============ Clarify and Error Handling Tests ============

    @Test
    fun `clarify classification returns no navigation`() = runTest {
        // Given: Low-confidence classification requiring clarification
        val suggestions = listOf(IntentType.DRILL_REQUEST, IntentType.SHOT_RECOMMENDATION)
        val clarifyMessage = "I'm not sure what you're asking. Did you mean:"
        val classification = ClassificationResult.Clarify(suggestions, clarifyMessage, "help me")

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return no navigation
        assertTrue(result is RoutingResult.NoNavigation)
        val noNavResult = result as RoutingResult.NoNavigation
        assertEquals(clarifyMessage, noNavResult.response)
    }

    @Test
    fun `error classification returns no navigation with error message`() = runTest {
        // Given: Classification error
        val errorMessage = "Unable to understand your request. Please try again."
        val classification = ClassificationResult.Error(Exception("Network timeout"), errorMessage)

        // When: Route is processed
        val result = orchestrator.route(classification)

        // Then: Should return no navigation with error message
        assertTrue(result is RoutingResult.NoNavigation)
        val noNavResult = result as RoutingResult.NoNavigation
        assertEquals(errorMessage, noNavResult.response)
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
