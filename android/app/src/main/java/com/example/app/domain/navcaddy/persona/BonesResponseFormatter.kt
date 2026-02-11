package caddypro.domain.navcaddy.persona

import caddypro.domain.navcaddy.models.MissPattern
import javax.inject.Inject

/**
 * Post-processes LLM responses to apply Bones persona and guardrails.
 *
 * Formats responses for conversational display, adds disclaimers where needed,
 * and includes pattern references when relevant. Ensures all responses maintain
 * the Bones voice and comply with persona rules.
 *
 * Spec reference: navcaddy-engine.md R4 (Bones Persona Layer)
 * Plan reference: navcaddy-engine-plan.md Task 16
 */
class BonesResponseFormatter @Inject constructor() {

    /**
     * Result of response formatting.
     *
     * @property formattedResponse Final formatted response text
     * @property disclaimerAdded Whether a disclaimer was added
     * @property disclaimerType Type of disclaimer added (if any)
     * @property patternsReferenced Number of patterns referenced in response
     */
    data class FormattedResponse(
        val formattedResponse: String,
        val disclaimerAdded: Boolean = false,
        val disclaimerType: PersonaGuardrails.DisclaimerType? = null,
        val patternsReferenced: Int = 0
    )

    /**
     * Format a response with Bones persona post-processing.
     *
     * Applies guardrails, adds disclaimers, and includes pattern references.
     *
     * @param rawResponse Raw LLM response text
     * @param relevantPatterns Miss patterns relevant to the query (optional)
     * @param includePatternReferences Whether to append pattern references
     * @return Formatted response with disclaimers and pattern info
     */
    fun format(
        rawResponse: String,
        relevantPatterns: List<MissPattern> = emptyList(),
        includePatternReferences: Boolean = true
    ): FormattedResponse {
        var formatted = rawResponse.trim()

        // Check for guardrail violations
        val guardrailResult = PersonaGuardrails.checkResponse(formatted)

        // Add disclaimer if needed
        if (guardrailResult.needsDisclaimer && guardrailResult.disclaimerType != null) {
            val disclaimer = PersonaGuardrails.getDisclaimer(guardrailResult.disclaimerType)
            formatted = "$formatted$disclaimer"
        }

        // Add pattern references if relevant and enabled
        var patternsReferenced = 0
        if (includePatternReferences && relevantPatterns.isNotEmpty()) {
            val patternInfo = formatPatternReferences(relevantPatterns)
            if (patternInfo.isNotEmpty()) {
                formatted = "$formatted\n\n$patternInfo"
                patternsReferenced = relevantPatterns.size
            }
        }

        return FormattedResponse(
            formattedResponse = formatted,
            disclaimerAdded = guardrailResult.needsDisclaimer,
            disclaimerType = guardrailResult.disclaimerType,
            patternsReferenced = patternsReferenced
        )
    }

    /**
     * Format pattern references for inclusion in response.
     *
     * Creates a natural language summary of relevant miss patterns.
     *
     * @param patterns List of relevant patterns
     * @return Formatted pattern reference text
     */
    private fun formatPatternReferences(patterns: List<MissPattern>): String {
        if (patterns.isEmpty()) return ""

        // Only include patterns with sufficient confidence
        val significantPatterns = patterns.filter { it.confidence >= 0.6f }
        if (significantPatterns.isEmpty()) return ""

        // Take top 2 patterns max (aligned with iOS)
        val topPatterns = significantPatterns.sortedByDescending { it.confidence }.take(2)

        return buildString {
            appendLine("**Based on your recent patterns:**")
            topPatterns.forEach { pattern ->
                val confidencePercent = (pattern.confidence * 100).toInt()
                val frequencyText = when {
                    pattern.frequency >= 10 -> "frequently"
                    pattern.frequency >= 5 -> "occasionally"
                    else -> "sometimes"
                }

                val contextText = buildString {
                    pattern.club?.let { club ->
                        append(" with ${club.name}")
                    }
                    pattern.pressureContext?.let { pressure ->
                        if (pressure.hasPressure) {
                            append(" under pressure")
                        }
                    }
                }

                appendLine("- ${pattern.direction.name.lowercase()} ($frequencyText$contextText, $confidencePercent% confidence)")
            }
        }.trim()
    }

