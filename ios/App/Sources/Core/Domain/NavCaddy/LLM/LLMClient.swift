import Foundation

/// Client for LLM-based intent classification.
///
/// This protocol allows for different implementations (Gemini, OpenAI, mock, etc.)
/// and facilitates testing by enabling dependency injection.
///
/// Spec R2: Intent classification and entity extraction using LLM.
protocol LLMClient {
    /// Classifies user input and extracts intent with entities.
    ///
    /// - Parameters:
    ///   - input: The user's input text or transcribed speech
    ///   - context: Optional session context for conversation continuity
    /// - Returns: LLM response containing classification result
    /// - Throws: LLMError if classification fails
    func classify(input: String, context: SessionContext?) async throws -> LLMResponse
}

/// Errors that can occur during LLM operations.
enum LLMError: Error, Equatable {
    /// Network request failed
    case networkError(String)

    /// Request timed out
    case timeout

    /// Failed to parse LLM response
    case parseError(String)

    /// Invalid API key or authentication failure
    case authenticationError

    /// Rate limit exceeded
    case rateLimitExceeded

    /// Invalid input provided
    case invalidInput(String)

    /// Generic error with message
    case unknown(String)
}

extension LLMError: LocalizedError {
    var errorDescription: String? {
        switch self {
        case .networkError(let message):
            return "Network error: \(message)"
        case .timeout:
            return "Request timed out"
        case .parseError(let message):
            return "Failed to parse response: \(message)"
        case .authenticationError:
            return "Authentication failed"
        case .rateLimitExceeded:
            return "Rate limit exceeded"
        case .invalidInput(let message):
            return "Invalid input: \(message)"
        case .unknown(let message):
            return "Unknown error: \(message)"
        }
    }
}
