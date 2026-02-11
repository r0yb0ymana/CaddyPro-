package caddypro.domain.navcaddy.persona

/**
 * Persona guardrails for forbidden pattern detection.
 *
 * Detects responses that violate persona rules and adds appropriate disclaimers.
 * Ensures the assistant never provides unsafe advice or makes inappropriate claims.
 *
 * Spec reference: navcaddy-engine.md R4 (Persona guardrails)
 * Plan reference: navcaddy-engine-plan.md Task 16
 */
object PersonaGuardrails {

    /**
     * Result of guardrail check.
     *
     * @property needsDisclaimer Whether a disclaimer should be added
     * @property disclaimerType Type of disclaimer needed (if any)
     * @property violatedRule Description of the violated rule (if any)
     */
    data class GuardrailResult(
        val needsDisclaimer: Boolean = false,
        val disclaimerType: DisclaimerType? = null,
        val violatedRule: String? = null
    )

    /**
     * Types of disclaimers that can be added to responses.
     */
    enum class DisclaimerType {
        /** Medical or physical health related disclaimer */
        MEDICAL,

        /** Swing technique or coaching disclaimer */
        SWING_TECHNIQUE,

        /** Betting or gambling disclaimer */
        BETTING,

        /** General safety disclaimer */
        SAFETY
    }

    // ========================================================================
    // Forbidden Patterns
    // ========================================================================

    /**
     * Medical-related patterns that require disclaimers.
     *
     * Detects when response discusses pain, injury, or physical conditions.
     */
    private val medicalPatterns = listOf(
        Regex("""\b(pain|injury|hurt|strain|sprain|tear|inflammation)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(doctor|physician|physical therapy|medical)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(diagnose|diagnosis|treatment|heal|recovery time)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(back pain|knee pain|shoulder pain|elbow pain|wrist pain)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(tendonitis|arthritis|nerve|muscle damage)\b""", RegexOption.IGNORE_CASE)
    )

    /**
     * Swing technique patterns that require disclaimers.
     *
     * Detects when response provides specific swing instruction.
     */
    private val swingTechniquePatterns = listOf(
        Regex("""\b(swing path|swing plane|club face|impact position)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(grip pressure|grip change|stance width|ball position)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(weight shift|hip rotation|shoulder turn|backswing)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(wrist hinge|release point|follow through)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bfix your (slice|hook|push|pull)\b""", RegexOption.IGNORE_CASE)
    )

    /**
     * Betting-related patterns that are forbidden.
     *
     * Detects when response discusses gambling or betting.
     */
    private val bettingPatterns = listOf(
        Regex("""\b(bet|wager|gamble|odds|spread)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(gambling|betting line|over under)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bmoney on\b""", RegexOption.IGNORE_CASE)
    )

    /**
     * Absolute guarantee patterns that should be softened.
     *
     * Detects when response makes definitive claims about results.
     */
    private val guaranteePatterns = listOf(
        Regex("""\bwill (fix|cure|eliminate|stop|prevent)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(guaranteed|definitely|certainly) (fix|improve|solve)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bthis will\b""", RegexOption.IGNORE_CASE),
        Regex("""\byou['']ll never\b""", RegexOption.IGNORE_CASE)
    )

    // ========================================================================
    // Guardrail Checks
    // ========================================================================

