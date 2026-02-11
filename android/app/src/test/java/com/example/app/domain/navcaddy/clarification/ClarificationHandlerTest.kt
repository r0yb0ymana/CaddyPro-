package caddypro.domain.navcaddy.clarification

import caddypro.domain.navcaddy.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ClarificationHandler.
 *
 * Tests cover:
 * - Ambiguous input generates clarification
 * - Max 3 suggestions enforced
 * - Contextually relevant suggestions returned
 * - User-friendly messages generated
 *
 * Spec reference: navcaddy-engine.md A3, navcaddy-engine-plan.md Task 8
 */
class ClarificationHandlerTest {

    private lateinit var handler: ClarificationHandler

    @Before
    fun setup() {
        handler = ClarificationHandler()
    }

    // Test: Ambiguous input generates clarification
    @Test
    fun `ambiguous input generates clarification with suggestions`() {
        // GIVEN: Ambiguous user input
        val input = "It feels off today"

        // WHEN: Generate clarification
        val response = handler.generateClarification(input, null)

        // THEN: Response contains message and suggestions
        assertNotNull(response.message)
        assertTrue(response.message.isNotBlank())
        assertTrue(response.suggestions.isNotEmpty())
        assertEquals(input, response.originalInput)
    }

    // Test: Max 3 suggestions enforced
    @Test
    fun `clarification response has maximum 3 suggestions`() {
        // GIVEN: Various ambiguous inputs
        val inputs = listOf(
            "It feels off today",
            "Help me with my game",
            "What should I do?",
            "I need help",
            "Something is wrong"
        )

        inputs.forEach { input ->
            // WHEN: Generate clarification
            val response = handler.generateClarification(input, null)

            // THEN: Max 3 suggestions returned
            assertTrue(
                "Expected max 3 suggestions for input '$input', got ${response.suggestions.size}",
                response.suggestions.size <= 3
            )
        }
    }

    // Test: At least 1 suggestion always provided
    @Test
    fun `clarification response always has at least 1 suggestion`() {
        // GIVEN: Various ambiguous inputs
        val inputs = listOf(
            "xyz",
            "???",
            "hmm",
            "ugh",
            "whatever"
        )

        inputs.forEach { input ->
            // WHEN: Generate clarification
            val response = handler.generateClarification(input, null)

            // THEN: At least 1 suggestion returned
            assertTrue(
                "Expected at least 1 suggestion for input '$input'",
                response.suggestions.isNotEmpty()
            )
        }
    }

    // Test: Contextually relevant suggestions for "It feels off today"
    @Test
    fun `"It feels off today" suggests club adjustment, pattern query, recovery check`() {
        // GIVEN: Input about something feeling off
        val input = "It feels off today"

        // WHEN: Generate clarification
        val response = handler.generateClarification(input, null)

        // THEN: Suggestions are contextually relevant
        val intentTypes = response.suggestions.map { it.intentType }
        assertTrue(
            "Expected club_adjustment, pattern_query, or recovery_check suggestions",
            intentTypes.any { it in listOf(
                IntentType.CLUB_ADJUSTMENT,
                IntentType.PATTERN_QUERY,
                IntentType.RECOVERY_CHECK
            )}
        )
    }

    // Test: Contextually relevant suggestions for "Help me with my game"
    @Test
    fun `"Help me with my game" suggests shot recommendation, drill request, stats lookup`() {
        // GIVEN: Input asking for help with game
        val input = "Help me with my game"

        // WHEN: Generate clarification
        val response = handler.generateClarification(input, null)

        // THEN: Suggestions include advice-related intents
        val intentTypes = response.suggestions.map { it.intentType }
        assertTrue(
            "Expected shot_recommendation, drill_request, or stats_lookup suggestions",
            intentTypes.any { it in listOf(
                IntentType.SHOT_RECOMMENDATION,
                IntentType.DRILL_REQUEST,
                IntentType.STATS_LOOKUP,
                IntentType.HELP_REQUEST
            )}
        )
    }

    // Test: Contextually relevant suggestions for "What should I do?"
    @Test
    fun `"What should I do?" suggests shot recommendation, help request, weather check`() {
        // GIVEN: General question about what to do
        val input = "What should I do?"

        // WHEN: Generate clarification
        val response = handler.generateClarification(input, null)

        // THEN: Suggestions include action-oriented intents
        val intentTypes = response.suggestions.map { it.intentType }
        assertTrue(
            "Expected shot_recommendation, help_request, or other action intents",
            intentTypes.any { it in listOf(
                IntentType.SHOT_RECOMMENDATION,
                IntentType.HELP_REQUEST
            )}
        )
    }

