package caddypro.domain.navcaddy.clarification

import caddypro.domain.navcaddy.intent.IntentRegistry
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.ParsedIntent

/**
 * Handles clarification generation for ambiguous user inputs.
 *
 * When intent confidence is below the clarification threshold (< 0.50),
 * this handler generates:
 * 1. Targeted clarification questions (1-2 questions)
 * 2. Up to 3 suggested intents as selectable chips
 * 3. User-friendly labels for each suggestion
 *
 * Spec reference: navcaddy-engine.md R2, G6, A3, navcaddy-engine-plan.md Task 8
 */
class ClarificationHandler {
    /**
     * Generate clarification response for ambiguous input.
     *
     * Algorithm:
     * 1. Analyze input for keywords related to intents
     * 2. Find up to 3 most relevant intents (based on keyword matching)
     * 3. Generate contextual clarification message
     * 4. Create user-friendly chip labels for suggestions
     *
     * @param input Original user input (normalized)
     * @param parsedIntent Low-confidence parsed intent (may provide hints)
     * @return ClarificationResponse with message and suggestions
     */
    fun generateClarification(
        input: String,
        parsedIntent: ParsedIntent?
    ): ClarificationResponse {
        // Find relevant intents based on input keywords and parsed intent
        val suggestedIntents = findRelevantIntents(input, parsedIntent)

        // Generate contextual clarification message
        val message = generateClarificationMessage(input, suggestedIntents)

        // Create intent suggestions with labels and descriptions
        val suggestions = suggestedIntents.map { intentType ->
            createIntentSuggestion(intentType)
        }

        return ClarificationResponse(
            message = message,
            suggestions = suggestions,
            originalInput = input
        )
    }

    /**
     * Find up to 3 relevant intents based on input analysis.
     *
     * Strategy:
     * 1. Start with parsed intent if confidence is not too low (e.g., 0.30-0.49)
     * 2. Match keywords from input against intent example phrases
     * 3. Fall back to common/general intents if no strong matches
     *
     * @param input User input
     * @param parsedIntent Low-confidence parsed intent (optional)
     * @return List of up to 3 relevant intent types
     */
    private fun findRelevantIntents(
        input: String,
        parsedIntent: ParsedIntent?
    ): List<IntentType> {
        val inputLower = input.lowercase()
        val suggestions = mutableListOf<IntentType>()

        // Step 1: Include parsed intent if confidence is not abysmal (>= 0.30)
        if (parsedIntent != null && parsedIntent.confidence >= 0.30f) {
            suggestions.add(parsedIntent.intentType)
        }

        // Step 2: Match keywords against intent schemas
        val keywordMatches = findKeywordMatches(inputLower)
        keywordMatches.forEach { intentType ->
            if (intentType !in suggestions && suggestions.size < 3) {
                suggestions.add(intentType)
            }
        }

        // Step 3: Fill remaining slots with contextual fallbacks
        if (suggestions.size < 3) {
            val fallbacks = getFallbackIntents(inputLower)
            fallbacks.forEach { intentType ->
                if (intentType !in suggestions && suggestions.size < 3) {
                    suggestions.add(intentType)
                }
            }
        }

        // Step 4: If still empty, use common default intents
        if (suggestions.isEmpty()) {
            suggestions.addAll(
                listOf(
                    IntentType.SHOT_RECOMMENDATION,
                    IntentType.HELP_REQUEST,
                    IntentType.CLUB_ADJUSTMENT
                )
            )
        }

        return suggestions.take(3)
    }

