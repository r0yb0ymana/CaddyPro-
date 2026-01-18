package caddypro.domain.navcaddy.integration

import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.persona.BonesResponseFormatter
import caddypro.domain.navcaddy.persona.PersonaGuardrails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for persona consistency.
 *
 * Tests the interaction between PersonaGuardrails and BonesResponseFormatter
 * to ensure consistent persona voice and compliance with guardrails across
 * all response types.
 *
 * Task: Task 17 - Write Memory & Persona Integration Tests
 * Spec reference: navcaddy-engine.md R4, Q6
 */
class PersonaConsistencyIntegrationTest {

    private lateinit var formatter: BonesResponseFormatter

    @Before
    fun setup() {
        formatter = BonesResponseFormatter()
    }

    // ========================================================================
    // Forbidden Phrase Detection Tests
    // ========================================================================

    @Test
    fun `forbidden phrases detected across all response types`() {
        val forbiddenResponses = listOf(
            "As an AI assistant, I can help you with that.",
            "As a language model, I don't have personal experience.",
            "I cannot diagnose your injury, but here's some advice.",
            "I am not a doctor, so take this with a grain of salt.",
            "You should bet on making this putt.",
            "I'd place a wager on the driver here."
        )

        forbiddenResponses.forEach { response ->
            val forbiddenPhrases = PersonaGuardrails.checkForbiddenPhrases(response)
            assertTrue(
                "Response should contain forbidden phrases: '$response'",
                forbiddenPhrases.isNotEmpty()
            )
        }
    }

    @Test
    fun `clean responses have no forbidden phrases`() {
        val cleanResponses = listOf(
            "Based on your recent slice pattern, try aiming slightly left.",
            "Your 7-iron tends to go right under pressure. Account for that.",
            "The wind is picking up. Consider one more club.",
            "You've been hitting it straight today. Trust your swing.",
            "That lie looks tough. Maybe take a wedge to play it safe."
        )

        cleanResponses.forEach { response ->
            val forbiddenPhrases = PersonaGuardrails.checkForbiddenPhrases(response)
            assertTrue(
                "Clean response should have no forbidden phrases: '$response'",
                forbiddenPhrases.isEmpty()
            )
        }
    }

    @Test
    fun `formatter removes generic language consistently`() {
        val genericResponses = listOf(
            "As an AI assistant, I recommend using your 7-iron here.",
            "I'm here to help you choose the right club for this shot.",
            "Feel free to ask me about any other clubs in your bag.",
            "Let me know if you need anything else for this round."
        )

        genericResponses.forEach { response ->
            val cleaned = formatter.removeGenericLanguage(response)
            assertFalse(
                "Generic language should be removed: '$cleaned'",
                cleaned.contains("As an AI", ignoreCase = true) ||
                cleaned.contains("I'm here to help", ignoreCase = true) ||
                cleaned.contains("Feel free to ask", ignoreCase = true) ||
                cleaned.contains("Let me know if you need anything else", ignoreCase = true)
            )
        }
    }

    @Test
    fun `overly generic responses detected consistently`() {
        val genericResponse = "I am here to help you with your golf game. " +
                "I can assist you with club selection and strategy. " +
                "Feel free to ask me anything."

        val isGeneric = PersonaGuardrails.isOverlyGeneric(genericResponse)
        assertTrue("Response should be detected as overly generic", isGeneric)

        val specificResponse = "Your 7-iron tends to slice. Try gripping down slightly " +
                "and aiming left to compensate."

        val isSpecific = PersonaGuardrails.isOverlyGeneric(specificResponse)
        assertFalse("Golf-specific response should not be generic", isSpecific)
    }

    // ========================================================================
    // Disclaimer Addition Tests
    // ========================================================================

