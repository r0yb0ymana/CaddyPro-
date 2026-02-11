import XCTest
@testable import App

/// Tests for routing determinism and reproducibility.
///
/// Spec C3: Deterministic routing
/// "Routing decisions must be reproducible from the same (input + context + memory snapshot)
/// in QA builds."
///
/// Task 13: Write Routing Tests - Determinism verification
///
/// Verifies that:
/// - Same ParsedIntent + same context → same RoutingResult
/// - Same RoutingTarget → same NavCaddyDestination
/// - Same RoutingResult → same NavigationAction
/// - Multiple runs produce consistent results
@MainActor
final class RoutingDeterminismTests: XCTestCase {
    var mockChecker: MockPrerequisiteChecker!
    var orchestrator: RoutingOrchestrator!
    var coordinator: NavigationCoordinator!
    var executor: NavigationExecutor!
    var deepLinkBuilder: DeepLinkBuilder!

    override func setUp() async throws {
        try await super.setUp()
        mockChecker = MockPrerequisiteChecker()
        orchestrator = RoutingOrchestrator(prerequisiteChecker: mockChecker)
        coordinator = NavigationCoordinator()
        executor = NavigationExecutor(coordinator: coordinator)
        deepLinkBuilder = DeepLinkBuilder()
    }

    override func tearDown() async throws {
        mockChecker = nil
        orchestrator = nil
        coordinator = nil
        executor = nil
        deepLinkBuilder = nil
        try await super.tearDown()
    }

    // MARK: - ParsedIntent → RoutingResult Determinism

    func testDeterminism_sameIntent_sameRoutingResult() async {
        // Given: Same intent configuration
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

        // When: Route multiple times
        let results = await withTaskGroup(of: RoutingResult.self) { group in
            for _ in 0..<10 {
                group.addTask {
                    await self.orchestrator.route(classification)
                }
            }

            var collected: [RoutingResult] = []
            for await result in group {
                collected.append(result)
            }
            return collected
        }

        // Then: All results should be identical
        XCTAssertEqual(results.count, 10)

        for result in results {
            guard case .navigate(let routedTarget, let routedIntent) = result else {
                XCTFail("Expected navigate result")
                return
            }

            XCTAssertEqual(routedTarget.module, .caddy)
            XCTAssertEqual(routedTarget.screen, "club_adjustment")
            XCTAssertEqual(routedTarget.parameters["club"], "7-iron")
            XCTAssertEqual(routedIntent.intentType, .clubAdjustment)
            XCTAssertEqual(routedIntent.confidence, 0.85)
        }
    }

    func testDeterminism_samePrerequisiteState_consistentResults() async {
        // Given: Consistent prerequisite state across multiple checks
        await mockChecker.setSatisfied(.recoveryData, satisfied: false)

        let intent = ParsedIntent(intentType: .recoveryCheck, confidence: 0.90)
        let target = RoutingTarget(module: .recovery, screen: "overview")
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route 20 times
        var results: [RoutingResult] = []
        for _ in 0..<20 {
            let result = await orchestrator.route(classification)
            results.append(result)
        }

        // Then: All results should be prerequisiteMissing with same details
        XCTAssertEqual(results.count, 20)

        for result in results {
            guard case .prerequisiteMissing(let routedIntent, let missing, let message) = result else {
                XCTFail("Expected prerequisiteMissing result")
                return
            }

            XCTAssertEqual(routedIntent.intentType, .recoveryCheck)
            XCTAssertEqual(missing.count, 1)
            XCTAssertEqual(missing.first, .recoveryData)
            XCTAssertTrue(message.contains("recovery data"))
        }
    }

    func testDeterminism_noNavigationIntents_consistent() async {
        // Given: No-navigation intent types
        let intentTypes: [IntentType] = [.patternQuery, .helpRequest, .feedback]

        for intentType in intentTypes {
            let intent = ParsedIntent(intentType: intentType, confidence: 0.88)
            let target = RoutingTarget(module: .caddy, screen: "test")
            let classification = ClassificationResult.route(intent: intent, target: target)

            // When: Route 15 times
            var results: [RoutingResult] = []
            for _ in 0..<15 {
                let result = await orchestrator.route(classification)
                results.append(result)
            }

            // Then: All should be noNavigation with same response
            XCTAssertEqual(results.count, 15)

            let firstResponse: String
            if case .noNavigation(_, let response) = results[0] {
                firstResponse = response
            } else {
                XCTFail("Expected noNavigation for \(intentType)")
                return
            }

            for result in results {
                guard case .noNavigation(let routedIntent, let response) = result else {
                    XCTFail("Expected noNavigation")
                    return
                }

                XCTAssertEqual(routedIntent.intentType, intentType)
                XCTAssertEqual(response, firstResponse, "Response should be identical across runs")
            }
        }
    }

    // MARK: - RoutingTarget → NavCaddyDestination Determinism

    func testDeterminism_sameTarget_sameDestination() {
        // Given: Same routing target
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: ["club": "7-iron"]
        )

