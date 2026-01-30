package caddypro.domain.navcaddy

import caddypro.domain.navcaddy.classifier.ClassificationResult
import caddypro.domain.navcaddy.classifier.IntentClassifier
import caddypro.domain.navcaddy.llm.LLMClient
import caddypro.domain.navcaddy.llm.LLMResponse
import caddypro.domain.navcaddy.models.*
import caddypro.domain.navcaddy.normalizer.InputNormalizer
import caddypro.domain.navcaddy.clarification.ClarificationHandler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for the Intent Pipeline.
 *
 * Tests the full flow: Input → Normalizer → Classifier → Result
 * Uses mocked LLM responses to verify different confidence scenarios.
 *
 * Spec reference: navcaddy-engine.md R2, A1, A3
 * Acceptance criteria: A1 (high confidence routing), A3 (low confidence clarification)
 * Plan reference: navcaddy-engine-plan.md Task 9
 */
class IntentPipelineIntegrationTest {

    private lateinit var mockLLMClient: MockLLMClient
    private lateinit var normalizer: InputNormalizer
    private lateinit var clarificationHandler: ClarificationHandler
    private lateinit var classifier: IntentClassifier

    @Before
    fun setup() {
        mockLLMClient = MockLLMClient()
        normalizer = InputNormalizer()
        clarificationHandler = ClarificationHandler()
        classifier = IntentClassifier(mockLLMClient, normalizer, clarificationHandler)
    }

    // =========================
    // A1: High Confidence Routing Tests
    // =========================

    @Test
    fun `A1 - high confidence club adjustment routes correctly`() = runTest {
        // Given: User says "My 7i feels long today"
        mockLLMClient.setMockResponse(
            input = "My 7-iron feels long today",
            intentType = IntentType.CLUB_ADJUSTMENT,
            confidence = 0.85f,
            entities = mapOf("club" to "7-iron")
        )

        // When: Classifying the input
        val result = classifier.classify("My 7i feels long today")

        // Then: Should route to ClubAdjustmentScreen with club=7-iron
        assertTrue("Expected Route result for high confidence", result is ClassificationResult.Route)

        val route = result as ClassificationResult.Route
        assertEquals("Intent type should be CLUB_ADJUSTMENT", IntentType.CLUB_ADJUSTMENT, route.intent.intentType)
        assertEquals("Confidence should be 0.85", 0.85f, route.intent.confidence, 0.001f)
        assertNotNull("Routing target should not be null", route.target)
        assertEquals("Should route to COACH module", Module.COACH, route.target.module)

        // Verify club was extracted
        assertNotNull("Club entity should be extracted", route.intent.entities.club)
        assertEquals("Club should be 7-Iron", "7-Iron", route.intent.entities.club?.name)
    }

    @Test
    fun `A1 - high confidence shot recommendation with yardage routes correctly`() = runTest {
        // Given: User asks "What club for 150 yards?"
        mockLLMClient.setMockResponse(
            input = "What club for 150 yards?",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.92f,
            entities = mapOf("yardage" to 150)
        )

        // When: Classifying the input
        val result = classifier.classify("What club for 150 yards?")

        // Then: Should route to shot recommendation
        assertTrue(result is ClassificationResult.Route)

        val route = result as ClassificationResult.Route
        assertEquals(IntentType.SHOT_RECOMMENDATION, route.intent.intentType)
        assertEquals(0.92f, route.intent.confidence, 0.001f)
        assertEquals(Module.CADDY, route.target.module)
        assertEquals(150, route.intent.entities.yardage)
    }

    @Test
    fun `A1 - high confidence with normalized input routes correctly`() = runTest {
        // Given: Input with slang and spoken numbers "I have one fifty with my pw"
        mockLLMClient.setMockResponse(
            input = "I have 150 with my pitching wedge",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.88f,
            entities = mapOf(
                "yardage" to 150,
                "club" to "pitching wedge"
            )
        )

        // When: Classifying the input (normalizer will expand "one fifty" and "pw")
        val result = classifier.classify("I have one fifty with my pw")

        // Then: Should route correctly with normalized entities
        assertTrue(result is ClassificationResult.Route)

        val route = result as ClassificationResult.Route
        assertEquals(IntentType.SHOT_RECOMMENDATION, route.intent.intentType)
        assertEquals(150, route.intent.entities.yardage)
        assertNotNull(route.intent.entities.club)
        assertEquals("Pitching Wedge", route.intent.entities.club?.name)
    }

