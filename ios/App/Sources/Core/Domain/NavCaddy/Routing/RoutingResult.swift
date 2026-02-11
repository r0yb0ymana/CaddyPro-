import Foundation

/// Result of routing orchestration.
///
/// Spec R3: Routing orchestrator returns Navigate, NoNavigation, or PrerequisiteMissing.
enum RoutingResult {
    /// Navigate to target screen
    /// High-confidence intents route directly.
    case navigate(target: RoutingTarget, intent: ParsedIntent)

    /// Answer without navigation
    /// For informational intents like pattern_query, help_request.
    case noNavigation(intent: ParsedIntent, response: String)

    /// Prerequisite is missing
    /// Some intents require data before routing (e.g., recovery_check needs recovery data).
    case prerequisiteMissing(intent: ParsedIntent, missing: [Prerequisite], message: String)

    /// Confirmation required before routing
    /// Mid-confidence intents (0.50-0.74) need user confirmation.
    case confirmationRequired(intent: ParsedIntent, message: String)
}

extension RoutingResult {
    /// The parsed intent associated with this result
    var intent: ParsedIntent {
        switch self {
        case .navigate(_, let intent):
            return intent
        case .noNavigation(let intent, _):
            return intent
        case .prerequisiteMissing(let intent, _, _):
            return intent
        case .confirmationRequired(let intent, _):
            return intent
        }
    }

    /// Whether this result requires navigation
    var requiresNavigation: Bool {
        switch self {
        case .navigate:
            return true
        case .noNavigation, .prerequisiteMissing, .confirmationRequired:
            return false
        }
    }

    /// User-facing message if applicable
    var message: String? {
        switch self {
        case .navigate:
            return nil
        case .noNavigation(_, let response):
            return response
        case .prerequisiteMissing(_, _, let message):
            return message
        case .confirmationRequired(_, let message):
            return message
        }
    }
}
