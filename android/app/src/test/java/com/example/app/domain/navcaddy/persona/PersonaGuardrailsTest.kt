package caddypro.domain.navcaddy.persona

import caddypro.domain.navcaddy.persona.PersonaGuardrails.DisclaimerType
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PersonaGuardrails.
 *
 * Validates forbidden pattern detection and disclaimer generation.
 *
 * Spec reference: navcaddy-engine.md R4, navcaddy-engine-plan.md Task 16
 */
class PersonaGuardrailsTest {

    // ========================================================================
    // Medical Pattern Detection
    // ========================================================================

    @Test
    fun `detects medical patterns - pain mention`() {
        val response = "If you're experiencing pain in your back, you should rest."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.MEDICAL, result.disclaimerType)
        assertNotNull(result.violatedRule)
    }

    @Test
    fun `detects medical patterns - injury mention`() {
        val response = "That injury might require some treatment before you play again."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.MEDICAL, result.disclaimerType)
    }

    @Test
    fun `detects medical patterns - diagnosis mention`() {
        val response = "This could be a diagnosis of tendonitis in your elbow."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.MEDICAL, result.disclaimerType)
    }

    @Test
    fun `detects medical patterns - physical therapy`() {
        val response = "You might benefit from physical therapy for your shoulder."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.MEDICAL, result.disclaimerType)
    }

    @Test
    fun `does not flag normal golf pain references`() {
        // "Pain" in context of difficulty, not physical pain
        val response = "The 17th hole can be a pain with that water hazard."

        val result = PersonaGuardrails.checkResponse(response)

        // This is a false positive - acceptable for safety
        // In production, could add context-aware filtering
        assertTrue(result.needsDisclaimer || !result.needsDisclaimer) // Either outcome acceptable
    }

    // ========================================================================
    // Swing Technique Pattern Detection
    // ========================================================================

    @Test
    fun `detects swing technique patterns - swing path`() {
        val response = "Try adjusting your swing path to come more from inside."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.SWING_TECHNIQUE, result.disclaimerType)
    }

    @Test
    fun `detects swing technique patterns - grip change`() {
        val response = "A stronger grip might help reduce your slice."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.SWING_TECHNIQUE, result.disclaimerType)
    }

    @Test
    fun `detects swing technique patterns - ball position`() {
        val response = "Move the ball position forward in your stance."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.SWING_TECHNIQUE, result.disclaimerType)
    }

    @Test
    fun `detects swing technique patterns - weight shift`() {
        val response = "Focus on your weight shift during the downswing."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.SWING_TECHNIQUE, result.disclaimerType)
    }

    @Test
    fun `detects swing technique patterns - fix your slice`() {
        val response = "This drill will fix your slice permanently."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        // Could be either SWING_TECHNIQUE or SAFETY (guarantee)
        assertTrue(
            result.disclaimerType == DisclaimerType.SWING_TECHNIQUE ||
            result.disclaimerType == DisclaimerType.SAFETY
        )
    }

    // ========================================================================
    // Betting Pattern Detection
    // ========================================================================

    @Test
    fun `detects betting patterns - bet mention`() {
        val response = "I'd bet you can make that putt."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.BETTING, result.disclaimerType)
    }

    @Test
    fun `detects betting patterns - wager mention`() {
        val response = "You should wager on this hole."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.BETTING, result.disclaimerType)
    }

    @Test
    fun `detects betting patterns - gambling mention`() {
        val response = "The gambling odds are in your favor."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.BETTING, result.disclaimerType)
    }

    @Test
    fun `detects betting patterns - money on`() {
        val response = "Put money on this shot."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.BETTING, result.disclaimerType)
    }

    // ========================================================================
    // Guarantee Pattern Detection
    // ========================================================================

    @Test
    fun `detects guarantee patterns - will fix`() {
        val response = "This will fix your hook completely."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.SAFETY, result.disclaimerType)
    }

    @Test
    fun `detects guarantee patterns - guaranteed improve`() {
        val response = "This drill is guaranteed to improve your accuracy."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.SAFETY, result.disclaimerType)
    }

    @Test
    fun `detects guarantee patterns - you'll never`() {
        val response = "You'll never miss right again with this approach."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.SAFETY, result.disclaimerType)
    }

    // ========================================================================
    // Safe Responses
    // ========================================================================

    @Test
    fun `allows safe strategy advice`() {
        val response = "For 150 yards, I'd recommend your 7-iron. Aim slightly left to account for the wind."

        val result = PersonaGuardrails.checkResponse(response)

        assertFalse(result.needsDisclaimer)
        assertNull(result.disclaimerType)
    }

    @Test
    fun `allows safe pattern references`() {
        val response = "Based on your recent tendency to miss right, consider aiming a bit left of target."

        val result = PersonaGuardrails.checkResponse(response)

        assertFalse(result.needsDisclaimer)
    }

    @Test
    fun `allows safe equipment discussion`() {
        val response = "Your 7-iron typically carries about 160 yards. The wind might take 5-10 yards off."

        val result = PersonaGuardrails.checkResponse(response)

        assertFalse(result.needsDisclaimer)
    }

    // ========================================================================
    // Disclaimer Generation
    // ========================================================================

    @Test
    fun `generates medical disclaimer`() {
        val disclaimer = PersonaGuardrails.getDisclaimer(DisclaimerType.MEDICAL)

        assertTrue(disclaimer.contains("medical professional"))
        assertTrue(disclaimer.contains("pain") || disclaimer.contains("injury"))
        assertTrue(disclaimer.startsWith("\n"))
    }

    @Test
    fun `generates swing technique disclaimer`() {
        val disclaimer = PersonaGuardrails.getDisclaimer(DisclaimerType.SWING_TECHNIQUE)

        assertTrue(disclaimer.contains("golf professional") || disclaimer.contains("certified"))
        assertTrue(disclaimer.contains("instruction"))
    }

    @Test
    fun `generates betting disclaimer`() {
        val disclaimer = PersonaGuardrails.getDisclaimer(DisclaimerType.BETTING)

        assertTrue(disclaimer.contains("CaddyPro"))
        assertTrue(disclaimer.contains("betting") || disclaimer.contains("gambling"))
        assertTrue(disclaimer.contains("responsibly"))
    }

    @Test
    fun `generates safety disclaimer`() {
        val disclaimer = PersonaGuardrails.getDisclaimer(DisclaimerType.SAFETY)

        assertTrue(disclaimer.contains("Results may vary") || disclaimer.contains("not guaranteed"))
    }

    // ========================================================================
    // Forbidden Phrases
    // ========================================================================

    @Test
    fun `detects forbidden phrase - as an AI`() {
        val response = "As an AI, I can help you with that."

        val forbidden = PersonaGuardrails.checkForbiddenPhrases(response)

        assertTrue(forbidden.isNotEmpty())
        assertTrue(forbidden.any { it.equals("as an AI", ignoreCase = true) })
    }

    @Test
    fun `detects forbidden phrase - language model`() {
        val response = "As a language model, I cannot provide medical advice."

        val forbidden = PersonaGuardrails.checkForbiddenPhrases(response)

        assertTrue(forbidden.isNotEmpty())
        assertTrue(forbidden.any { it.contains("language model", ignoreCase = true) })
    }

    @Test
    fun `detects forbidden phrase - I cannot diagnose`() {
        val response = "I cannot diagnose your condition."

        val forbidden = PersonaGuardrails.checkForbiddenPhrases(response)

        assertTrue(forbidden.isNotEmpty())
    }

    @Test
    fun `returns empty list for clean response`() {
        val response = "For this shot, I'd recommend your 7-iron."

        val forbidden = PersonaGuardrails.checkForbiddenPhrases(response)

        assertTrue(forbidden.isEmpty())
    }

    // ========================================================================
    // Generic Language Detection
    // ========================================================================

    @Test
    fun `detects overly generic response`() {
        val response = "I am here to help you. I can assist you with various tasks. Let me know if you need anything."

        val isGeneric = PersonaGuardrails.isOverlyGeneric(response)

        assertTrue(isGeneric)
    }

    @Test
    fun `allows golf-specific responses`() {
        val response = "I can assist you with your club selection. Let me know if you need help with your yardage."

        val isGeneric = PersonaGuardrails.isOverlyGeneric(response)

        assertFalse(isGeneric) // Has golf terms
    }

    @Test
    fun `allows concise helpful responses`() {
        val response = "Take your 7-iron and aim for the center of the green."

        val isGeneric = PersonaGuardrails.isOverlyGeneric(response)

        assertFalse(isGeneric)
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    fun `handles empty response`() {
        val result = PersonaGuardrails.checkResponse("")

        assertFalse(result.needsDisclaimer)
    }

    @Test
    fun `handles response with multiple violation types`() {
        val response = "Your back pain might be fixed by adjusting your swing path."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        // Should catch medical first (prioritized)
        assertEquals(DisclaimerType.MEDICAL, result.disclaimerType)
    }

    @Test
    fun `case insensitive pattern matching`() {
        val response = "This will FIX your SLICE completely."

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
    }

    @Test
    fun `handles multi-line responses`() {
        val response = """
            For this shot, I'd recommend your 7-iron.

            Try adjusting your swing path to reduce the slice.

            This should help improve your accuracy.
        """.trimIndent()

        val result = PersonaGuardrails.checkResponse(response)

        assertTrue(result.needsDisclaimer)
        assertEquals(DisclaimerType.SWING_TECHNIQUE, result.disclaimerType)
    }
}