    @Test
    fun `A1 - high confidence recovery check routes to recovery module`() = runTest {
        // Given: User asks "How's my recovery looking?"
        mockLLMClient.setMockResponse(
            input = "How's my recovery looking?",
            intentType = IntentType.RECOVERY_CHECK,
            confidence = 0.90f,
            entities = emptyMap()
        )

        // When: Classifying the input
        val result = classifier.classify("How's my recovery looking?")

        // Then: Should route to recovery module
        assertTrue(result is ClassificationResult.Route)

        val route = result as ClassificationResult.Route
        assertEquals(IntentType.RECOVERY_CHECK, route.intent.intentType)
        assertEquals(Module.RECOVERY, route.target.module)
    }

    // =========================
    // A3: Low Confidence Clarification Tests
    // =========================

    @Test
    fun `A3 - low confidence triggers clarification with suggestions`() = runTest {
        // Given: Ambiguous input "It feels off today"
        mockLLMClient.setMockResponse(
            input = "It feels off today",
            intentType = IntentType.HELP_REQUEST,
            confidence = 0.35f,
            entities = emptyMap()
        )

        // When: Classifying ambiguous input
        val result = classifier.classify("It feels off today")

        // Then: Should return clarification with suggestions
        assertTrue("Expected Clarify result for low confidence", result is ClassificationResult.Clarify)

        val clarify = result as ClassificationResult.Clarify
        assertEquals("Original input should be preserved", "It feels off today", clarify.originalInput)
        assertTrue("Should have suggestions", clarify.suggestions.isNotEmpty())
        assertTrue("Should have max 3 suggestions", clarify.suggestions.size <= 3)
        assertFalse("Message should not be empty", clarify.message.isEmpty())
        assertTrue("Message should indicate need for clarification",
            clarify.message.contains("not quite sure", ignoreCase = true) ||
            clarify.message.contains("clarify", ignoreCase = true))
    }

    @Test
    fun `A3 - very low confidence provides helpful suggestions`() = runTest {
        // Given: Very ambiguous input "help me"
        mockLLMClient.setMockResponse(
            input = "help me",
            intentType = IntentType.HELP_REQUEST,
            confidence = 0.25f,
            entities = emptyMap()
        )

        // When: Classifying very ambiguous input
        val result = classifier.classify("help me")

        // Then: Should return clarification with helpful suggestions
        assertTrue(result is ClassificationResult.Clarify)

        val clarify = result as ClassificationResult.Clarify
        assertTrue("Should have suggestions", clarify.suggestions.isNotEmpty())
        assertTrue("Should have up to 3 suggestions", clarify.suggestions.size in 1..3)

        // Suggestions should be common, helpful intents
        val suggestionTypes = clarify.suggestions.toSet()
        assertTrue("Should suggest common intents",
            suggestionTypes.any { it in setOf(
                IntentType.SHOT_RECOMMENDATION,
                IntentType.HELP_REQUEST,
                IntentType.CLUB_ADJUSTMENT,
                IntentType.RECOVERY_CHECK
            )}
        )
    }

    @Test
    fun `A3 - low confidence with partial understanding suggests related intents`() = runTest {
        // Given: Partial information "my club"
        mockLLMClient.setMockResponse(
            input = "my club",
            intentType = IntentType.CLUB_ADJUSTMENT,
            confidence = 0.40f,
            entities = emptyMap()
        )

        // When: Classifying input with partial information
        val result = classifier.classify("my club")

        // Then: Should clarify but suggest club-related intents
        assertTrue(result is ClassificationResult.Clarify)

        val clarify = result as ClassificationResult.Clarify
        assertTrue("Should include the parsed intent",
            IntentType.CLUB_ADJUSTMENT in clarify.suggestions)
    }

