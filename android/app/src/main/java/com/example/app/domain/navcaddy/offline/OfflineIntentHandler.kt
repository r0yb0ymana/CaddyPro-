package caddypro.domain.navcaddy.offline

import caddypro.domain.navcaddy.clarification.ClarificationResponse
import caddypro.domain.navcaddy.clarification.IntentSuggestion
// import caddypro.domain.navcaddy.fallback.LocalIntentSuggestions
import caddypro.domain.navcaddy.models.ExtractedEntities
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.ParsedIntent
import caddypro.domain.navcaddy.normalizer.InputNormalizer
// import javax.inject.Inject
// import javax.inject.Singleton

/**
 * Handles intent processing when device is offline.
 */
// @Singleton
class OfflineIntentHandler /* @Inject */ constructor(
    private val inputNormalizer: InputNormalizer
    // private val localSuggestions: LocalIntentSuggestions
) {

    sealed class OfflineResult {
        data class Match(val parsedIntent: ParsedIntent) : OfflineResult()
        data class Clarify(val clarification: ClarificationResponse) : OfflineResult()
        data class RequiresOnline(
            val intentType: IntentType,
            val message: String
        ) : OfflineResult()
        data class NoMatch(val message: String) : OfflineResult()
    }

    fun processOffline(input: String): OfflineResult {
        val normalizedInput = inputNormalizer.normalize(input).normalizedInput.lowercase()
        val matches = matchOfflineIntents(normalizedInput)

        return when {
            matches.size == 1 && matches.first().score >= STRONG_MATCH_THRESHOLD -> {
                OfflineResult.Match(
                    parsedIntent = ParsedIntent(
                        intentId = java.util.UUID.randomUUID().toString(),
                        intentType = matches.first().intentType,
                        confidence = matches.first().score,
                        entities = extractBasicEntities(normalizedInput, matches.first().intentType)
                    )
                )
            }

            matches.isNotEmpty() && matches.first().score >= WEAK_MATCH_THRESHOLD -> {
                val suggestions = matches.take(3).map { match ->
                    IntentSuggestion(
                        intentType = match.intentType,
                        label = match.intentType.toDisplayName(),
                        description = match.intentType.toDescription()
                    )
                }
                OfflineResult.Clarify(
                    clarification = ClarificationResponse(
                        message = "I'm offline and need a bit more clarity. Did you mean:",
                        suggestions = suggestions,
                        originalInput = input
                    )
                )
            }

            else -> {
                val onlineIntentMatch = matchOnlineRequiredIntents(normalizedInput)
                if (onlineIntentMatch != null) {
                    OfflineResult.RequiresOnline(
                        intentType = onlineIntentMatch,
                        message = OfflineCapability.getOfflineLimitationMessage(onlineIntentMatch)
                    )
                } else {
                    OfflineResult.NoMatch(
                        message = "I'm offline and didn't understand that. " +
                            OfflineCapability.getOfflineModeMessage()
                    )
                }
            }
        }
    }

    fun getOfflineIntents(): List<Any> {
        // return localSuggestions.getOfflineIntents()
        return emptyList() // Stub
    }

    private fun matchOfflineIntents(normalizedInput: String): List<IntentMatch> {
        val matches = mutableListOf<IntentMatch>()

        for (intentType in OfflineCapability.OFFLINE_AVAILABLE_INTENTS) {
            val score = calculateMatchScore(normalizedInput, intentType)
            if (score > 0f) {
                matches.add(IntentMatch(intentType, score))
            }
        }

        return matches.sortedByDescending { it.score }
    }

    private fun matchOnlineRequiredIntents(normalizedInput: String): IntentType? {
        var bestMatch: IntentType? = null
        var bestScore = 0f

        for (intentType in OfflineCapability.ONLINE_REQUIRED_INTENTS) {
            val score = calculateMatchScore(normalizedInput, intentType)
            if (score > bestScore && score >= WEAK_MATCH_THRESHOLD) {
                bestScore = score
                bestMatch = intentType
            }
        }

        return bestMatch
    }

    private fun calculateMatchScore(input: String, intentType: IntentType): Float {
        val keywords = getKeywordsForIntent(intentType)
        var score = 0f
        var matchedKeywords = 0

        for ((keyword, weight) in keywords) {
            if (input.contains(keyword)) {
                score += weight
                matchedKeywords++
            }
        }

        return if (matchedKeywords > 0) {
            score / keywords.size
        } else {
            0f
        }
    }

    private fun extractBasicEntities(input: String, intentType: IntentType): ExtractedEntities {
        return when (intentType) {
            IntentType.SCORE_ENTRY -> {
                // Score entry uses hole number context
                ExtractedEntities(
                    holeNumber = extractHoleNumber(input)
                )
            }

            IntentType.CLUB_ADJUSTMENT -> {
                ExtractedEntities(
                    yardage = extractNumber(input)?.toInt()
                )
            }

            else -> ExtractedEntities()
        }
    }

    private fun extractNumber(input: String): Double? {
        val numberRegex = Regex("""\b(\d+)\b""")
        return numberRegex.find(input)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractHoleNumber(input: String): Int? {
        val holeRegex = Regex("""hole\s+(\d+)""")
        return holeRegex.find(input)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractClubName(input: String): String? {
        val clubKeywords = listOf(
            "driver", "wood", "hybrid", "iron", "wedge", "putter",
            "3w", "5w", "3h", "4h", "5h",
            "3i", "4i", "5i", "6i", "7i", "8i", "9i",
            "pw", "gw", "sw", "lw"
        )

        return clubKeywords.firstOrNull { input.contains(it) }
    }

    private fun getKeywordsForIntent(intentType: IntentType): List<Pair<String, Float>> {
        return when (intentType) {
            IntentType.SCORE_ENTRY -> listOf(
                "score" to 1.0f,
                "enter" to 0.8f,
                "record" to 0.8f,
                "par" to 0.6f,
                "birdie" to 0.6f,
                "bogey" to 0.6f,
                "hole" to 0.5f
            )

            IntentType.STATS_LOOKUP -> listOf(
                "stats" to 1.0f,
                "statistics" to 1.0f,
                "performance" to 0.8f,
                "average" to 0.7f,
                "handicap" to 0.7f,
                "summary" to 0.6f
            )

            IntentType.EQUIPMENT_INFO -> listOf(
                "equipment" to 1.0f,
                "bag" to 0.9f,
                "clubs" to 0.8f,
                "what's in" to 0.7f,
                "my bag" to 0.8f
            )

            IntentType.ROUND_START -> listOf(
                "start" to 0.9f,
                "new round" to 1.0f,
                "begin" to 0.8f,
                "tee off" to 0.9f,
                "first hole" to 0.7f
            )

            IntentType.ROUND_END -> listOf(
                "end" to 0.9f,
                "finish" to 0.9f,
                "complete" to 0.8f,
                "done" to 0.7f,
                "last hole" to 0.8f
            )

            IntentType.SETTINGS_CHANGE -> listOf(
                "settings" to 1.0f,
                "preferences" to 0.9f,
                "options" to 0.8f,
                "configure" to 0.8f,
                "setup" to 0.7f
            )

            IntentType.HELP_REQUEST -> listOf(
                "help" to 1.0f,
                "how to" to 0.9f,
                "instructions" to 0.8f,
                "guide" to 0.7f,
                "tutorial" to 0.7f
            )

            IntentType.CLUB_ADJUSTMENT -> listOf(
                "club" to 0.8f,
                "adjust" to 0.9f,
                "change" to 0.7f,
                "distance" to 0.8f,
                "yardage" to 0.8f
            )

            IntentType.PATTERN_QUERY -> listOf(
                "pattern" to 1.0f,
                "miss" to 0.9f,
                "tendency" to 0.9f,
                "slice" to 0.7f,
                "hook" to 0.7f
            )

            IntentType.SHOT_RECOMMENDATION -> listOf(
                "shot" to 0.9f,
                "recommend" to 1.0f,
                "advice" to 0.9f,
                "what club" to 1.0f,
                "which club" to 1.0f
            )

            IntentType.RECOVERY_CHECK -> listOf(
                "recovery" to 1.0f,
                "readiness" to 0.9f,
                "sore" to 0.8f,
                "tired" to 0.7f
            )

            IntentType.DRILL_REQUEST -> listOf(
                "drill" to 1.0f,
                "practice" to 0.9f,
                "exercise" to 0.8f,
                "training" to 0.8f
            )

            IntentType.WEATHER_CHECK -> listOf(
                "weather" to 1.0f,
                "wind" to 0.9f,
                "rain" to 0.8f,
                "forecast" to 0.9f
            )

            IntentType.COURSE_INFO -> listOf(
                "course" to 1.0f,
                "hole" to 0.6f,
                "layout" to 0.9f,
                "map" to 0.8f
            )

            IntentType.FEEDBACK -> listOf(
                "feedback" to 1.0f,
                "report" to 0.8f,
                "bug" to 0.8f,
                "suggestion" to 0.8f
            )

            IntentType.BAILOUT_QUERY -> listOf(
                "bailout" to 1.0f,
                "safe" to 0.8f,
                "trouble" to 0.7f,
                "escape" to 0.8f,
                "recovery" to 0.6f
            )

            IntentType.READINESS_CHECK -> listOf(
                "readiness" to 1.0f,
                "ready" to 0.9f,
                "feel" to 0.6f,
                "energy" to 0.8f,
                "fatigue" to 0.8f
            )
        }
    }

    private data class IntentMatch(
        val intentType: IntentType,
        val score: Float
    )

    companion object {
        private const val STRONG_MATCH_THRESHOLD = 0.7f
        private const val WEAK_MATCH_THRESHOLD = 0.4f
    }
}

/**
 * Extension function to get display name for IntentType.
 */
private fun IntentType.toDisplayName(): String = when (this) {
    IntentType.CLUB_ADJUSTMENT -> "Adjust Club"
    IntentType.RECOVERY_CHECK -> "Check Recovery"
    IntentType.SHOT_RECOMMENDATION -> "Shot Advice"
    IntentType.SCORE_ENTRY -> "Enter Score"
    IntentType.PATTERN_QUERY -> "View Patterns"
    IntentType.DRILL_REQUEST -> "Practice Drill"
    IntentType.WEATHER_CHECK -> "Check Weather"
    IntentType.STATS_LOOKUP -> "View Stats"
    IntentType.ROUND_START -> "Start Round"
    IntentType.ROUND_END -> "End Round"
    IntentType.EQUIPMENT_INFO -> "Equipment Info"
    IntentType.COURSE_INFO -> "Course Info"
    IntentType.SETTINGS_CHANGE -> "Settings"
    IntentType.HELP_REQUEST -> "Help"
    IntentType.FEEDBACK -> "Feedback"
    IntentType.BAILOUT_QUERY -> "Bailout Zone"
    IntentType.READINESS_CHECK -> "Check Readiness"
}

/**
 * Extension function to get description for IntentType.
 */
private fun IntentType.toDescription(): String = when (this) {
    IntentType.CLUB_ADJUSTMENT -> "Adjust club distances or selection"
    IntentType.RECOVERY_CHECK -> "Check your recovery status"
    IntentType.SHOT_RECOMMENDATION -> "Get shot recommendations"
    IntentType.SCORE_ENTRY -> "Enter or update your score"
    IntentType.PATTERN_QUERY -> "View your miss patterns"
    IntentType.DRILL_REQUEST -> "Get practice drill suggestions"
    IntentType.WEATHER_CHECK -> "Check weather conditions"
    IntentType.STATS_LOOKUP -> "View your golf statistics"
    IntentType.ROUND_START -> "Start a new round"
    IntentType.ROUND_END -> "Complete current round"
    IntentType.EQUIPMENT_INFO -> "View equipment information"
    IntentType.COURSE_INFO -> "Get course information"
    IntentType.SETTINGS_CHANGE -> "Change app settings"
    IntentType.HELP_REQUEST -> "Get help using the app"
    IntentType.FEEDBACK -> "Provide feedback"
    IntentType.BAILOUT_QUERY -> "Find safe bailout zones"
    IntentType.READINESS_CHECK -> "Check your readiness score"
}
