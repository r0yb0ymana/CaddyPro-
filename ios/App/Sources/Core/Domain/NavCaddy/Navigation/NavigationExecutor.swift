import Foundation

/// Processes RoutingResult and executes navigation actions.
///
/// Spec R3: Executes navigation as deep link with <100ms latency budget.
/// Converts routing decisions into navigation coordinator actions.
@MainActor
struct NavigationExecutor {
    private let coordinator: NavigationCoordinator
    private let deepLinkBuilder: DeepLinkBuilder

    init(coordinator: NavigationCoordinator, deepLinkBuilder: DeepLinkBuilder = DeepLinkBuilder()) {
        self.coordinator = coordinator
        self.deepLinkBuilder = deepLinkBuilder
    }

    /// Executes a routing result
    ///
    /// - Parameter result: The routing result from RoutingOrchestrator
    /// - Returns: NavigationAction describing what UI action should be taken
    func execute(_ result: RoutingResult) -> NavigationAction {
        switch result {
        case .navigate(let target, let intent):
            return executeNavigation(target: target, intent: intent)

        case .noNavigation(let intent, let response):
            // No navigation needed, just show response
            return .showResponse(response: response, intent: intent)

        case .prerequisiteMissing(let intent, let missing, let message):
            // Prerequisites are missing, show message and optionally navigate to setup
            return .showPrerequisitePrompt(
                message: message,
                prerequisites: missing,
                intent: intent
            )

        case .confirmationRequired(let intent, let message):
            // Need user confirmation before proceeding
            return .requestConfirmation(message: message, intent: intent)
        }
    }

    /// Executes navigation for a valid routing target
    private func executeNavigation(target: RoutingTarget, intent: ParsedIntent) -> NavigationAction {
        // Validate target first
        guard deepLinkBuilder.validate(target) else {
            return .showError(
                message: "Invalid navigation target for \(intent.intentType.rawValue)",
                intent: intent
            )
        }

        // Build destination from target
        guard let destination = deepLinkBuilder.buildDestination(from: target) else {
            return .showError(
                message: "Could not build destination for \(target.screen)",
                intent: intent
            )
        }

        // Execute navigation
        coordinator.navigate(to: destination)

        // Return success action
        return .navigated(destination: destination, intent: intent)
    }

    /// Navigates back
    func navigateBack() {
        coordinator.navigateBack()
    }

    /// Pops to root
    func popToRoot() {
        coordinator.popToRoot()
    }

    /// Replaces current destination
    func replace(with destination: NavCaddyDestination) {
        coordinator.replace(with: destination)
    }
}

/// Describes the action that should be taken by the UI layer after processing a routing result
enum NavigationAction {
    /// Navigation was executed successfully
    case navigated(destination: NavCaddyDestination, intent: ParsedIntent)

    /// Show a response without navigation
    case showResponse(response: String, intent: ParsedIntent)

    /// Show a prerequisite prompt with missing items
    case showPrerequisitePrompt(message: String, prerequisites: [Prerequisite], intent: ParsedIntent)

    /// Request user confirmation before proceeding
    case requestConfirmation(message: String, intent: ParsedIntent)

    /// Show an error message
    case showError(message: String, intent: ParsedIntent)
}

extension NavigationAction {
    /// The intent associated with this action
    var intent: ParsedIntent {
        switch self {
        case .navigated(_, let intent),
             .showResponse(_, let intent),
             .showPrerequisitePrompt(_, _, let intent),
             .requestConfirmation(_, let intent),
             .showError(_, let intent):
            return intent
        }
    }

    /// User-facing message, if applicable
    var message: String? {
        switch self {
        case .navigated:
            return nil
        case .showResponse(let response, _):
            return response
        case .showPrerequisitePrompt(let message, _, _):
            return message
        case .requestConfirmation(let message, _):
            return message
        case .showError(let message, _):
            return message
        }
    }

    /// Whether this action requires showing UI feedback
    var requiresUIFeedback: Bool {
        switch self {
        case .navigated:
            return false // Navigation itself is visual feedback
        case .showResponse, .showPrerequisitePrompt, .requestConfirmation, .showError:
            return true
        }
    }
}
