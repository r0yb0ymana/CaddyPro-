import Foundation

/// Response from an LLM classification request.
///
/// Contains the raw response text and metadata about the request.
struct LLMResponse {
    /// Raw response text from the LLM
    let rawResponse: String

    /// Latency of the LLM request in milliseconds
    let latencyMs: Int

    /// Model identifier used for the request
    let model: String

    /// Timestamp when the request was made
    let timestamp: Date

    /// Optional token usage information
    let tokenUsage: TokenUsage?

    init(
        rawResponse: String,
        latencyMs: Int,
        model: String,
        timestamp: Date = Date(),
        tokenUsage: TokenUsage? = nil
    ) {
        self.rawResponse = rawResponse
        self.latencyMs = latencyMs
        self.model = model
        self.timestamp = timestamp
        self.tokenUsage = tokenUsage
    }
}

/// Token usage information for an LLM request.
struct TokenUsage {
    let inputTokens: Int
    let outputTokens: Int

    var totalTokens: Int {
        inputTokens + outputTokens
    }
}