        // When: Build destination 30 times
        var destinations: [NavCaddyDestination] = []
        for _ in 0..<30 {
            if let destination = deepLinkBuilder.buildDestination(from: target) {
                destinations.append(destination)
            }
        }

        // Then: All destinations should be identical
        XCTAssertEqual(destinations.count, 30)

        let firstDestination = destinations[0]
        for destination in destinations {
            XCTAssertEqual(destination, firstDestination)

            guard case .clubAdjustment(let club) = destination else {
                XCTFail("Expected clubAdjustment destination")
                return
            }
            XCTAssertEqual(club, "7-iron")
        }
    }

    func testDeterminism_parametersOrder_doesNotAffectResult() {
        // Given: Same parameters in different order
        let target1 = RoutingTarget(
            module: .caddy,
            screen: "shot_recommendation",
            parameters: [
                "yardage": "150",
                "club": "7-iron",
                "lie": "fairway"
            ]
        )

        let target2 = RoutingTarget(
            module: .caddy,
            screen: "shot_recommendation",
            parameters: [
                "lie": "fairway",
                "yardage": "150",
                "club": "7-iron"
            ]
        )

        // When: Build destinations
        let dest1 = deepLinkBuilder.buildDestination(from: target1)
        let dest2 = deepLinkBuilder.buildDestination(from: target2)

        // Then: Should be identical (dictionary order doesn't matter)
        XCTAssertNotNil(dest1)
        XCTAssertNotNil(dest2)
        XCTAssertEqual(dest1, dest2)
    }

    func testDeterminism_validation_consistent() {
        // Given: Various routing targets
        let testCases: [(RoutingTarget, Bool)] = [
            (RoutingTarget(module: .caddy, screen: "club_adjustment", parameters: ["club": "7-iron"]), true),
            (RoutingTarget(module: .caddy, screen: "club_adjustment", parameters: [:]), false),
            (RoutingTarget(module: .caddy, screen: "score_entry", parameters: ["hole": "5"]), true),
            (RoutingTarget(module: .caddy, screen: "score_entry", parameters: ["hole": "25"]), false),
            (RoutingTarget(module: .recovery, screen: "overview", parameters: [:]), true)
        ]

        for (target, expectedValid) in testCases {
            // When: Validate multiple times
            var validations: [Bool] = []
            for _ in 0..<20 {
                validations.append(deepLinkBuilder.validate(target))
            }

            // Then: All validations should be consistent
            XCTAssertEqual(validations.count, 20)
            for validation in validations {
                XCTAssertEqual(validation, expectedValid,
                             "Validation of \(target.screen) should always return \(expectedValid)")
            }
        }
    }

    // MARK: - RoutingResult → NavigationAction Determinism

    func testDeterminism_sameRoutingResult_sameAction() {
        // Given: Same routing result
        let intent = ParsedIntent(intentType: .clubAdjustment, confidence: 0.85)
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: ["club": "7-iron"]
        )
        let routingResult = RoutingResult.navigate(target: target, intent: intent)

        // When: Execute 25 times (resetting coordinator each time)
        var actions: [NavigationAction] = []
        for _ in 0..<25 {
            coordinator.popToRoot() // Reset state
            let action = executor.execute(routingResult)
            actions.append(action)
        }

        // Then: All actions should be identical
        XCTAssertEqual(actions.count, 25)

        for action in actions {
            guard case .navigated(let destination, let actionIntent) = action else {
                XCTFail("Expected navigated action")
                return
            }

            guard case .clubAdjustment(let club) = destination else {
                XCTFail("Expected clubAdjustment destination")
                return
            }

            XCTAssertEqual(club, "7-iron")
            XCTAssertEqual(actionIntent.intentType, .clubAdjustment)
            XCTAssertEqual(actionIntent.confidence, 0.85)
        }
    }

    func testDeterminism_noNavigationResult_sameResponse() {
        // Given: No-navigation result
        let intent = ParsedIntent(intentType: .helpRequest, confidence: 0.90)
        let routingResult = RoutingResult.noNavigation(
            intent: intent,
            response: "I'm Bones, your digital caddy."
        )

        // When: Execute 30 times
        var actions: [NavigationAction] = []
        for _ in 0..<30 {
            let action = executor.execute(routingResult)
            actions.append(action)
        }

        // Then: All actions should have same response
        XCTAssertEqual(actions.count, 30)

        for action in actions {
            guard case .showResponse(let response, let actionIntent) = action else {
                XCTFail("Expected showResponse action")
                return
            }

            XCTAssertEqual(response, "I'm Bones, your digital caddy.")
            XCTAssertEqual(actionIntent.intentType, .helpRequest)
        }
    }

    // MARK: - Cross-Component Determinism

    func testDeterminism_fullPipeline_multipleRuns() async {
        // Given: Complete pipeline configuration
        await mockChecker.setSatisfied(.bagConfigured, satisfied: true)

        let intent = ParsedIntent(intentType: .shotRecommendation, confidence: 0.88)
        let target = RoutingTarget(
            module: .caddy,
            screen: "shot_recommendation",
            parameters: [:]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Run full pipeline 15 times
        struct PipelineResult: Equatable {
            let routingModule: Module
            let routingScreen: String
            let actionType: String
            let stackDepth: Int

            static func == (lhs: PipelineResult, rhs: PipelineResult) -> Bool {
                return lhs.routingModule == rhs.routingModule &&
                       lhs.routingScreen == rhs.routingScreen &&
                       lhs.actionType == rhs.actionType &&
                       lhs.stackDepth == rhs.stackDepth
            }
        }

        var pipelineResults: [PipelineResult] = []
        for _ in 0..<15 {
            coordinator.popToRoot() // Reset state

            let routingResult = await orchestrator.route(classification)
            let action = executor.execute(routingResult)

            let result: PipelineResult
            if case .navigate(let routedTarget, _) = routingResult {
                result = PipelineResult(
                    routingModule: routedTarget.module,
                    routingScreen: routedTarget.screen,
                    actionType: "navigated",
                    stackDepth: coordinator.stackDepth
                )
            } else {
                result = PipelineResult(
                    routingModule: .caddy,
                    routingScreen: "",
                    actionType: "other",
                    stackDepth: 0
                )
            }

            pipelineResults.append(result)
        }

        // Then: All pipeline results should be identical
        XCTAssertEqual(pipelineResults.count, 15)

        let firstResult = pipelineResults[0]
        for result in pipelineResults {
            XCTAssertEqual(result, firstResult,
                          "All pipeline runs should produce identical results")
        }

        // Verify specific values
        XCTAssertEqual(firstResult.routingModule, .caddy)
        XCTAssertEqual(firstResult.routingScreen, "shot_recommendation")
        XCTAssertEqual(firstResult.actionType, "navigated")
        XCTAssertEqual(firstResult.stackDepth, 1)
    }

    func testDeterminism_prerequisiteChange_detectsImmediately() async {
        // Given: Initial prerequisite state
        await mockChecker.setSatisfied(.bagConfigured, satisfied: true)

        let intent = ParsedIntent(intentType: .clubAdjustment, confidence: 0.85)
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: ["club": "7-iron"]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route with satisfied prerequisite
        var result1 = await orchestrator.route(classification)
        guard case .navigate = result1 else {
            XCTFail("Expected navigate")
            return
        }

        // When: Change prerequisite state
        await mockChecker.setSatisfied(.bagConfigured, satisfied: false)

        // When: Route again
        var result2 = await orchestrator.route(classification)
        guard case .prerequisiteMissing = result2 else {
            XCTFail("Expected prerequisiteMissing")
            return
        }

        // When: Change back
        await mockChecker.setSatisfied(.bagConfigured, satisfied: true)

        // When: Route again
        result1 = await orchestrator.route(classification)
        guard case .navigate = result1 else {
            XCTFail("Expected navigate after prerequisite satisfied again")
            return
        }

        // Then: Behavior should be deterministic based on prerequisite state
        // This verifies that state changes are properly detected
    }

    // MARK: - Concurrent Access Determinism

    func testDeterminism_concurrentRouting_consistentResults() async {
        // Given: Multiple concurrent routing requests
        await mockChecker.setSatisfied(.bagConfigured, satisfied: true)

        let intents: [(IntentType, String)] = [
            (.clubAdjustment, "club_adjustment"),
            (.shotRecommendation, "shot_recommendation"),
            (.weatherCheck, "weather_check")
        ]

        // When: Process all intents concurrently, multiple times
        for runNumber in 0..<5 {
            let results = await withTaskGroup(of: (IntentType, RoutingResult).self) { group in
                for (intentType, screen) in intents {
                    group.addTask {
                        let intent = ParsedIntent(intentType: intentType, confidence: 0.85)
                        let target = RoutingTarget(
                            module: .caddy,
                            screen: screen,
                            parameters: intentType == .clubAdjustment ? ["club": "7-iron"] : [:]
                        )
                        let classification = ClassificationResult.route(intent: intent, target: target)
                        let result = await self.orchestrator.route(classification)
                        return (intentType, result)
                    }
                }

                var collected: [(IntentType, RoutingResult)] = []
                for await result in group {
                    collected.append(result)
                }
                return collected
            }

            // Then: All results should be navigate (no race conditions)
            XCTAssertEqual(results.count, 3, "Run \(runNumber): Should have 3 results")

            for (intentType, result) in results {
                guard case .navigate(let target, let intent) = result else {
                    XCTFail("Run \(runNumber): Expected navigate for \(intentType)")
                    return
                }

                XCTAssertEqual(intent.intentType, intentType,
                             "Run \(runNumber): Intent should match for \(intentType)")
                XCTAssertEqual(target.module, .caddy,
                             "Run \(runNumber): Should route to caddy module")
            }
        }
    }
}