    // =========================
    // Medium Confidence Confirmation Tests
    // =========================

    @Test
    fun `medium confidence at 0_65 triggers confirmation`() = runTest {
        // Given: Medium confidence classification
        mockLLMClient.setMockResponse(
            input = "Check my recovery",
            intentType = IntentType.RECOVERY_CHECK,
            confidence = 0.65f,
            entities = emptyMap()
        )

        // When: Classifying input
        val result = classifier.classify("Check my recovery")

        // Then: Should return confirmation request
        assertTrue("Expected Confirm result for medium confidence", result is ClassificationResult.Confirm)

        val confirm = result as ClassificationResult.Confirm
        assertEquals(IntentType.RECOVERY_CHECK, confirm.intent.intentType)
        assertFalse("Confirmation message should not be empty", confirm.message.isEmpty())
        assertTrue("Message should ask for confirmation",
            confirm.message.contains("did you", ignoreCase = true) ||
            confirm.message.contains("want to", ignoreCase = true))
    }

    @Test
    fun `medium confidence includes entity details in confirmation`() = runTest {
        // Given: Medium confidence with entities
        mockLLMClient.setMockResponse(
            input = "7-iron adjustment",
            intentType = IntentType.CLUB_ADJUSTMENT,
            confidence = 0.60f,
            entities = mapOf("club" to "7-iron")
        )

        // When: Classifying input
        val result = classifier.classify("7-iron adjustment")

        // Then: Confirmation should include entity details
        assertTrue(result is ClassificationResult.Confirm)

        val confirm = result as ClassificationResult.Confirm
        assertTrue("Message should mention the club",
            confirm.message.contains("7-iron", ignoreCase = true))
    }

    // =========================
    // Threshold Boundary Tests
    // =========================

    @Test
    fun `confidence at 0_75 exactly triggers route`() = runTest {
        // Given: Confidence exactly at route threshold
        mockLLMClient.setMockResponse(
            input = "shot recommendation",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.75f,
            entities = emptyMap()
        )

        // When: Classifying input
        val result = classifier.classify("shot recommendation")

        // Then: Should route (>= 0.75)
        assertTrue("Confidence 0.75 should trigger Route", result is ClassificationResult.Route)
    }

    @Test
    fun `confidence at 0_74 triggers confirm`() = runTest {
        // Given: Confidence just below route threshold
        mockLLMClient.setMockResponse(
            input = "shot recommendation",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.74f,
            entities = emptyMap()
        )

        // When: Classifying input
        val result = classifier.classify("shot recommendation")

        // Then: Should confirm (0.50 - 0.74)
        assertTrue("Confidence 0.74 should trigger Confirm", result is ClassificationResult.Confirm)
    }

    @Test
    fun `confidence at 0_50 exactly triggers confirm`() = runTest {
        // Given: Confidence exactly at confirm threshold
        mockLLMClient.setMockResponse(
            input = "help request",
            intentType = IntentType.HELP_REQUEST,
            confidence = 0.50f,
            entities = emptyMap()
        )

        // When: Classifying input
        val result = classifier.classify("help request")

        // Then: Should confirm (>= 0.50)
        assertTrue("Confidence 0.50 should trigger Confirm", result is ClassificationResult.Confirm)
    }

    @Test
    fun `confidence at 0_49 triggers clarify`() = runTest {
        // Given: Confidence just below confirm threshold
        mockLLMClient.setMockResponse(
            input = "help request",
            intentType = IntentType.HELP_REQUEST,
            confidence = 0.49f,
            entities = emptyMap()
        )

        // When: Classifying input
        val result = classifier.classify("help request")

        // Then: Should clarify (< 0.50)
        assertTrue("Confidence 0.49 should trigger Clarify", result is ClassificationResult.Clarify)
    }

    // =========================
    // Edge Cases and Complex Scenarios
    // =========================

