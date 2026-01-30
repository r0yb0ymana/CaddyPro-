package caddypro.domain.navcaddy.classifier

import caddypro.domain.navcaddy.llm.LLMClient
import caddypro.domain.navcaddy.llm.LLMResponse
import caddypro.domain.navcaddy.models.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Live Caddy mode intent routing.
 *
 * Tests the integration of voice queries with Live Caddy routing for:
 * - SHOT_RECOMMENDATION with expandStrategy parameter
 * - CLUB_ADJUSTMENT routing to LiveCaddyScreen
 * - BAILOUT_QUERY with highlightBailout parameter
 * - WEATHER_CHECK with expandWeather parameter
 * - READINESS_CHECK with expandReadiness parameter
 *
 * Spec reference: live-caddy-mode.md R5, live-caddy-mode-plan.md Task 24
 */
class LiveCaddyIntentRoutingTest {

    private lateinit var mockLLMClient: LLMClient
    private lateinit var classifier: IntentClassifier

    @Before
    fun setup() {
        mockLLMClient = mockk()
        classifier = IntentClassifier(mockLLMClient)
    }

    @Test
    fun `SHOT_RECOMMENDATION routes to LiveCaddyScreen with expandStrategy`() = runTest {
        // Given: User asks "What's the play off the tee?"
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.92,
                  "entities": {},
                  "user_goal": "Get shot recommendation for tee shot"
                }
            """.trimIndent(),
            latencyMs = 1100,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying the voice query
        val result = classifier.classify("What's the play off the tee?")

        // Then: Should route to LiveCaddyScreen with expandStrategy=true
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        assertEquals(IntentType.SHOT_RECOMMENDATION, route.intent.intentType)
        assertEquals(Module.CADDY, route.target.module)
        assertEquals("LiveCaddyScreen", route.target.screen)
        assertTrue(route.target.parameters.containsKey("expandStrategy"))
        assertEquals(true, route.target.parameters["expandStrategy"])
    }

    @Test
    fun `SHOT_RECOMMENDATION with yardage context routes correctly`() = runTest {
        // Given: User asks "150 yards into the wind, what's the play?"
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.88,
                  "entities": {
                    "yardage": 150,
                    "wind": "into the wind"
                  },
                  "user_goal": "Shot recommendation with wind consideration"
                }
            """.trimIndent(),
            latencyMs = 1200,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying the query
        val result = classifier.classify("150 yards into the wind, what's the play?")

