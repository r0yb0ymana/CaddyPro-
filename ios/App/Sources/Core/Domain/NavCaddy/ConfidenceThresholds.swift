import Foundation

/// Confidence threshold constants for intent routing decisions.
///
/// Spec Q2 (Resolved): Balanced thresholds approach
/// - Route: >= 0.75 (high confidence, direct navigation)
/// - Confirm: 0.50 - 0.74 (medium confidence, ask for confirmation)
/// - Clarify: < 0.50 (low confidence, request clarification)
enum ConfidenceThresholds {
    /// Minimum confidence to route directly without confirmation
    static let route: Double = 0.75

    /// Minimum confidence to show confirmation before routing
    static let confirm: Double = 0.50

    /// Below this threshold, clarification is required
    /// (Anything below confirm threshold triggers clarification)
    static let clarify: Double = 0.50

    /// Determines the action to take based on confidence score
    static func action(for confidence: Double) -> ThresholdAction {
        if confidence >= route {
            return .route
        } else if confidence >= confirm {
            return .confirm
        } else {
            return .clarify
        }
    }
}

/// Represents the action to take based on confidence threshold
enum ThresholdAction {
    case route      // High confidence: navigate directly
    case confirm    // Medium confidence: ask for confirmation
    case clarify    // Low confidence: request clarification
}