    /**
     * Find intents matching keywords in the input.
     *
     * Uses IntentRegistry example phrases to identify potential matches.
     */
    private fun findKeywordMatches(inputLower: String): List<IntentType> {
        val matches = mutableMapOf<IntentType, Int>() // Intent to match score

        IntentRegistry.getAllSchemas().forEach { schema ->
            var score = 0

            // Check if any example phrase keywords appear in input
            schema.examplePhrases.forEach { phrase ->
                val phraseWords = phrase.lowercase().split(" ")
                phraseWords.forEach { word ->
                    if (word.length > 3 && inputLower.contains(word)) {
                        score++
                    }
                }
            }

            if (score > 0) {
                matches[schema.intentType] = score
            }
        }

        // Sort by score descending and return intent types
        return matches.entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    /**
     * Get fallback intents based on input context/sentiment.
     *
     * Uses simple keyword detection to infer user needs.
     */
    private fun getFallbackIntents(inputLower: String): List<IntentType> {
        return when {
            // Physical/recovery related
            containsAny(inputLower, listOf("feel", "pain", "sore", "tired", "ready")) ->
                listOf(IntentType.RECOVERY_CHECK, IntentType.PATTERN_QUERY, IntentType.STATS_LOOKUP)

            // Performance/technique related
            containsAny(inputLower, listOf("off", "wrong", "bad", "problem", "issue", "fix")) ->
                listOf(IntentType.CLUB_ADJUSTMENT, IntentType.PATTERN_QUERY, IntentType.DRILL_REQUEST)

            // Strategy/advice related
            containsAny(inputLower, listOf("what", "should", "help", "advice", "recommend")) ->
                listOf(IntentType.SHOT_RECOMMENDATION, IntentType.HELP_REQUEST, IntentType.DRILL_REQUEST)

            // Equipment/setup related
            containsAny(inputLower, listOf("club", "bag", "equipment", "distance", "yardage")) ->
                listOf(IntentType.CLUB_ADJUSTMENT, IntentType.EQUIPMENT_INFO, IntentType.STATS_LOOKUP)

            // Game/round related
            containsAny(inputLower, listOf("score", "round", "play", "game", "hole")) ->
                listOf(IntentType.SCORE_ENTRY, IntentType.ROUND_START, IntentType.STATS_LOOKUP)

            // General/unclear
            else ->
                listOf(IntentType.SHOT_RECOMMENDATION, IntentType.HELP_REQUEST, IntentType.STATS_LOOKUP)
        }
    }

    /**
     * Check if input contains any of the specified keywords.
     */
    private fun containsAny(input: String, keywords: List<String>): Boolean {
        return keywords.any { input.contains(it) }
    }

    /**
     * Generate contextual clarification message.
     *
     * Creates a friendly message that includes:
     * - Acknowledgment of ambiguity
     * - Targeted clarification question(s)
     * - Invitation to select from suggestions
     */
    private fun generateClarificationMessage(
        input: String,
        suggestedIntents: List<IntentType>
    ): String {
        // Analyze input to generate targeted question
        val inputLower = input.lowercase()

        return when {
            // Very vague inputs
            input.split(" ").size <= 3 ->
                "I'm not quite sure what you need. Did you mean:"

            // Physical/performance mentions
            containsAny(inputLower, listOf("feel", "feels")) ->
                "I'm not quite sure what you're referring to. Are you looking to:"

            // Problem/issue mentions
            containsAny(inputLower, listOf("off", "wrong", "problem")) ->
                "Could you clarify what's off? Are you looking to:"

            // General help/advice requests
            containsAny(inputLower, listOf("help", "what", "how")) ->
                "I can help with that. What would you like to do:"

            // Default clarification
            else ->
                "I'm not quite sure what you're asking. Did you want to:"
        }
    }

    /**
     * Create an IntentSuggestion with user-friendly label and description.
     */
    private fun createIntentSuggestion(intentType: IntentType): IntentSuggestion {
        val schema = IntentRegistry.getSchema(intentType)

        // Generate short, actionable chip label
        val label = when (intentType) {
            IntentType.CLUB_ADJUSTMENT -> "Adjust Club"
            IntentType.RECOVERY_CHECK -> "Check Recovery"
            IntentType.SHOT_RECOMMENDATION -> "Get Shot Advice"
            IntentType.SCORE_ENTRY -> "Enter Score"
            IntentType.PATTERN_QUERY -> "View Patterns"
            IntentType.DRILL_REQUEST -> "Get Drill"
            IntentType.WEATHER_CHECK -> "Check Weather"
            IntentType.STATS_LOOKUP -> "View Stats"
            IntentType.ROUND_START -> "Start Round"
            IntentType.ROUND_END -> "End Round"
            IntentType.EQUIPMENT_INFO -> "View Equipment"
            IntentType.COURSE_INFO -> "Course Info"
            IntentType.SETTINGS_CHANGE -> "Settings"
            IntentType.HELP_REQUEST -> "Get Help"
            IntentType.FEEDBACK -> "Send Feedback"
        }

        return IntentSuggestion(
            intentType = intentType,
            label = label,
            description = schema.description
        )
    }
}
