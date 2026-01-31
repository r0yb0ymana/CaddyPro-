package caddypro.domain.navcaddy.persona

import caddypro.domain.navcaddy.models.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BonesResponseFormatter.
 *
 * Validates response formatting, disclaimer addition, and pattern references.
 *
 * Spec reference: navcaddy-engine.md R4, navcaddy-engine-plan.md Task 16
 */
class BonesResponseFormatterTest {

    private lateinit var formatter: BonesResponseFormatter

    @Before
    fun setUp() {
        formatter = BonesResponseFormatter()
    }

    // ========================================================================
    // Basic Formatting
    // ========================================================================

    @Test
    fun `formats clean response without changes`() {
        val response = "Take your 7-iron and aim for the center of the green."

        val result = formatter.format(response)

        assertEquals(response, result.formattedResponse)
        assertFalse(result.disclaimerAdded)
        assertNull(result.disclaimerType)
        assertEquals(0, result.patternsReferenced)
    }

    @Test
    fun `trims whitespace from response`() {
        val response = "   Take your 7-iron.   \n\n"

        val result = formatter.format(response)

        assertEquals("Take your 7-iron.", result.formattedResponse)
    }

    // ========================================================================
    // Disclaimer Addition
    // ========================================================================

    @Test
    fun `adds medical disclaimer when needed`() {
        val response = "If you're experiencing back pain, rest is important."

        val result = formatter.format(response)

        assertTrue(result.disclaimerAdded)
        assertEquals(PersonaGuardrails.DisclaimerType.MEDICAL, result.disclaimerType)
        assertTrue(result.formattedResponse.contains("medical professional"))
    }

    @Test
    fun `adds swing technique disclaimer when needed`() {
        val response = "Try adjusting your swing path to reduce the slice."

        val result = formatter.format(response)

        assertTrue(result.disclaimerAdded)
        assertEquals(PersonaGuardrails.DisclaimerType.SWING_TECHNIQUE, result.disclaimerType)
        assertTrue(result.formattedResponse.contains("golf professional"))
    }

    @Test
    fun `adds betting disclaimer when needed`() {
        val response = "I'd bet you can make this putt."

        val result = formatter.format(response)

        assertTrue(result.disclaimerAdded)
        assertEquals(PersonaGuardrails.DisclaimerType.BETTING, result.disclaimerType)
        assertTrue(result.formattedResponse.contains("CaddyPro"))
    }

    @Test
    fun `adds safety disclaimer for guarantees`() {
        val response = "This will fix your hook completely."

        val result = formatter.format(response)

        assertTrue(result.disclaimerAdded)
        assertEquals(PersonaGuardrails.DisclaimerType.SAFETY, result.disclaimerType)
        assertTrue(result.formattedResponse.contains("Results may vary"))
    }

    // ========================================================================
    // Pattern References
    // ========================================================================

    @Test
    fun `adds pattern references when relevant`() {
        val response = "Consider aiming left to compensate."
        val patterns = listOf(
            MissPattern(
                direction = MissDirection.PUSH,
                frequency = 8,
                confidence = 0.75f,
                club = Club(name = "7-iron", type = ClubType.IRON, estimatedCarry = 150),
                lastOccurrence = System.currentTimeMillis()
            )
        )

        val result = formatter.format(response, patterns, includePatternReferences = true)

        assertTrue(result.formattedResponse.contains("Based on your recent patterns"))
        assertTrue(result.formattedResponse.contains("push"))
        assertEquals(1, result.patternsReferenced)
    }

    @Test
    fun `excludes low confidence patterns`() {
        val response = "Consider aiming left."
        val patterns = listOf(
            MissPattern(
                direction = MissDirection.PUSH,
                frequency = 2,
                confidence = 0.4f, // Below 0.6 threshold
                lastOccurrence = System.currentTimeMillis()
            )
        )

        val result = formatter.format(response, patterns, includePatternReferences = true)

        assertFalse(result.formattedResponse.contains("Based on your recent patterns"))
        assertEquals(0, result.patternsReferenced)
    }