    /**
     * Format a clarification request with Bones voice.
     *
     * Used when intent confidence is low and clarification is needed.
     *
     * @param ambiguousInput The user's ambiguous input
     * @param suggestedIntents List of possible intent interpretations
     * @return Clarification question in Bones voice
     */
    fun formatClarification(
        ambiguousInput: String,
        suggestedIntents: List<String>
    ): String = buildString {
        appendLine("I want to make sure I understand correctly.")
        appendLine()

        when (suggestedIntents.size) {
            0 -> {
                append("Could you provide more details about what you need help with?")
            }
            1 -> {
                append("Did you mean: ${suggestedIntents[0]}?")
            }
            else -> {
                appendLine("Which of these were you asking about?")
                suggestedIntents.take(3).forEach { intent ->
                    appendLine("- $intent")
                }
            }
        }
    }

    /**
     * Format an error message with Bones voice.
     *
     * Maintains persona even in error states.
     *
     * @param errorType Type of error that occurred
     * @return User-friendly error message
     */
    fun formatError(errorType: ErrorType): String = when (errorType) {
        ErrorType.NETWORK_ERROR ->
            "I'm having trouble connecting right now. Check your connection and let's try again."

        ErrorType.TIMEOUT ->
            "That's taking longer than expected. Let's try again, or I can show you some quick options."

        ErrorType.UNKNOWN_INTENT ->
            "I didn't quite catch that. Could you rephrase what you need help with?"

        ErrorType.MISSING_DATA ->
            "I need a bit more information to help with that. What specific details can you provide?"

        ErrorType.INVALID_INPUT ->
            "Something doesn't look quite right with that input. Can you double-check and try again?"

        ErrorType.SERVICE_UNAVAILABLE ->
            "I'm having technical difficulties at the moment. You can still browse manually or try again in a bit."
    }

    /**
     * Format a follow-up prompt for context continuation.
     *
     * Encourages natural conversation flow.
     *
     * @return Follow-up prompt text
     */
    fun formatFollowUpPrompt(): String =
        "Anything else I can help with?"

    /**
     * Format a confidence qualifier when data is limited.
     *
     * Adds explicit uncertainty signaling per persona rules.
     *
     * @param confidence Confidence level (0.0 - 1.0)
     * @return Qualifier text to prepend to response
     */
    fun formatConfidenceQualifier(confidence: Double): String? = when {
        confidence < 0.5 -> "I'm not entirely sure, but "
        confidence < 0.7 -> "Based on limited data, "
        else -> null // High confidence - no qualifier needed
    }

    /**
     * Remove generic AI language from responses.
     *
     * Strips out filler phrases that don't fit the Bones persona.
     *
     * @param response Response text to clean
     * @return Cleaned response
     */
    fun removeGenericLanguage(response: String): String {
        var cleaned = response

        // Remove common generic phrases
        val genericPhrases = listOf(
            "As an AI assistant, ",
            "As a language model, ",
            "I'm here to help you ",
            "Feel free to ask me ",
            "Let me know if you need anything else",
            "Is there anything else I can help you with?"
        )

        genericPhrases.forEach { phrase ->
            cleaned = cleaned.replace(phrase, "", ignoreCase = true)
        }

        return cleaned.trim()
    }

    /**
     * Apply Bones voice polish to response.
     *
     * Makes minor adjustments to ensure natural caddy tone.
     *
     * @param response Response text to polish
     * @return Polished response
     */
    fun applyVoicePolish(response: String): String {
        var polished = response

        // Replace overly formal language with more natural caddy language
        val replacements = mapOf(
            "utilize" to "use",
            "approximately" to "about",
            "in order to" to "to",
            "it is recommended that you" to "I'd recommend",
            "you should consider" to "consider",
            "it would be beneficial to" to "it'll help to"
        )

        replacements.forEach { (formal, natural) ->
            polished = polished.replace(formal, natural, ignoreCase = true)
        }

        return polished
    }

    /**
     * Types of errors that can occur during conversation.
     */
    enum class ErrorType {
        NETWORK_ERROR,
        TIMEOUT,
        UNKNOWN_INTENT,
        MISSING_DATA,
        INVALID_INPUT,
        SERVICE_UNAVAILABLE
    }
}
