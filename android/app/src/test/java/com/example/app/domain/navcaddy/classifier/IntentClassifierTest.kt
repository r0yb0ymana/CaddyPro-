package caddypro.domain.navcaddy.classifier

import caddypro.domain.navcaddy.llm.LLMClient
import caddypro.domain.navcaddy.llm.LLMException
import caddypro.domain.navcaddy.llm.LLMResponse
import caddypro.domain.navcaddy.models.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for IntentClassifier.
 *
 * Tests classification logic with mocked LLM responses to verify:
 * - High confidence -> Route
 * - Medium confidence -> Confirm
 * - Low confidence -> Clarify
 * - Error handling
 *
 * Spec reference: navcaddy-engine-plan.md Task 6
 */
class IntentClassifierTest {

    private lateinit var mockLLMClient: LLMClient
    private lateinit var classifier: IntentClassifier

    @Before
    fun setup() {
        mockLLMClient = mockk()
        classifier = IntentClassifier(mockLLMClient)
    }

    @Test
    fun `high confidence classification returns Route result`() = runTest {
        // Given: LLM returns high confidence response (>= 0.75)
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.92,
                  "entities": {
                    "yardage": 150,
                    "lie": "fairway"
                  },
                  "user_goal": "Get shot recommendation"
                }
            """.trimIndent(),
            latencyMs = 1200,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying user input
        val result = classifier.classify("What club for 150 yards?")

        // Then: Should return Route result
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        assertEquals(IntentType.SHOT_RECOMMENDATION, route.intent.intentType)
        assertEquals(0.92f, route.intent.confidence, 0.001f)
        assertNotNull(route.target)
        assertEquals(Module.CADDY, route.target.module)
    }

    @Test
    fun `medium confidence classification returns Confirm result`() = runTest {
        // Given: LLM returns medium confidence response (0.50 - 0.74)
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "CLUB_ADJUSTMENT",
                  "confidence": 0.65,
                  "entities": {
                    "club": "7-iron"
                  },
                  "user_goal": "Adjust club distance"
                }
            """.trimIndent(),
            latencyMs = 1000,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying user input
        val result = classifier.classify("My 7 iron feels different")

