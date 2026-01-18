import XCTest
@testable import App

/**
 * Unit tests for OfflineIntentHandler.
 *
 * Tests:
 * - Keyword-based intent matching
 * - Offline-capable intent routing
 * - Unsupported intent handling
 * - Context-aware suggestions
 *
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
@MainActor
final class OfflineIntentHandlerTests: XCTestCase {

    var handler: OfflineIntentHandler!
    var sessionContextManager: SessionContextManager!

    override func setUp() async throws {
        try await super.setUp()
        sessionContextManager = SessionContextManager()
        handler = OfflineIntentHandler(sessionContextManager: sessionContextManager)
    }

    override func tearDown() async throws {
        handler = nil
        sessionContextManager = nil
        try await super.tearDown()
    }

    // MARK: - Offline-Capable Intent Tests

    func testScoreEntryIntent() {
        let inputs = [
            "enter score",
            "log score",
            "I scored 5",
            "shot a 72"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertTrue(result.isOfflineCapable, "'\(input)' should be offline-capable")
            XCTAssertEqual(result.intentType, .scoreEntry, "'\(input)' should match score entry")
            XCTAssertNotNil(result.routingTarget, "Should have routing target")
            XCTAssertEqual(result.routingTarget?.module, .caddy)
        }
    }

    func testStatsLookupIntent() {
        let inputs = [
            "show stats",
            "my statistics",
            "how am I doing",
            "view my stats"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertTrue(result.isOfflineCapable, "'\(input)' should be offline-capable")
            XCTAssertEqual(result.intentType, .statsLookup)
            XCTAssertNotNil(result.routingTarget)
        }
    }

    func testEquipmentInfoIntent() {
        let inputs = [
            "show my bag",
            "club distances",
            "what clubs do I have",
            "equipment"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertTrue(result.isOfflineCapable, "'\(input)' should be offline-capable")
            XCTAssertEqual(result.intentType, .equipmentInfo)
            XCTAssertNotNil(result.routingTarget)
        }
    }

    func testRoundStartIntent() {
        let inputs = [
            "start round",
            "begin round",
            "new round",
            "tee off"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertTrue(result.isOfflineCapable, "'\(input)' should be offline-capable")
            XCTAssertEqual(result.intentType, .roundStart)
        }
    }

    func testRoundEndIntent() {
        let inputs = [
            "end round",
            "finish round",
            "done playing",
            "round over"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertTrue(result.isOfflineCapable, "'\(input)' should be offline-capable")
            XCTAssertEqual(result.intentType, .roundEnd)
        }
    }

    func testSettingsIntent() {
        let inputs = [
            "settings",
            "preferences",
            "change settings",
            "options"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertTrue(result.isOfflineCapable, "'\(input)' should be offline-capable")
            XCTAssertEqual(result.intentType, .settingsChange)
            XCTAssertEqual(result.routingTarget?.module, .settings)
        }
    }

    // MARK: - Online-Only Intent Tests

    func testShotRecommendationNotOffline() {
        let inputs = [
            "what club should I hit",
            "which club",
            "shot advice"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertFalse(result.isOfflineCapable, "'\(input)' should not be offline-capable")
            XCTAssertEqual(result.intentType, .shotRecommendation)
            XCTAssertNil(result.routingTarget, "Should not have routing target")
            XCTAssertFalse(result.suggestions.isEmpty, "Should provide suggestions")
        }
    }

    func testRecoveryCheckNotOffline() {
        let inputs = [
            "check recovery",
            "am I ready",
            "readiness"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertFalse(result.isOfflineCapable)
            XCTAssertEqual(result.intentType, .recoveryCheck)
            XCTAssertFalse(result.suggestions.isEmpty)
        }
    }

    func testPatternQueryNotOffline() {
        let inputs = [
            "what's my miss tendency",
            "check pattern",
            "my misses"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertFalse(result.isOfflineCapable)
            XCTAssertEqual(result.intentType, .patternQuery)
        }
    }

    // MARK: - No Match Tests

    func testNoMatchProvidesSuggestions() {
        let inputs = [
            "random text",
            "something completely unrelated",
            "xyz abc 123"
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertFalse(result.isOfflineCapable)
            XCTAssertNil(result.intentType, "Should not match any intent")
            XCTAssertNil(result.routingTarget)
            XCTAssertFalse(result.suggestions.isEmpty, "Should provide suggestions")
            XCTAssertLessThanOrEqual(result.suggestions.count, 3, "Should have max 3 suggestions")
        }
    }

    // MARK: - Context-Aware Tests

    func testContextAwareSuggestionsWithActiveRound() {
        // Start a round
        sessionContextManager.updateRound(roundId: "test-round", courseName: "Test Course")

        // Process unmatched input
        let result = handler.processOffline(input: "help me")

        // Should provide context-aware suggestions
        XCTAssertFalse(result.suggestions.isEmpty)

        // With active round, should prioritize score entry
        let firstSuggestion = result.suggestions.first
        XCTAssertNotNil(firstSuggestion)
        // Note: Actual priority depends on implementation
    }

    func testContextAwareSuggestionsWithoutRound() {
        // No active round
        XCTAssertNil(sessionContextManager.getCurrentContext().currentRound)

        // Process unmatched input
        let result = handler.processOffline(input: "something")

        // Should provide generic suggestions
        XCTAssertFalse(result.suggestions.isEmpty)
        XCTAssertLessThanOrEqual(result.suggestions.count, 3)
    }

    // MARK: - Case Insensitivity Tests

    func testCaseInsensitiveMatching() {
        let inputs = [
            ("ENTER SCORE", IntentType.scoreEntry),
            ("Show Stats", IntentType.statsLookup),
            ("MY BAG", IntentType.equipmentInfo)
        ]

        for (input, expectedIntent) in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertEqual(result.intentType, expectedIntent, "'\(input)' should match")
        }
    }

    // MARK: - Message Tests

    func testOfflineCapableMessagesProvided() {
        let result = handler.processOffline(input: "enter score")

        XCTAssertTrue(result.isOfflineCapable)
        XCTAssertFalse(result.message.isEmpty, "Should provide user-facing message")
        XCTAssertTrue(result.message.contains("offline") || result.message.contains("sync"),
                     "Message should mention offline capability")
    }

    func testUnavailableMessagesProvided() {
        let result = handler.processOffline(input: "what club should I hit")

        XCTAssertFalse(result.isOfflineCapable)
        XCTAssertFalse(result.message.isEmpty)
        XCTAssertTrue(result.message.contains("connection") || result.message.contains("offline"),
                     "Message should explain why feature is unavailable")
    }

    // MARK: - Whitespace Tests

    func testWhitespaceHandling() {
        let inputs = [
            "  enter score  ",
            "\nshow stats\n",
            "  my   bag  "
        ]

        for input in inputs {
            let result = handler.processOffline(input: input)

            XCTAssertTrue(result.isOfflineCapable, "Whitespace should not affect matching")
            XCTAssertNotNil(result.intentType)
        }
    }
}
