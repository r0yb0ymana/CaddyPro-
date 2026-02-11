import XCTest
@testable import App

/**
 * Unit tests for NavCaddyErrorHandler.
 *
 * Tests error handling, recovery strategies, retry logic, and pattern detection.
 *
 * Spec reference: navcaddy-engine.md R7, G6
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
@MainActor
final class NavCaddyErrorHandlerTests: XCTestCase {
    var errorHandler: NavCaddyErrorHandler!
    var analytics: ConsoleAnalytics!

    override func setUp() async throws {
        try await super.setUp()
        analytics = ConsoleAnalytics(enablePIIRedaction: false, verbose: false)
        errorHandler = NavCaddyErrorHandler(analytics: analytics)
    }

    override func tearDown() async throws {
        errorHandler = nil
        analytics = nil
        try await super.tearDown()
    }

    // MARK: - Error Message Tests

    func testLLMTimeoutErrorMessage() {
        let error = NavCaddyError.llmTimeout
        let strategy = errorHandler.handle(error, context: nil)

        XCTAssertEqual(strategy.userMessage, "The request took too long. Let me suggest some options instead.")
        XCTAssertTrue(strategy.isRecoverable)
        XCTAssertTrue(strategy.hasSuggestions)
    }

    func testNetworkUnavailableErrorMessage() {
        let error = NavCaddyError.networkUnavailable
        let strategy = errorHandler.handle(error, context: nil)

        XCTAssertEqual(strategy.userMessage, "You're offline. Here are some things I can help with without a connection.")
        XCTAssertTrue(strategy.isRecoverable)
        XCTAssertTrue(strategy.hasSuggestions)
    }

    func testVoicePermissionDeniedErrorMessage() {
        let error = NavCaddyError.voicePermissionDenied
        let strategy = errorHandler.handle(error, context: nil)

        XCTAssertEqual(strategy.userMessage, "Please enable Speech Recognition in Settings to use voice input.")
        XCTAssertFalse(strategy.isRecoverable)
        XCTAssertTrue(strategy.hasRecoveryActions)
    }

    // MARK: - Recovery Strategy Tests

    func testLLMTimeoutShouldAutoRetry() {
        let error = NavCaddyError.llmTimeout
        let operationId = "test-op-1"

        let strategy = errorHandler.handle(error, context: nil, operationId: operationId)

        XCTAssertTrue(strategy.shouldAutoRetry)
        XCTAssertNotNil(strategy.retryBackoffMs)
        XCTAssertGreaterThanOrEqual(strategy.retryBackoffMs ?? 0, 250) // Base backoff with jitter
    }

    func testLLMNetworkErrorShouldAutoRetry() {
        let error = NavCaddyError.llmNetworkError("Connection failed")
        let strategy = errorHandler.handle(error, context: nil)

        XCTAssertTrue(strategy.shouldAutoRetry)
        XCTAssertNotNil(strategy.retryBackoffMs)
    }

    func testVoiceErrorShouldNotAutoRetry() {
        let error = NavCaddyError.noSpeechDetected
        let strategy = errorHandler.handle(error, context: nil)

        XCTAssertFalse(strategy.shouldAutoRetry)
        XCTAssertNil(strategy.retryBackoffMs)
    }

    // MARK: - Retry Backoff Tests

    func testExponentialBackoff() {
        let error = NavCaddyError.llmTimeout
        let operationId = "test-backoff"

        // First attempt
        let strategy1 = errorHandler.handle(error, context: nil, operationId: operationId)
        let backoff1 = strategy1.retryBackoffMs ?? 0

        // Second attempt
        let strategy2 = errorHandler.handle(error, context: nil, operationId: operationId)
        let backoff2 = strategy2.retryBackoffMs ?? 0

        // Third attempt
        let strategy3 = errorHandler.handle(error, context: nil, operationId: operationId)
        let backoff3 = strategy3.retryBackoffMs ?? 0

        // Backoff should increase
        XCTAssertGreaterThan(backoff2, backoff1)
        XCTAssertGreaterThan(backoff3, backoff2)

        // Fourth attempt should not retry (max 3 retries)
        let strategy4 = errorHandler.handle(error, context: nil, operationId: operationId)
        XCTAssertFalse(strategy4.shouldAutoRetry)
        XCTAssertNil(strategy4.retryBackoffMs)
    }

    func testMaxBackoffLimit() {
        let error = NavCaddyError.llmTimeout
        let operationId = "test-max-backoff"

        // Run many attempts to test max backoff
        var maxBackoff = 0
        for _ in 0..<3 {
            let strategy = errorHandler.handle(error, context: nil, operationId: operationId)
            if let backoff = strategy.retryBackoffMs {
                maxBackoff = max(maxBackoff, backoff)
            }
        }

        // Backoff should not exceed max (5000ms)
        XCTAssertLessThanOrEqual(maxBackoff, 5000)
    }

    func testResetRetries() {
        let error = NavCaddyError.llmTimeout
        let operationId = "test-reset"

        // First attempt
        _ = errorHandler.handle(error, context: nil, operationId: operationId)

        // Reset retries
        errorHandler.resetRetries(for: operationId)

        // Next attempt should be first attempt again
        let strategy = errorHandler.handle(error, context: nil, operationId: operationId)
        XCTAssertTrue(strategy.shouldAutoRetry)
    }

    func testClearRetryState() {
        let error = NavCaddyError.llmTimeout

        // Create attempts for multiple operations
        _ = errorHandler.handle(error, context: nil, operationId: "op1")
        _ = errorHandler.handle(error, context: nil, operationId: "op2")
        _ = errorHandler.handle(error, context: nil, operationId: "op3")

        // Clear all retry state
        errorHandler.clearRetryState()

        // All operations should start fresh
        let strategy1 = errorHandler.handle(error, context: nil, operationId: "op1")
        let strategy2 = errorHandler.handle(error, context: nil, operationId: "op2")

        XCTAssertTrue(strategy1.shouldAutoRetry)
        XCTAssertTrue(strategy2.shouldAutoRetry)
    }

    // MARK: - Suggestion Tests

    func testLLMTimeoutProvidesSuggestions() {
        let error = NavCaddyError.llmTimeout
        let strategy = errorHandler.handle(error, context: nil)

        XCTAssertFalse(strategy.suggestions.isEmpty)
        XCTAssertLessThanOrEqual(strategy.suggestions.count, 3)
    }

    func testNetworkUnavailableProvidesOfflineSuggestions() {
        let error = NavCaddyError.networkUnavailable
        let strategy = errorHandler.handle(error, context: nil)

        XCTAssertFalse(strategy.suggestions.isEmpty)

        // Check that suggestions are offline-capable
        let offlineTypes = LocalIntentSuggestions.offlineCapableIntents()
        for suggestion in strategy.suggestions {
            XCTAssertTrue(offlineTypes.contains(suggestion.intentType))
        }
    }

    func testClassificationFailureProvidesSuggestions() {
        let error = NavCaddyError.classificationFailed("Could not parse input")
        let strategy = errorHandler.handle(error, context: nil)

        XCTAssertTrue(strategy.hasSuggestions)
    }

    func testPermissionErrorProvidesRecoveryActions() {
        let error = NavCaddyError.voicePermissionDenied
        let strategy = errorHandler.handle(error, context: nil)

        XCTAssertFalse(strategy.suggestions.isEmpty || strategy.recoveryActions.isEmpty)
    }

    // MARK: - Context-Aware Suggestions Tests

    func testContextAwareSuggestionsWithActiveRound() {
        let context = createMockContext(isRoundActive: true)
        let error = NavCaddyError.llmTimeout

        let strategy = errorHandler.handle(error, context: context)

        // Score entry should be prioritized for active rounds
        let firstSuggestion = strategy.suggestions.first
        XCTAssertEqual(firstSuggestion?.intentType, .scoreEntry)
    }

    // MARK: - Analytics Tests

    func testErrorLogsToAnalytics() {
        let error = NavCaddyError.llmTimeout
        let sessionId = "test-session-123"

        let context = SessionContext(
            sessionId: sessionId,
            currentRoundId: nil,
            currentHole: nil,
            conversationHistory: [],
            lastShotTimestamp: nil,
            lastRecommendation: nil
        )

        _ = errorHandler.handle(error, context: context, operationId: "test")

        let events = analytics.getEvents(for: sessionId)
        let errorEvents = events.compactMap { event -> ErrorOccurredEvent? in
            if case .errorOccurred(let errorEvent) = event {
                return errorEvent
            }
            return nil
        }

        XCTAssertEqual(errorEvents.count, 1)
        XCTAssertEqual(errorEvents.first?.errorType, .timeout)
        XCTAssertTrue(errorEvents.first?.isRecoverable ?? false)
    }

    // MARK: - Error Pattern Detection Tests

    func testDetectRepeatedTimeouts() {
        let sessionId = "test-session-pattern"
        let context = SessionContext(
            sessionId: sessionId,
            currentRoundId: nil,
            currentHole: nil,
            conversationHistory: [],
            lastShotTimestamp: nil,
            lastRecommendation: nil
        )

        // Simulate multiple timeout errors
        for i in 0..<4 {
            _ = errorHandler.handle(
                .llmTimeout,
                context: context,
                operationId: "op-\(i)"
            )
        }

        let patterns = errorHandler.detectPatterns(sessionId: sessionId)

        XCTAssertTrue(patterns.contains(where: {
            if case .repeatedTimeouts(let count) = $0 {
                return count >= 3
            }
            return false
        }))
    }

    func testDetectNetworkInstability() {
        let sessionId = "test-session-network"
        let context = SessionContext(
            sessionId: sessionId,
            currentRoundId: nil,
            currentHole: nil,
            conversationHistory: [],
            lastShotTimestamp: nil,
            lastRecommendation: nil
        )

        // Simulate multiple network errors
        for i in 0..<4 {
            _ = errorHandler.handle(
                .networkUnavailable,
                context: context,
                operationId: "op-\(i)"
            )
        }

        let patterns = errorHandler.detectPatterns(sessionId: sessionId)

        XCTAssertTrue(patterns.contains(where: {
            if case .networkInstability(let count) = $0 {
                return count >= 3
            }
            return false
        }))
    }

    func testDetectPermissionIssues() {
        let sessionId = "test-session-permission"
        let context = SessionContext(
            sessionId: sessionId,
            currentRoundId: nil,
            currentHole: nil,
            conversationHistory: [],
            lastShotTimestamp: nil,
            lastRecommendation: nil
        )

        _ = errorHandler.handle(.voicePermissionDenied, context: context)

        let patterns = errorHandler.detectPatterns(sessionId: sessionId)

        XCTAssertTrue(patterns.contains(where: {
            if case .permissionIssues = $0 {
                return true
            }
            return false
        }))
    }

    // MARK: - Error Conversion Tests

    func testConvertFromLLMError() {
        let llmError = LLMError.timeout
        let navCaddyError = NavCaddyError.from(llmError: llmError)

        XCTAssertEqual(navCaddyError, .llmTimeout)
    }

    func testConvertFromVoiceError() {
        let voiceError = VoiceInputError.noSpeechDetected
        let navCaddyError = NavCaddyError.from(voiceError: voiceError)

        XCTAssertEqual(navCaddyError, .noSpeechDetected)
    }

    // MARK: - Helper Methods

    private func createMockContext(isRoundActive: Bool) -> SessionContext {
        SessionContext(
            sessionId: UUID().uuidString,
            currentRoundId: isRoundActive ? UUID().uuidString : nil,
            currentHole: isRoundActive ? 1 : nil,
            conversationHistory: [],
            lastShotTimestamp: nil,
            lastRecommendation: nil
        )
    }
}
