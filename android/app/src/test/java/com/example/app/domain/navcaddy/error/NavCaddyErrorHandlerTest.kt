package com.example.app.domain.navcaddy.error

import caddypro.domain.navcaddy.models.IntentType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NavCaddyErrorHandler.
 *
 * Tests cover:
 * - Error type mapping to recovery strategies
 * - Context-aware error handling
 * - Recovery action generation
 * - Suggested intents for fallback
 * - User-friendly error messages in Bones persona
 *
 * Spec reference: navcaddy-engine.md R7, G6, A6
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
class NavCaddyErrorHandlerTest {

    private lateinit var errorHandler: NavCaddyErrorHandler

    @Before
    fun setup() {
        errorHandler = NavCaddyErrorHandler()
    }

    // Test: Network error returns appropriate recovery strategy
    @Test
    fun `network error provides network recovery strategy`() {
        // GIVEN: Network error
        val error = NavCaddyError.NetworkError()

        // WHEN: Handle error
        val strategy = errorHandler.handleError(error)

        // THEN: Strategy is NetworkRecovery
        assertTrue(strategy is RecoveryStrategy.NetworkRecovery)
        assertTrue(strategy.actions.contains(RecoveryAction.RetryWithNetwork))
        assertTrue(strategy.actions.contains(RecoveryAction.UseOfflineMode))
        assertTrue(strategy.suggestedIntents.isNotEmpty())
    }

    // Test: Timeout error returns timeout recovery strategy
    @Test
    fun `timeout error provides timeout recovery strategy`() {
        // GIVEN: Timeout error
        val error = NavCaddyError.TimeoutError()

        // WHEN: Handle error
        val strategy = errorHandler.handleError(error)

        // THEN: Strategy is TimeoutRecovery
        assertTrue(strategy is RecoveryStrategy.TimeoutRecovery)
        assertTrue(strategy.actions.contains(RecoveryAction.Retry))
        assertTrue(strategy.actions.contains(RecoveryAction.ShowLocalSuggestions))
    }

    // Test: Transcription error returns transcription recovery strategy
    @Test
    fun `transcription error provides transcription recovery strategy`() {
        // GIVEN: Transcription error
        val error = NavCaddyError.TranscriptionError()

        // WHEN: Handle error
        val strategy = errorHandler.handleError(error)

        // THEN: Strategy is TranscriptionRecovery
        assertTrue(strategy is RecoveryStrategy.TranscriptionRecovery)
        assertTrue(strategy.actions.contains(RecoveryAction.UseTextInput))
        assertTrue(strategy.actions.contains(RecoveryAction.RetryVoice))
    }

    // Test: Repeated transcription failures suggest text input
    @Test
    fun `repeated transcription errors prioritize text input`() {
        // GIVEN: Repeated transcription error
        val error = NavCaddyError.TranscriptionError()
        val context = ErrorContext(attemptCount = 3)

        // WHEN: Handle error
        val strategy = errorHandler.handleError(error, context)

        // THEN: Text input is prioritized
        assertTrue(strategy is RecoveryStrategy.TranscriptionRecovery)
        assertTrue(strategy.userMessage.contains("typing"))
        assertTrue(strategy.actions.first() == RecoveryAction.UseTextInput)
    }

    // Test: Classification error returns classification recovery strategy
    @Test
    fun `classification error provides classification recovery strategy`() {
        // GIVEN: Classification error
        val error = NavCaddyError.ClassificationError(userInput = "something unclear")

        // WHEN: Handle error
        val strategy = errorHandler.handleError(error)

        // THEN: Strategy is ClassificationRecovery
        assertTrue(strategy is RecoveryStrategy.ClassificationRecovery)
        assertTrue(strategy.actions.contains(RecoveryAction.ShowClarification))
        assertTrue(strategy.actions.contains(RecoveryAction.ShowCommonIntents))
        assertTrue(strategy.suggestedIntents.contains(IntentType.HELP_REQUEST))
    }

    // Test: Service unavailable error returns service recovery strategy
    @Test
    fun `service unavailable error provides service recovery strategy`() {
        // GIVEN: Service unavailable error
        val error = NavCaddyError.ServiceUnavailableError()

        // WHEN: Handle error
        val strategy = errorHandler.handleError(error)

        // THEN: Strategy is ServiceRecovery
        assertTrue(strategy is RecoveryStrategy.ServiceRecovery)
        assertTrue(strategy.actions.contains(RecoveryAction.RetryLater))
        assertTrue(strategy.actions.contains(RecoveryAction.UseOfflineMode))
    }

    // Test: Invalid input error returns invalid input recovery strategy
    @Test
    fun `invalid input error provides invalid input recovery strategy`() {
        // GIVEN: Invalid input error
        val error = NavCaddyError.InvalidInputError(inputField = "yardage")

        // WHEN: Handle error
        val strategy = errorHandler.handleError(error)

        // THEN: Strategy is InvalidInputRecovery
        assertTrue(strategy is RecoveryStrategy.InvalidInputRecovery)
        assertTrue(strategy.userMessage.contains("yardage"))
        assertTrue(strategy.actions.contains(RecoveryAction.ShowInputHints))
    }