    @Test
    fun `limits pattern references to top 3`() {
        val response = "Your shot tendencies show some patterns."
        val now = System.currentTimeMillis()
        val patterns = listOf(
            MissPattern(MissDirection.PUSH, 10, 0.9f, null, null, now),
            MissPattern(MissDirection.SLICE, 8, 0.85f, null, null, now),
            MissPattern(MissDirection.PULL, 7, 0.8f, null, null, now),
            MissPattern(MissDirection.HOOK, 5, 0.7f, null, null, now),
            MissPattern(MissDirection.FAT, 3, 0.65f, null, null, now)
        )

        val result = formatter.format(response, patterns, includePatternReferences = true)

        // Should only include top 3
        val patternSection = result.formattedResponse.substringAfter("Based on your recent patterns:")
        val bulletPoints = patternSection.split("-").filter { it.trim().isNotEmpty() }
        assertTrue(bulletPoints.size <= 3)
    }

    @Test
    fun `includes club information in pattern reference`() {
        val response = "Consider club selection."
        val patterns = listOf(
            MissPattern(
                direction = MissDirection.SLICE,
                frequency = 8,
                confidence = 0.8f,
                club = Club(name = "driver", type = ClubType.DRIVER, estimatedCarry = 250),
                lastOccurrence = System.currentTimeMillis()
            )
        )

        val result = formatter.format(response, patterns, includePatternReferences = true)

        assertTrue(result.formattedResponse.contains("with driver"))
    }

    @Test
    fun `includes pressure context in pattern reference`() {
        val response = "Be aware of your tendencies."
        val patterns = listOf(
            MissPattern(
                direction = MissDirection.PUSH,
                frequency = 6,
                confidence = 0.7f,
                club = null,
                pressureContext = PressureContext(isUserTagged = true),
                lastOccurrence = System.currentTimeMillis()
            )
        )

        val result = formatter.format(response, patterns, includePatternReferences = true)

        assertTrue(result.formattedResponse.contains("under pressure"))
    }

    @Test
    fun `respects includePatternReferences flag`() {
        val response = "Take your shot."
        val patterns = listOf(
            MissPattern(
                direction = MissDirection.PUSH,
                frequency = 8,
                confidence = 0.75f,
                lastOccurrence = System.currentTimeMillis()
            )
        )

        val result = formatter.format(response, patterns, includePatternReferences = false)

        assertFalse(result.formattedResponse.contains("Based on your recent patterns"))
        assertEquals(0, result.patternsReferenced)
    }

    @Test
    fun `formats frequency descriptions correctly`() {
        val now = System.currentTimeMillis()
        val highFreq = MissPattern(MissDirection.PUSH, 15, 0.8f, null, null, now)
        val medFreq = MissPattern(MissDirection.SLICE, 7, 0.75f, null, null, now)
        val lowFreq = MissPattern(MissDirection.PULL, 3, 0.7f, null, null, now)

        val result = formatter.format("Test", listOf(highFreq, medFreq, lowFreq), true)

        assertTrue(result.formattedResponse.contains("frequently"))
        assertTrue(result.formattedResponse.contains("occasionally"))
        assertTrue(result.formattedResponse.contains("sometimes"))
    }

    // ========================================================================
    // Clarification Formatting
    // ========================================================================

    @Test
    fun `formats clarification with no suggestions`() {
        val clarification = formatter.formatClarification("it's off", emptyList())

        assertTrue(clarification.contains("I want to make sure I understand"))
        assertTrue(clarification.contains("more details"))
    }

    @Test
    fun `formats clarification with single suggestion`() {
        val clarification = formatter.formatClarification(
            "help with driver",
            listOf("Club distance adjustment")
        )

        assertTrue(clarification.contains("Did you mean"))
        assertTrue(clarification.contains("Club distance adjustment"))
    }

    @Test
    fun `formats clarification with multiple suggestions`() {
        val clarification = formatter.formatClarification(
            "what about my swing",
            listOf("Pattern analysis", "Swing drill", "Club selection")
        )

        assertTrue(clarification.contains("Which of these"))
        assertTrue(clarification.contains("Pattern analysis"))
        assertTrue(clarification.contains("Swing drill"))
        assertTrue(clarification.contains("Club selection"))
    }

