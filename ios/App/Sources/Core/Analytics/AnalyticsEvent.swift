import Foundation

/**
 * Event schema for NavCaddy analytics and observability.
 *
 * Tracks key pipeline events with latency, confidence, and session correlation.
 *
 * Spec reference: navcaddy-engine.md R8
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
enum AnalyticsEvent {
    /// User input received (text or voice)
    case inputReceived(InputReceivedEvent)

    /// Intent classified by LLM
    case intentClassified(IntentClassifiedEvent)

    /// Route executed (navigation)
    case routeExecuted(RouteExecutedEvent)

    /// Error occurred in pipeline
    case errorOccurred(ErrorOccurredEvent)

    /// Voice transcription completed
    case voiceTranscription(VoiceTranscriptionEvent)
}

// MARK: - Event Details

/// Event: Input received from user
struct InputReceivedEvent {
    let timestamp: Date
    let inputType: InputType
    let sessionId: String

    enum InputType: String, Codable {
        case text
        case voice
    }
}

/// Event: Intent classified
struct IntentClassifiedEvent {
    let intent: String
    let confidence: Double
    let latencyMs: Int
    let sessionId: String
}

/// Event: Route executed (navigation triggered)
struct RouteExecutedEvent {
    let module: String
    let screen: String
    let latencyMs: Int
    let sessionId: String
}

/// Event: Error occurred
struct ErrorOccurredEvent {
    let errorType: ErrorType
    let message: String
    let isRecoverable: Bool
    let sessionId: String

    enum ErrorType: String, Codable {
        case network
        case timeout
        case transcription
        case classification
        case routing
        case unknown
    }
}

/// Event: Voice transcription completed
struct VoiceTranscriptionEvent {
    let latencyMs: Int
    let wordCount: Int
    let sessionId: String
}

// MARK: - Event Metadata

extension AnalyticsEvent {
    /// Get the event name for logging
    var name: String {
        switch self {
        case .inputReceived:
            return "input_received"
        case .intentClassified:
            return "intent_classified"
        case .routeExecuted:
            return "route_executed"
        case .errorOccurred:
            return "error_occurred"
        case .voiceTranscription:
            return "voice_transcription"
        }
    }

    /// Get the timestamp for the event
    var timestamp: Date {
        switch self {
        case .inputReceived(let event):
            return event.timestamp
        case .intentClassified, .routeExecuted, .errorOccurred, .voiceTranscription:
            return Date()
        }
    }

    /// Get the session ID for correlation
    var sessionId: String {
        switch self {
        case .inputReceived(let event):
            return event.sessionId
        case .intentClassified(let event):
            return event.sessionId
        case .routeExecuted(let event):
            return event.sessionId
        case .errorOccurred(let event):
            return event.sessionId
        case .voiceTranscription(let event):
            return event.sessionId
        }
    }

    /// Convert event to dictionary for logging
    var properties: [String: Any] {
        var props: [String: Any] = [
            "timestamp": ISO8601DateFormatter().string(from: timestamp),
            "session_id": sessionId
        ]

        switch self {
        case .inputReceived(let event):
            props["input_type"] = event.inputType.rawValue

        case .intentClassified(let event):
            props["intent"] = event.intent
            props["confidence"] = event.confidence
            props["latency_ms"] = event.latencyMs

        case .routeExecuted(let event):
            props["module"] = event.module
            props["screen"] = event.screen
            props["latency_ms"] = event.latencyMs

        case .errorOccurred(let event):
            props["error_type"] = event.errorType.rawValue
            props["message"] = event.message
            props["is_recoverable"] = event.isRecoverable

        case .voiceTranscription(let event):
            props["latency_ms"] = event.latencyMs
            props["word_count"] = event.wordCount
        }

        return props
    }
}
