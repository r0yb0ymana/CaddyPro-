package caddypro.domain.navcaddy.offline

import caddypro.domain.navcaddy.models.IntentType

/**
 * Defines which intents are available in offline mode.
 *
 * Offline-capable intents can be processed locally without LLM API calls.
 * These intents typically involve local data operations (score entry, stats lookup)
 * or navigation to local screens (settings, equipment).
 *
 * Spec reference: navcaddy-engine.md C6 (Offline behavior), NG4 (limited local intents only)
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
object OfflineCapability {

    /**
     * Set of intents that can be processed offline.
     *
     * These intents do not require LLM classification or external API calls.
     * They can be handled with local pattern matching and data operations.
     *
     * Per spec C6: "If offline: disable LLM calls and provide a limited local-intent menu"
     */
    val OFFLINE_AVAILABLE_INTENTS = setOf(
        // Score operations - local database only
        IntentType.SCORE_ENTRY,

        // Stats - read from local database
        IntentType.STATS_LOOKUP,

        // Equipment - local data
        IntentType.EQUIPMENT_INFO,

        // Round management - local state
        IntentType.ROUND_START,
        IntentType.ROUND_END,

        // Settings - local preferences
        IntentType.SETTINGS_CHANGE,

        // Help - static content
        IntentType.HELP_REQUEST,

        // Club adjustment - local data update
        IntentType.CLUB_ADJUSTMENT,

        // Pattern query - local database
        IntentType.PATTERN_QUERY
    )

    /**
     * Intents that require online connectivity.
     *
     * These intents need LLM classification, external APIs, or real-time data.
     */
    val ONLINE_REQUIRED_INTENTS = setOf(
        // Shot recommendation - requires LLM and context
        IntentType.SHOT_RECOMMENDATION,

        // Recovery - may require external data
        IntentType.RECOVERY_CHECK,

        // Drill request - requires LLM for personalization
        IntentType.DRILL_REQUEST,

        // Weather - external API required
        IntentType.WEATHER_CHECK,

        // Course info - may require external data
        IntentType.COURSE_INFO,

        // Feedback - requires network to submit
        IntentType.FEEDBACK
    )

    /**
     * Check if an intent can be processed offline.
     *
     * @param intentType The intent to check
     * @return true if intent can be processed without network, false otherwise
     */
    fun isOfflineAvailable(intentType: IntentType): Boolean {
        return intentType in OFFLINE_AVAILABLE_INTENTS
    }

    /**
     * Check if an intent requires online connectivity.
     *
     * @param intentType The intent to check
     * @return true if intent requires network, false otherwise
     */
    fun requiresOnline(intentType: IntentType): Boolean {
        return intentType in ONLINE_REQUIRED_INTENTS
    }

    /**
     * Get user-friendly message explaining offline limitations.
     *
     * @param intentType The intent that was attempted offline
     * @return Message explaining why intent is unavailable offline
     */
    fun getOfflineLimitationMessage(intentType: IntentType): String {
        return when (intentType) {
            IntentType.SHOT_RECOMMENDATION ->
                "Shot recommendations need an internet connection. Try checking your stats or equipment instead."

            IntentType.RECOVERY_CHECK ->
                "Recovery insights need an internet connection. Check back when you're online."

            IntentType.DRILL_REQUEST ->
                "Personalized drills need an internet connection. Check your patterns in the meantime."

            IntentType.WEATHER_CHECK ->
                "Weather data needs an internet connection. I can't check conditions offline."

            IntentType.COURSE_INFO ->
                "Course information needs an internet connection. Try looking at your saved rounds instead."

            IntentType.FEEDBACK ->
                "Feedback submission needs an internet connection. Your feedback will be saved and sent when you're back online."

            else ->
                "This feature needs an internet connection. You can still enter scores, check stats, or view your equipment."
        }
    }

    /**
     * Get list of offline-available intents for user presentation.
     *
     * Returns a prioritized list of the most useful offline intents.
     *
     * @return List of intents available offline, in priority order
     */
    fun getOfflineIntentsPrioritized(): List<IntentType> {
        return listOf(
            IntentType.SCORE_ENTRY,
            IntentType.STATS_LOOKUP,
            IntentType.EQUIPMENT_INFO,
            IntentType.PATTERN_QUERY,
            IntentType.CLUB_ADJUSTMENT,
            IntentType.ROUND_START,
            IntentType.ROUND_END,
            IntentType.SETTINGS_CHANGE,
            IntentType.HELP_REQUEST
        )
    }

    /**
     * Get general offline mode message for users.
     *
     * @return User-friendly message explaining offline mode
     */
    fun getOfflineModeMessage(): String {
        return "You're offline. I can help with scores, stats, equipment, and settings. " +
                "Full features will be back when you reconnect."
    }

    /**
     * Get online reconnection message for users.
     *
     * @return User-friendly message when connection is restored
     */
    fun getOnlineModeMessage(): String {
        return "You're back online! All features are now available."
    }
}
