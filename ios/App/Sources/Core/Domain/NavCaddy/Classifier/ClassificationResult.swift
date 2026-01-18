import Foundation

/// Result of intent classification.
///
/// Determines the next action based on confidence thresholds.
/// Spec Q2: Route (>=0.75), Confirm (0.50-0.74), Clarify (<0.50)
enum ClassificationResult {
    /// High confidence: route directly to target
    /// confidence >= 0.75
    case route(intent: ParsedIntent, target: RoutingTarget)

    /// Medium confidence: ask for confirmation before routing
    /// confidence 0.50-0.74
    case confirm(intent: ParsedIntent, message: String)

    /// Low confidence: request clarification
    /// confidence < 0.50
    case clarify(response: ClarificationResponse)

    /// Classification failed with error
    case error(Error)
}

extension ClassificationResult {
    /// The confidence level of the result
    var confidence: Double? {
        switch self {
        case .route(let intent, _):
            return intent.confidence
        case .confirm(let intent, _):
            return intent.confidence
        case .clarify, .error:
            return nil
        }
    }

    /// Whether this result represents a successful classification
    var isSuccess: Bool {
        switch self {
        case .route, .confirm, .clarify:
            return true
        case .error:
            return false
        }
    }

    /// The parsed intent if available
    var intent: ParsedIntent? {
        switch self {
        case .route(let intent, _):
            return intent
        case .confirm(let intent, _):
            return intent
        case .clarify, .error:
            return nil
        }
    }
}
