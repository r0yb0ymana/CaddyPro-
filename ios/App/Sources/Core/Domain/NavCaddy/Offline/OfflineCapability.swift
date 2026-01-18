import Foundation

/**
 * Defines which intents can be handled offline.
 *
 * Offline-capable intents are those that:
 * - Don't require LLM processing
 * - Use only local data (SwiftData, UserDefaults)
 * - Have deterministic routing
 *
 * Spec reference: navcaddy-engine.md C6 (Offline behavior)
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
struct OfflineCapability {

    // MARK: - Offline Intent List

    /// Intents that can be handled offline.
    ///
    /// These intents are supported without network connectivity:
    /// - SCORE_ENTRY: Log scores locally
    /// - STATS_LOOKUP: View local statistics
    /// - EQUIPMENT_INFO: View club distances
    /// - ROUND_START: Start round tracking
    /// - ROUND_END: End round and save locally
    /// - SETTINGS_CHANGE: Update app settings
    /// - HELP_REQUEST: View static help content
    /// - CLUB_ADJUSTMENT: Adjust club distances locally
    /// - PATTERN_QUERY: View local pattern data
    static let offlineCapableIntents: Set<IntentType> = [
        .scoreEntry,
        .statsLookup,
        .equipmentInfo,
        .roundStart,
        .roundEnd,
        .settingsChange,
        .helpRequest,
        .clubAdjustment,
        .patternQuery
    ]

    // MARK: - Capability Checks

    /// Check if an intent can be handled offline.
    static func isOfflineCapable(_ intentType: IntentType) -> Bool {
        offlineCapableIntents.contains(intentType)
    }

    /// Get the routing target for an offline-capable intent.
    ///
    /// Returns nil if the intent is not offline-capable or requires
    /// additional context to determine the target.
    static func routingTarget(for intentType: IntentType) -> RoutingTarget? {
        guard isOfflineCapable(intentType) else {
            return nil
        }

        switch intentType {
        case .scoreEntry:
            return RoutingTarget(
                module: .caddy,
                screen: "score_entry",
                parameters: [:]
            )

        case .statsLookup:
            return RoutingTarget(
                module: .caddy,
                screen: "stats",
                parameters: [:]
            )

        case .equipmentInfo:
            return RoutingTarget(
                module: .caddy,
                screen: "equipment",
                parameters: [:]
            )

        case .roundStart:
            return RoutingTarget(
                module: .caddy,
                screen: "round_start",
                parameters: [:]
            )

        case .roundEnd:
            return RoutingTarget(
                module: .caddy,
                screen: "round_summary",
                parameters: [:]
            )

        case .settingsChange:
            return RoutingTarget(
                module: .settings,
                screen: "main",
                parameters: [:]
            )

        case .helpRequest:
            return RoutingTarget(
                module: .settings,
                screen: "help",
                parameters: [:]
            )

        case .clubAdjustment:
            return RoutingTarget(
                module: .caddy,
                screen: "club_adjustment",
                parameters: [:]
            )

        case .patternQuery:
            return RoutingTarget(
                module: .caddy,
                screen: "patterns",
                parameters: [:]
            )

        default:
            return nil
        }
    }

    // MARK: - User Messages

    /// Get user-facing message for offline capability.
    static func offlineMessage(for intentType: IntentType) -> String {
        switch intentType {
        case .scoreEntry:
            return "You can enter your score offline. It will sync when you're back online."

        case .statsLookup:
            return "Here are your stats. Some data might not be current while offline."

        case .equipmentInfo:
            return "Here's your bag setup. Changes will sync when you're back online."

        case .roundStart:
            return "Starting your round. Tracking will continue offline."

        case .roundEnd:
            return "Round complete! Your data will sync when you're back online."

        case .settingsChange:
            return "Settings updated. Some features may be limited while offline."

        case .helpRequest:
            return "Here's some help for you. Full documentation is available when you're online."

        case .clubAdjustment:
            return "Adjusting your club distances. Changes will sync when you're back online."

        case .patternQuery:
            return "Here are your patterns from local data. Full analysis available when you're online."

        default:
            return "This action is available offline."
        }
    }

    /// Get message explaining why an intent is not available offline.
    static func unavailableMessage(for intentType: IntentType) -> String {
        switch intentType {
        case .shotRecommendation:
            return "Shot recommendations need a connection to provide personalized advice."

        case .recoveryCheck:
            return "Recovery checks need a connection to analyze your data."

        case .drillRequest:
            return "Drill recommendations need a connection."

        case .weatherCheck:
            return "Weather information requires a connection."

        case .courseInfo:
            return "Course information requires a connection."

        case .feedback:
            return "Feedback will be sent when you're back online."

        default:
            return "This feature requires a network connection."
        }
    }
}

// MARK: - Offline Mode Configuration

/// Configuration for offline mode behavior.
struct OfflineModeConfiguration {
    /// Whether to show offline indicator in UI.
    let showOfflineIndicator: Bool

    /// Whether to queue online-only requests for when connection returns.
    let queueOnlineRequests: Bool

    /// Whether to show limited functionality warnings.
    let showLimitedFunctionalityWarnings: Bool

    /// Default configuration.
    static let `default` = OfflineModeConfiguration(
        showOfflineIndicator: true,
        queueOnlineRequests: false, // Don't auto-queue for privacy
        showLimitedFunctionalityWarnings: true
    )
}

// MARK: - Offline Suggestions

extension OfflineCapability {
    /// Get suggested offline-capable intents.
    static func offlineSuggestions() -> [IntentSuggestion] {
        [
            IntentSuggestion(
                intentType: .scoreEntry,
                label: "Enter score",
                description: "Log your score for this hole"
            ),
            IntentSuggestion(
                intentType: .equipmentInfo,
                label: "View my bag",
                description: "See your club setup"
            ),
            IntentSuggestion(
                intentType: .statsLookup,
                label: "View stats",
                description: "Check your performance statistics"
            )
        ]
    }

    /// Get context-aware offline suggestions based on round state.
    static func contextAwareOfflineSuggestions(isRoundActive: Bool) -> [IntentSuggestion] {
        if isRoundActive {
            return [
                IntentSuggestion(
                    intentType: .scoreEntry,
                    label: "Enter score",
                    description: "Log your score for this hole"
                ),
                IntentSuggestion(
                    intentType: .roundEnd,
                    label: "End round",
                    description: "Finish and save your round"
                ),
                IntentSuggestion(
                    intentType: .statsLookup,
                    label: "View stats",
                    description: "Check your performance"
                )
            ]
        } else {
            return [
                IntentSuggestion(
                    intentType: .roundStart,
                    label: "Start round",
                    description: "Begin tracking your round"
                ),
                IntentSuggestion(
                    intentType: .equipmentInfo,
                    label: "View my bag",
                    description: "See your club setup"
                ),
                IntentSuggestion(
                    intentType: .statsLookup,
                    label: "View stats",
                    description: "Check your performance"
                )
            ]
        }
    }
}
