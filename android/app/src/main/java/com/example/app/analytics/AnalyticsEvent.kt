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
            OPERATION_FAILED,
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

    // Shot Logger Analytics Events (Task 21)
    // Spec reference: live-caddy-mode.md R6 (one-second logging flow)
    // Acceptance criteria: A4 (speed requirement)

    /**
     * Shot logger UI opened.
     *
     * Tracks when user initiates shot logging flow.
     * Used as baseline timestamp for latency measurements.
     */
    data class ShotLoggerOpened(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String
    ) : AnalyticsEvent

    /**
     * Club selected for shot logging.
     *
     * Tracks latency from shot logger open to club selection.
     * Critical metric for one-second logging flow target.
     *
     * @property clubType Type of club selected (e.g., DRIVER, IRON_7, WEDGE_PW)
     * @property latencyMs Time from shot logger open to club selection in milliseconds
     */
    data class ClubSelected(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val clubType: String,
        val latencyMs: Long
    ) : AnalyticsEvent

    /**
     * Shot logged successfully.
     *
     * Tracks total latency from FAB tap to shot saved.
     * This is the primary performance metric for shot logging speed.
     *
     * Target: <2000ms (spec requirement)
     * Goal: <1000ms (one-second logging flow)
     *
     * @property clubType Type of club used for the shot
     * @property lie Result lie (e.g., FAIRWAY, ROUGH, GREEN, BUNKER, HAZARD)
     * @property totalLatencyMs Total time from shot logger open to shot saved
     */
    data class ShotLogged(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val clubType: String,
        val lie: String,
        val totalLatencyMs: Long
    ) : AnalyticsEvent

    /**
     * Shot synced to backend.
     *
     * Tracks sync latency for offline-first shot logging.
     * Monitors sync queue performance and network reliability.
     *
     * @property shotId Unique identifier of the synced shot
     * @property latencyMs Time taken to sync the shot to backend
     */
    data class ShotSynced(
        override val timestamp: Long = System.currentTimeMillis(),
        override val sessionId: String,
        val shotId: String,
        val latencyMs: Long
    ) : AnalyticsEvent
}
