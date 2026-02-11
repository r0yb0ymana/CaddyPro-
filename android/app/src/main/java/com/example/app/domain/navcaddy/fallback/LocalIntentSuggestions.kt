package com.example.app.domain.navcaddy.fallback

import com.example.app.domain.navcaddy.models.IntentType
import com.example.app.domain.navcaddy.models.Module
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides local intent suggestions when LLM or network is unavailable.
 *
 * Uses keyword matching and context to suggest relevant intents without
 * requiring external API calls. Ensures users always have navigation options
 * even in offline or error scenarios.
 *
 * Spec reference: navcaddy-engine.md G6, A6 (Failure-mode resilience), C6 (Offline behavior)
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
@Singleton
class LocalIntentSuggestions @Inject constructor() {

    /**
     * Suggestion with intent type, display info, and routing target.
     */
    data class IntentSuggestion(
        val intentType: IntentType,
        val label: String,
        val description: String,
        val module: Module,
        val isOfflineAvailable: Boolean = false
    )

    /**
     * Get suggestions based on user input using local keyword matching.
     *
     * Falls back to common intents if no keywords match.
     *
     * @param input User's input text
     * @param isOffline Whether the device is currently offline
     * @param maxSuggestions Maximum number of suggestions to return (default 3 per spec A3)
     * @return List of intent suggestions, prioritized by relevance
     */
    fun getSuggestions(
        input: String,
        isOffline: Boolean = false,
        maxSuggestions: Int = 3
    ): List<IntentSuggestion> {
        val normalizedInput = input.lowercase().trim()

        // Try keyword matching first
        val keywordMatches = matchKeywords(normalizedInput, isOffline)

        // If we have enough keyword matches, return those
        if (keywordMatches.size >= maxSuggestions) {
            return keywordMatches.take(maxSuggestions)
        }

        // Otherwise, add common intents to fill up to maxSuggestions
        val commonIntents = getCommonIntents(isOffline)
        val combined = (keywordMatches + commonIntents).distinctBy { it.intentType }

        return combined.take(maxSuggestions)
    }

    /**
     * Get common intents that users frequently access.
     *
     * Filtered by offline availability if needed.
     */
    fun getCommonIntents(isOffline: Boolean = false): List<IntentSuggestion> {
        val allCommon = listOf(
            IntentSuggestion(
                intentType = IntentType.SHOT_RECOMMENDATION,
                label = "Get Shot Advice",
                description = "Recommend club and strategy for current shot",
                module = Module.CADDY,
                isOfflineAvailable = false
            ),
            IntentSuggestion(
                intentType = IntentType.SCORE_ENTRY,
                label = "Enter Score",
                description = "Record your score for a hole",
                module = Module.CADDY,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.CLUB_ADJUSTMENT,
                label = "Adjust Club",
                description = "Update club distances or selection",
                module = Module.CADDY,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.STATS_LOOKUP,
                label = "View Stats",
                description = "Check your golf statistics",
                module = Module.CADDY,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.RECOVERY_CHECK,
                label = "Check Recovery",
                description = "View your current recovery status",
                module = Module.RECOVERY,
                isOfflineAvailable = false
            )
        )

        return if (isOffline) {
            allCommon.filter { it.isOfflineAvailable }
        } else {
            allCommon
        }
    }

    /**
     * Get intents available in offline mode.
     */
    fun getOfflineIntents(): List<IntentSuggestion> {
        return ALL_SUGGESTIONS.filter { it.isOfflineAvailable }
    }

    /**
     * Get suggestions by module.
     */
    fun getSuggestionsByModule(module: Module, isOffline: Boolean = false): List<IntentSuggestion> {
        val suggestions = ALL_SUGGESTIONS.filter { it.module == module }
        return if (isOffline) {
            suggestions.filter { it.isOfflineAvailable }
        } else {
            suggestions
        }
    }

    /**
     * Match keywords in input to relevant intents.
     */
    private fun matchKeywords(input: String, isOffline: Boolean): List<IntentSuggestion> {
        val matches = mutableListOf<IntentSuggestion>()

        // Check each keyword category
        for ((keywords, intentType) in KEYWORD_MAP) {
            if (keywords.any { keyword -> input.contains(keyword) }) {
                val suggestion = ALL_SUGGESTIONS.find { it.intentType == intentType }
                if (suggestion != null) {
                    // Skip offline-unavailable suggestions if offline
                    if (!isOffline || suggestion.isOfflineAvailable) {
                        matches.add(suggestion)
                    }
                }
            }
        }

        return matches.distinctBy { it.intentType }
    }

