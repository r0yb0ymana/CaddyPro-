package caddypro.analytics

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analytics event logger for NavCaddy engine.
 *
 * Provides a simple interface that can be backed by different implementations
 * (Firebase, custom backend, local logging, etc.).
 *
 * Key features:
 * - Session tracking with unique session IDs
 * - PII redaction for privacy compliance
 * - Latency tracking for performance monitoring
 * - Event streaming for debug views
 *
 * Spec reference: navcaddy-engine.md R8
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
@Singleton
class NavCaddyAnalytics @Inject constructor() {

    // Current session ID
    private var currentSessionId: String = generateSessionId()

    // Event stream for debug trace view
    private val _eventStream = MutableSharedFlow<AnalyticsEvent>(
        replay = 50, // Keep last 50 events for late subscribers
        extraBufferCapacity = 100
    )
    val eventStream: SharedFlow<AnalyticsEvent> = _eventStream.asSharedFlow()

    // Latency tracking
    private val operationStartTimes = mutableMapOf<String, Long>()

    /**
     * Start a new session.
     *
     * Call this when the app starts or user starts a new conversation.
     */
    fun startSession() {
        currentSessionId = generateSessionId()
    }

    /**
     * Get current session ID.
     */
    fun getSessionId(): String = currentSessionId

    /**
     * Log an input received event.
     */
    fun logInputReceived(
        inputType: AnalyticsEvent.InputReceived.InputType,
        input: String
    ) {
        val event = AnalyticsEvent.InputReceived(
            sessionId = currentSessionId,
            inputType = inputType,
            inputLength = input.length
        )
        logEvent(event)
    }

    /**
     * Start tracking latency for an operation.
     *
     * @param operationId Unique identifier for this operation
     */
    fun startLatencyTracking(operationId: String) {
        operationStartTimes[operationId] = System.currentTimeMillis()
    }

    /**
     * Get elapsed time for an operation.
     *
     * @param operationId The operation ID
     * @return Elapsed time in milliseconds, or 0 if operation not started
     */
    fun getElapsedTime(operationId: String): Long {
        val startTime = operationStartTimes[operationId] ?: return 0
        return System.currentTimeMillis() - startTime
    }

    /**
     * Stop tracking latency for an operation and return elapsed time.
     *
     * @param operationId The operation ID
     * @return Elapsed time in milliseconds, or 0 if operation not started
     */
    fun stopLatencyTracking(operationId: String): Long {
        val elapsed = getElapsedTime(operationId)
        operationStartTimes.remove(operationId)
        return elapsed
    }

    /**
     * Log an intent classification event.
     */
    fun logIntentClassified(
        intent: String,
        confidence: Float,
        latencyMs: Long,
        wasSuccessful: Boolean
    ) {
        val event = AnalyticsEvent.IntentClassified(
            sessionId = currentSessionId,
            intent = intent,
            confidence = confidence,
            latencyMs = latencyMs,
            wasSuccessful = wasSuccessful
        )
        logEvent(event)
    }

    /**
     * Log a route execution event.
     */
    fun logRouteExecuted(
        module: String,
        screen: String,
        latencyMs: Long,
        parameters: Map<String, String> = emptyMap()
    ) {
        // Redact any PII in parameters
        val redactedParams = PIIRedactor.redactMap(parameters)

        val event = AnalyticsEvent.RouteExecuted(
            sessionId = currentSessionId,
            module = module,
            screen = screen,
            latencyMs = latencyMs,
            parameters = redactedParams
        )
        logEvent(event)
    }

    /**
     * Log an error event.
     */
    fun logError(
        errorType: AnalyticsEvent.ErrorOccurred.ErrorType,
        message: String,
        isRecoverable: Boolean,
        throwable: Throwable? = null
    ) {
        // Redact PII from error message
        val redactedMessage = PIIRedactor.redact(message)
        val stackTrace = throwable?.stackTraceToString()

        val event = AnalyticsEvent.ErrorOccurred(
            sessionId = currentSessionId,
            errorType = errorType,
            message = redactedMessage,
            isRecoverable = isRecoverable,
            stackTrace = stackTrace
        )
        logEvent(event)
    }

    /**
     * Log a voice transcription event.
     */
    fun logVoiceTranscription(
        latencyMs: Long,
        transcription: String,
        wasSuccessful: Boolean
    ) {
        val wordCount = if (wasSuccessful) {
            transcription.split("\\s+".toRegex()).size
        } else {
            0
        }

        val event = AnalyticsEvent.VoiceTranscription(
            sessionId = currentSessionId,
            latencyMs = latencyMs,
            wordCount = wordCount,
            wasSuccessful = wasSuccessful
        )
        logEvent(event)
    }