    @Test
    fun `medical disclaimers added consistently`() {
        val medicalResponses = listOf(
            "That back pain might be from your swing mechanics.",
            "Shoulder injuries are common in golf. You might want to check your rotation.",
            "If you're feeling pain in your elbow, consider taking a break.",
            "Knee strain can affect your weight transfer during the swing."
        )

        medicalResponses.forEach { response ->
            val result = formatter.format(response)
            assertTrue(
                "Medical disclaimer should be added to: '$response'",
                result.disclaimerAdded
            )
            assertEquals(
                "Disclaimer type should be MEDICAL",
                PersonaGuardrails.DisclaimerType.MEDICAL,
                result.disclaimerType
            )
            assertTrue(
                "Formatted response should contain disclaimer text",
                result.formattedResponse.contains("medical professional")
            )
        }
    }

    @Test
    fun `swing technique disclaimers added consistently`() {
        val techniqueResponses = listOf(
            "Try adjusting your grip pressure to fix that slice.",
            "Your swing path seems too inside-out. Work on the plane.",
            "The club face might be open at impact. Check your wrist hinge.",
            "Your stance width could be affecting your weight shift."
        )

        techniqueResponses.forEach { response ->
            val result = formatter.format(response)
            assertTrue(
                "Swing technique disclaimer should be added to: '$response'",
                result.disclaimerAdded
            )
            assertEquals(
                "Disclaimer type should be SWING_TECHNIQUE",
                PersonaGuardrails.DisclaimerType.SWING_TECHNIQUE,
                result.disclaimerType
            )
            assertTrue(
                "Formatted response should contain professional guidance note",
                result.formattedResponse.contains("golf professional")
            )
        }
    }

    @Test
    fun `betting disclaimers added consistently`() {
        val bettingResponses = listOf(
            "You should bet on sinking this putt.",
            "I'd wager you can reach the green with your driver.",
            "The odds are good for a birdie here."
        )

        bettingResponses.forEach { response ->
            val result = formatter.format(response)
            assertTrue(
                "Betting disclaimer should be added to: '$response'",
                result.disclaimerAdded
            )
            assertEquals(
                "Disclaimer type should be BETTING",
                PersonaGuardrails.DisclaimerType.BETTING,
                result.disclaimerType
            )
        }
    }

    @Test
    fun `guarantee disclaimers added consistently`() {
        val guaranteeResponses = listOf(
            "This will fix your slice permanently.",
            "You'll never hook again if you follow this advice.",
            "This is guaranteed to improve your game.",
            "You will definitely hit the green with this club."
        )

        guaranteeResponses.forEach { response ->
            val result = formatter.format(response)
            assertTrue(
                "Safety disclaimer should be added to guarantees: '$response'",
                result.disclaimerAdded
            )
            assertEquals(
                "Disclaimer type should be SAFETY",
                PersonaGuardrails.DisclaimerType.SAFETY,
                result.disclaimerType
            )
        }
    }

    @Test
    fun `no disclaimer added to clean advice`() {
        val cleanResponses = listOf(
            "Based on your pattern, aim slightly left.",
            "The wind is strong today. Consider one more club.",
            "You've been hitting it well. Trust your swing."
        )

        cleanResponses.forEach { response ->
            val result = formatter.format(response)
            assertFalse(
                "No disclaimer should be added to clean advice: '$response'",
                result.disclaimerAdded
            )
        }
    }

    // ========================================================================
    // Response Formatting Tests
    // ========================================================================

    @Test
    fun `bones voice maintained across response types`() {
        // Test that voice polishing is consistent
        val formalResponses = mapOf(
            "You should utilize your pitching wedge here." to "use",
            "It is approximately 150 yards to the pin." to "about",
            "In order to avoid the hazard, aim left." to "to",
            "It is recommended that you take more club." to "I'd recommend"
        )

        formalResponses.forEach { (formal, expectedWord) ->
            val polished = formatter.applyVoicePolish(formal)
            assertTrue(
                "Formal language should be converted to natural caddy voice: '$polished'",
                polished.contains(expectedWord, ignoreCase = true)
            )
        }
    }