        // Then: Should route with entities extracted
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        assertEquals(IntentType.SHOT_RECOMMENDATION, route.intent.intentType)
        assertEquals(150, route.intent.entities.yardage)
        assertEquals("into the wind", route.intent.entities.wind)
        assertEquals("LiveCaddyScreen", route.target.screen)
    }

    @Test
    fun `BAILOUT_QUERY routes to LiveCaddyScreen with highlightBailout`() = runTest {
        // Given: User asks "Where's the bailout?"
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "BAILOUT_QUERY",
                  "confidence": 0.90,
                  "entities": {},
                  "user_goal": "Find safe miss area"
                }
            """.trimIndent(),
            latencyMs = 1000,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying the bailout query
        val result = classifier.classify("Where's the bailout?")

        // Then: Should route to LiveCaddyScreen with both expandStrategy and highlightBailout
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        assertEquals(IntentType.BAILOUT_QUERY, route.intent.intentType)
        assertEquals(Module.CADDY, route.target.module)
        assertEquals("LiveCaddyScreen", route.target.screen)
        assertTrue(route.target.parameters.containsKey("expandStrategy"))
        assertTrue(route.target.parameters.containsKey("highlightBailout"))
        assertEquals(true, route.target.parameters["expandStrategy"])
        assertEquals(true, route.target.parameters["highlightBailout"])
    }

    @Test
    fun `BAILOUT_QUERY variations all route correctly`() = runTest {
        val testCases = listOf(
            "Where should I miss?",
            "What's the safe play?",
            "Where can I bail out?",
            "What's the safest area to miss?"
        )

        testCases.forEach { query ->
            // Given: Various bailout query phrasings
            val mockResponse = LLMResponse(
                rawResponse = """
                    {
                      "intent_type": "BAILOUT_QUERY",
                      "confidence": 0.87,
                      "entities": {},
                      "user_goal": "Bailout location"
                    }
                """.trimIndent(),
                latencyMs = 1000,
                modelName = "gemini-3-flash"
            )
            coEvery { mockLLMClient.classify(query, any()) } returns mockResponse

            // When: Classifying
            val result = classifier.classify(query)

            // Then: Should route to LiveCaddyScreen
            assertTrue("Failed for query: $query", result is ClassificationResult.Route)
            val route = result as ClassificationResult.Route
            assertEquals(IntentType.BAILOUT_QUERY, route.intent.intentType)
            assertEquals("LiveCaddyScreen", route.target.screen)
        }
    }

    @Test
    fun `WEATHER_CHECK routes to LiveCaddyScreen with expandWeather`() = runTest {
        // Given: User asks "How's the wind?"
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "WEATHER_CHECK",
                  "confidence": 0.94,
                  "entities": {},
                  "user_goal": "Check wind conditions"
                }
            """.trimIndent(),
            latencyMs = 950,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying the weather query
        val result = classifier.classify("How's the wind?")

        // Then: Should route to LiveCaddyScreen with expandWeather=true
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        assertEquals(IntentType.WEATHER_CHECK, route.intent.intentType)
        assertEquals(Module.CADDY, route.target.module)
        assertEquals("LiveCaddyScreen", route.target.screen)
        assertTrue(route.target.parameters.containsKey("expandWeather"))
        assertEquals(true, route.target.parameters["expandWeather"])
    }

    @Test
    fun `WEATHER_CHECK query variations route correctly`() = runTest {
        val testCases = listOf(
            "What's the weather looking like?",
            "How's the wind today?",
            "Check the forecast",
            "Show me the weather"
        )

        testCases.forEach { query ->
            // Given: Various weather query phrasings
            val mockResponse = LLMResponse(
                rawResponse = """
                    {
                      "intent_type": "WEATHER_CHECK",
                      "confidence": 0.91,
                      "entities": {},
                      "user_goal": "Weather check"
                    }
                """.trimIndent(),
                latencyMs = 1000,
                modelName = "gemini-3-flash"
            )
            coEvery { mockLLMClient.classify(query, any()) } returns mockResponse

            // When: Classifying
            val result = classifier.classify(query)

            // Then: Should route to LiveCaddyScreen
            assertTrue("Failed for query: $query", result is ClassificationResult.Route)
            val route = result as ClassificationResult.Route
            assertEquals(IntentType.WEATHER_CHECK, route.intent.intentType)
            assertEquals("LiveCaddyScreen", route.target.screen)
        }
    }

    @Test
    fun `READINESS_CHECK routes to LiveCaddyScreen with expandReadiness`() = runTest {
        // Given: User asks "How am I feeling?"
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "READINESS_CHECK",
                  "confidence": 0.89,
                  "entities": {},
                  "user_goal": "Check readiness and how it affects strategy"
                }
            """.trimIndent(),
            latencyMs = 1050,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying the readiness query
        val result = classifier.classify("How am I feeling?")

        // Then: Should route to LiveCaddyScreen with expandReadiness=true
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        assertEquals(IntentType.READINESS_CHECK, route.intent.intentType)
        assertEquals(Module.CADDY, route.target.module)
        assertEquals("LiveCaddyScreen", route.target.screen)
        assertTrue(route.target.parameters.containsKey("expandReadiness"))
        assertEquals(true, route.target.parameters["expandReadiness"])
    }

    @Test
    fun `READINESS_CHECK query variations route correctly`() = runTest {
        val testCases = listOf(
            "What's my readiness?",
            "Am I ready for this shot?",
            "How's my body doing?",
            "Should I play conservative today?"
        )

        testCases.forEach { query ->
            // Given: Various readiness query phrasings
            val mockResponse = LLMResponse(
                rawResponse = """
                    {
                      "intent_type": "READINESS_CHECK",
                      "confidence": 0.86,
                      "entities": {},
                      "user_goal": "Readiness check"
                    }
                """.trimIndent(),
                latencyMs = 1000,
                modelName = "gemini-3-flash"
            )
            coEvery { mockLLMClient.classify(query, any()) } returns mockResponse

            // When: Classifying
            val result = classifier.classify(query)

            // Then: Should route to LiveCaddyScreen
            assertTrue("Failed for query: $query", result is ClassificationResult.Route)
            val route = result as ClassificationResult.Route
            assertEquals(IntentType.READINESS_CHECK, route.intent.intentType)
            assertEquals("LiveCaddyScreen", route.target.screen)
        }
    }

    @Test
    fun `CLUB_ADJUSTMENT maintains original routing for non-Live Caddy context`() = runTest {
        // Given: User asks about club adjustment outside Live Caddy context
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "CLUB_ADJUSTMENT",
                  "confidence": 0.88,
                  "entities": {
                    "club": "7-iron"
                  },
                  "user_goal": "Adjust club distance"
                }
            """.trimIndent(),
            latencyMs = 1100,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying club adjustment query
        val result = classifier.classify("My 7-iron feels long today")

        // Then: Should route to ClubAdjustmentScreen (not LiveCaddyScreen)
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        assertEquals(IntentType.CLUB_ADJUSTMENT, route.intent.intentType)
        assertEquals(Module.CADDY, route.target.module)
        assertEquals("ClubAdjustmentScreen", route.target.screen)
    }

    @Test
    fun `Live Caddy intents work with session context`() = runTest {
        // Given: Active round context
        val context = SessionContext(
            currentRound = Round(
                id = "round-1",
                courseName = "Pebble Beach",
                startTime = System.currentTimeMillis()
            ),
            currentHole = 7,
            conversationHistory = emptyList()
        )

        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.93,
                  "entities": {},
                  "user_goal": "Get recommendation for current hole"
                }
            """.trimIndent(),
            latencyMs = 1000,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), context) } returns mockResponse

        // When: Classifying with round context
        val result = classifier.classify("What should I do here?", context)

        // Then: Should successfully route with context
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        assertEquals(IntentType.SHOT_RECOMMENDATION, route.intent.intentType)
        assertEquals("LiveCaddyScreen", route.target.screen)
    }

    @Test
    fun `medium confidence Live Caddy intent returns Confirm result`() = runTest {
        // Given: Medium confidence bailout query
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "BAILOUT_QUERY",
                  "confidence": 0.68,
                  "entities": {},
                  "user_goal": "Bailout location"
                }
            """.trimIndent(),
            latencyMs = 1000,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying ambiguous bailout query
        val result = classifier.classify("Where to go?")

        // Then: Should ask for confirmation
        assertTrue(result is ClassificationResult.Confirm)
        val confirm = result as ClassificationResult.Confirm
        assertEquals(IntentType.BAILOUT_QUERY, confirm.intent.intentType)
        assertTrue(confirm.message.contains("bailout", ignoreCase = true))
    }

    @Test
    fun `low confidence Live Caddy intent returns Clarify result`() = runTest {
        // Given: Low confidence shot query
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.42,
                  "entities": {},
                  "user_goal": "Unclear"
                }
            """.trimIndent(),
            latencyMs = 950,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying vague query
        val result = classifier.classify("What about that?")

        // Then: Should request clarification
        assertTrue(result is ClassificationResult.Clarify)
        val clarify = result as ClassificationResult.Clarify
        assertTrue(clarify.suggestions.isNotEmpty())
        assertEquals("What about that?", clarify.originalInput)
    }

    @Test
    fun `all Live Caddy intents are registered in IntentRegistry`() {
        // Verify all Live Caddy intents have schemas
        val shotRec = caddypro.domain.navcaddy.intent.IntentRegistry.getSchema(IntentType.SHOT_RECOMMENDATION)
        assertEquals("Shot Recommendation", shotRec.displayName)
        assertEquals("LiveCaddyScreen", shotRec.defaultRoutingTarget?.screen)

        val bailout = caddypro.domain.navcaddy.intent.IntentRegistry.getSchema(IntentType.BAILOUT_QUERY)
        assertEquals("Bailout Query", bailout.displayName)
        assertEquals("LiveCaddyScreen", bailout.defaultRoutingTarget?.screen)

        val weather = caddypro.domain.navcaddy.intent.IntentRegistry.getSchema(IntentType.WEATHER_CHECK)
        assertEquals("Weather Check", weather.displayName)
        assertEquals("LiveCaddyScreen", weather.defaultRoutingTarget?.screen)

        val readiness = caddypro.domain.navcaddy.intent.IntentRegistry.getSchema(IntentType.READINESS_CHECK)
        assertEquals("Readiness Check", readiness.displayName)
        assertEquals("LiveCaddyScreen", readiness.defaultRoutingTarget?.screen)
    }

    @Test
    fun `Live Caddy intents have correct example phrases`() {
        val bailout = caddypro.domain.navcaddy.intent.IntentRegistry.getSchema(IntentType.BAILOUT_QUERY)
        assertTrue(bailout.examplePhrases.contains("Where's the bailout?"))
        assertTrue(bailout.examplePhrases.contains("Where should I miss?"))

        val readiness = caddypro.domain.navcaddy.intent.IntentRegistry.getSchema(IntentType.READINESS_CHECK)
        assertTrue(readiness.examplePhrases.contains("How am I feeling?"))
        assertTrue(readiness.examplePhrases.contains("What's my readiness?"))

        val shotRec = caddypro.domain.navcaddy.intent.IntentRegistry.getSchema(IntentType.SHOT_RECOMMENDATION)
        assertTrue(shotRec.examplePhrases.contains("What's the play off the tee?"))
    }

    @Test
    fun `Live Caddy routing parameters are type-safe`() = runTest {
        // Given: Shot recommendation query
        val mockResponse = LLMResponse(
            rawResponse = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.90,
                  "entities": {},
                  "user_goal": "Shot recommendation"
                }
            """.trimIndent(),
            latencyMs = 1000,
            modelName = "gemini-3-flash"
        )
        coEvery { mockLLMClient.classify(any(), any()) } returns mockResponse

        // When: Classifying
        val result = classifier.classify("What's the play?")

        // Then: Parameters should be Boolean type
        assertTrue(result is ClassificationResult.Route)
        val route = result as ClassificationResult.Route
        val expandStrategy = route.target.parameters["expandStrategy"]
        assertTrue(expandStrategy is Boolean)
        assertEquals(true, expandStrategy)
    }

    @Test
    fun `multiple Live Caddy intents can be distinguished`() = runTest {
        // Test that different Live Caddy queries route to different intents with different parameters

        // Shot recommendation
        coEvery { mockLLMClient.classify("What's the play?", any()) } returns LLMResponse(
            """{"intent_type": "SHOT_RECOMMENDATION", "confidence": 0.90, "entities": {}}""",
            1000, "gemini-3-flash"
        )
        val shotResult = classifier.classify("What's the play?")
        assertTrue(shotResult is ClassificationResult.Route)
        assertEquals(IntentType.SHOT_RECOMMENDATION, (shotResult as ClassificationResult.Route).intent.intentType)

        // Bailout query
        coEvery { mockLLMClient.classify("Where's the bailout?", any()) } returns LLMResponse(
            """{"intent_type": "BAILOUT_QUERY", "confidence": 0.90, "entities": {}}""",
            1000, "gemini-3-flash"
        )
        val bailoutResult = classifier.classify("Where's the bailout?")
        assertTrue(bailoutResult is ClassificationResult.Route)
        assertEquals(IntentType.BAILOUT_QUERY, (bailoutResult as ClassificationResult.Route).intent.intentType)

        // Weather check
        coEvery { mockLLMClient.classify("How's the wind?", any()) } returns LLMResponse(
            """{"intent_type": "WEATHER_CHECK", "confidence": 0.90, "entities": {}}""",
            1000, "gemini-3-flash"
        )
        val weatherResult = classifier.classify("How's the wind?")
        assertTrue(weatherResult is ClassificationResult.Route)
        assertEquals(IntentType.WEATHER_CHECK, (weatherResult as ClassificationResult.Route).intent.intentType)

        // Readiness check
        coEvery { mockLLMClient.classify("How am I feeling?", any()) } returns LLMResponse(
            """{"intent_type": "READINESS_CHECK", "confidence": 0.90, "entities": {}}""",
            1000, "gemini-3-flash"
        )
        val readinessResult = classifier.classify("How am I feeling?")
        assertTrue(readinessResult is ClassificationResult.Route)
        assertEquals(IntentType.READINESS_CHECK, (readinessResult as ClassificationResult.Route).intent.intentType)
    }
}