    /**
     * Log a clarification request event.
     */
    fun logClarificationRequested(
        originalInput: String,
        confidence: Float,
        suggestionsCount: Int
    ) {
        // Redact PII from input
        val redactedInput = PIIRedactor.redact(originalInput)

        val event = AnalyticsEvent.ClarificationRequested(
            sessionId = currentSessionId,
            originalInput = redactedInput,
            confidence = confidence,
            suggestionsCount = suggestionsCount
        )
        logEvent(event)
    }

    /**
     * Log a suggestion selection event.
     */
    fun logSuggestionSelected(
        intentType: String,
        suggestionIndex: Int
    ) {
        val event = AnalyticsEvent.SuggestionSelected(
            sessionId = currentSessionId,
            intentType = intentType,
            suggestionIndex = suggestionIndex
        )
        logEvent(event)
    }

    // Shot Logger Analytics Methods (Task 21)
    // Spec reference: live-caddy-mode.md R6 (one-second logging flow)
    // Acceptance criteria: A4 (speed requirement)

    /**
     * Log shot logger opened event.
     *
     * Call this when the user taps the FAB to open shot logger.
     * This establishes the baseline timestamp for latency measurements.
     */
    fun logShotLoggerOpened() {
        val event = AnalyticsEvent.ShotLoggerOpened(
            sessionId = currentSessionId
        )
        logEvent(event)
    }

    /**
     * Log club selection event.
     *
     * Tracks the latency from shot logger open to club selection.
     * Critical for measuring one-second logging flow performance.
     *
     * @param clubType Type of club selected (e.g., DRIVER, IRON_7)
     * @param latencyMs Time from shot logger open to club selection
     */
    fun logClubSelected(
        clubType: String,
        latencyMs: Long
    ) {
        val event = AnalyticsEvent.ClubSelected(
            sessionId = currentSessionId,
            clubType = clubType,
            latencyMs = latencyMs
        )
        logEvent(event)
    }

    /**
     * Log shot logged event.
     *
     * Tracks the total latency from FAB tap to shot saved.
     * This is the primary performance metric for shot logging.
     *
     * Target: <2000ms (spec requirement)
     * Goal: <1000ms (one-second logging flow)
     *
     * @param clubType Type of club used for the shot
     * @param lie Result lie (e.g., FAIRWAY, ROUGH, GREEN)
     * @param totalLatencyMs Total time from shot logger open to shot saved
     */
    fun logShotLogged(
        clubType: String,
        lie: String,
        totalLatencyMs: Long
    ) {
        val event = AnalyticsEvent.ShotLogged(
            sessionId = currentSessionId,
            clubType = clubType,
            lie = lie,
            totalLatencyMs = totalLatencyMs
        )
        logEvent(event)

        // Performance monitoring: warn if latency exceeds target in debug builds
        if (caddypro.BuildConfig.DEBUG && totalLatencyMs > 2000) {
            android.util.Log.w(
                "ShotLogger",
                "Shot logging exceeded 2s target: ${totalLatencyMs}ms (club=$clubType, lie=$lie)"
            )
        }
    }

    /**
     * Log shot synced event.
     *
     * Tracks sync latency for offline-first shot logging.
     * Monitors sync queue performance and network reliability.
     *
     * @param shotId Unique identifier of the synced shot
     * @param latencyMs Time taken to sync the shot to backend
     */
    fun logShotSynced(
        shotId: String,
        latencyMs: Long
    ) {
        val event = AnalyticsEvent.ShotSynced(
            sessionId = currentSessionId,
            shotId = shotId,
            latencyMs = latencyMs
        )
        logEvent(event)
    }

    /**
     * Internal event logging.
     *
     * This method can be extended to send events to different backends:
     * - Firebase Analytics
     * - Custom analytics backend
     * - Local logging
     * - Crash reporting (for errors)
     */
    private fun logEvent(event: AnalyticsEvent) {
        // Emit to event stream for debug view
        _eventStream.tryEmit(event)

        // TODO: Add integration with analytics backend
        // Example: firebaseAnalytics.logEvent(...)
        // Example: customBackend.sendEvent(...)

        // For now, just log to console in debug builds
        if (caddypro.BuildConfig.DEBUG) {
            logToConsole(event)
        }
    }

    /**
     * Log event to console for debugging.
     */
    private fun logToConsole(event: AnalyticsEvent) {
        val eventType = event::class.simpleName
        val timestamp = android.text.format.DateFormat.format(
            "HH:mm:ss.SSS",
            event.timestamp
        )
        println("[$timestamp] NavCaddy Analytics: $eventType - $event")
    }

    /**
     * Generate a unique session ID.
     */
    private fun generateSessionId(): String {
        return UUID.randomUUID().toString().take(8)
    }

    /**
     * Clear all tracked operations.
     */
    fun clearTracking() {
        operationStartTimes.clear()
    }
}