    // Test: User-friendly messages generated
    @Test
    fun `clarification messages are user-friendly and contextual`() {
        // GIVEN: Different types of ambiguous inputs
        val testCases = mapOf(
            "It feels off today" to "feel",
            "Something is wrong" to "problem",
            "Help me" to "help",
            "???" to "sure"
        )

        testCases.forEach { (input, expectedKeyword) ->
            // WHEN: Generate clarification
            val response = handler.generateClarification(input, null)

            // THEN: Message is friendly and contextual
            val messageLower = response.message.lowercase()
            assertFalse(
                "Message should not be empty for input '$input'",
                response.message.isBlank()
            )
            assertTrue(
                "Message should contain clarification intent for input '$input'",
                messageLower.contains("sure") ||
                messageLower.contains("clarify") ||
                messageLower.contains("mean") ||
                messageLower.contains("did you") ||
                messageLower.contains("looking") ||
                messageLower.contains("want")
            )
        }
    }

    // Test: Suggestion labels are user-friendly
    @Test
    fun `suggestion labels are short and actionable`() {
        // GIVEN: Ambiguous input
        val input = "Help me"

        // WHEN: Generate clarification
        val response = handler.generateClarification(input, null)

        // THEN: Labels are concise and user-friendly
        response.suggestions.forEach { suggestion ->
            assertTrue(
                "Label should not be empty",
                suggestion.label.isNotBlank()
            )
            assertTrue(
                "Label should be concise (<30 chars), got '${suggestion.label}'",
                suggestion.label.length < 30
            )
            assertTrue(
                "Description should not be empty",
                suggestion.description.isNotBlank()
            )
        }
    }

    // Test: Parsed intent with medium-low confidence is included
    @Test
    fun `parsed intent with confidence 0_30-0_49 is included in suggestions`() {
        // GIVEN: Input with a parsed intent at medium-low confidence
        val input = "I want to adjust something"
        val parsedIntent = ParsedIntent(
            intentType = IntentType.CLUB_ADJUSTMENT,
            confidence = 0.35f,
            entities = ExtractedEntities(),
            routingTarget = null,
            userGoal = null,
            responseStyle = null
        )

        // WHEN: Generate clarification
        val response = handler.generateClarification(input, parsedIntent)

        // THEN: Parsed intent is included in suggestions
        val intentTypes = response.suggestions.map { it.intentType }
        assertTrue(
            "Expected CLUB_ADJUSTMENT to be included",
            IntentType.CLUB_ADJUSTMENT in intentTypes
        )
    }

    // Test: Parsed intent with very low confidence may be excluded
    @Test
    fun `parsed intent with confidence below 0_30 may not be first suggestion`() {
        // GIVEN: Input with a parsed intent at very low confidence
        val input = "xyz"
        val parsedIntent = ParsedIntent(
            intentType = IntentType.FEEDBACK,
            confidence = 0.15f,
            entities = ExtractedEntities(),
            routingTarget = null,
            userGoal = null,
            responseStyle = null
        )

        // WHEN: Generate clarification
        val response = handler.generateClarification(input, parsedIntent)

        // THEN: Suggestions are provided (may or may not include FEEDBACK)
        assertTrue("Should have suggestions", response.suggestions.isNotEmpty())
        // No strict assertion on whether FEEDBACK is included since confidence is very low
    }

    // Test: Physical/recovery keywords trigger relevant suggestions
    @Test
    fun `input with physical keywords suggests recovery-related intents`() {
        // GIVEN: Input mentioning physical state
        val inputs = listOf("I feel tired", "My back is sore", "Am I ready to play?")

        inputs.forEach { input ->
            // WHEN: Generate clarification
            val response = handler.generateClarification(input, null)

            // THEN: Suggestions include recovery-related intents
            val intentTypes = response.suggestions.map { it.intentType }
            assertTrue(
                "Expected recovery-related suggestions for '$input'",
                intentTypes.any { it in listOf(
                    IntentType.RECOVERY_CHECK,
                    IntentType.PATTERN_QUERY,
                    IntentType.STATS_LOOKUP
                )}
            )
        }
    }

    // Test: Equipment keywords trigger relevant suggestions
    @Test
    fun `input with equipment keywords suggests club-related intents`() {
        // GIVEN: Input mentioning equipment
        val inputs = listOf("My clubs feel weird", "What's in my bag?", "Change club distance")

        inputs.forEach { input ->
            // WHEN: Generate clarification
            val response = handler.generateClarification(input, null)

            // THEN: Suggestions include equipment-related intents
            val intentTypes = response.suggestions.map { it.intentType }
            assertTrue(
                "Expected equipment-related suggestions for '$input'",
                intentTypes.any { it in listOf(
                    IntentType.CLUB_ADJUSTMENT,
                    IntentType.EQUIPMENT_INFO,
                    IntentType.STATS_LOOKUP
                )}
            )
        }
    }

    // Test: Score/round keywords trigger relevant suggestions
    @Test
    fun `input with score keywords suggests scoring-related intents`() {
        // GIVEN: Input mentioning score or round
        val inputs = listOf("Enter my score", "Start playing", "How's my round going?")

        inputs.forEach { input ->
            // WHEN: Generate clarification
            val response = handler.generateClarification(input, null)

            // THEN: Suggestions include scoring-related intents
            val intentTypes = response.suggestions.map { it.intentType }
            assertTrue(
                "Expected scoring-related suggestions for '$input'",
                intentTypes.any { it in listOf(
                    IntentType.SCORE_ENTRY,
                    IntentType.ROUND_START,
                    IntentType.STATS_LOOKUP
                )}
            )
        }
    }

