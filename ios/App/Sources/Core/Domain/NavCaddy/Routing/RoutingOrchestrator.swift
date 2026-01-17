import Foundation

/// Orchestrates routing decisions based on classified intents.
///
/// Spec R3: Maps intent -> module route + parameters, validates prerequisites,
/// executes navigation as deep link or returns no-navigation result.
///
/// Thread-safe actor that processes classification results and determines
/// the appropriate routing action.
actor RoutingOrchestrator {
    private let prerequisiteChecker: PrerequisiteChecker

    init(prerequisiteChecker: PrerequisiteChecker) {
        self.prerequisiteChecker = prerequisiteChecker
    }

    /// Processes a classification result and returns the appropriate routing action
    ///
    /// - Parameter result: The classification result from IntentClassifier
    /// - Returns: A RoutingResult indicating the action to take
    func route(_ result: ClassificationResult) async -> RoutingResult {
        switch result {
        case .route(let intent, let target):
            // High confidence: validate prerequisites and route
            return await routeWithPrerequisites(intent: intent, target: target)

        case .confirm(let intent, let message):
            // Medium confidence: need user confirmation
            return .confirmationRequired(intent: intent, message: message)

        case .clarify(let response):
            // Low confidence: return as no-navigation with clarification message
            // Note: ClarificationResponse should be handled by UI layer
            return .noNavigation(
                intent: ParsedIntent(
                    intentType: .helpRequest,
                    confidence: 0.0
                ),
                response: response.message
            )

        case .error(let error):
            // Classification error: return as no-navigation with error message
            return .noNavigation(
                intent: ParsedIntent(
                    intentType: .helpRequest,
                    confidence: 0.0
                ),
                response: "Sorry, I couldn't understand that. \(error.localizedDescription)"
            )
        }
    }

    /// Routes an intent after validating prerequisites
    private func routeWithPrerequisites(intent: ParsedIntent, target: RoutingTarget) async -> RoutingResult {
        // Check if this intent type requires navigation
        if isNoNavigationIntent(intent.intentType) {
            return .noNavigation(
                intent: intent,
                response: generateNoNavigationResponse(for: intent)
            )
        }

        // Get required prerequisites for this intent
        let requiredPrerequisites = getRequiredPrerequisites(for: intent.intentType)

        // Check all prerequisites
        var missingPrerequisites: [Prerequisite] = []
        for prerequisite in requiredPrerequisites {
            let satisfied = await prerequisiteChecker.check(prerequisite)
            if !satisfied {
                missingPrerequisites.append(prerequisite)
            }
        }

        // If any prerequisites are missing, return prerequisite missing result
        if !missingPrerequisites.isEmpty {
            let message = buildPrerequisiteMessage(
                intent: intent.intentType,
                missing: missingPrerequisites
            )
            return .prerequisiteMissing(
                intent: intent,
                missing: missingPrerequisites,
                message: message
            )
        }

        // All prerequisites satisfied, proceed with navigation
        return .navigate(target: target, intent: intent)
    }

    /// Determines if an intent type should not navigate
    private func isNoNavigationIntent(_ intentType: IntentType) -> Bool {
        switch intentType {
        case .patternQuery, .helpRequest, .feedback:
            return true
        default:
            return false
        }
    }

    /// Gets the required prerequisites for an intent type
    private func getRequiredPrerequisites(for intentType: IntentType) -> [Prerequisite] {
        switch intentType {
        case .recoveryCheck:
            return [.recoveryData]

        case .scoreEntry, .roundEnd:
            return [.roundActive]

        case .clubAdjustment, .shotRecommendation:
            return [.bagConfigured]

        case .courseInfo:
            return [.courseSelected]

        case .drillRequest:
            // Optionally requires bag configured, but can work without it
            return []

        case .weatherCheck, .statsLookup, .roundStart, .equipmentInfo,
             .settingsChange, .helpRequest, .feedback, .patternQuery:
            return []
        }
    }

    /// Builds a user-facing message for missing prerequisites
    private func buildPrerequisiteMessage(
        intent: IntentType,
        missing: [Prerequisite]
    ) -> String {
        if missing.count == 1 {
            return missing[0].missingMessage
        } else {
            let items = missing.map { $0.description }.joined(separator: ", ")
            return "You need to set up: \(items) first."
        }
    }

    /// Generates a no-navigation response for informational intents (Bones persona)
    private func generateNoNavigationResponse(for intent: ParsedIntent) -> String {
        switch intent.intentType {
        case .patternQuery:
            return "Let me check your miss patterns. Based on your recent shots, I'll give you insights."

        case .helpRequest:
            return "I'm Bones, your digital caddy. Ask me about club selection, check your recovery, enter scores, or get coaching tips. What can I help you with?"

        case .feedback:
            return "Thanks for the feedback! I'm always learning to serve you better."

        default:
            return "I understand. Let me help you with that."
        }
    }
}
