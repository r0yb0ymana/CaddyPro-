import XCTest
@testable import App

/**
 * Unit tests for LocalIntentSuggestions.
 *
 * Tests fallback suggestions for various error scenarios and offline mode.
 *
 * Spec reference: navcaddy-engine.md R7, G6
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
final class LocalIntentSuggestionsTests: XCTestCase {

    // MARK: - Offline Suggestions Tests

    func testOfflineSuggestionsAreOfflineCapable() {
        let suggestions = LocalIntentSuggestions.offlineSuggestions()

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)

        // All suggestions should be offline-capable
        let offlineTypes = LocalIntentSuggestions.offlineCapableIntents()
        for suggestion in suggestions {
            XCTAssertTrue(
                offlineTypes.contains(suggestion.intentType),
                "\(suggestion.intentType) should be offline-capable"
            )
        }
    }

    func testOfflineSuggestionsIncludeScoreEntry() {
        let suggestions = LocalIntentSuggestions.offlineSuggestions()

        let hasScoreEntry = suggestions.contains { $0.intentType == .scoreEntry }
        XCTAssertTrue(hasScoreEntry, "Offline suggestions should include score entry")
    }

    // MARK: - LLM Unavailable Suggestions Tests

    func testLLMUnavailableSuggestions() {
        let suggestions = LocalIntentSuggestions.llmUnavailableSuggestions()

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)

        // Should include basic intents that don't require complex AI
        let intentTypes = suggestions.map { $0.intentType }
        XCTAssertTrue(
            intentTypes.contains(.scoreEntry) ||
            intentTypes.contains(.recoveryCheck) ||
            intentTypes.contains(.statsLookup)
        )
    }

    // MARK: - Clarification Suggestions Tests

    func testClarificationSuggestions() {
        let suggestions = LocalIntentSuggestions.clarificationSuggestions()

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)

        // Should include common, high-value intents
        let intentTypes = suggestions.map { $0.intentType }
        XCTAssertTrue(intentTypes.contains(.shotRecommendation) || intentTypes.contains(.scoreEntry))
    }

    // MARK: - Common Task Suggestions Tests

    func testCommonTaskSuggestions() {
        let suggestions = LocalIntentSuggestions.commonTaskSuggestions()

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)

        // Should include frequently-used intents
        let intentTypes = suggestions.map { $0.intentType }
        XCTAssertTrue(intentTypes.contains(.shotRecommendation) || intentTypes.contains(.scoreEntry))
    }

    // MARK: - Error-Specific Suggestions Tests

    func testSuggestionsForLLMTimeout() {
        let error = NavCaddyError.llmTimeout
        let suggestions = LocalIntentSuggestions.suggestions(for: error)

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)
    }

    func testSuggestionsForNetworkUnavailable() {
        let error = NavCaddyError.networkUnavailable
        let suggestions = LocalIntentSuggestions.suggestions(for: error)

        XCTAssertFalse(suggestions.isEmpty)

        // All should be offline-capable
        let offlineTypes = LocalIntentSuggestions.offlineCapableIntents()
        for suggestion in suggestions {
            XCTAssertTrue(offlineTypes.contains(suggestion.intentType))
        }
    }

    func testSuggestionsForClassificationFailed() {
        let error = NavCaddyError.classificationFailed("Parse error")
        let suggestions = LocalIntentSuggestions.suggestions(for: error)

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)
    }

    func testSuggestionsForVoiceError() {
        let error = NavCaddyError.noSpeechDetected
        let suggestions = LocalIntentSuggestions.suggestions(for: error)

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)
    }

    func testSuggestionsForUnknownError() {
        let error = NavCaddyError.unknown("Something went wrong")
        let suggestions = LocalIntentSuggestions.suggestions(for: error)

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)
    }

    // MARK: - Context-Aware Suggestions Tests

    func testContextAwareSuggestionsWithoutContext() {
        let error = NavCaddyError.llmTimeout
        let suggestions = LocalIntentSuggestions.contextAwareSuggestions(for: error, context: nil)

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)
    }

    func testContextAwareSuggestionsWithActiveRound() {
        let context = SessionContext(
            sessionId: UUID().uuidString,
            currentRoundId: UUID().uuidString,
            currentHole: 5,
            conversationHistory: [],
            lastShotTimestamp: nil,
            lastRecommendation: nil
        )

        let error = NavCaddyError.llmTimeout
        let suggestions = LocalIntentSuggestions.contextAwareSuggestions(for: error, context: context)

        XCTAssertFalse(suggestions.isEmpty)

        // Score entry should be first for active rounds
        XCTAssertEqual(suggestions.first?.intentType, .scoreEntry)
    }

    func testContextAwareSuggestionsWithClubMention() {
        let context = SessionContext(
            sessionId: UUID().uuidString,
            currentRoundId: nil,
            currentHole: nil,
            conversationHistory: [
                ConversationTurn(
                    id: UUID().uuidString,
                    timestamp: Date(),
                    role: .user,
                    content: "How far does my 7-iron go?"
                ),
                ConversationTurn(
                    id: UUID().uuidString,
                    timestamp: Date(),
                    role: .assistant,
                    content: "Your 7-iron averages 150 yards."
                )
            ],
            lastShotTimestamp: nil,
            lastRecommendation: nil
        )

        let error = NavCaddyError.llmTimeout
        let suggestions = LocalIntentSuggestions.contextAwareSuggestions(for: error, context: context)

        // Should include club-related intent
        let hasClubIntent = suggestions.contains { $0.intentType == .clubAdjustment }
        XCTAssertTrue(hasClubIntent)
    }

    // MARK: - Offline Capability Tests

    func testScoreEntryIsOfflineCapable() {
        XCTAssertTrue(LocalIntentSuggestions.isOfflineCapable(.scoreEntry))
    }

    func testStatsLookupIsOfflineCapable() {
        XCTAssertTrue(LocalIntentSuggestions.isOfflineCapable(.statsLookup))
    }

    func testEquipmentInfoIsOfflineCapable() {
        XCTAssertTrue(LocalIntentSuggestions.isOfflineCapable(.equipmentInfo))
    }

    func testShotRecommendationIsNotOfflineCapable() {
        XCTAssertFalse(LocalIntentSuggestions.isOfflineCapable(.shotRecommendation))
    }

    func testRecoveryCheckIsNotOfflineCapable() {
        XCTAssertFalse(LocalIntentSuggestions.isOfflineCapable(.recoveryCheck))
    }

    func testOfflineCapableIntentsReturnsValidList() {
        let offlineIntents = LocalIntentSuggestions.offlineCapableIntents()

        XCTAssertFalse(offlineIntents.isEmpty)

        // Known offline intents
        XCTAssertTrue(offlineIntents.contains(.scoreEntry))
        XCTAssertTrue(offlineIntents.contains(.statsLookup))
        XCTAssertTrue(offlineIntents.contains(.equipmentInfo))
    }

    // MARK: - All Intents Tests

    func testAllIntentsReturnsComprehensiveList() {
        let allIntents = LocalIntentSuggestions.allIntents()

        XCTAssertFalse(allIntents.isEmpty)

        // Should include all major intent types
        let intentTypes = allIntents.map { $0.intentType }
        XCTAssertTrue(intentTypes.contains(.shotRecommendation))
        XCTAssertTrue(intentTypes.contains(.scoreEntry))
        XCTAssertTrue(intentTypes.contains(.recoveryCheck))
        XCTAssertTrue(intentTypes.contains(.clubAdjustment))
    }

    func testAllIntentsSorted() {
        let allIntents = LocalIntentSuggestions.allIntents()

        // Check that intents are sorted alphabetically by raw value
        let rawValues = allIntents.map { $0.intentType.rawValue }
        let sortedRawValues = rawValues.sorted()

        XCTAssertEqual(rawValues, sortedRawValues)
    }

    // MARK: - Recovery Actions Tests

    func testRecoveryActionsForPermissionError() {
        let error = NavCaddyError.voicePermissionDenied
        let actions = LocalIntentSuggestions.recoveryActions(for: error)

        XCTAssertFalse(actions.isEmpty)

        // Should include "Open Settings" action
        let hasOpenSettings = actions.contains { $0.action == .openSettings }
        XCTAssertTrue(hasOpenSettings)
    }

    func testRecoveryActionsForNetworkError() {
        let error = NavCaddyError.networkUnavailable
        let actions = LocalIntentSuggestions.recoveryActions(for: error)

        XCTAssertFalse(actions.isEmpty)

        // Should include check network and offline mode actions
        let actionTypes = actions.map { $0.action }
        XCTAssertTrue(actionTypes.contains(.checkNetwork) || actionTypes.contains(.useOfflineMode))
    }

    func testRecoveryActionsForTimeoutError() {
        let error = NavCaddyError.llmTimeout
        let actions = LocalIntentSuggestions.recoveryActions(for: error)

        XCTAssertFalse(actions.isEmpty)

        // Should include retry action
        let hasRetry = actions.contains { $0.action == .retry }
        XCTAssertTrue(hasRetry)
    }

    func testRecoveryActionsForRecoverableError() {
        let error = NavCaddyError.classificationFailed("Parse error")
        let actions = LocalIntentSuggestions.recoveryActions(for: error)

        // Recoverable errors may or may not have actions
        // Just verify it doesn't crash
        _ = actions
    }

    // MARK: - Intent Suggestion Model Tests

    func testIntentSuggestionIdentifiable() {
        let suggestion = IntentSuggestion(
            intentType: .scoreEntry,
            label: "Enter score",
            description: "Log your score"
        )

        XCTAssertEqual(suggestion.id, IntentType.scoreEntry.rawValue)
    }

    func testIntentSuggestionEquatable() {
        let suggestion1 = IntentSuggestion(
            intentType: .scoreEntry,
            label: "Enter score",
            description: "Log your score"
        )

        let suggestion2 = IntentSuggestion(
            intentType: .scoreEntry,
            label: "Enter score",
            description: "Log your score"
        )

        XCTAssertEqual(suggestion1, suggestion2)
    }

    // MARK: - Recovery Action Model Tests

    func testRecoveryActionIdentifiable() {
        let action = RecoveryAction(
            label: "Open Settings",
            action: .openSettings,
            description: "Grant permissions"
        )

        // ID should be unique
        XCTAssertNotNil(action.id)
    }

    func testRecoveryActionEquatable() {
        let action1 = RecoveryAction(
            label: "Open Settings",
            action: .openSettings,
            description: "Grant permissions"
        )

        let action2 = RecoveryAction(
            label: "Open Settings",
            action: .openSettings,
            description: "Grant permissions"
        )

        // Should be equal based on type, label, description (but different IDs)
        XCTAssertEqual(action1.action, action2.action)
        XCTAssertEqual(action1.label, action2.label)
    }

    // MARK: - Edge Cases

    func testMaxThreeSuggestions() {
        let error = NavCaddyError.llmTimeout
        let suggestions = LocalIntentSuggestions.contextAwareSuggestions(for: error, context: nil)

        // Should never exceed 3 suggestions (per spec)
        XCTAssertLessThanOrEqual(suggestions.count, 3)
    }

    func testNonEmptySuggestions() {
        // Test all major error types have suggestions
        let errors: [NavCaddyError] = [
            .llmTimeout,
            .llmNetworkError("test"),
            .networkUnavailable,
            .classificationFailed("test"),
            .needsClarification,
            .noSpeechDetected
        ]

        for error in errors {
            let suggestions = LocalIntentSuggestions.suggestions(for: error)
            XCTAssertFalse(suggestions.isEmpty, "Error \(error) should have suggestions")
        }
    }
}
