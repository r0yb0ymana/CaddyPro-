package caddypro.ui.conversation.voice

/**
 * State definitions for voice input functionality.
 *
 * Represents all possible states of the voice input system with
 * appropriate error types and messages.
 *
 * Spec reference: navcaddy-engine.md R7
 * Plan reference: navcaddy-engine-plan.md Task 20
 */
sealed interface VoiceInputState {
    /**
     * Voice input is idle and ready to start.
     */
    data object Idle : VoiceInputState

    /**
     * Voice input is actively listening.
     *
     * @param partialResult Partial transcription result (optional)
     */
    data class Listening(val partialResult: String = "") : VoiceInputState

    /**
     * Voice input is processing the final result.
     */
    data object Processing : VoiceInputState

    /**
     * Voice input completed successfully.
     *
     * @param transcription Final transcription result
     */
    data class Result(val transcription: String) : VoiceInputState

    /**
     * Voice input encountered an error.
     *
     * @param error The error type
     * @param message User-friendly error message
     * @param isRecoverable Whether the error can be retried
     */
    data class Error(
        val error: VoiceInputError,
        val message: String,
        val isRecoverable: Boolean = true
    ) : VoiceInputState
}

/**
 * Enumeration of voice input error types.
 */
enum class VoiceInputError {
    /**
     * Microphone permission not granted.
     */
    PERMISSION_DENIED,

    /**
     * Speech recognizer is not available on device.
     */
    RECOGNIZER_NOT_AVAILABLE,

    /**
     * No speech detected.
     */
    NO_SPEECH,

    /**
     * Network error during recognition.
     */
    NETWORK_ERROR,

    /**
     * Insufficient permissions (e.g., "never ask again").
     */
    INSUFFICIENT_PERMISSIONS,

    /**
     * Server error from speech recognition service.
     */
    SERVER_ERROR,

    /**
     * Speech was too short to process.
     */
    SPEECH_TOO_SHORT,

    /**
     * Audio recording error.
     */
    AUDIO_ERROR,

    /**
     * Timeout waiting for speech.
     */
    TIMEOUT,

    /**
     * Generic recognition error.
     */
    RECOGNITION_ERROR,

    /**
     * Recognition service busy.
     */
    SERVICE_BUSY,

    /**
     * Unknown error occurred.
     */
    UNKNOWN
}
