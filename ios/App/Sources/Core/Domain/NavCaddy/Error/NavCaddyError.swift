import Foundation

/**
 * Comprehensive error types for the NavCaddy system.
 *
 * Consolidates all error types from LLM, voice, routing, and network layers
 * into a unified error hierarchy with recovery information.
 *
 * Spec reference: navcaddy-engine.md R7, G6
 * Plan reference: navcaddy-engine-plan.md Task 23
 */

/// Unified error type for NavCaddy operations.
enum NavCaddyError: Error, Equatable {
    // MARK: - LLM Errors

    /// LLM request timed out
    case llmTimeout

    /// LLM network error
    case llmNetworkError(String)

    /// LLM authentication failed
    case llmAuthenticationError

    /// LLM rate limit exceeded
    case llmRateLimitExceeded

    /// LLM response parsing failed
    case llmParseError(String)

    /// Invalid LLM input
    case llmInvalidInput(String)

    // MARK: - Voice Input Errors

    /// Speech recognition not available
    case voiceNotAvailable

    /// Speech recognition permission denied
    case voicePermissionDenied

    /// Microphone permission denied
    case microphonePermissionDenied

    /// No speech detected during voice input
    case noSpeechDetected

    /// Audio recording error
    case audioError

    /// Speech recognition locale not supported
    case localeNotSupported

    /// Voice recognition cancelled by user
    case voiceCancelled

    // MARK: - Network Errors

    /// General network error (offline, connection lost, etc.)
    case networkUnavailable

    /// Server error (5xx)
    case serverError(Int)

    /// Client error (4xx, except auth)
    case clientError(Int, String)

    // MARK: - Classification Errors

    /// Intent classification failed
    case classificationFailed(String)

    /// Low confidence classification (needs clarification)
    case needsClarification

    // MARK: - Routing Errors

    /// Required prerequisite data missing
    case prerequisiteMissing(String)

    /// Invalid routing target
    case invalidRoute(String)

    // MARK: - Generic Errors

    /// Unknown error with optional message
    case unknown(String?)
}

// MARK: - Error Properties

extension NavCaddyError {
    /// User-friendly error message suitable for display.
    var userMessage: String {
        switch self {
        // LLM Errors
        case .llmTimeout:
            return "The request took too long. Let me suggest some options instead."
        case .llmNetworkError:
            return "I'm having trouble connecting right now. Here are some things I can help with offline."
        case .llmAuthenticationError:
            return "Authentication error. Please check your settings."
        case .llmRateLimitExceeded:
            return "Too many requests. Please try again in a moment."
        case .llmParseError:
            return "I had trouble understanding that. Could you try rephrasing?"
        case .llmInvalidInput:
            return "I couldn't process that input. Please try again."

        // Voice Errors
        case .voiceNotAvailable:
            return "Voice input is not available on this device."
        case .voicePermissionDenied:
            return "Please enable Speech Recognition in Settings to use voice input."
        case .microphonePermissionDenied:
            return "Please enable Microphone access in Settings to use voice input."
        case .noSpeechDetected:
            return "I didn't catch that. Please try speaking again."
        case .audioError:
            return "Audio recording error. Please check your microphone."
        case .localeNotSupported:
            return "Voice input is not available for your language."
        case .voiceCancelled:
            return "Voice input was cancelled."

        // Network Errors
        case .networkUnavailable:
            return "You're offline. Here are some things I can help with without a connection."
        case .serverError(let code):
            return "Server error (\(code)). Please try again later."
        case .clientError(let code, let message):
            return "Error (\(code)): \(message)"

        // Classification Errors
        case .classificationFailed(let message):
            return "I couldn't understand that: \(message)"
        case .needsClarification:
            return "I'm not sure what you meant. Could you clarify?"

        // Routing Errors
        case .prerequisiteMissing(let message):
            return message
        case .invalidRoute(let message):
            return "Navigation error: \(message)"

        // Generic
        case .unknown(let message):
            if let message = message {
                return "Something went wrong: \(message)"
            }
            return "Something went wrong. Please try again."
        }
    }