    // Test: Prerequisite missing error returns prerequisite recovery strategy
    @Test
    fun `prerequisite missing error provides prerequisite recovery strategy`() {
        // GIVEN: Prerequisite missing error
        val error = NavCaddyError.PrerequisiteMissingError(
            missingPrerequisite = "recovery data"
        )

        // WHEN: Handle error
        val strategy = errorHandler.handleError(error)

        // THEN: Strategy is PrerequisiteRecovery
        assertTrue(strategy is RecoveryStrategy.PrerequisiteRecovery)
        assertTrue(strategy.userMessage.contains("recovery"))
        assertTrue(strategy.actions.contains(RecoveryAction.ShowSetupGuide))
    }

    // Test: Unknown error returns unknown recovery strategy
    @Test
    fun `unknown error provides unknown recovery strategy`() {
        // GIVEN: Unknown error
        val error = NavCaddyError.UnknownError()

        // WHEN: Handle error
        val strategy = errorHandler.handleError(error)

        // THEN: Strategy is UnknownRecovery
        assertTrue(strategy is RecoveryStrategy.UnknownRecovery)
        assertTrue(strategy.actions.contains(RecoveryAction.Retry))
        assertTrue(strategy.actions.contains(RecoveryAction.ShowHelp))
    }

    // Test: All errors are recoverable by default
    @Test
    fun `all error types are recoverable`() {
        // GIVEN: Various error types
        val errors = listOf(
            NavCaddyError.NetworkError(),
            NavCaddyError.TimeoutError(),
            NavCaddyError.TranscriptionError(),
            NavCaddyError.ClassificationError(),
            NavCaddyError.ServiceUnavailableError(),
            NavCaddyError.InvalidInputError(),
            NavCaddyError.PrerequisiteMissingError(),
            NavCaddyError.UnknownError()
        )

        errors.forEach { error ->
            // WHEN: Check recoverability
            val isRecoverable = errorHandler.isRecoverable(error)

            // THEN: Error is recoverable
            assertTrue("$error should be recoverable", isRecoverable)
        }
    }

    // Test: User messages are in Bones persona
    @Test
    fun `user messages use Bones persona`() {
        // GIVEN: Various errors
        val errors = listOf(
            NavCaddyError.NetworkError(),
            NavCaddyError.TimeoutError(),
            NavCaddyError.ClassificationError()
        )

        errors.forEach { error ->
            // WHEN: Get user message
            val message = errorHandler.getUserMessage(error)

            // THEN: Message is user-friendly and conversational
            assertFalse("Message should not be empty", message.isBlank())
            assertFalse("Message should avoid technical jargon", message.contains("Exception"))
            assertFalse("Message should avoid technical jargon", message.contains("null"))
        }
    }

    // Test: Recovery actions are always provided
    @Test
    fun `recovery actions always provided for errors`() {
        // GIVEN: Various error types
        val errors = listOf(
            NavCaddyError.NetworkError(),
            NavCaddyError.TimeoutError(),
            NavCaddyError.TranscriptionError(),
            NavCaddyError.ClassificationError(),
            NavCaddyError.ServiceUnavailableError(),
            NavCaddyError.InvalidInputError(),
            NavCaddyError.PrerequisiteMissingError(),
            NavCaddyError.UnknownError()
        )

        errors.forEach { error ->
            // WHEN: Get recovery actions
            val actions = errorHandler.getRecoveryActions(error)

            // THEN: At least one action is provided
            assertTrue("${error::class.simpleName} should have recovery actions", actions.isNotEmpty())
        }
    }

    // Test: Suggested intents provided for relevant errors
    @Test
    fun `suggested intents provided for classification and network errors`() {
        // GIVEN: Errors that should have suggested intents
        val errors = listOf(
            NavCaddyError.NetworkError(),
            NavCaddyError.ClassificationError(),
            NavCaddyError.UnknownError()
        )

        errors.forEach { error ->
            // WHEN: Get suggested intents
            val intents = errorHandler.getSuggestedIntents(error)

            // THEN: Suggestions are provided
            assertTrue("${error::class.simpleName} should have suggested intents", intents.isNotEmpty())
        }
    }

    // Test: No suggested intents for input-specific errors
    @Test
    fun `no suggested intents for transcription and invalid input errors`() {
        // GIVEN: Input-specific errors
        val errors = listOf(
            NavCaddyError.TranscriptionError(),
            NavCaddyError.InvalidInputError()
        )

        errors.forEach { error ->
            // WHEN: Get suggested intents
            val intents = errorHandler.getSuggestedIntents(error)

            // THEN: No suggestions (user needs to fix input)
            assertTrue("${error::class.simpleName} should not suggest intents", intents.isEmpty())
        }
    }

