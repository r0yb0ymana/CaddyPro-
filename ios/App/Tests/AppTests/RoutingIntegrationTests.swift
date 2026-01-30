import XCTest
@testable import App

/// Integration tests for the complete routing pipeline.
///
/// Spec R3: Routing Orchestrator
/// Task 13: Write Routing Tests - Integration testing
///
/// Tests the full flow:
/// ClassificationResult → RoutingOrchestrator → RoutingResult → NavigationExecutor → NavigationAction
@MainActor
final class RoutingIntegrationTests: XCTestCase {
    var mockChecker: MockPrerequisiteChecker!
    var orchestrator: RoutingOrchestrator!
    var coordinator: NavigationCoordinator!
    var executor: NavigationExecutor!

    override func setUp() async throws {
        try await super.setUp()
        mockChecker = MockPrerequisiteChecker()
        orchestrator = RoutingOrchestrator(prerequisiteChecker: mockChecker)
        coordinator = NavigationCoordinator()
        executor = NavigationExecutor(coordinator: coordinator)
    }

    override func tearDown() async throws {
        mockChecker = nil
        orchestrator = nil
        coordinator = nil
        executor = nil
        try await super.tearDown()
    }

    // MARK: - Full Pipeline Tests

    func testFullPipeline_clubAdjustment_success() async {
        // Given: All prerequisites satisfied
        await mockChecker.setSatisfied(.bagConfigured, satisfied: true)

        let intent = ParsedIntent(
            intentType: .clubAdjustment,
            confidence: 0.85,
            entities: ExtractedEntities(club: Club(type: .iron, number: 7))
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: ["club": "7-iron"]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Process through full pipeline
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate successfully
        guard case .navigated(let destination, let returnedIntent) = action else {
            XCTFail("Expected navigated action")
            return
        }

        // Verify destination
        guard case .clubAdjustment(let club) = destination else {
            XCTFail("Expected clubAdjustment destination")
            return
        }
        XCTAssertEqual(club, "7-iron")

        // Verify intent passed through
        XCTAssertEqual(returnedIntent.intentType, .clubAdjustment)
        XCTAssertEqual(returnedIntent.confidence, 0.85)

        // Verify coordinator state
        XCTAssertEqual(coordinator.stackDepth, 1)
        XCTAssertEqual(coordinator.currentDestination, destination)
    }

    func testFullPipeline_recoveryCheck_missingPrerequisite() async {
        // Given: Recovery data does NOT exist
        await mockChecker.setSatisfied(.recoveryData, satisfied: false)

        let intent = ParsedIntent(
            intentType: .recoveryCheck,
            confidence: 0.90
        )
        let target = RoutingTarget(
            module: .recovery,
            screen: "overview"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Process through full pipeline
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should show prerequisite prompt
        guard case .showPrerequisitePrompt(let message, let missing, let returnedIntent) = action else {
            XCTFail("Expected showPrerequisitePrompt action")
            return
        }

        XCTAssertEqual(missing.count, 1)
        XCTAssertEqual(missing.first, .recoveryData)
        XCTAssertTrue(message.contains("recovery data"))
        XCTAssertEqual(returnedIntent.intentType, .recoveryCheck)

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)
    }

    func testFullPipeline_patternQuery_noNavigation() async {
        // Given: Pattern query intent (no prerequisites)
        let intent = ParsedIntent(
            intentType: .patternQuery,
            confidence: 0.88
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "patterns"
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Process through full pipeline
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should show response without navigation
        guard case .showResponse(let response, let returnedIntent) = action else {
            XCTFail("Expected showResponse action")
            return
        }

        XCTAssertFalse(response.isEmpty)
        XCTAssertTrue(response.contains("patterns") || response.contains("insights"))
        XCTAssertEqual(returnedIntent.intentType, .patternQuery)

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)
    }

    func testFullPipeline_mediumConfidence_confirmation() async {
        // Given: Medium confidence intent
        let intent = ParsedIntent(
            intentType: .shotRecommendation,
            confidence: 0.65
        )
        let message = "Did you mean: Get a shot recommendation?"
        let classification = ClassificationResult.confirm(intent: intent, message: message)

        // When: Process through full pipeline
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should request confirmation
        guard case .requestConfirmation(let confirmMessage, let returnedIntent) = action else {
            XCTFail("Expected requestConfirmation action")
            return
        }

        XCTAssertEqual(confirmMessage, message)
        XCTAssertEqual(returnedIntent.intentType, .shotRecommendation)

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)
    }

