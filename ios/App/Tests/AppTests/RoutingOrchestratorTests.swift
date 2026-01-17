import XCTest
@testable import App

final class RoutingOrchestratorTests: XCTestCase {
    var mockChecker: MockPrerequisiteChecker!
    var orchestrator: RoutingOrchestrator!

    override func setUp() async throws {
        try await super.setUp()
        mockChecker = MockPrerequisiteChecker()
        orchestrator = RoutingOrchestrator(prerequisiteChecker: mockChecker)
    }

    override func tearDown() async throws {
        mockChecker = nil
        orchestrator = nil
        try await super.tearDown()
    }

    // MARK: - High Confidence Routing Tests

    func testHighConfidenceRouteWithSatisfiedPrerequisites() async {
        // Given: Recovery data exists
        await mockChecker.setSatisfied(.recoveryData, satisfied: true)

        let intent = ParsedIntent(
            intentType: .recoveryCheck,
            confidence: 0.85
        )
        let target = RoutingTarget(
            module: .recovery,
            screen: "overview"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should navigate
        guard case .navigate(let routedTarget, let routedIntent) = result else {
            XCTFail("Expected navigate result")
            return
        }

        XCTAssertEqual(routedTarget.module, .recovery)
        XCTAssertEqual(routedTarget.screen, "overview")
        XCTAssertEqual(routedIntent.intentType, .recoveryCheck)
    }

    func testHighConfidenceRouteWithMissingPrerequisite() async {
        // Given: Recovery data does NOT exist
        await mockChecker.setSatisfied(.recoveryData, satisfied: false)

        let intent = ParsedIntent(
            intentType: .recoveryCheck,
            confidence: 0.85
        )
        let target = RoutingTarget(
            module: .recovery,
            screen: "overview"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should return prerequisite missing
        guard case .prerequisiteMissing(let routedIntent, let missing, let message) = result else {
            XCTFail("Expected prerequisiteMissing result")
            return
        }

        XCTAssertEqual(routedIntent.intentType, .recoveryCheck)
        XCTAssertEqual(missing.count, 1)
        XCTAssertEqual(missing.first, .recoveryData)
        XCTAssertFalse(message.isEmpty)
        XCTAssertTrue(message.contains("recovery data"))
    }

    // MARK: - No-Navigation Intent Tests

    func testPatternQueryReturnsNoNavigation() async {
        // Given: Pattern query intent
        let intent = ParsedIntent(
            intentType: .patternQuery,
            confidence: 0.90
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "patterns"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should return no navigation with response
        guard case .noNavigation(let routedIntent, let response) = result else {
            XCTFail("Expected noNavigation result")
            return
        }

        XCTAssertEqual(routedIntent.intentType, .patternQuery)
        XCTAssertFalse(response.isEmpty)
        XCTAssertTrue(response.contains("patterns") || response.contains("history"))
    }

    func testHelpRequestReturnsNoNavigation() async {
        // Given: Help request intent
        let intent = ParsedIntent(
            intentType: .helpRequest,
            confidence: 0.95
        )
        let target = RoutingTarget(
            module: .settings,
            screen: "help"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should return no navigation with help response
        guard case .noNavigation(let routedIntent, let response) = result else {
            XCTFail("Expected noNavigation result")
            return
        }

        XCTAssertEqual(routedIntent.intentType, .helpRequest)
        XCTAssertFalse(response.isEmpty)
        XCTAssertTrue(response.contains("help"))
    }

    func testFeedbackReturnsNoNavigation() async {
        // Given: Feedback intent
        let intent = ParsedIntent(
            intentType: .feedback,
            confidence: 0.88
        )
        let target = RoutingTarget(
            module: .settings,
            screen: "feedback"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should return no navigation with acknowledgment
        guard case .noNavigation(let routedIntent, let response) = result else {
            XCTFail("Expected noNavigation result")
            return
        }

        XCTAssertEqual(routedIntent.intentType, .feedback)
        XCTAssertFalse(response.isEmpty)
        XCTAssertTrue(response.contains("feedback") || response.contains("Thanks"))
    }

    // MARK: - Prerequisite Validation Tests

    func testScoreEntryRequiresActiveRound() async {
        // Given: No active round
        await mockChecker.setSatisfied(.roundActive, satisfied: false)

        let intent = ParsedIntent(
            intentType: .scoreEntry,
            confidence: 0.92
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "score_entry"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should require active round
        guard case .prerequisiteMissing(_, let missing, _) = result else {
            XCTFail("Expected prerequisiteMissing result")
            return
        }

        XCTAssertEqual(missing.count, 1)
        XCTAssertEqual(missing.first, .roundActive)
    }

    func testClubAdjustmentRequiresBagConfiguration() async {
        // Given: Bag not configured
        await mockChecker.setSatisfied(.bagConfigured, satisfied: false)

        let intent = ParsedIntent(
            intentType: .clubAdjustment,
            confidence: 0.87,
            entities: ExtractedEntities(club: Club(type: .iron, number: 7))
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should require bag configuration
        guard case .prerequisiteMissing(_, let missing, _) = result else {
            XCTFail("Expected prerequisiteMissing result")
            return
        }

        XCTAssertEqual(missing.count, 1)
        XCTAssertEqual(missing.first, .bagConfigured)
    }

    func testShotRecommendationRequiresBagConfiguration() async {
        // Given: Bag not configured
        await mockChecker.setSatisfied(.bagConfigured, satisfied: false)

        let intent = ParsedIntent(
            intentType: .shotRecommendation,
            confidence: 0.91
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "shot_recommendation"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should require bag configuration
        guard case .prerequisiteMissing(_, let missing, _) = result else {
            XCTFail("Expected prerequisiteMissing result")
            return
        }

        XCTAssertEqual(missing.count, 1)
        XCTAssertEqual(missing.first, .bagConfigured)
    }

    func testCourseInfoRequiresCourseSelection() async {
        // Given: No course selected
        await mockChecker.setSatisfied(.courseSelected, satisfied: false)

        let intent = ParsedIntent(
            intentType: .courseInfo,
            confidence: 0.89
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "course_info"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should require course selection
        guard case .prerequisiteMissing(_, let missing, _) = result else {
            XCTFail("Expected prerequisiteMissing result")
            return
        }

        XCTAssertEqual(missing.count, 1)
        XCTAssertEqual(missing.first, .courseSelected)
    }

    func testRoundEndRequiresActiveRound() async {
        // Given: No active round
        await mockChecker.setSatisfied(.roundActive, satisfied: false)

        let intent = ParsedIntent(
            intentType: .roundEnd,
            confidence: 0.93
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "round_summary"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should require active round
        guard case .prerequisiteMissing(_, let missing, _) = result else {
            XCTFail("Expected prerequisiteMissing result")
            return
        }

        XCTAssertEqual(missing.count, 1)
        XCTAssertEqual(missing.first, .roundActive)
    }

    // MARK: - Intents Without Prerequisites

    func testWeatherCheckNoPrerequisites() async {
        // Given: Weather check intent (no prerequisites)
        let intent = ParsedIntent(
            intentType: .weatherCheck,
            confidence: 0.88
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "weather"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should navigate without checking prerequisites
        guard case .navigate(let routedTarget, _) = result else {
            XCTFail("Expected navigate result")
            return
        }

        XCTAssertEqual(routedTarget.module, .caddy)
        XCTAssertEqual(routedTarget.screen, "weather")
    }

    func testRoundStartNoPrerequisites() async {
        // Given: Round start intent (no prerequisites)
        let intent = ParsedIntent(
            intentType: .roundStart,
            confidence: 0.90
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "round_start"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should navigate without checking prerequisites
        guard case .navigate = result else {
            XCTFail("Expected navigate result")
            return
        }
    }

    func testDrillRequestNoPrerequisites() async {
        // Given: Drill request (optional prerequisites)
        let intent = ParsedIntent(
            intentType: .drillRequest,
            confidence: 0.86
        )
        let target = RoutingTarget(
            module: .coach,
            screen: "drills"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should navigate even without bag configured
        guard case .navigate = result else {
            XCTFail("Expected navigate result")
            return
        }
    }

    // MARK: - Confirmation Flow Tests

    func testMediumConfidenceRequiresConfirmation() async {
        // Given: Medium confidence intent
        let intent = ParsedIntent(
            intentType: .shotRecommendation,
            confidence: 0.65
        )
        let message = "Did you mean: Get a shot recommendation?"
        let classification = ClassificationResult.confirm(intent: intent, message: message)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should return confirmation required
        guard case .confirmationRequired(let routedIntent, let confirmMessage) = result else {
            XCTFail("Expected confirmationRequired result")
            return
        }

        XCTAssertEqual(routedIntent.intentType, .shotRecommendation)
        XCTAssertEqual(routedIntent.confidence, 0.65)
        XCTAssertEqual(confirmMessage, message)
    }

    // MARK: - Clarification Flow Tests

    func testLowConfidenceReturnsClarification() async {
        // Given: Low confidence requiring clarification
        let clarificationResponse = ClarificationResponse(
            message: "I'm not sure what you mean. Did you want to:",
            suggestions: [
                IntentSuggestion(
                    intentType: .shotRecommendation,
                    label: "Get a shot recommendation",
                    description: "Get club and strategy advice for your next shot"
                ),
                IntentSuggestion(
                    intentType: .clubAdjustment,
                    label: "Adjust club distances",
                    description: "Update yardages for a club"
                )
            ],
            originalInput: "help me with something"
        )
        let classification = ClassificationResult.clarify(response: clarificationResponse)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should return no navigation with clarification message
        guard case .noNavigation(_, let response) = result else {
            XCTFail("Expected noNavigation result")
            return
        }

        XCTAssertFalse(response.isEmpty)
        XCTAssertTrue(response.contains("not sure") || response.contains("mean"))
    }

    // MARK: - Error Handling Tests

    func testClassificationErrorReturnsNoNavigation() async {
        // Given: Classification error
        let error = NSError(domain: "TestError", code: -1, userInfo: [
            NSLocalizedDescriptionKey: "Network timeout"
        ])
        let classification = ClassificationResult.error(error)

        // When: Route the classification
        let result = await orchestrator.route(classification)

        // Then: Should return no navigation with error message
        guard case .noNavigation(_, let response) = result else {
            XCTFail("Expected noNavigation result")
            return
        }

        XCTAssertFalse(response.isEmpty)
        XCTAssertTrue(response.contains("couldn't understand") || response.contains("Sorry"))
    }

    // MARK: - RoutingResult Extension Tests

    func testRoutingResultIntent() {
        let intent = ParsedIntent(intentType: .weatherCheck, confidence: 0.9)
        let target = RoutingTarget(module: .caddy, screen: "weather")

        let navigateResult = RoutingResult.navigate(target: target, intent: intent)
        XCTAssertEqual(navigateResult.intent.intentType, .weatherCheck)

        let noNavResult = RoutingResult.noNavigation(intent: intent, response: "Test")
        XCTAssertEqual(noNavResult.intent.intentType, .weatherCheck)

        let prerequisiteResult = RoutingResult.prerequisiteMissing(
            intent: intent,
            missing: [.recoveryData],
            message: "Test"
        )
        XCTAssertEqual(prerequisiteResult.intent.intentType, .weatherCheck)

        let confirmResult = RoutingResult.confirmationRequired(intent: intent, message: "Test")
        XCTAssertEqual(confirmResult.intent.intentType, .weatherCheck)
    }

    func testRoutingResultRequiresNavigation() {
        let intent = ParsedIntent(intentType: .weatherCheck, confidence: 0.9)
        let target = RoutingTarget(module: .caddy, screen: "weather")

        let navigateResult = RoutingResult.navigate(target: target, intent: intent)
        XCTAssertTrue(navigateResult.requiresNavigation)

        let noNavResult = RoutingResult.noNavigation(intent: intent, response: "Test")
        XCTAssertFalse(noNavResult.requiresNavigation)

        let prerequisiteResult = RoutingResult.prerequisiteMissing(
            intent: intent,
            missing: [.recoveryData],
            message: "Test"
        )
        XCTAssertFalse(prerequisiteResult.requiresNavigation)

        let confirmResult = RoutingResult.confirmationRequired(intent: intent, message: "Test")
        XCTAssertFalse(confirmResult.requiresNavigation)
    }

    func testRoutingResultMessage() {
        let intent = ParsedIntent(intentType: .weatherCheck, confidence: 0.9)
        let target = RoutingTarget(module: .caddy, screen: "weather")

        let navigateResult = RoutingResult.navigate(target: target, intent: intent)
        XCTAssertNil(navigateResult.message)

        let noNavResult = RoutingResult.noNavigation(intent: intent, response: "Test response")
        XCTAssertEqual(noNavResult.message, "Test response")

        let prerequisiteResult = RoutingResult.prerequisiteMissing(
            intent: intent,
            missing: [.recoveryData],
            message: "Missing data"
        )
        XCTAssertEqual(prerequisiteResult.message, "Missing data")

        let confirmResult = RoutingResult.confirmationRequired(intent: intent, message: "Confirm?")
        XCTAssertEqual(confirmResult.message, "Confirm?")
    }
}