    @Test
    fun `input with profanity gets filtered but intent still detected`() = runTest {
        // Given: Input with profanity "My damn 7i feels long"
        mockLLMClient.setMockResponse(
            input = "My **** 7-iron feels long", // Normalizer filters profanity
            intentType = IntentType.CLUB_ADJUSTMENT,
            confidence = 0.82f,
            entities = mapOf("club" to "7-iron")
        )

        // When: Classifying input with profanity
        val result = classifier.classify("My damn 7i feels long")

        // Then: Should route correctly (profanity filtered doesn't affect intent)
        assertTrue(result is ClassificationResult.Route)

        val route = result as ClassificationResult.Route
        assertEquals(IntentType.CLUB_ADJUSTMENT, route.intent.intentType)
        assertNotNull(route.intent.entities.club)
    }

    @Test
    fun `input with golf slang expands correctly before classification`() = runTest {
        // Given: Input with multiple slang terms
        mockLLMClient.setMockResponse(
            input = "Should I use 3-wood or driver to the dance floor",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.87f,
            entities = emptyMap()
        )

        // When: Classifying input with slang ("3w", "d", "dance floor")
        val result = classifier.classify("Should I use 3w or d to the dance floor")

        // Then: Should classify correctly with expanded terms
        assertTrue(result is ClassificationResult.Route)
        assertEquals(IntentType.SHOT_RECOMMENDATION, (result as ClassificationResult.Route).intent.intentType)
    }

    @Test
    fun `spoken numbers normalize before classification`() = runTest {
        // Given: Input with spoken numbers
        mockLLMClient.setMockResponse(
            input = "I have 175 yards with the 8-iron",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.89f,
            entities = mapOf(
                "yardage" to 175,
                "club" to "8-iron"
            )
        )

        // When: Classifying input with spoken numbers
        val result = classifier.classify("I have one seventy five yards with the 8i")

        // Then: Should extract normalized yardage
        assertTrue(result is ClassificationResult.Route)

        val route = result as ClassificationResult.Route
        assertEquals(175, route.intent.entities.yardage)
    }

    @Test
    fun `empty input returns error`() = runTest {
        // When: Classifying empty input
        val result = classifier.classify("")

        // Then: Should return error
        assertTrue("Empty input should return Error", result is ClassificationResult.Error)

        val error = result as ClassificationResult.Error
        assertTrue("Error message should mention blank input",
            error.message.contains("say or type", ignoreCase = true))
    }

    @Test
    fun `blank input returns error`() = runTest {
        // When: Classifying blank input (spaces only)
        val result = classifier.classify("   ")

        // Then: Should return error
        assertTrue("Blank input should return Error", result is ClassificationResult.Error)
    }

    @Test
    fun `multiple normalizations in single input work correctly`() = runTest {
        // Given: Complex input with multiple normalizations needed
        mockLLMClient.setMockResponse(
            input = "My 7-iron from 150 yards at the green",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.91f,
            entities = mapOf(
                "club" to "7-iron",
                "yardage" to 150,
                "lie" to "green"
            )
        )

        // When: Input requires club expansion, number normalization, and slang expansion
        val result = classifier.classify("My 7i from one fifty yards at the dance floor")

        // Then: All normalizations should work together
        assertTrue(result is ClassificationResult.Route)

        val route = result as ClassificationResult.Route
        assertEquals(IntentType.SHOT_RECOMMENDATION, route.intent.intentType)
        assertNotNull("Club should be extracted", route.intent.entities.club)
        assertEquals("7-Iron", route.intent.entities.club?.name)
        assertEquals(150, route.intent.entities.yardage)
        assertEquals(Lie.GREEN, route.intent.entities.lie)
    }

    @Test
    fun `pipeline preserves context through full flow`() = runTest {
        // Given: Input with session context
        val context = SessionContext(
            currentRound = Round(
                id = "round-123",
                courseName = "Test Course",
                startTime = System.currentTimeMillis()
            ),
            currentHole = 5,
            conversationHistory = listOf(
                ConversationTurn(
                    userInput = "What's my 7-iron distance?",
                    assistantResponse = "Your 7-iron averages 155 yards",
                    timestamp = System.currentTimeMillis()
                )
            )
        )

        mockLLMClient.setMockResponse(
            input = "Use it for this shot",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.88f,
            entities = mapOf("club" to "7-iron")
        )

        // When: Classifying with context (follow-up)
        val result = classifier.classify("Use it for this shot", context)

        // Then: Should successfully use context
        assertTrue(result is ClassificationResult.Route)
        assertEquals(IntentType.SHOT_RECOMMENDATION, (result as ClassificationResult.Route).intent.intentType)
    }

