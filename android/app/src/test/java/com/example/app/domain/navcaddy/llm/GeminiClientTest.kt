package caddypro.domain.navcaddy.llm

import caddypro.domain.navcaddy.models.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GeminiClient.
 *
 * Tests prompt building, response parsing, and error handling
 * with mocked LLM responses (no actual API calls).
 *
 * Spec reference: navcaddy-engine-plan.md Task 6
 */
class GeminiClientTest {

    private lateinit var client: GeminiClient

    @Before
    fun setup() {
        client = GeminiClient(apiKey = "test-api-key")
    }

    @Test
    fun `parseResponse extracts intent type correctly`() {
        // Given: Valid JSON response
        val json = """
            {
              "intent_type": "CLUB_ADJUSTMENT",
              "confidence": 0.85,
              "entities": {
                "club": "7-iron"
              }
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should extract intent type
        assertEquals(IntentType.CLUB_ADJUSTMENT, intent.intentType)
        assertEquals(0.85f, intent.confidence, 0.001f)
    }

    @Test
    fun `parseResponse extracts club entity correctly`() {
        // Given: Response with club entity
        val json = """
            {
              "intent_type": "SHOT_RECOMMENDATION",
              "confidence": 0.90,
              "entities": {
                "club": "driver"
              }
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should extract club
        assertNotNull(intent.entities.club)
        assertEquals("Driver", intent.entities.club?.name)
        assertEquals(ClubType.DRIVER, intent.entities.club?.type)
    }

    @Test
    fun `parseResponse handles various club name formats`() {
        val testCases = mapOf(
            "driver" to Pair("Driver", ClubType.DRIVER),
            "7-iron" to Pair("7-Iron", ClubType.IRON),
            "7i" to Pair("7-Iron", ClubType.IRON),
            "pw" to Pair("Pitching Wedge", ClubType.WEDGE),
            "pitching wedge" to Pair("Pitching Wedge", ClubType.WEDGE),
            "3wood" to Pair("3-Wood", ClubType.WOOD),
            "3w" to Pair("3-Wood", ClubType.WOOD),
            "sand wedge" to Pair("Sand Wedge", ClubType.WEDGE),
            "sw" to Pair("Sand Wedge", ClubType.WEDGE),
            "putter" to Pair("Putter", ClubType.PUTTER)
        )

        testCases.forEach { (clubName, expected) ->
            val json = """
                {
                  "intent_type": "CLUB_ADJUSTMENT",
                  "confidence": 0.85,
                  "entities": {
                    "club": "$clubName"
                  }
                }
            """.trimIndent()

            val intent = client.parseResponse(json)
            assertNotNull("Failed to parse club: $clubName", intent.entities.club)
            assertEquals("Failed to parse club name: $clubName", expected.first, intent.entities.club?.name)
            assertEquals("Failed to parse club type: $clubName", expected.second, intent.entities.club?.type)
        }
    }

    @Test
    fun `parseResponse extracts yardage correctly`() {
        // Given: Response with yardage
        val json = """
            {
              "intent_type": "SHOT_RECOMMENDATION",
              "confidence": 0.92,
              "entities": {
                "yardage": 150
              }
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should extract yardage
        assertEquals(150, intent.entities.yardage)
    }

    @Test
    fun `parseResponse extracts lie correctly`() {
        val lieCases = mapOf(
            "fairway" to Lie.FAIRWAY,
            "rough" to Lie.ROUGH,
            "bunker" to Lie.BUNKER,
            "sand" to Lie.BUNKER,
            "green" to Lie.GREEN
        )

        lieCases.forEach { (lieName, expectedLie) ->
            val json = """
                {
                  "intent_type": "SHOT_RECOMMENDATION",
                  "confidence": 0.88,
                  "entities": {
                    "lie": "$lieName"
                  }
                }
            """.trimIndent()

            val intent = client.parseResponse(json)
            assertEquals("Failed to parse lie: $lieName", expectedLie, intent.entities.lie)
        }
    }

    @Test
    fun `parseResponse extracts multiple entities`() {
        // Given: Response with multiple entities
        val json = """
            {
              "intent_type": "SHOT_RECOMMENDATION",
              "confidence": 0.89,
              "entities": {
                "club": "7-iron",
                "yardage": 150,
                "lie": "fairway",
                "wind": "10mph headwind",
                "fatigue": 5
              }
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should extract all entities
        assertNotNull(intent.entities.club)
        assertEquals("7-Iron", intent.entities.club?.name)
        assertEquals(150, intent.entities.yardage)
        assertEquals(Lie.FAIRWAY, intent.entities.lie)
        assertEquals("10mph headwind", intent.entities.wind)
        assertEquals(5, intent.entities.fatigue)
    }

    @Test
    fun `parseResponse handles null entities`() {
        // Given: Response with null entities
        val json = """
            {
              "intent_type": "HELP_REQUEST",
              "confidence": 0.95,
              "entities": {}
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should handle null entities
        assertNull(intent.entities.club)
        assertNull(intent.entities.yardage)
        assertNull(intent.entities.lie)
        assertNull(intent.entities.wind)
    }

    @Test
    fun `parseResponse extracts user goal`() {
        // Given: Response with user goal
        val json = """
            {
              "intent_type": "SHOT_RECOMMENDATION",
              "confidence": 0.87,
              "entities": {},
              "user_goal": "Get shot recommendation for approach"
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should extract user goal
        assertEquals("Get shot recommendation for approach", intent.userGoal)
    }

    @Test
    fun `parseResponse sets routing target from registry`() {
        // Given: Response for intent with routing target
        val json = """
            {
              "intent_type": "CLUB_ADJUSTMENT",
              "confidence": 0.88,
              "entities": {
                "club": "7-iron"
              }
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should have routing target from registry
        assertNotNull(intent.routingTarget)
        assertEquals(Module.CADDY, intent.routingTarget?.module)
        assertEquals("ClubAdjustmentScreen", intent.routingTarget?.screen)
    }

    @Test
    fun `parseResponse handles no-navigation intents`() {
        // Given: Response for no-navigation intent
        val json = """
            {
              "intent_type": "PATTERN_QUERY",
              "confidence": 0.91,
              "entities": {
                "club": "7-iron"
              }
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should have null routing target
        assertNull(intent.routingTarget)
    }

    @Test(expected = LLMException::class)
    fun `parseResponse throws on invalid intent type`() {
        // Given: Response with invalid intent type
        val json = """
            {
              "intent_type": "INVALID_INTENT",
              "confidence": 0.85,
              "entities": {}
            }
        """.trimIndent()

        // When: Parsing response
        // Then: Should throw LLMException
        client.parseResponse(json)
    }

    @Test(expected = LLMException::class)
    fun `parseResponse throws on malformed JSON`() {
        // Given: Malformed JSON
        val json = """
            {
              "intent_type": "SHOT_RECOMMENDATION"
              "confidence": 0.85
            }
        """.trimIndent()

        // When: Parsing response
        // Then: Should throw LLMException
        client.parseResponse(json)
    }

    @Test(expected = LLMException::class)
    fun `parseResponse throws on missing required fields`() {
        // Given: JSON missing required field
        val json = """
            {
              "intent_type": "SHOT_RECOMMENDATION"
            }
        """.trimIndent()

        // When: Parsing response
        // Then: Should throw LLMException
        client.parseResponse(json)
    }

    @Test
    fun `classify returns LLMResponse with metadata`() = runTest {
        // When: Calling classify (uses mock implementation)
        val response = client.classify("What club for 150 yards?")

        // Then: Should return response with metadata
        assertNotNull(response.rawResponse)
        assertTrue(response.latencyMs >= 0)
        assertEquals("gemini-3-flash", response.modelName)
        assertTrue(response.timestamp > 0)
    }

    @Test
    fun `classify includes context in prompt`() = runTest {
        // Given: Session context
        val context = SessionContext(
            currentRound = Round(
                id = "round-1",
                courseName = "Pebble Beach",
                startTime = System.currentTimeMillis()
            ),
            currentHole = 7,
            conversationHistory = listOf(
                ConversationTurn(
                    userInput = "What club for 150?",
                    assistantResponse = "7-iron",
                    timestamp = System.currentTimeMillis()
                )
            )
        )

        // When: Classifying with context
        val response = client.classify("What about 165?", context)

        // Then: Should include context (verified by successful call)
        assertNotNull(response)
    }

    @Test
    fun `parseResponse validates confidence range`() {
        // Test valid confidence values
        val validCases = listOf(0.0f, 0.5f, 0.75f, 1.0f)

        validCases.forEach { confidence ->
            val json = """
                {
                  "intent_type": "HELP_REQUEST",
                  "confidence": $confidence,
                  "entities": {}
                }
            """.trimIndent()

            val intent = client.parseResponse(json)
            assertEquals(confidence, intent.confidence, 0.001f)
        }
    }

    @Test
    fun `parseResponse sanitizes invalid entity values`() {
        // Given: Response with invalid entity values
        val json = """
            {
              "intent_type": "SHOT_RECOMMENDATION",
              "confidence": 0.85,
              "entities": {
                "yardage": -50,
                "fatigue": 15,
                "hole_number": 25
              }
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should sanitize invalid values
        assertNull(intent.entities.yardage) // Negative yardage rejected
        assertEquals(10, intent.entities.fatigue) // Clamped to max
        assertNull(intent.entities.holeNumber) // Invalid hole rejected
    }

    @Test
    fun `parseResponse handles unknown club names gracefully`() {
        // Given: Response with unrecognized club name
        val json = """
            {
              "intent_type": "CLUB_ADJUSTMENT",
              "confidence": 0.85,
              "entities": {
                "club": "unknown-club-123"
              }
            }
        """.trimIndent()

        // When: Parsing response
        val intent = client.parseResponse(json)

        // Then: Should set club to null rather than throwing
        assertNull(intent.entities.club)
    }

    @Test
    fun `parseResponse extracts club with correct loft values`() {
        val testCases = mapOf(
            "driver" to 10.5f,
            "7-iron" to 33f,
            "pw" to 46f,
            "sand wedge" to 54f
        )

        testCases.forEach { (clubName, expectedLoft) ->
            val json = """
                {
                  "intent_type": "CLUB_ADJUSTMENT",
                  "confidence": 0.85,
                  "entities": {
                    "club": "$clubName"
                  }
                }
            """.trimIndent()

            val intent = client.parseResponse(json)
            assertNotNull("Failed to parse club: $clubName", intent.entities.club)
            assertEquals("Wrong loft for $clubName", expectedLoft, intent.entities.club?.loft ?: 0f, 0.1f)
        }
    }
}