    /// Whether the error is recoverable through retry.
    var isRecoverable: Bool {
        switch self {
        // Recoverable with retry
        case .llmTimeout,
             .llmNetworkError,
             .llmRateLimitExceeded,
             .llmParseError,
             .noSpeechDetected,
             .audioError,
             .voiceCancelled,
             .networkUnavailable,
             .serverError,
             .clientError,
             .classificationFailed,
             .needsClarification,
             .prerequisiteMissing,
             .invalidRoute,
             .unknown:
            return true

        // Not recoverable by retry (need user action or not supported)
        case .llmAuthenticationError,
             .llmInvalidInput,
             .voiceNotAvailable,
             .voicePermissionDenied,
             .microphonePermissionDenied,
             .localeNotSupported:
            return false
        }
    }

    /// Category of the error for analytics tracking.
    var category: ErrorCategory {
        switch self {
        case .llmTimeout, .llmNetworkError, .llmAuthenticationError, .llmRateLimitExceeded,
             .llmParseError, .llmInvalidInput:
            return .llm

        case .voiceNotAvailable, .voicePermissionDenied, .microphonePermissionDenied,
             .noSpeechDetected, .audioError, .localeNotSupported, .voiceCancelled:
            return .voice

        case .networkUnavailable, .serverError, .clientError:
            return .network

        case .classificationFailed, .needsClarification:
            return .classification

        case .prerequisiteMissing, .invalidRoute:
            return .routing

        case .unknown:
            return .unknown
        }
    }

    /// Whether fallback suggestions should be shown.
    var shouldShowSuggestions: Bool {
        switch self {
        case .llmTimeout,
             .llmNetworkError,
             .llmRateLimitExceeded,
             .networkUnavailable,
             .classificationFailed,
             .needsClarification:
            return true
        default:
            return false
        }
    }

    /// Whether the system should automatically retry with exponential backoff.
    var shouldAutoRetry: Bool {
        switch self {
        case .llmTimeout, .llmNetworkError, .llmRateLimitExceeded, .serverError:
            return true
        default:
            return false
        }
    }
}

// MARK: - Error Category

/// High-level error category for analytics and routing.
enum ErrorCategory: String {
    case llm
    case voice
    case network
    case classification
    case routing
    case unknown
}

// MARK: - Error Conversion

extension NavCaddyError {
    /// Convert from LLMError.
    static func from(llmError: LLMError) -> NavCaddyError {
        switch llmError {
        case .networkError(let message):
            return .llmNetworkError(message)
        case .timeout:
            return .llmTimeout
        case .parseError(let message):
            return .llmParseError(message)
        case .authenticationError:
            return .llmAuthenticationError
        case .rateLimitExceeded:
            return .llmRateLimitExceeded
        case .invalidInput(let message):
            return .llmInvalidInput(message)
        case .unknown(let message):
            return .unknown(message)
        }
    }

    /// Convert from VoiceInputError.
    static func from(voiceError: VoiceInputError) -> NavCaddyError {
        switch voiceError {
        case .notAvailable:
            return .voiceNotAvailable
        case .permissionDenied:
            return .voicePermissionDenied
        case .microphonePermissionDenied:
            return .microphonePermissionDenied
        case .cancelled:
            return .voiceCancelled
        case .noSpeechDetected:
            return .noSpeechDetected
        case .audioError:
            return .audioError
        case .localeNotSupported:
            return .localeNotSupported
        case .networkError:
            return .networkUnavailable
        case .unknown(let message):
            return .unknown(message)
        }
    }
}

// MARK: - LocalizedError Conformance

extension NavCaddyError: LocalizedError {
    var errorDescription: String? {
        userMessage
    }
}
