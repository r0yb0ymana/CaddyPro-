package caddypro.analytics

/**
 * Event schema for NavCaddy analytics.
 *
 * Spec reference: navcaddy-engine.md R8
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
sealed interface AnalyticsEvent {
    val timestamp: Long
    val sessionId: String

    /**
     * User input received (text or voice).
     */
    data class InputReceived(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val inputType: InputType,
        val inputLength: Int
    ) : AnalyticsEvent {
        enum class InputType {
            TEXT,
            VOICE
        }
    }

    /**
     * Intent classification completed.
     */
    data class IntentClassified(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val intent: String,
        val confidence: Float,
        val latencyMs: Long,
        val wasSuccessful: Boolean
    ) : AnalyticsEvent

    /**
     * Route execution completed.
     */
    data class RouteExecuted(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val module: String,
        val screen: String,
        val latencyMs: Long,
        val parameters: Map<String, String> = emptyMap()
    ) : AnalyticsEvent

    /**
     * Error occurred in the pipeline.
     */
    data class ErrorOccurred(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val errorType: ErrorType,
        val message: String,
        val isRecoverable: Boolean,
        val stackTrace: String? = null
    ) : AnalyticsEvent {
        enum class ErrorType {
            NETWORK_ERROR,
            TIMEOUT,
            CLASSIFICATION_ERROR,
            ROUTING_ERROR,
            VOICE_TRANSCRIPTION_ERROR,
            SERVICE_UNAVAILABLE,
            UNKNOWN
        }
    }

    /**
     * Voice transcription completed.
     */
    data class VoiceTranscription(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val latencyMs: Long,
        val wordCount: Int,
        val wasSuccessful: Boolean
    ) : AnalyticsEvent

    /**
     * Clarification requested due to low confidence.
     */
    data class ClarificationRequested(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val originalInput: String,
        val confidence: Float,
        val suggestionsCount: Int
    ) : AnalyticsEvent

    /**
     * User selected a clarification suggestion.
     */
    data class SuggestionSelected(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val intentType: String,
        val suggestionIndex: Int
    ) : AnalyticsEvent
}