        // Then: Should return Confirm result
        assertTrue(result is ClassificationResult.Confirm)
        val confirm = result as ClassificationResult.Confirm
        assertEquals(IntentType.CLUB_ADJUSTMENT, confirm.intent.intentType)
        assertEquals(0.65f, confirm.intent.confidence, 0.001f)
        assertTrue(confirm.message.contains("club adjustment", ignoreCase = true))
    }

    @Test
    fun `low confidence classification returns Clarify result`() = runTest {
        // Given: LLM returns low confidence response (< 0.50)
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "HELP_REQUEST",
                  "confidence": 0.35,
                  "entities": {},
                  "user_goal": "Unknown"
                }
            """.trimIndent(),
            latencyMs = 900,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying ambiguous input
        val result = classifier.classify("It feels off today")

        // Then: Should return Clarify result
        assertTrue(result is ClassificationResult.Clarify)
        val clarify = result as ClassificationResult.Clarify
        assertTrue(clarify.suggestions.isNotEmpty())
        assertTrue(clarify.suggestions.size <= 3)
        assertEquals("It feels off today", clarify.originalInput)
    }

    @Test
    fun `missing required entities returns Clarify result`() = runTest {
        // Given: LLM returns classification with missing required entities
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "CLUB_ADJUSTMENT",
                  "confidence": 0.85,
                  "entities": {},
                  "user_goal": "Adjust club"
                }
            """.trimIndent(),
            latencyMs = 1100,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying input
        val result = classifier.classify("I need to adjust something")

        // Then: Should return Clarify result for missing entities
        assertTrue(result is ClassificationResult.Clarify)
        val clarify = result as ClassificationResult.Clarify
        assertTrue(clarify.message.contains("more information", ignoreCase = true))
    }

    @Test
    fun `LLM exception returns Error result`() = runTest {
        // Given: LLM throws exception
        coEvery { mockLLMClient.classify(any(), any()) } throws LLMException("API timeout")

        // When: Classifying input
        val result = classifier.classify("What club should I use?")

        // Then: Should return Error result
        assertTrue(result is ClassificationResult.Error)
        val error = result as ClassificationResult.Error
        assertTrue(error.cause is LLMException)
        assertTrue(error.message.contains("unable", ignoreCase = true))
    }

    @Test
    fun `blank input returns Error result`() = runTest {
        // When: Classifying blank input
        val result = classifier.classify("   ")

        // Then: Should return Error result
        assertTrue(result is ClassificationResult.Error)
        val error = result as ClassificationResult.Error
        assertTrue(error.message.contains("say or type", ignoreCase = true))
    }

    @Test
    fun `no-navigation intent with high confidence returns Error`() = runTest {
        // Given: LLM returns high confidence for no-navigation intent
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "PATTERN_QUERY",
                  "confidence": 0.90,
                  "entities": {
                    "club": "7-iron"
                  },
                  "user_goal": "Check patterns"
                }
            """.trimIndent(),
            latencyMs = 1000,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying input
        val result = classifier.classify("What are my 7-iron patterns?")

        // Then: Should return Error (no routing target for pure answer intents)
        assertTrue(result is ClassificationResult.Error)
        val error = result as ClassificationResult.Error
        assertTrue(error.message.contains("doesn't require navigation", ignoreCase = true))
    }

    @Test
    fun `context is passed to LLM client`() = runTest {
        // Given: Session context with conversation history
        val context = SessionContext(
            currentRound = Round(
                id = "round-1",
                courseName = "Pebble Beach",
                startTime = System.currentTimeMillis()
            ),
            currentHole = 7,
            conversationHistory = listOf(
                ConversationTurn(
                    userInput = "What club for 150 yards?",
                    assistantResponse = "I recommend 7-iron",
                    timestamp = System.currentTimeMillis()
                )
            )
        )

        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.88,
                  "entities": {
                    "yardage": 165
                  },
                  "user_goal": "Follow-up shot recommendation"
                }
            """.trimIndent(),
            latencyMs = 1100,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), context) } returns mockResponse

        // When: Classifying with context
        val result = classifier.classify("What about 165?", context)

        // Then: Should successfully classify with context
        assertTrue(result is ClassificationResult.Route)
    }

    @Test
    fun `entity extraction includes multiple entities`() = runTest {
        // Given: LLM returns multiple entities
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.89,
                  "entities": {
                    "club": "driver",
                    "yardage": 280,
                    "lie": "fairway",
                    "wind": "10mph left-to-right"
                  },
                  "user_goal": "Shot recommendation with wind"
                }
            """.trimIndent(),
            latencyMs = 1200,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying input with multiple entities
        val result = classifier.classify("Driver from fairway, 280 yards, wind left to right")

        // Then: Should extract all entities
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        assertNotNull(route.intent.entities.club)
        assertEquals("Driver", route.intent.entities.club?.name)
        assertEquals(280, route.intent.entities.yardage)
        assertEquals(Lie.FAIRWAY, route.intent.entities.lie)
        assertEquals("10mph left-to-right", route.intent.entities.wind)
    }

    @Test
    fun `confidence at boundary conditions`() = runTest {
        // Test at 0.75 (route threshold)
        val routeResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.75,
                  "entities": {},
                  "user_goal": "Shot recommendation"
                }
            """.trimIndent(),
            latencyMs = 1000,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify("route", any()) } returns routeResponse

        val routeResult = classifier.classify("route")
        assertTrue(routeResult is ClassificationResult.Route)

        // Test at 0.74 (confirm threshold)
        val confirmResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.74,
                  "entities": {},
                  "user_goal": "Shot recommendation"
                }
            """.trimIndent(),
            latencyMs = 1000,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify("confirm", any()) } returns confirmResponse

        val confirmResult = classifier.classify("confirm")
        assertTrue(confirmResult is ClassificationResult.Confirm)

        // Test at 0.49 (clarify threshold)
        val clarifyResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "HELP_REQUEST",
                  "confidence": 0.49,
                  "entities": {},
                  "user_goal": "Unknown"
                }
            """.trimIndent(),
            latencyMs = 1000,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify("clarify", any()) } returns clarifyResponse

        val clarifyResult = classifier.classify("clarify")
        assertTrue(clarifyResult is ClassificationResult.Clarify)
    }
}