    @Test
    fun `high confidence with multiple entities routes with all data`() = runTest {
        // Given: Complex input with multiple entities
        mockLLMClient.setMockResponse(
            input = "Driver from fairway 280 yards with 10mph left-to-right wind",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.93f,
            entities = mapOf(
                "club" to "driver",
                "lie" to "fairway",
                "yardage" to 280,
                "wind" to "10mph left-to-right"
            )
        )

        // When: Classifying rich input
        val result = classifier.classify("Driver from fairway 280 yards with 10mph left-to-right wind")

        // Then: All entities should be extracted
        assertTrue(result is ClassificationResult.Route)

        val route = result as ClassificationResult.Route
        assertNotNull(route.intent.entities.club)
        assertEquals("Driver", route.intent.entities.club?.name)
        assertEquals(Lie.FAIRWAY, route.intent.entities.lie)
        assertEquals(280, route.intent.entities.yardage)
        assertEquals("10mph left-to-right", route.intent.entities.wind)
    }

    @Test
    fun `low confidence but contextually relevant provides smart suggestions`() = runTest {
        // Given: Low confidence input about clubs
        mockLLMClient.setMockResponse(
            input = "club distance",
            intentType = IntentType.CLUB_ADJUSTMENT,
            confidence = 0.38f,
            entities = emptyMap()
        )

        // When: Classifying
        val result = classifier.classify("club distance")

        // Then: Suggestions should be club-related
        assertTrue(result is ClassificationResult.Clarify)

        val clarify = result as ClassificationResult.Clarify
        val hasClubRelated = clarify.suggestions.any {
            it in setOf(
                IntentType.CLUB_ADJUSTMENT,
                IntentType.EQUIPMENT_INFO,
                IntentType.STATS_LOOKUP
            )
        }
        assertTrue("Should suggest club-related intents", hasClubRelated)
    }
}

/**
 * Mock LLM client for integration testing.
 *
 * Returns predictable responses based on configured input patterns.
 * Simulates the real Gemini client behavior without network calls.
 */
private class MockLLMClient : LLMClient {
    private val responseMap = mutableMapOf<String, MockResponseConfig>()

    fun setMockResponse(
        input: String,
        intentType: IntentType,
        confidence: Float,
        entities: Map<String, Any>
    ) {
        responseMap[input] = MockResponseConfig(intentType, confidence, entities)
    }

    override suspend fun classify(input: String, context: SessionContext?): LLMResponse {
        val config = responseMap[input] ?: MockResponseConfig(
            intentType = IntentType.HELP_REQUEST,
            confidence = 0.30f,
            entities = emptyMap()
        )

        // Build JSON response matching Gemini format
        val json = buildJsonResponse(config)

        return LLMResponse(
            rawResponse = json,
            latencyMs = 100,
            modelName = "mock-llm"
        )
    }

    private fun buildJsonResponse(config: MockResponseConfig): String {
        val entitiesJson = buildString {
            append("{")
            config.entities.entries.forEachIndexed { index, entry ->
                if (index > 0) append(", ")
                append("\"${entry.key}\": ")
                when (val value = entry.value) {
                    is String -> append("\"$value\"")
                    is Int -> append(value)
                    is Float -> append(value)
                    else -> append("\"$value\"")
                }
            }
            append("}")
        }

        return """
            {
              "intent_type": "${config.intentType.name}",
              "confidence": ${config.confidence},
              "entities": $entitiesJson,
              "user_goal": "Mock user goal"
            }
        """.trimIndent()
    }

    private data class MockResponseConfig(
        val intentType: IntentType,
        val confidence: Float,
        val entities: Map<String, Any>
    )
}