    @Test
    fun `limits clarification suggestions to 3`() {
        val suggestions = listOf("Option 1", "Option 2", "Option 3", "Option 4", "Option 5")

        val clarification = formatter.formatClarification("unclear input", suggestions)

        // Should only show first 3
        val lines = clarification.split("\n")
        val bulletLines = lines.count { it.startsWith("-") }
        assertTrue(bulletLines <= 3)
    }

    // ========================================================================
    // Error Formatting
    // ========================================================================

    @Test
    fun `formats network error in Bones voice`() {
        val error = formatter.formatError(BonesResponseFormatter.ErrorType.NETWORK_ERROR)

        assertTrue(error.contains("trouble connecting") || error.contains("connection"))
        assertFalse(error.contains("Error:"))
        assertFalse(error.contains("Exception"))
    }

    @Test
    fun `formats timeout error in Bones voice`() {
        val error = formatter.formatError(BonesResponseFormatter.ErrorType.TIMEOUT)

        assertTrue(error.contains("longer than expected") || error.contains("try again"))
    }

    @Test
    fun `formats unknown intent error in Bones voice`() {
        val error = formatter.formatError(BonesResponseFormatter.ErrorType.UNKNOWN_INTENT)

        assertTrue(error.contains("didn't quite catch") || error.contains("rephrase"))
    }

    @Test
    fun `formats missing data error in Bones voice`() {
        val error = formatter.formatError(BonesResponseFormatter.ErrorType.MISSING_DATA)

        assertTrue(error.contains("more information") || error.contains("details"))
    }

    @Test
    fun `formats invalid input error in Bones voice`() {
        val error = formatter.formatError(BonesResponseFormatter.ErrorType.INVALID_INPUT)

        assertTrue(error.contains("doesn't look quite right") || error.contains("double-check"))
    }

    @Test
    fun `formats service unavailable error in Bones voice`() {
        val error = formatter.formatError(BonesResponseFormatter.ErrorType.SERVICE_UNAVAILABLE)

        assertTrue(error.contains("technical difficulties") || error.contains("try again"))
    }

    @Test
    fun `error messages maintain conversational tone`() {
        BonesResponseFormatter.ErrorType.values().forEach { errorType ->
            val error = formatter.formatError(errorType)

            // Should not contain technical jargon
            assertFalse(error.contains("null pointer", ignoreCase = true))
            assertFalse(error.contains("exception", ignoreCase = true))
            assertFalse(error.contains("stack trace", ignoreCase = true))

            // Should be conversational
            assertTrue(error.isNotBlank())
            assertTrue(error.length > 20)
        }
    }

    // ========================================================================
    // Follow-up Prompts
    // ========================================================================

    @Test
    fun `formats follow-up prompt`() {
        val prompt = formatter.formatFollowUpPrompt()

        assertTrue(prompt.contains("help") || prompt.contains("else"))
        assertTrue(prompt.length < 50) // Should be concise
    }

    // ========================================================================
    // Confidence Qualifiers
    // ========================================================================

    @Test
    fun `adds qualifier for low confidence`() {
        val qualifier = formatter.formatConfidenceQualifier(0.4)

        assertNotNull(qualifier)
        assertTrue(qualifier!!.contains("not entirely sure") || qualifier.contains("uncertain"))
    }

    @Test
    fun `adds qualifier for medium confidence`() {
        val qualifier = formatter.formatConfidenceQualifier(0.6)

        assertNotNull(qualifier)
        assertTrue(qualifier!!.contains("limited data") || qualifier.contains("Based on"))
    }

    @Test
    fun `no qualifier for high confidence`() {
        val qualifier = formatter.formatConfidenceQualifier(0.9)

        assertNull(qualifier)
    }

    @Test
    fun `no qualifier at confidence threshold`() {
        val qualifier = formatter.formatConfidenceQualifier(0.7)

        assertNull(qualifier)
    }