    /**
     * Check response for guardrail violations.
     *
     * Examines the response text for patterns that require disclaimers
     * or indicate violation of persona rules.
     *
     * @param response The LLM response text to check
     * @return GuardrailResult indicating any violations and needed disclaimers
     */
    fun checkResponse(response: String): GuardrailResult {
        // Check medical patterns
        if (containsPattern(response, medicalPatterns)) {
            return GuardrailResult(
                needsDisclaimer = true,
                disclaimerType = DisclaimerType.MEDICAL,
                violatedRule = "Response discusses medical or physical health topics"
            )
        }

        // Check betting patterns (strict - should not appear at all)
        if (containsPattern(response, bettingPatterns)) {
            return GuardrailResult(
                needsDisclaimer = true,
                disclaimerType = DisclaimerType.BETTING,
                violatedRule = "Response discusses betting or gambling"
            )
        }

        // Check swing technique patterns
        if (containsPattern(response, swingTechniquePatterns)) {
            return GuardrailResult(
                needsDisclaimer = true,
                disclaimerType = DisclaimerType.SWING_TECHNIQUE,
                violatedRule = "Response provides swing technique advice"
            )
        }

        // Check for absolute guarantees (should be softened)
        if (containsPattern(response, guaranteePatterns)) {
            return GuardrailResult(
                needsDisclaimer = true,
                disclaimerType = DisclaimerType.SAFETY,
                violatedRule = "Response contains absolute guarantees"
            )
        }

        return GuardrailResult()
    }

    /**
     * Get the appropriate disclaimer text for a given type.
     *
     * @param type The type of disclaimer needed
     * @return Disclaimer text to append to the response
     */
    fun getDisclaimer(type: DisclaimerType): String = when (type) {
        DisclaimerType.MEDICAL -> buildString {
            appendLine()
            appendLine()
            append("*Note: This is general information only. ")
            append("For pain, injury concerns, or persistent physical issues, ")
            append("please consult with a qualified medical professional or physical therapist.*")
        }

        DisclaimerType.SWING_TECHNIQUE -> buildString {
            appendLine()
            appendLine()
            append("*Note: This is general guidance. ")
            append("For personalized swing instruction, consider working with a certified golf professional.*")
        }

        DisclaimerType.BETTING -> buildString {
            appendLine()
            appendLine()
            append("*Note: CaddyPro does not provide betting or gambling advice. ")
            append("Please bet responsibly and within your means.*")
        }

        DisclaimerType.SAFETY -> buildString {
            appendLine()
            appendLine()
            append("*Note: Results may vary. ")
            append("These are suggestions based on patterns, not guaranteed outcomes.*")
        }
    }

    /**
     * Check if text contains any pattern from the list.
     *
     * @param text Text to check
     * @param patterns List of regex patterns to match
     * @return True if any pattern matches
     */
    private fun containsPattern(text: String, patterns: List<Regex>): Boolean {
        return patterns.any { pattern -> pattern.containsMatchIn(text) }
    }

    /**
     * Check if response contains forbidden phrases.
     *
     * Friendly Expert persona has fewer forbidden phrases, but we still
     * want to catch problematic language.
     *
     * @param response Response text to check
     * @return List of forbidden phrases found (empty if none)
     */
    fun checkForbiddenPhrases(response: String): List<String> {
        val forbiddenPhrases = listOf(
            "as an AI",
            "as a language model",
            "I cannot diagnose",
            "I am not a doctor",
            "bet on",
            "place a wager"
        )

        return forbiddenPhrases.filter { phrase ->
            response.contains(phrase, ignoreCase = true)
        }
    }

    /**
     * Check if response is overly generic or lacks golf context.
     *
     * Detects generic AI assistant language that doesn't fit the Bones persona.
     *
     * @param response Response text to check
     * @return True if response seems too generic
     */
    fun isOverlyGeneric(response: String): Boolean {
        val genericPatterns = listOf(
            Regex("""\bI am here to help\b""", RegexOption.IGNORE_CASE),
            Regex("""\bI can assist you with\b""", RegexOption.IGNORE_CASE),
            Regex("""\bLet me know if you need\b""", RegexOption.IGNORE_CASE),
            Regex("""\bFeel free to ask\b""", RegexOption.IGNORE_CASE)
        )

        // Generic if it has multiple generic patterns and no golf-specific terms
        val genericCount = genericPatterns.count { it.containsMatchIn(response) }
        val hasGolfTerms = response.contains(Regex("""\b(club|shot|hole|green|fairway|swing)\b""", RegexOption.IGNORE_CASE))

        return genericCount >= 2 && !hasGolfTerms
    }
}
