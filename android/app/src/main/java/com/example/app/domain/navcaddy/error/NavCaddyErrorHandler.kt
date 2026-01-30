package com.example.app.domain.navcaddy.error

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central error handler for the NavCaddy system.
 *
 * Maps errors to appropriate recovery strategies and provides user-facing
 * error messages in the Bones persona. Ensures no dead-end states by always
 * providing actionable recovery options.
 *
 * Spec reference: navcaddy-engine.md R7, G6, A6 (Failure-mode resilience)
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
@Singleton
class NavCaddyErrorHandler @Inject constructor() {

    /**
     * Handle an error and return appropriate recovery strategy.
     *
     * @param error The error that occurred
     * @param context Additional context about the error (optional)
     * @return Recovery strategy with user message and available actions
     */
    fun handleError(
        error: NavCaddyError,
        context: ErrorContext? = null
    ): RecoveryStrategy {
        return when (error) {
            is NavCaddyError.NetworkError -> RecoveryStrategy.NetworkRecovery()

            is NavCaddyError.TimeoutError -> RecoveryStrategy.TimeoutRecovery()

            is NavCaddyError.TranscriptionError -> handleTranscriptionError(error, context)

            is NavCaddyError.ClassificationError -> handleClassificationError(error, context)

            is NavCaddyError.ServiceUnavailableError -> RecoveryStrategy.ServiceRecovery()

            is NavCaddyError.InvalidInputError -> handleInvalidInputError(error, context)

            is NavCaddyError.PrerequisiteMissingError -> handlePrerequisiteError(error, context)

            is NavCaddyError.UnknownError -> RecoveryStrategy.UnknownRecovery()
        }
    }

    /**
     * Handle transcription errors with context-aware recovery.
     */
    private fun handleTranscriptionError(
        error: NavCaddyError.TranscriptionError,
        context: ErrorContext?
    ): RecoveryStrategy {
        // Check if this is a repeated transcription failure
        val isRepeatedFailure = context?.attemptCount?.let { it > 1 } ?: false

        return if (isRepeatedFailure) {
            RecoveryStrategy.TranscriptionRecovery(
                userMessage = "Voice input isn't working right now. Let's try typing instead.",
                actions = listOf(
                    RecoveryAction.UseTextInput,
                    RecoveryAction.ShowVoiceTips,
                    RecoveryAction.ShowHelp
                )
            )
        } else {
            RecoveryStrategy.TranscriptionRecovery()
        }
    }

    /**
     * Handle classification errors with fallback to clarification.
     */
    private fun handleClassificationError(
        error: NavCaddyError.ClassificationError,
        context: ErrorContext?
    ): RecoveryStrategy {
        val userInput = error.userInput ?: context?.userInput

        return RecoveryStrategy.ClassificationRecovery(
            userMessage = if (userInput.isNullOrBlank()) {
                "I didn't quite catch that. Could you rephrase, or pick from these common options?"
            } else {
                "I'm not sure what you meant by \"$userInput\". Could you rephrase, or pick from these options?"
            }
        )
    }

    /**
     * Handle invalid input errors with specific hints.
     */
    private fun handleInvalidInputError(
        error: NavCaddyError.InvalidInputError,
        context: ErrorContext?
    ): RecoveryStrategy {
        val fieldHint = error.inputField?.let { field ->
            " The issue is with: $field."
        } ?: ""

        return RecoveryStrategy.InvalidInputRecovery(
            userMessage = "Something doesn't look quite right with that input.$fieldHint Can you double-check and try again?"
        )
    }

    /**
     * Handle prerequisite errors with specific guidance.
     */
    private fun handlePrerequisiteError(
        error: NavCaddyError.PrerequisiteMissingError,
        context: ErrorContext?
    ): RecoveryStrategy {
        val prerequisiteHint = error.missingPrerequisite?.let { prereq ->
            when (prereq.lowercase()) {
                "recovery data" -> "To check your recovery, I need some data first. Let's set that up."
                "round data" -> "I need an active round to help with that. Want to start one?"
                "club data" -> "I need your club distances first. Let's add those."
                "score data" -> "I need some scores to show stats. Want to enter your latest round?"
                else -> "I need $prereq to help with that. Let's get that set up."
            }
        } ?: error.message

        return RecoveryStrategy.PrerequisiteRecovery(
            userMessage = prerequisiteHint,
            missingPrerequisite = error.missingPrerequisite
        )
    }

    /**
     * Check if error is recoverable.
     */
    fun isRecoverable(error: NavCaddyError): Boolean {
        return error.isRecoverable
    }

    /**
     * Get user-facing error message in Bones persona.
     */
    fun getUserMessage(error: NavCaddyError, context: ErrorContext? = null): String {
        val strategy = handleError(error, context)
        return strategy.userMessage
    }

    /**
     * Get available recovery actions for an error.
     */
    fun getRecoveryActions(error: NavCaddyError, context: ErrorContext? = null): List<RecoveryAction> {
        val strategy = handleError(error, context)
        return strategy.actions
    }

    /**
     * Get suggested intents for fallback navigation.
     */
    fun getSuggestedIntents(error: NavCaddyError, context: ErrorContext? = null): List<caddypro.domain.navcaddy.models.IntentType> {
        val strategy = handleError(error, context)
        return strategy.suggestedIntents
    }
}

/**
 * Additional context about an error for better recovery strategies.
 */
data class ErrorContext(
    /** User's original input that caused the error */
    val userInput: String? = null,

    /** Number of attempts made (for repeated failures) */
    val attemptCount: Int = 1,

    /** Whether user is currently offline */
    val isOffline: Boolean = false,

    /** Current conversation state (for context-aware recovery) */
    val conversationState: String? = null,

    /** Additional metadata */
    val metadata: Map<String, Any> = emptyMap()
)
