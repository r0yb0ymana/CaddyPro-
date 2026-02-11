import Foundation

/**
 * Handles intents locally without LLM when offline.
 *
 * Features:
 * - Keyword-based intent matching
 * - Deterministic routing for offline-capable intents
 * - User-friendly error messages for unsupported intents
 * - Context-aware suggestions
 *
 * Spec reference: navcaddy-engine.md C6 (Offline behavior)
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
@MainActor
final class OfflineIntentHandler {

    // MARK: - Dependencies

    private let sessionContextManager: SessionContextManager

    // MARK: - Initialization

    init(sessionContextManager: SessionContextManager) {
        self.sessionContextManager = sessionContextManager
    }

    // MARK: - Intent Processing

    /// Process user input offline using keyword matching.
    ///
    /// - Parameter input: User's text input
    /// - Returns: Offline classification result
    func processOffline(input: String) -> OfflineClassificationResult {
        let normalizedInput = input.lowercased().trimmingCharacters(in: .whitespacesAndNewlines)

        // Try to match intent using keywords
        if let intentType = matchIntent(from: normalizedInput) {
            // Check if intent is offline-capable
            if OfflineCapability.isOfflineCapable(intentType) {
                return handleOfflineCapableIntent(intentType, input: input)
            } else {
                return handleUnsupportedIntent(intentType, input: input)
            }
        }

        // No match - return suggestions
        return noMatchResult(input: input)
    }

    // MARK: - Intent Matching

    /// Match intent from normalized input using keyword patterns.
    private func matchIntent(from input: String) -> IntentType? {
        // Define keyword patterns for each intent
        let patterns: [(IntentType, [String])] = [
            // Score entry
            (.scoreEntry, ["score", "scored", "log score", "enter score", "record score", "made a", "shot a"]),

            // Stats lookup
            (.statsLookup, ["stats", "statistics", "performance", "how am i doing", "show stats", "my stats"]),

            // Equipment info
            (.equipmentInfo, ["bag", "clubs", "equipment", "my bag", "show bag", "club distances", "what clubs"]),

            // Round start
            (.roundStart, ["start round", "begin round", "new round", "start playing", "tee off"]),

            // Round end
            (.roundEnd, ["end round", "finish round", "done playing", "round over", "complete round"]),

            // Settings
            (.settingsChange, ["settings", "preferences", "change settings", "configure", "options"]),

            // Club adjustment
            (.clubAdjustment, ["adjust club", "change distance", "update distance", "club distance", "my distances"]),

            // Shot recommendation (not offline-capable, but we want to detect it)
            (.shotRecommendation, ["what club", "which club", "club selection", "shot advice", "what should i hit"]),

            // Recovery check
            (.recoveryCheck, ["recovery", "am i ready", "readiness", "check recovery"]),

            // Pattern query
            (.patternQuery, ["pattern", "miss tendency", "what's my miss", "tendency", "misses"]),

            // Help
            (.helpRequest, ["help", "what can you do", "how do i", "instructions"])
        ]

        // Try to match each pattern
        for (intentType, keywords) in patterns {
            for keyword in keywords {
                if input.contains(keyword) {
                    return intentType
                }
            }
        }

        return nil
    }

    // MARK: - Result Builders

    /// Handle an offline-capable intent.
    private func handleOfflineCapableIntent(
        _ intentType: IntentType,
        input: String
    ) -> OfflineClassificationResult {
        guard let target = OfflineCapability.routingTarget(for: intentType) else {
            return noMatchResult(input: input)
        }

        let message = OfflineCapability.offlineMessage(for: intentType)

        return OfflineClassificationResult(
            isOfflineCapable: true,
            intentType: intentType,
            routingTarget: target,
            message: message,
            suggestions: []
        )
    }

    /// Handle an unsupported (online-only) intent.
    private func handleUnsupportedIntent(
        _ intentType: IntentType,
        input: String
    ) -> OfflineClassificationResult {
        let message = OfflineCapability.unavailableMessage(for: intentType)
        let context = sessionContextManager.getCurrentContext()
        let suggestions = OfflineCapability.contextAwareOfflineSuggestions(
            isRoundActive: context.isRoundActive
        )

        return OfflineClassificationResult(
            isOfflineCapable: false,
            intentType: intentType,
            routingTarget: nil,
            message: message + " Here are some things I can help with offline:",
            suggestions: suggestions
        )
    }

    /// Handle no match - show suggestions.
    private func noMatchResult(input: String) -> OfflineClassificationResult {
        let context = sessionContextManager.getCurrentContext()
        let suggestions = OfflineCapability.contextAwareOfflineSuggestions(
            isRoundActive: context.isRoundActive
        )

        return OfflineClassificationResult(
            isOfflineCapable: false,
            intentType: nil,
            routingTarget: nil,
            message: "You're offline. Here are some things I can help with:",
            suggestions: suggestions
        )
    }
}

// MARK: - Offline Classification Result

/// Result of offline intent classification.
struct OfflineClassificationResult {
    /// Whether the intent can be handled offline.
    let isOfflineCapable: Bool

    /// Detected intent type (nil if no match).
    let intentType: IntentType?

    /// Routing target (nil if not offline-capable or no match).
    let routingTarget: RoutingTarget?

    /// User-facing message.
    let message: String

    /// Suggested offline-capable intents.
    let suggestions: [IntentSuggestion]
}
