import Foundation

/**
 * Centralized error handling for the NavCaddy system.
 *
 * Provides:
 * - Error recovery strategies per error type
 * - User-facing error messages and suggestions
 * - Automatic retry with exponential backoff
 * - Analytics tracking for error patterns
 * - Fallback to local suggestions when services unavailable
 *
 * Spec reference: navcaddy-engine.md R7, G6 (No dead-end states)
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
@MainActor
final class NavCaddyErrorHandler {
    // MARK: - Dependencies

    private let analytics: NavCaddyAnalytics

    // MARK: - Retry State

    private var retryAttempts: [String: Int] = [:] // operationId -> attempt count
    private let maxRetries = 3
    private let baseBackoffMs = 500.0
    private let maxBackoffMs = 5000.0

    // MARK: - Initialization

    init(analytics: NavCaddyAnalytics) {
        self.analytics = analytics
    }

    // MARK: - Error Handling

    /// Handle an error and produce a recovery strategy.
    ///
    /// - Parameters:
    ///   - error: The error to handle
    ///   - context: Optional session context for personalized suggestions
    ///   - operationId: Unique ID for this operation (for retry tracking)
    /// - Returns: Error recovery strategy with user-facing information
    func handle(
        _ error: NavCaddyError,
        context: SessionContext?,
        operationId: String = UUID().uuidString
    ) -> ErrorRecoveryStrategy {
        // Track error in analytics
        analytics.log(.errorOccurred(ErrorOccurredEvent(
            errorType: mapErrorType(error),
            message: error.userMessage,
            isRecoverable: error.isRecoverable,
            sessionId: context?.sessionId ?? "unknown"
        )))

        // Determine recovery strategy
        let strategy = buildRecoveryStrategy(
            for: error,
            context: context,
            operationId: operationId
        )

        return strategy
    }

    /// Handle an LLM error.
    func handle(
        llmError: LLMError,
        context: SessionContext?,
        operationId: String = UUID().uuidString
    ) -> ErrorRecoveryStrategy {
        let navCaddyError = NavCaddyError.from(llmError: llmError)
        return handle(navCaddyError, context: context, operationId: operationId)
    }

    /// Handle a voice input error.
    func handle(
        voiceError: VoiceInputError,
        context: SessionContext?,
        operationId: String = UUID().uuidString
    ) -> ErrorRecoveryStrategy {
        let navCaddyError = NavCaddyError.from(voiceError: voiceError)
        return handle(navCaddyError, context: context, operationId: operationId)
    }

    // MARK: - Recovery Strategy Building

    private func buildRecoveryStrategy(
        for error: NavCaddyError,
        context: SessionContext?,
        operationId: String
    ) -> ErrorRecoveryStrategy {
        // Get retry attempts for this operation
        let attempts = retryAttempts[operationId, default: 0]

        // Determine if we should retry
        let shouldRetry = error.shouldAutoRetry && attempts < maxRetries

        // Get suggestions
        let suggestions: [IntentSuggestion]
        if error.shouldShowSuggestions {
            suggestions = LocalIntentSuggestions.contextAwareSuggestions(
                for: error,
                context: context
            )
        } else {
            suggestions = []
        }

        // Get recovery actions
        let actions = LocalIntentSuggestions.recoveryActions(for: error)

        // Calculate backoff if retrying
        let backoffMs: Int?
        if shouldRetry {
            let backoff = calculateBackoff(attempt: attempts)
            backoffMs = Int(backoff)
            // Increment retry count
            retryAttempts[operationId] = attempts + 1
        } else {
            backoffMs = nil
            // Clear retry count for this operation
            retryAttempts.removeValue(forKey: operationId)
        }

        return ErrorRecoveryStrategy(
            error: error,
            userMessage: error.userMessage,
            isRecoverable: error.isRecoverable,
            shouldAutoRetry: shouldRetry,
            retryBackoffMs: backoffMs,
            suggestions: suggestions,
            recoveryActions: actions
        )
    }

    /// Calculate exponential backoff with jitter.
    private func calculateBackoff(attempt: Int) -> Double {
        let exponential = baseBackoffMs * pow(2.0, Double(attempt))
        let withJitter = exponential * (0.5 + Double.random(in: 0...0.5))
        return min(withJitter, maxBackoffMs)
    }

    /// Reset retry attempts for an operation.
    func resetRetries(for operationId: String) {
        retryAttempts.removeValue(forKey: operationId)
    }

    /// Clear all retry state.
    func clearRetryState() {
        retryAttempts.removeAll()
    }

    // MARK: - Error Type Mapping

    private func mapErrorType(_ error: NavCaddyError) -> ErrorOccurredEvent.ErrorType {
        switch error.category {
        case .llm:
            if case .llmTimeout = error {
                return .timeout
            }
            return .classification
        case .voice:
            return .transcription
        case .network:
            return .network
        case .classification:
            return .classification
        case .routing:
            return .routing
        case .unknown:
            return .unknown
        }
    }
}

// MARK: - Error Recovery Strategy

/// Represents a strategy for recovering from an error.
struct ErrorRecoveryStrategy {
    /// The original error
    let error: NavCaddyError

    /// User-friendly error message
    let userMessage: String

    /// Whether the error is recoverable
    let isRecoverable: Bool

    /// Whether the system should automatically retry
    let shouldAutoRetry: Bool

    /// Backoff time in milliseconds before retry (if auto-retrying)
    let retryBackoffMs: Int?

    /// Suggested intents the user can try
    let suggestions: [IntentSuggestion]

    /// Recovery actions the user can take
    let recoveryActions: [RecoveryAction]

    /// Whether suggestions should be shown
    var hasSuggestions: Bool {
        !suggestions.isEmpty
    }

    /// Whether recovery actions should be shown
    var hasRecoveryActions: Bool {
        !recoveryActions.isEmpty
    }
}

// MARK: - Convenience Extensions

extension NavCaddyErrorHandler {
    /// Handle an error and execute automatic retry if applicable.
    ///
    /// - Parameters:
    ///   - error: The error to handle
    ///   - context: Optional session context
    ///   - operationId: Unique operation ID
    ///   - retryBlock: Async block to retry
    /// - Returns: Recovery strategy
    func handleWithRetry<T>(
        _ error: NavCaddyError,
        context: SessionContext?,
        operationId: String = UUID().uuidString,
        retryBlock: () async throws -> T
    ) async -> ErrorRecoveryStrategy {
        let strategy = handle(error, context: context, operationId: operationId)

        // If should auto-retry, schedule it
        if strategy.shouldAutoRetry, let backoffMs = strategy.retryBackoffMs {
            // Wait for backoff period
            try? await Task.sleep(nanoseconds: UInt64(backoffMs) * 1_000_000)

            // Retry the operation
            do {
                _ = try await retryBlock()
                // Success - reset retry count
                resetRetries(for: operationId)
            } catch {
                // Retry failed - will be handled by caller
            }
        }

        return strategy
    }
}

// MARK: - Error Pattern Detection

extension NavCaddyErrorHandler {
    /// Detect patterns in errors for diagnostics.
    ///
    /// This can help identify systemic issues (e.g., consistent timeouts,
    /// repeated permission denials) that need attention.
    func detectPatterns(sessionId: String) -> [ErrorPattern] {
        let events = analytics.getEvents(for: sessionId)

        let errorEvents = events.compactMap { event -> ErrorOccurredEvent? in
            if case .errorOccurred(let errorEvent) = event {
                return errorEvent
            }
            return nil
        }

        guard !errorEvents.isEmpty else { return [] }

        var patterns: [ErrorPattern] = []

        // Check for repeated timeouts
        let timeouts = errorEvents.filter { $0.errorType == .timeout }
        if timeouts.count >= 3 {
            patterns.append(.repeatedTimeouts(count: timeouts.count))
        }

        // Check for repeated network errors
        let networkErrors = errorEvents.filter { $0.errorType == .network }
        if networkErrors.count >= 3 {
            patterns.append(.networkInstability(count: networkErrors.count))
        }

        // Check for permission issues
        let permissionErrors = errorEvents.filter {
            $0.message.contains("permission") || $0.message.contains("Permission")
        }
        if !permissionErrors.isEmpty {
            patterns.append(.permissionIssues)
        }

        return patterns
    }
}

// MARK: - Error Pattern

/// Represents a detected error pattern.
enum ErrorPattern {
    case repeatedTimeouts(count: Int)
    case networkInstability(count: Int)
    case permissionIssues

    var userMessage: String {
        switch self {
        case .repeatedTimeouts(let count):
            return "I've noticed \(count) timeouts in this session. This might indicate slow network or high server load."
        case .networkInstability(let count):
            return "I've detected \(count) network errors. Your connection might be unstable."
        case .permissionIssues:
            return "Some features need permissions to work properly. Check your settings."
        }
    }
}