    func testFullPipeline_lowConfidence_clarification() async {
        // Given: Low confidence requiring clarification
        let clarification = ClarificationResponse(
            message: "I'm not sure what you mean.",
            suggestions: [
                IntentSuggestion(
                    intentType: .shotRecommendation,
                    label: "Get shot recommendation",
                    description: "Get club and strategy advice"
                )
            ],
            originalInput: "help with shot"
        )
        let classification = ClassificationResult.clarify(response: clarification)

        // When: Process through full pipeline
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should show response with clarification
        guard case .showResponse(let response, _) = action else {
            XCTFail("Expected showResponse action")
            return
        }

        XCTAssertTrue(response.contains("not sure") || response.contains("mean"))

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)
    }

    func testFullPipeline_error_handling() async {
        // Given: Classification error
        let error = NSError(domain: "TestError", code: -1, userInfo: [
            NSLocalizedDescriptionKey: "Network timeout"
        ])
        let classification = ClassificationResult.error(error)

        // When: Process through full pipeline
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should show response with error message
        guard case .showResponse(let response, _) = action else {
            XCTFail("Expected showResponse action")
            return
        }

        XCTAssertTrue(response.contains("couldn't understand") || response.contains("Sorry"))

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)
    }

    // MARK: - All 15 Intent Types Routing Tests

    func testAllIntentTypes_routeToCorrectModules() async {
        // Configure all prerequisites as satisfied for this test
        await mockChecker.setSatisfied(.bagConfigured, satisfied: true)
        await mockChecker.setSatisfied(.roundActive, satisfied: true)
        await mockChecker.setSatisfied(.recoveryData, satisfied: true)
        await mockChecker.setSatisfied(.courseSelected, satisfied: true)

        // Test data: (intentType, expectedModule, screen, requiredParams)
        let testCases: [(IntentType, Module, String, [String: String])] = [
            // Core intents
            (.clubAdjustment, .caddy, "club_adjustment", ["club": "7-iron"]),
            (.recoveryCheck, .recovery, "overview", [:]),
            (.shotRecommendation, .caddy, "shot_recommendation", [:]),
            (.scoreEntry, .caddy, "score_entry", ["hole": "5"]),
            (.patternQuery, .coach, "pattern_query", [:]),

            // Extended intents
            (.drillRequest, .coach, "drill_screen", [:]),
            (.weatherCheck, .caddy, "weather_check", [:]),
            (.statsLookup, .coach, "stats_lookup", [:]),
            (.roundStart, .caddy, "round_start", [:]),
            (.roundEnd, .caddy, "round_end", [:]),

            // Additional intents
            (.equipmentInfo, .settings, "equipment_info", [:]),
            (.courseInfo, .caddy, "course_info", ["course_id": "test-course-123"]),
            (.settingsChange, .settings, "settings", [:]),
            (.helpRequest, .settings, "help", [:]),
            (.feedback, .settings, "feedback", [:])
        ]

        for (intentType, expectedModule, screen, params) in testCases {
            // Reset coordinator for each test
            coordinator.popToRoot()

            let intent = ParsedIntent(intentType: intentType, confidence: 0.85)
            let target = RoutingTarget(module: expectedModule, screen: screen, parameters: params)
            let classification = ClassificationResult.route(intent: intent, target: target)

            // When: Process through pipeline
            let routingResult = await orchestrator.route(classification)

            // Then: Verify correct routing behavior
            switch intentType {
            case .patternQuery, .helpRequest, .feedback:
                // These should be no-navigation intents
                guard case .noNavigation = routingResult else {
                    XCTFail("Expected noNavigation for \(intentType)")
                    continue
                }

            default:
                // These should navigate
                guard case .navigate(let routedTarget, _) = routingResult else {
                    XCTFail("Expected navigate for \(intentType)")
                    continue
                }
                XCTAssertEqual(routedTarget.module, expectedModule,
                             "Intent \(intentType) should route to module \(expectedModule)")
            }
        }
    }

    // MARK: - Prerequisite Validation Across Pipeline

    func testPrerequisiteValidation_multiplePrerequisites() async {
        // Test that prerequisite validation works correctly across the pipeline
        let testCases: [(IntentType, [Prerequisite])] = [
            (.recoveryCheck, [.recoveryData]),
            (.scoreEntry, [.roundActive]),
            (.roundEnd, [.roundActive]),
            (.clubAdjustment, [.bagConfigured]),
            (.shotRecommendation, [.bagConfigured]),
            (.courseInfo, [.courseSelected])
        ]

        for (intentType, requiredPrereqs) in testCases {
            // Given: All prerequisites NOT satisfied
            await mockChecker.setSatisfied(.recoveryData, satisfied: false)
            await mockChecker.setSatisfied(.roundActive, satisfied: false)
            await mockChecker.setSatisfied(.bagConfigured, satisfied: false)
            await mockChecker.setSatisfied(.courseSelected, satisfied: false)

            var params: [String: String] = [:]
            if intentType == .clubAdjustment {
                params["club"] = "7-iron"
            } else if intentType == .scoreEntry {
                params["hole"] = "5"
            } else if intentType == .courseInfo {
                params["course_id"] = "test-123"
            }

            let intent = ParsedIntent(intentType: intentType, confidence: 0.85)
            let target = RoutingTarget(
                module: .caddy, // Will be overridden by actual routing
                screen: "test",
                parameters: params
            )
            let classification = ClassificationResult.route(intent: intent, target: target)

            // When: Process through pipeline
            let routingResult = await orchestrator.route(classification)

            // Then: Should require prerequisites
            guard case .prerequisiteMissing(_, let missing, _) = routingResult else {
                XCTFail("Expected prerequisiteMissing for \(intentType)")
                continue
            }

            XCTAssertEqual(Set(missing), Set(requiredPrereqs),
                          "Intent \(intentType) should require: \(requiredPrereqs)")
        }
    }

    // MARK: - Error Propagation Tests

    func testErrorPropagation_invalidTarget() {
        // Given: Invalid routing target (missing required parameter)
        let intent = ParsedIntent(intentType: .clubAdjustment, confidence: 0.85)
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: [:] // Missing required "club" parameter
        )
        let routingResult = RoutingResult.navigate(target: target, intent: intent)

        // When: Execute navigation with invalid target
        let action = executor.execute(routingResult)

        // Then: Should show error
        guard case .showError(let message, let returnedIntent) = action else {
            XCTFail("Expected showError action")
            return
        }

        XCTAssertTrue(message.contains("Invalid"))
        XCTAssertEqual(returnedIntent.intentType, .clubAdjustment)

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)
    }

    func testErrorPropagation_unknownScreen() {
        // Given: Unknown screen in routing target
        let intent = ParsedIntent(intentType: .clubAdjustment, confidence: 0.85)
        let target = RoutingTarget(
            module: .caddy,
            screen: "unknown_screen_xyz",
            parameters: ["club": "7-iron"]
        )
        let routingResult = RoutingResult.navigate(target: target, intent: intent)

        // When: Execute navigation
        let action = executor.execute(routingResult)

        // Then: Should show error
        guard case .showError(let message, _) = action else {
            XCTFail("Expected showError action")
            return
        }

        XCTAssertTrue(message.contains("Could not build destination") ||
                     message.contains("unknown_screen_xyz"))

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)
    }

    // MARK: - Complex Scenario Tests

    func testComplexScenario_multipleNavigations() async {
        // Given: Prerequisites satisfied
        await mockChecker.setSatisfied(.bagConfigured, satisfied: true)

        // Scenario: User navigates through multiple screens
        let scenarios: [(IntentType, String, [String: String])] = [
            (.clubAdjustment, "club_adjustment", ["club": "7-iron"]),
            (.shotRecommendation, "shot_recommendation", [:]),
            (.weatherCheck, "weather_check", [:])
        ]

        for (intentType, screen, params) in scenarios {
            let intent = ParsedIntent(intentType: intentType, confidence: 0.85)
            let target = RoutingTarget(module: .caddy, screen: screen, parameters: params)
            let classification = ClassificationResult.route(intent: intent, target: target)

            let routingResult = await orchestrator.route(classification)
            _ = executor.execute(routingResult)
        }

        // Then: Stack should have all three destinations
        XCTAssertEqual(coordinator.stackDepth, 3)

        // When: Navigate back
        executor.navigateBack()
        XCTAssertEqual(coordinator.stackDepth, 2)

        // When: Pop to root
        executor.popToRoot()
        XCTAssertEqual(coordinator.stackDepth, 0)
        XCTAssertTrue(coordinator.isEmpty)
    }

    func testComplexScenario_satisfyPrerequisiteThenRetry() async {
        // Given: Initial state with missing prerequisite
        await mockChecker.setSatisfied(.recoveryData, satisfied: false)

        let intent = ParsedIntent(intentType: .recoveryCheck, confidence: 0.85)
        let target = RoutingTarget(module: .recovery, screen: "overview")
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: First attempt
        var routingResult = await orchestrator.route(classification)
        var action = executor.execute(routingResult)

        // Then: Should fail with prerequisite missing
        guard case .showPrerequisitePrompt = action else {
            XCTFail("Expected prerequisite prompt")
            return
        }
        XCTAssertEqual(coordinator.stackDepth, 0)

        // When: Satisfy prerequisite and retry
        await mockChecker.setSatisfied(.recoveryData, satisfied: true)
        routingResult = await orchestrator.route(classification)
        action = executor.execute(routingResult)

        // Then: Should succeed
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigation after prerequisite satisfied")
            return
        }

        guard case .recoveryOverview = destination else {
            XCTFail("Expected recoveryOverview destination")
            return
        }

        XCTAssertEqual(coordinator.stackDepth, 1)
    }
}