    @Test
    fun `clarification formatting maintains voice`() {
        val ambiguousInput = "What about the 7?"
        val suggestions = listOf(
            "View club information for 7-iron",
            "Get shot recommendation with 7-iron",
            "Check miss patterns for 7-iron"
        )

        val clarification = formatter.formatClarification(ambiguousInput, suggestions)

        // Should use Bones voice
        assertFalse(
            "Should not use overly formal language",
            clarification.contains("Could you please")
        )
        assertTrue(
            "Should use conversational clarification",
            clarification.contains("I want to make sure I understand")
        )
        assertTrue(
            "Should include suggestions",
            suggestions.any { clarification.contains(it) }
        )
    }

    @Test
    fun `error messages maintain voice consistency`() {
        val errorTypes = BonesResponseFormatter.ErrorType.values()

        errorTypes.forEach { errorType ->
            val errorMessage = formatter.formatError(errorType)

            // Should not contain generic AI language
            assertFalse(
                "Error should not contain 'AI assistant': $errorMessage",
                errorMessage.contains("AI assistant", ignoreCase = true)
            )

            // Should be conversational
            assertFalse(
                "Error should not be overly formal: $errorMessage",
                errorMessage.contains("We apologize", ignoreCase = true)
            )

            // Should maintain helpful tone
            assertTrue(
                "Error should provide guidance: $errorMessage",
                errorMessage.isNotEmpty() && errorMessage.length > 10
            )
        }
    }

    // ========================================================================
    // Pattern Reference Formatting Tests
    // ========================================================================

    @Test
    fun `pattern references formatted consistently`() {
        val patterns = listOf(
            MissPattern(
                id = "pattern-1",
                direction = MissDirection.SLICE,
                frequency = 12,
                confidence = 0.75f,
                lastOccurrence = System.currentTimeMillis()
            ),
            MissPattern(
                id = "pattern-2",
                direction = MissDirection.HOOK,
                frequency = 5,
                confidence = 0.62f,
                lastOccurrence = System.currentTimeMillis()
            )
        )

        val response = "Try aiming left based on your tendencies."
        val result = formatter.format(response, patterns, includePatternReferences = true)

        assertTrue("Pattern references should be included", result.patternsReferenced > 0)
        assertTrue(
            "Response should mention patterns",
            result.formattedResponse.contains("Based on your recent patterns")
        )
        assertTrue(
            "Should include confidence percentages",
            result.formattedResponse.contains("%")
        )
    }

    @Test
    fun `pattern references respect confidence threshold`() {
        val lowConfidencePatterns = listOf(
            MissPattern(
                id = "pattern-1",
                direction = MissDirection.SLICE,
                frequency = 3,
                confidence = 0.45f, // Below 0.6 threshold
                lastOccurrence = System.currentTimeMillis()
            )
        )

        val response = "Your shot looks good."
        val result = formatter.format(response, lowConfidencePatterns, includePatternReferences = true)

        assertEquals("Low confidence patterns should not be referenced", 0, result.patternsReferenced)
        assertFalse(
            "Response should not contain pattern info",
            result.formattedResponse.contains("Based on your recent patterns")
        )
    }

    @Test
    fun `pattern references limited to top 2 patterns`() {
        val manyPatterns = listOf(
            MissPattern("1", MissDirection.SLICE, 10, 0.8f, lastOccurrence = System.currentTimeMillis()),
            MissPattern("2", MissDirection.HOOK, 8, 0.75f, lastOccurrence = System.currentTimeMillis()),
            MissPattern("3", MissDirection.PUSH, 6, 0.7f, lastOccurrence = System.currentTimeMillis()),
            MissPattern("4", MissDirection.PULL, 5, 0.65f, lastOccurrence = System.currentTimeMillis())
        )

        val response = "Based on your patterns."
        val result = formatter.format(response, manyPatterns, includePatternReferences = true)

        // Count pattern mentions in formatted response
        val patternMentions = listOf(
            MissDirection.SLICE.name.lowercase(),
            MissDirection.HOOK.name.lowercase(),
            MissDirection.PUSH.name.lowercase(),
            MissDirection.PULL.name.lowercase()
        ).count { result.formattedResponse.contains(it) }

        assertTrue(
            "Should mention at most 2 patterns",
            patternMentions <= 2
        )
    }

    // ========================================================================
    // Cross-Component Consistency Tests
    // ========================================================================

