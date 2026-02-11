package com.example.app.domain.navcaddy.error

/**
 * Represents different types of errors that can occur in the NavCaddy system.
 *
 * This sealed class categorizes errors by their source and recoverability,
 * enabling targeted recovery strategies for each error type.
 *
 * Spec reference: navcaddy-engine.md R7 (Multi-Modal Input Handling), G6 (Deterministic fallbacks)
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
sealed class NavCaddyError(
    open val message: String,
    open val cause: Throwable? = null,
    open val isRecoverable: Boolean = true
) {
    /**
     * Network-related errors (connectivity issues, DNS failures).
     *
     * Recovery: Offer offline mode, cached suggestions, retry option.
     */
    data class NetworkError(
        override val message: String = "No internet connection available",
        override val cause: Throwable? = null
    ) : NavCaddyError(message, cause, isRecoverable = true)

    /**
     * Request timeout errors (LLM response too slow, API timeout).
     *
     * Recovery: Show local intent suggestions, allow retry with shorter timeout.
     */
    data class TimeoutError(
        override val message: String = "Request took too long",
        override val cause: Throwable? = null,
        val timeoutDurationMs: Long? = null
    ) : NavCaddyError(message, cause, isRecoverable = true)

    /**
     * Voice transcription errors (speech recognition failed, no speech detected).
     *
     * Recovery: Prompt for text input, offer retry, show voice tips.
     */
    data class TranscriptionError(
        override val message: String = "Could not understand speech",
        override val cause: Throwable? = null,
        val errorCode: String? = null
    ) : NavCaddyError(message, cause, isRecoverable = true)

    /**
     * Intent classification errors (LLM returned invalid response, parsing failed).
     *
     * Recovery: Fall back to clarification flow, show common intents.
     */
    data class ClassificationError(
        override val message: String = "Could not understand your request",
        override val cause: Throwable? = null,
        val userInput: String? = null
    ) : NavCaddyError(message, cause, isRecoverable = true)

    /**
     * Service unavailable errors (API down, rate limited, server error).
     *
     * Recovery: Show offline options, inform user to try later.
     */
    data class ServiceUnavailableError(
        override val message: String = "Service is currently unavailable",
        override val cause: Throwable? = null,
        val httpStatusCode: Int? = null
    ) : NavCaddyError(message, cause, isRecoverable = true)

    /**
     * Invalid input errors (malformed data, validation failures).
     *
     * Recovery: Show input format hints, provide examples.
     */
    data class InvalidInputError(
        override val message: String = "Invalid input format",
        override val cause: Throwable? = null,
        val inputField: String? = null
    ) : NavCaddyError(message, cause, isRecoverable = true)

    /**
     * Missing prerequisite errors (required data not available for intent).
     *
     * Recovery: Guide user to provide missing data, show setup instructions.
     */
    data class PrerequisiteMissingError(
        override val message: String = "Required information is missing",
        override val cause: Throwable? = null,
        val missingPrerequisite: String? = null
    ) : NavCaddyError(message, cause, isRecoverable = true)

    /**
     * Unknown errors (unexpected exceptions, unhandled cases).
     *
     * Recovery: Generic fallback, show help options, allow restart.
     */
    data class UnknownError(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : NavCaddyError(message, cause, isRecoverable = true)

    companion object {
        /**
         * Create appropriate NavCaddyError from a Throwable.
         *
         * Maps common exception types to specific error categories.
         */
        fun fromThrowable(throwable: Throwable, userInput: String? = null): NavCaddyError {
            return when (throwable) {
                is java.net.UnknownHostException,
                is java.net.NoRouteToHostException,
                is java.net.ConnectException -> NetworkError(
                    message = "No internet connection available",
                    cause = throwable
                )

                is java.net.SocketTimeoutException,
                is java.util.concurrent.TimeoutException -> TimeoutError(
                    message = "Request took too long",
                    cause = throwable
                )

                is IllegalArgumentException,
                is IllegalStateException -> ClassificationError(
                    message = "Could not understand your request",
                    cause = throwable,
                    userInput = userInput
                )

                else -> UnknownError(
                    message = throwable.message ?: "An unexpected error occurred",
                    cause = throwable
                )
            }
        }

        /**
         * Create error from HTTP status code.
         */
        fun fromHttpStatus(statusCode: Int, message: String? = null): NavCaddyError {
            return when (statusCode) {
                in 400..499 -> InvalidInputError(
                    message = message ?: "Invalid request"
                )

                in 500..599 -> ServiceUnavailableError(
                    message = message ?: "Service is currently unavailable",
                    httpStatusCode = statusCode
                )

                else -> UnknownError(
                    message = message ?: "Unexpected error (HTTP $statusCode)"
                )
            }
        }
    }
}
