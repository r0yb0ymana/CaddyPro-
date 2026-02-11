import Foundation

/**
 * Provides local intent suggestions when LLM/network is unavailable.
 *
 * Offers a curated list of common intents that can be handled locally
 * or with minimal network connectivity. This ensures users always have
 * actionable options even during errors.
 *
 * Spec reference: navcaddy-engine.md R7, G6
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
struct LocalIntentSuggestions {
    /// Get fallback suggestions for a specific error type.
    ///
    /// - Parameter error: The error that occurred
    /// - Returns: Array of suggested intents with labels
    static func suggestions(for error: NavCaddyError) -> [IntentSuggestion] {
        switch error {
        case .llmTimeout, .llmNetworkError, .llmRateLimitExceeded:
            return llmUnavailableSuggestions()

        case .networkUnavailable:
            return offlineSuggestions()

        case .classificationFailed, .needsClarification:
            return clarificationSuggestions()

        case .noSpeechDetected, .voiceCancelled:
            return commonTaskSuggestions()

        default:
            return defaultSuggestions()
        }
    }

    /// Get suggestions when offline.
    ///
    /// Returns intents that can be handled without network connectivity.
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

    /// Get suggestions when LLM is unavailable.
    ///
    /// Returns intents that require minimal AI processing.
    static func llmUnavailableSuggestions() -> [IntentSuggestion] {
        [
            IntentSuggestion(
                intentType: .scoreEntry,
                label: "Enter score",
                description: "Log your score for this hole"
            ),
            IntentSuggestion(
                intentType: .recoveryCheck,
                label: "Check recovery",
                description: "View your readiness status"
            ),
            IntentSuggestion(
                intentType: .statsLookup,
                label: "View stats",
                description: "Check your performance"
            )
        ]
    }

    /// Get suggestions for clarification scenarios.
    ///
    /// Returns common intents users might be looking for.
    static func clarificationSuggestions() -> [IntentSuggestion] {
        [
            IntentSuggestion(
                intentType: .shotRecommendation,
                label: "Get shot advice",
                description: "What club should I hit?"
            ),
            IntentSuggestion(
                intentType: .scoreEntry,
                label: "Enter score",
                description: "Log your score"
            ),
            IntentSuggestion(
                intentType: .patternQuery,
                label: "Check my patterns",
                description: "What's my miss tendency?"
            )
        ]
    }

    /// Get common task suggestions.
    ///
    /// Returns the most frequently used intents.
    static func commonTaskSuggestions() -> [IntentSuggestion] {
        [
            IntentSuggestion(
                intentType: .shotRecommendation,
                label: "Get advice",
                description: "What's the play here?"
            ),
            IntentSuggestion(
                intentType: .scoreEntry,
                label: "Log score",
                description: "Enter your score"
            ),
            IntentSuggestion(
                intentType: .clubAdjustment,
                label: "Adjust clubs",
                description: "Update club distances"
            )
        ]
    }

    /// Get default suggestions.
    ///
    /// Returns a balanced set of common intents.
    static func defaultSuggestions() -> [IntentSuggestion] {
        [
            IntentSuggestion(
                intentType: .scoreEntry,
                label: "Enter score",
                description: "Log your score"
            ),
            IntentSuggestion(
                intentType: .shotRecommendation,
                label: "Get advice",
                description: "What club should I hit?"
            ),
            IntentSuggestion(
                intentType: .helpRequest,
                label: "Get help",
                description: "Learn what I can do"
            )
        ]
    }

    /// Get all available intents for help/exploration.
    ///
    /// Returns a comprehensive list of supported intents.
    static func allIntents() -> [IntentSuggestion] {
        IntentRegistry.getAllSchemas()
            .sorted { $0.intentType.rawValue < $1.intentType.rawValue }
            .map { schema in
                IntentSuggestion(
                    intentType: schema.intentType,
                    label: schema.displayName,
                    description: schema.description
                )
            }
    }

    /// Get intents available offline.
    static func offlineCapableIntents() -> [IntentType] {
        [
            .scoreEntry,
            .statsLookup,
            .equipmentInfo,
            .roundStart,
            .roundEnd,
            .settingsChange
        ]
    }

    /// Check if an intent can be handled offline.
    static func isOfflineCapable(_ intentType: IntentType) -> Bool {
        offlineCapableIntents().contains(intentType)
    }
}

// MARK: - Context-Aware Suggestions

extension LocalIntentSuggestions {
    /// Get context-aware suggestions based on session state.
    ///
    /// - Parameters:
    ///   - error: The error that occurred
    ///   - context: Optional session context for personalization
    /// - Returns: Personalized suggestions based on context
    static func contextAwareSuggestions(
        for error: NavCaddyError,
        context: SessionContext?
    ) -> [IntentSuggestion] {
        var baseSuggestions = suggestions(for: error)

        // If we have context, try to personalize
        if let context = context {
            baseSuggestions = personalizeWithContext(baseSuggestions, context: context)
        }

        // Limit to 3 suggestions max (per spec)
        return Array(baseSuggestions.prefix(3))
    }

    /// Personalize suggestions with session context.
    private static func personalizeWithContext(
        _ suggestions: [IntentSuggestion],
        context: SessionContext
    ) -> [IntentSuggestion] {
        var personalized = suggestions

        // If user is in an active round, prioritize round-related intents
        if context.isRoundActive {
            // Move score entry to front if not already there
            if let index = personalized.firstIndex(where: { $0.intentType == .scoreEntry }),
               index != 0 {
                let scoreEntry = personalized.remove(at: index)
                personalized.insert(scoreEntry, at: 0)
            }
        }

        // If user has recent conversation about clubs, suggest club-related intents
        if hasRecentClubMention(in: context) {
            let clubSuggestion = IntentSuggestion(
                intentType: .clubAdjustment,
                label: "Adjust club",
                description: "Update club distances"
            )
            if !personalized.contains(where: { $0.intentType == .clubAdjustment }) {
                personalized.insert(clubSuggestion, at: min(1, personalized.count))
            }
        }

        return personalized
    }

    /// Check if recent conversation mentions clubs.
    private static func hasRecentClubMention(in context: SessionContext) -> Bool {
        let recentTurns = context.conversationHistory.suffix(3)
        let clubKeywords = ["club", "iron", "driver", "wood", "wedge", "putter"]

        return recentTurns.contains { turn in
            clubKeywords.contains { keyword in
                turn.content.lowercased().contains(keyword)
            }
        }
    }
}

// MARK: - Recovery Actions

extension LocalIntentSuggestions {
    /// Get recovery actions for specific errors.
    ///
    /// Returns system-level actions users can take to resolve errors.
    static func recoveryActions(for error: NavCaddyError) -> [RecoveryAction] {
        switch error {
        case .voicePermissionDenied, .microphonePermissionDenied:
            return [
                RecoveryAction(
                    label: "Open Settings",
                    action: .openSettings,
                    description: "Grant microphone and speech permissions"
                )
            ]

        case .networkUnavailable, .llmNetworkError:
            return [
                RecoveryAction(
                    label: "Check connection",
                    action: .checkNetwork,
                    description: "Verify your internet connection"
                ),
                RecoveryAction(
                    label: "Use offline mode",
                    action: .useOfflineMode,
                    description: "Continue with limited features"
                )
            ]

        case .llmTimeout, .llmRateLimitExceeded:
            return [
                RecoveryAction(
                    label: "Try again",
                    action: .retry,
                    description: "Retry your request"
                )
            ]

        default:
            return []
        }
    }
}

// MARK: - Recovery Action Model

/// Represents a recovery action the user can take.
struct RecoveryAction: Identifiable, Equatable {
    let id = UUID()
    let label: String
    let action: RecoveryActionType
    let description: String
}

/// Type of recovery action.
enum RecoveryActionType: Equatable {
    case openSettings
    case checkNetwork
    case useOfflineMode
    case retry
    case dismiss
}
