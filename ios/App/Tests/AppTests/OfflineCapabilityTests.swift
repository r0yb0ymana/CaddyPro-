import XCTest
@testable import App

/**
 * Unit tests for OfflineCapability.
 *
 * Tests:
 * - Offline-capable intent list
 * - Routing target generation
 * - User message generation
 * - Offline suggestions
 *
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
final class OfflineCapabilityTests: XCTestCase {

    // MARK: - Capability Tests

    func testOfflineCapableIntents() {
        let offlineIntents: [IntentType] = [
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

        for intent in offlineIntents {
            XCTAssertTrue(
                OfflineCapability.isOfflineCapable(intent),
                "\(intent) should be offline-capable"
            )
        }
    }

    func testOnlineOnlyIntents() {
        let onlineOnlyIntents: [IntentType] = [
            .shotRecommendation,
            .recoveryCheck,
            .drillRequest,
            .weatherCheck,
            .courseInfo,
            .feedback
        ]

        for intent in onlineOnlyIntents {
            XCTAssertFalse(
                OfflineCapability.isOfflineCapable(intent),
                "\(intent) should NOT be offline-capable"
            )
        }
    }

    // MARK: - Routing Target Tests

    func testOfflineRoutingTargets() {
        let testCases: [(IntentType, Module, String)] = [
            (.scoreEntry, .caddy, "score_entry"),
            (.statsLookup, .caddy, "stats"),
            (.equipmentInfo, .caddy, "equipment"),
            (.roundStart, .caddy, "round_start"),
            (.roundEnd, .caddy, "round_summary"),
            (.settingsChange, .settings, "main"),
            (.helpRequest, .settings, "help"),
            (.clubAdjustment, .caddy, "club_adjustment"),
            (.patternQuery, .caddy, "patterns")
        ]

        for (intent, expectedModule, expectedScreen) in testCases {
            let target = OfflineCapability.routingTarget(for: intent)

            XCTAssertNotNil(target, "\(intent) should have routing target")
            XCTAssertEqual(target?.module, expectedModule)
            XCTAssertEqual(target?.screen, expectedScreen)
        }
    }

    func testOnlineIntentsHaveNoRoutingTarget() {
        let onlineIntents: [IntentType] = [
            .shotRecommendation,
            .recoveryCheck,
            .drillRequest
        ]

        for intent in onlineIntents {
            let target = OfflineCapability.routingTarget(for: intent)
            XCTAssertNil(target, "\(intent) should not have offline routing target")
        }
    }

    // MARK: - Message Tests

    func testOfflineMessages() {
        let offlineIntents: [IntentType] = [
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

        for intent in offlineIntents {
            let message = OfflineCapability.offlineMessage(for: intent)

            XCTAssertFalse(message.isEmpty, "\(intent) should have offline message")
            XCTAssertTrue(
                message.lowercased().contains("offline") ||
                message.lowercased().contains("sync") ||
                message.lowercased().contains("online"),
                "Message should mention offline/sync: \(message)"
            )
        }
    }

    func testUnavailableMessages() {
        let onlineIntents: [IntentType] = [
            .shotRecommendation,
            .recoveryCheck,
            .weatherCheck,
            .drillRequest
        ]

        for intent in onlineIntents {
            let message = OfflineCapability.unavailableMessage(for: intent)

            XCTAssertFalse(message.isEmpty, "\(intent) should have unavailable message")
            XCTAssertTrue(
                message.lowercased().contains("connection") ||
                message.lowercased().contains("offline"),
                "Message should explain network requirement: \(message)"
            )
        }
    }

    // MARK: - Suggestion Tests

    func testOfflineSuggestions() {
        let suggestions = OfflineCapability.offlineSuggestions()

        XCTAssertFalse(suggestions.isEmpty, "Should provide offline suggestions")
        XCTAssertLessThanOrEqual(suggestions.count, 3, "Should have max 3 suggestions")

        // All suggestions should be offline-capable
        for suggestion in suggestions {
            XCTAssertTrue(
                OfflineCapability.isOfflineCapable(suggestion.intentType),
                "\(suggestion.intentType) in suggestions should be offline-capable"
            )
        }
    }

    func testContextAwareOfflineSuggestionsWithRound() {
        let suggestions = OfflineCapability.contextAwareOfflineSuggestions(isRoundActive: true)

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)

        // Should prioritize round-related intents
        let intentTypes = suggestions.map { $0.intentType }
        XCTAssertTrue(
            intentTypes.contains(.scoreEntry) || intentTypes.contains(.roundEnd),
            "Should include round-related intents"
        )
    }

    func testContextAwareOfflineSuggestionsWithoutRound() {
        let suggestions = OfflineCapability.contextAwareOfflineSuggestions(isRoundActive: false)

        XCTAssertFalse(suggestions.isEmpty)
        XCTAssertLessThanOrEqual(suggestions.count, 3)

        // Should include general intents
        let intentTypes = suggestions.map { $0.intentType }
        XCTAssertTrue(intentTypes.contains(.roundStart), "Should suggest starting a round")
    }

    // MARK: - Configuration Tests

    func testOfflineModeConfiguration() {
        let config = OfflineModeConfiguration.default

        XCTAssertTrue(config.showOfflineIndicator, "Should show offline indicator by default")
        XCTAssertFalse(config.queueOnlineRequests, "Should not queue requests by default (privacy)")
        XCTAssertTrue(config.showLimitedFunctionalityWarnings, "Should show warnings by default")
    }

    // MARK: - Suggestion Quality Tests

    func testSuggestionLabelsAreUserFriendly() {
        let suggestions = OfflineCapability.offlineSuggestions()

        for suggestion in suggestions {
            // Label should be short and clear
            XCTAssertFalse(suggestion.label.isEmpty)
            XCTAssertLessThan(suggestion.label.count, 30, "Label should be concise")

            // Description should be informative
            XCTAssertFalse(suggestion.description.isEmpty)
            XCTAssertGreaterThan(suggestion.description.count, 10, "Description should be meaningful")
        }
    }

    func testSuggestionIdsAreUnique() {
        let suggestions = OfflineCapability.offlineSuggestions()
        let ids = Set(suggestions.map { $0.id })

        XCTAssertEqual(ids.count, suggestions.count, "All suggestion IDs should be unique")
    }
}