    companion object {
        /**
         * All available intent suggestions.
         */
        private val ALL_SUGGESTIONS = listOf(
            IntentSuggestion(
                intentType = IntentType.CLUB_ADJUSTMENT,
                label = "Adjust Club",
                description = "Update club distances or selection",
                module = Module.CADDY,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.RECOVERY_CHECK,
                label = "Check Recovery",
                description = "View your current recovery status",
                module = Module.RECOVERY,
                isOfflineAvailable = false
            ),
            IntentSuggestion(
                intentType = IntentType.SHOT_RECOMMENDATION,
                label = "Get Shot Advice",
                description = "Recommend club and strategy for current shot",
                module = Module.CADDY,
                isOfflineAvailable = false
            ),
            IntentSuggestion(
                intentType = IntentType.SCORE_ENTRY,
                label = "Enter Score",
                description = "Record your score for a hole",
                module = Module.CADDY,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.PATTERN_QUERY,
                label = "View Patterns",
                description = "See your miss patterns and tendencies",
                module = Module.COACH,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.DRILL_REQUEST,
                label = "Get Drill",
                description = "Practice drills and exercises",
                module = Module.COACH,
                isOfflineAvailable = false
            ),
            IntentSuggestion(
                intentType = IntentType.WEATHER_CHECK,
                label = "Check Weather",
                description = "View current weather conditions",
                module = Module.CADDY,
                isOfflineAvailable = false
            ),
            IntentSuggestion(
                intentType = IntentType.STATS_LOOKUP,
                label = "View Stats",
                description = "Check your golf statistics",
                module = Module.CADDY,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.ROUND_START,
                label = "Start Round",
                description = "Begin a new golf round",
                module = Module.CADDY,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.ROUND_END,
                label = "End Round",
                description = "Complete your current round",
                module = Module.CADDY,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.EQUIPMENT_INFO,
                label = "Equipment Info",
                description = "View your bag and club details",
                module = Module.CADDY,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.COURSE_INFO,
                label = "Course Info",
                description = "Get course details and layout",
                module = Module.CADDY,
                isOfflineAvailable = false
            ),
            IntentSuggestion(
                intentType = IntentType.SETTINGS_CHANGE,
                label = "Settings",
                description = "Change app preferences",
                module = Module.SETTINGS,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.HELP_REQUEST,
                label = "Help",
                description = "Get help using the app",
                module = Module.SETTINGS,
                isOfflineAvailable = true
            ),
            IntentSuggestion(
                intentType = IntentType.FEEDBACK,
                label = "Feedback",
                description = "Provide app feedback",
                module = Module.SETTINGS,
                isOfflineAvailable = false
            )
        )

        /**
         * Keyword mapping for local intent matching.
         *
         * Maps common words/phrases to intent types.
         */
        private val KEYWORD_MAP = mapOf(
            // Club adjustment keywords
            listOf("club", "distance", "yardage", "adjust", "change club", "iron", "driver", "wedge") to IntentType.CLUB_ADJUSTMENT,

            // Recovery keywords
            listOf("recovery", "readiness", "sore", "tired", "body", "health", "rest") to IntentType.RECOVERY_CHECK,

            // Shot recommendation keywords
            listOf("shot", "recommend", "advice", "what club", "which club", "club selection", "strategy", "play") to IntentType.SHOT_RECOMMENDATION,

            // Score entry keywords
            listOf("score", "enter", "record", "input score", "hole score", "birdie", "bogey", "par") to IntentType.SCORE_ENTRY,

            // Pattern query keywords
            listOf("pattern", "miss", "tendency", "slice", "hook", "push", "pull", "fade", "draw") to IntentType.PATTERN_QUERY,

            // Drill request keywords
            listOf("drill", "practice", "exercise", "training", "workout", "improve") to IntentType.DRILL_REQUEST,

            // Weather keywords
            listOf("weather", "wind", "rain", "temperature", "forecast", "conditions") to IntentType.WEATHER_CHECK,

            // Stats keywords
            listOf("stats", "statistics", "performance", "handicap", "average", "summary") to IntentType.STATS_LOOKUP,

            // Round start keywords
            listOf("start round", "new round", "begin", "tee off", "first hole") to IntentType.ROUND_START,

            // Round end keywords
            listOf("end round", "finish", "complete", "done", "last hole") to IntentType.ROUND_END,

            // Equipment keywords
            listOf("equipment", "bag", "clubs in bag", "what's in my bag") to IntentType.EQUIPMENT_INFO,

            // Course keywords
            listOf("course", "hole", "layout", "yardage", "map") to IntentType.COURSE_INFO,

            // Settings keywords
            listOf("settings", "preferences", "options", "configure", "setup") to IntentType.SETTINGS_CHANGE,

            // Help keywords
            listOf("help", "how to", "instructions", "guide", "tutorial") to IntentType.HELP_REQUEST,

            // Feedback keywords
            listOf("feedback", "report", "bug", "suggestion", "feature request") to IntentType.FEEDBACK
        )
    }
}