    // Test: All suggestion labels are unique and follow expected patterns
    @Test
    fun `all intent types have proper suggestion labels`() {
        // GIVEN: All intent types
        val allIntentTypes = IntentType.values()

        allIntentTypes.forEach { intentType ->
            // WHEN: Create suggestion for intent type
            val input = "test"
            val parsedIntent = ParsedIntent(
                intentType = intentType,
                confidence = 0.40f,
                entities = ExtractedEntities(),
                routingTarget = null,
                userGoal = null,
                responseStyle = null
            )
            val response = handler.generateClarification(input, parsedIntent)

            // THEN: Label is defined and reasonable
            val suggestion = response.suggestions.find { it.intentType == intentType }
            if (suggestion != null) {
                assertNotNull("Label should exist for $intentType", suggestion.label)
                assertTrue(
                    "Label should be concise for $intentType",
                    suggestion.label.length in 5..25
                )
            }
        }
    }

    // Test: ClarificationResponse validation
    @Test
    fun `ClarificationResponse validates constraints`() {
        // GIVEN: Valid suggestion
        val validSuggestions = listOf(
            IntentSuggestion(
                intentType = IntentType.SHOT_RECOMMENDATION,
                label = "Get Shot Advice",
                description = "Get shot advice based on current situation"
            )
        )

        // WHEN: Create response with valid data
        val validResponse = ClarificationResponse(
            message = "What do you need?",
            suggestions = validSuggestions,
            originalInput = "help"
        )

        // THEN: Response is created successfully
        assertEquals("What do you need?", validResponse.message)
        assertEquals(1, validResponse.suggestions.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ClarificationResponse rejects too many suggestions`() {
        // GIVEN: More than 3 suggestions
        val tooManySuggestions = listOf(
            IntentSuggestion(IntentType.SHOT_RECOMMENDATION, "Label 1", "Desc 1"),
            IntentSuggestion(IntentType.CLUB_ADJUSTMENT, "Label 2", "Desc 2"),
            IntentSuggestion(IntentType.RECOVERY_CHECK, "Label 3", "Desc 3"),
            IntentSuggestion(IntentType.DRILL_REQUEST, "Label 4", "Desc 4")
        )

        // WHEN/THEN: Creating response throws exception
        ClarificationResponse(
            message = "Test",
            suggestions = tooManySuggestions,
            originalInput = "test"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ClarificationResponse rejects empty suggestions`() {
        // GIVEN: Empty suggestions list
        val emptySuggestions = emptyList<IntentSuggestion>()

        // WHEN/THEN: Creating response throws exception
        ClarificationResponse(
            message = "Test",
            suggestions = emptySuggestions,
            originalInput = "test"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ClarificationResponse rejects blank message`() {
        // GIVEN: Blank message
        val validSuggestions = listOf(
            IntentSuggestion(IntentType.HELP_REQUEST, "Help", "Get help")
        )

        // WHEN/THEN: Creating response throws exception
        ClarificationResponse(
            message = "   ",
            suggestions = validSuggestions,
            originalInput = "test"
        )
    }

    // Test: IntentSuggestion validation
    @Test(expected = IllegalArgumentException::class)
    fun `IntentSuggestion rejects blank label`() {
        // WHEN/THEN: Creating suggestion with blank label throws exception
        IntentSuggestion(
            intentType = IntentType.HELP_REQUEST,
            label = "",
            description = "Valid description"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `IntentSuggestion rejects blank description`() {
        // WHEN/THEN: Creating suggestion with blank description throws exception
        IntentSuggestion(
            intentType = IntentType.HELP_REQUEST,
            label = "Valid label",
            description = ""
        )
    }

    // Test: Keyword matching works correctly
    @Test
    fun `keyword matching identifies relevant intents from example phrases`() {
        // GIVEN: Input that matches specific intent example phrases
        val testCases = mapOf(
            "adjust my driver" to IntentType.CLUB_ADJUSTMENT,
            "how's my recovery" to IntentType.RECOVERY_CHECK,
            "what club should I hit" to IntentType.SHOT_RECOMMENDATION,
            "enter my score" to IntentType.SCORE_ENTRY,
            "show my patterns" to IntentType.PATTERN_QUERY
        )

        testCases.forEach { (input, expectedIntent) ->
            // WHEN: Generate clarification
            val response = handler.generateClarification(input, null)

            // THEN: Expected intent is in suggestions
            val intentTypes = response.suggestions.map { it.intentType }
            assertTrue(
                "Expected $expectedIntent in suggestions for '$input', got $intentTypes",
                expectedIntent in intentTypes
            )
        }
    }
}