    // ========================================================================
    // Generic Language Removal
    // ========================================================================

    @Test
    fun `removes generic AI language`() {
        val response = "As an AI assistant, I can help you with club selection."

        val cleaned = formatter.removeGenericLanguage(response)

        assertFalse(cleaned.contains("As an AI"))
        assertTrue(cleaned.contains("club selection"))
    }

    @Test
    fun `removes multiple generic phrases`() {
        val response = "I'm here to help you with your golf game. Feel free to ask me anything."

        val cleaned = formatter.removeGenericLanguage(response)

        assertFalse(cleaned.contains("I'm here to help you"))
        assertFalse(cleaned.contains("Feel free to ask"))
    }

    @Test
    fun `preserves golf-specific content`() {
        val response = "For 150 yards, take your 7-iron. Let me know if you need anything else."

        val cleaned = formatter.removeGenericLanguage(response)

        assertTrue(cleaned.contains("7-iron"))
        assertTrue(cleaned.contains("150 yards"))
    }

    // ========================================================================
    // Voice Polishing
    // ========================================================================

    @Test
    fun `replaces formal language with natural language`() {
        val response = "You should utilize your 7-iron in order to reach the green."

        val polished = formatter.applyVoicePolish(response)

        assertTrue(polished.contains("use"))
        assertFalse(polished.contains("utilize"))
        assertTrue(polished.contains("to reach"))
        assertFalse(polished.contains("in order to"))
    }

    @Test
    fun `makes recommendations more natural`() {
        val response = "It is recommended that you take one more club."

        val polished = formatter.applyVoicePolish(response)

        assertTrue(polished.contains("I'd recommend") || polished.contains("recommend"))
        assertFalse(polished.contains("It is recommended that you"))
    }

    @Test
    fun `converts approximately to about`() {
        val response = "The distance is approximately 150 yards."

        val polished = formatter.applyVoicePolish(response)

        assertTrue(polished.contains("about"))
        assertFalse(polished.contains("approximately"))
    }

    // ========================================================================
    // Integration Tests
    // ========================================================================

    @Test
    fun `full formatting with disclaimer and patterns`() {
        val response = "Based on your back pain, adjust your swing path to reduce strain."
        val patterns = listOf(
            MissPattern(
                direction = MissDirection.SLICE,
                frequency = 10,
                confidence = 0.85f,
                club = Club(name = "driver", type = ClubType.DRIVER, estimatedCarry = 250),
                pressureContext = PressureContext(isUserTagged = true),
                lastOccurrence = System.currentTimeMillis()
            )
        )

        val result = formatter.format(response, patterns, includePatternReferences = true)

        // Should have disclaimer
        assertTrue(result.disclaimerAdded)
        assertTrue(result.formattedResponse.contains("medical professional"))

        // Should have pattern reference
        assertTrue(result.formattedResponse.contains("Based on your recent patterns"))
        assertEquals(1, result.patternsReferenced)

        // Should have all sections
        val sections = result.formattedResponse.split("\n\n")
        assertTrue(sections.size >= 3) // Original + patterns + disclaimer
    }

    @Test
    fun `handles empty response gracefully`() {
        val result = formatter.format("")

        assertEquals("", result.formattedResponse)
        assertFalse(result.disclaimerAdded)
    }

    @Test
    fun `handles null patterns gracefully`() {
        val response = "Take your 7-iron."

        val result = formatter.format(response, emptyList(), includePatternReferences = true)

        assertEquals("Take your 7-iron.", result.formattedResponse)
        assertEquals(0, result.patternsReferenced)
    }

    @Test
    fun `maintains response structure with multiple formatting operations`() {
        val response = """
            For this shot, I'd recommend your 7-iron.

            The wind is picking up, so aim slightly left.

            This should help you reach the green.
        """.trimIndent()

        val result = formatter.format(response)

        // Should preserve line breaks
        assertTrue(result.formattedResponse.contains("\n"))

        // Should maintain readability
        val lines = result.formattedResponse.split("\n").filter { it.isNotBlank() }
        assertTrue(lines.size >= 3)
    }
}