    @Test
    fun `guardrails and formatter work together correctly`() {
        val riskyResponse = "This will fix your back pain and cure your slice for good."

        // First, guardrails should detect issues
        val guardrailResult = PersonaGuardrails.checkResponse(riskyResponse)
        assertTrue("Guardrails should flag response", guardrailResult.needsDisclaimer)
        assertNotNull("Should identify disclaimer type", guardrailResult.disclaimerType)

        // Then, formatter should apply corrections
        val formattedResult = formatter.format(riskyResponse)
        assertTrue("Formatter should add disclaimer", formattedResult.disclaimerAdded)
        assertTrue(
            "Final response should include disclaimer text",
            formattedResult.formattedResponse.length > riskyResponse.length
        )
    }

    @Test
    fun `persona consistency maintained through full pipeline`() {
        val rawLLMResponse = "As an AI assistant, I can help you. I cannot diagnose issues, " +
                "but your swing path seems wrong. This will definitely fix it."

        // Step 1: Remove generic language
        val cleaned = formatter.removeGenericLanguage(rawLLMResponse)
        assertFalse("Generic language removed", cleaned.contains("As an AI"))

        // Step 2: Apply voice polish
        val polished = formatter.applyVoicePolish(cleaned)
        assertFalse("Formal language converted", polished.contains("definitely"))

        // Step 3: Full format with guardrails
        val result = formatter.format(rawLLMResponse)

        // Verify final result maintains persona
        assertFalse(
            "Final response should not contain AI references",
            result.formattedResponse.contains("AI", ignoreCase = true)
        )
        assertTrue(
            "Final response should have disclaimer for technical advice",
            result.disclaimerAdded
        )
    }

    @Test
    fun `all response types maintain consistent tone`() {
        // Test various response types
        val clarification = formatter.formatClarification("unclear", listOf("option1"))
        val error = formatter.formatError(BonesResponseFormatter.ErrorType.NETWORK_ERROR)
        val followUp = formatter.formatFollowUpPrompt()

        val allResponses = listOf(clarification, error, followUp)

        allResponses.forEach { response ->
            // None should contain forbidden phrases
            val forbidden = PersonaGuardrails.checkForbiddenPhrases(response)
            assertTrue(
                "Response should not contain forbidden phrases: '$response'",
                forbidden.isEmpty()
            )

            // None should be overly generic
            val isGeneric = PersonaGuardrails.isOverlyGeneric(response)
            assertFalse(
                "Response should not be overly generic: '$response'",
                isGeneric
            )
        }
    }

    @Test
    fun `confidence qualifiers applied consistently`() {
        val confidenceLevels = listOf(0.4, 0.6, 0.8)

        confidenceLevels.forEach { confidence ->
            val qualifier = formatter.formatConfidenceQualifier(confidence)

            when {
                confidence < 0.5 -> {
                    assertNotNull("Low confidence should have qualifier", qualifier)
                    assertTrue(
                        "Should signal uncertainty",
                        qualifier!!.contains("not entirely sure", ignoreCase = true)
                    )
                }
                confidence < 0.7 -> {
                    assertNotNull("Medium confidence should have qualifier", qualifier)
                    assertTrue(
                        "Should mention limited data",
                        qualifier!!.contains("limited data", ignoreCase = true)
                    )
                }
                else -> {
                    assertEquals("High confidence should have no qualifier", null, qualifier)
                }
            }
        }
    }

    @Test
    fun `pressure context patterns formatted consistently`() {
        val pressurePattern = MissPattern(
            id = "pattern-1",
            direction = MissDirection.SLICE,
            frequency = 8,
            confidence = 0.7f,
            pressureContext = PressureContext(isUserTagged = true, isInferred = false),
            lastOccurrence = System.currentTimeMillis()
        )

        val response = "Watch out for that tendency."
        val result = formatter.format(response, listOf(pressurePattern), includePatternReferences = true)

        assertTrue(
            "Should mention pressure context",
            result.formattedResponse.contains("under pressure", ignoreCase = true)
        )
    }
}