    // Test: Network error suggests offline-capable intents
    @Test
    fun `network error suggests offline-capable intents`() {
        // GIVEN: Network error
        val error = NavCaddyError.NetworkError()

        // WHEN: Get suggested intents
        val intents = errorHandler.getSuggestedIntents(error)

        // THEN: Suggests offline-capable intents
        assertTrue(intents.contains(IntentType.SCORE_ENTRY))
        assertTrue(intents.contains(IntentType.STATS_LOOKUP))
        assertTrue(intents.contains(IntentType.EQUIPMENT_INFO))
    }

    // Test: Classification error suggests common intents
    @Test
    fun `classification error suggests common intents`() {
        // GIVEN: Classification error
        val error = NavCaddyError.ClassificationError()

        // WHEN: Get suggested intents
        val intents = errorHandler.getSuggestedIntents(error)

        // THEN: Suggests common intents
        assertTrue(intents.contains(IntentType.SHOT_RECOMMENDATION))
        assertTrue(intents.contains(IntentType.HELP_REQUEST))
    }

    // Test: fromThrowable creates appropriate error types
    @Test
    fun `fromThrowable maps exceptions to appropriate error types`() {
        // GIVEN: Various throwable types
        val testCases = mapOf(
            java.net.UnknownHostException() to NavCaddyError.NetworkError::class,
            java.net.SocketTimeoutException() to NavCaddyError.TimeoutError::class,
            java.util.concurrent.TimeoutException() to NavCaddyError.TimeoutError::class,
            IllegalArgumentException() to NavCaddyError.ClassificationError::class,
            RuntimeException() to NavCaddyError.UnknownError::class
        )

        testCases.forEach { (throwable, expectedClass) ->
            // WHEN: Create error from throwable
            val error = NavCaddyError.fromThrowable(throwable)

            // THEN: Correct error type created
            assertTrue(
                "Expected ${expectedClass.simpleName} for ${throwable::class.simpleName}",
                expectedClass.isInstance(error)
            )
        }
    }

    // Test: fromHttpStatus creates appropriate error types
    @Test
    fun `fromHttpStatus maps status codes to appropriate error types`() {
        // GIVEN: Various HTTP status codes
        val testCases = mapOf(
            400 to NavCaddyError.InvalidInputError::class,
            404 to NavCaddyError.InvalidInputError::class,
            500 to NavCaddyError.ServiceUnavailableError::class,
            503 to NavCaddyError.ServiceUnavailableError::class,
            200 to NavCaddyError.UnknownError::class // Unexpected success code
        )

        testCases.forEach { (statusCode, expectedClass) ->
            // WHEN: Create error from HTTP status
            val error = NavCaddyError.fromHttpStatus(statusCode)

            // THEN: Correct error type created
            assertTrue(
                "Expected ${expectedClass.simpleName} for HTTP $statusCode",
                expectedClass.isInstance(error)
            )
        }
    }

    // Test: Error context influences recovery strategy
    @Test
    fun `error context influences recovery strategy message`() {
        // GIVEN: Classification error with user input context
        val error = NavCaddyError.ClassificationError(userInput = "test input")
        val contextWithInput = ErrorContext(userInput = "test input")

        // WHEN: Handle error with context
        val strategyWithContext = errorHandler.handleError(error, contextWithInput)

        // THEN: User message includes context
        assertTrue(strategyWithContext.userMessage.contains("test input"))
    }

    // Test: No dead-end states - always have actions
    @Test
    fun `all errors provide at least one recovery action preventing dead-ends`() {
        // GIVEN: All error types
        val errors = listOf(
            NavCaddyError.NetworkError(),
            NavCaddyError.TimeoutError(),
            NavCaddyError.TranscriptionError(),
            NavCaddyError.ClassificationError(),
            NavCaddyError.ServiceUnavailableError(),
            NavCaddyError.InvalidInputError(),
            NavCaddyError.PrerequisiteMissingError("test"),
            NavCaddyError.UnknownError()
        )

        errors.forEach { error ->
            // WHEN: Handle error
            val strategy = errorHandler.handleError(error)

            // THEN: At least one actionable recovery option provided
            assertTrue(
                "${error::class.simpleName} should prevent dead-end with recovery actions",
                strategy.actions.isNotEmpty()
            )
            // AND: User message is actionable
            assertFalse(
                "${error::class.simpleName} message should not be empty",
                strategy.userMessage.isBlank()
            )
        }
    }

    // Test: Prerequisite-specific messages
    @Test
    fun `prerequisite errors have context-specific messages`() {
        // GIVEN: Various prerequisite errors
        val prerequisites = mapOf(
            "recovery data" to "recovery",
            "round data" to "round",
            "club data" to "club",
            "score data" to "score"
        )

        prerequisites.forEach { (prereq, keyword) ->
            // GIVEN: Prerequisite error
            val error = NavCaddyError.PrerequisiteMissingError(missingPrerequisite = prereq)

            // WHEN: Handle error
            val strategy = errorHandler.handleError(error)

            // THEN: Message contains relevant keyword
            assertTrue(
                "Message for '$prereq' should contain '$keyword'",
                strategy.userMessage.lowercase().contains(keyword)
            )
        }
    }
}
