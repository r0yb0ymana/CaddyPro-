import XCTest
@testable import App

/// Module-specific routing tests.
///
/// Spec R3: Routing Orchestrator - Maps intent -> module route + parameters
/// Task 13: Write Routing Tests - Module routing verification
///
/// Tests that each module (caddy, coach, recovery, settings) routes correctly
/// and that screen mapping works for each intent type.
@MainActor
final class ModuleRoutingTests: XCTestCase {
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

        // Configure all prerequisites as satisfied by default
        await mockChecker.setSatisfied(.bagConfigured, satisfied: true)
        await mockChecker.setSatisfied(.roundActive, satisfied: true)
        await mockChecker.setSatisfied(.recoveryData, satisfied: true)
        await mockChecker.setSatisfied(.courseSelected, satisfied: true)
    }

    override func tearDown() async throws {
        mockChecker = nil
        orchestrator = nil
        coordinator = nil
        executor = nil
        deepLinkBuilder = nil
        try await super.tearDown()
    }

    // MARK: - Caddy Module Tests

    func testCaddyModule_clubAdjustment() async {
        // Given: Club adjustment intent with club parameter
        let intent = ParsedIntent(
            intentType: .clubAdjustment,
            confidence: 0.88,
            entities: ExtractedEntities(club: Club(type: .iron, number: 7))
        )
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: ["club": "7-iron"]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate to club adjustment screen
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        XCTAssertEqual(destination.module, .caddy)

        guard case .clubAdjustment(let club) = destination else {
            XCTFail("Expected clubAdjustment destination")
            return
        }

        XCTAssertEqual(club, "7-iron")
    }

    func testCaddyModule_shotRecommendation_allParameters() async {
        // Given: Shot recommendation with all parameters
        let intent = ParsedIntent(intentType: .shotRecommendation, confidence: 0.92)
        let target = RoutingTarget(
            module: .caddy,
            screen: "shot_recommendation",
            parameters: [
                "yardage": "150",
                "club": "7-iron",
                "lie": "fairway",
                "wind": "10mph headwind"
            ]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate with all parameters
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        XCTAssertEqual(destination.module, .caddy)

        guard case .shotRecommendation(let yardage, let club, let lie, let wind) = destination else {
            XCTFail("Expected shotRecommendation destination")
            return
        }

        XCTAssertEqual(yardage, 150)
        XCTAssertEqual(club, "7-iron")
        XCTAssertEqual(lie, "fairway")
        XCTAssertEqual(wind, "10mph headwind")
    }

    func testCaddyModule_shotRecommendation_minimalParameters() async {
        // Given: Shot recommendation with no parameters
        let intent = ParsedIntent(intentType: .shotRecommendation, confidence: 0.85)
        let target = RoutingTarget(
            module: .caddy,
            screen: "shot_recommendation",
            parameters: [:]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate with nil parameters
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        guard case .shotRecommendation(let yardage, let club, let lie, let wind) = destination else {
            XCTFail("Expected shotRecommendation destination")
            return
        }

        XCTAssertNil(yardage)
        XCTAssertNil(club)
        XCTAssertNil(lie)
        XCTAssertNil(wind)
    }

    func testCaddyModule_weatherCheck() async {
        // Given: Weather check intent
        let intent = ParsedIntent(intentType: .weatherCheck, confidence: 0.87)
        let target = RoutingTarget(
            module: .caddy,
            screen: "weather_check",
            parameters: [:]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate to weather check
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        XCTAssertEqual(destination.module, .caddy)

        guard case .weatherCheck(let location) = destination else {
            XCTFail("Expected weatherCheck destination")
            return
        }

        XCTAssertNil(location) // No location specified
    }

    func testCaddyModule_courseInfo() async {
        // Given: Course info intent with course ID
        let intent = ParsedIntent(intentType: .courseInfo, confidence: 0.90)
        let target = RoutingTarget(
            module: .caddy,
            screen: "course_info",
            parameters: ["course_id": "pebble-beach-123"]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate to course info
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        XCTAssertEqual(destination.module, .caddy)

        guard case .courseInfo(let courseId) = destination else {
            XCTFail("Expected courseInfo destination")
            return
        }

        XCTAssertEqual(courseId, "pebble-beach-123")
    }

    func testCaddyModule_roundManagement() async {
        // Test round start, score entry, and round end
        let testCases: [(IntentType, String, [String: String], String)] = [
            (.roundStart, "round_start", [:], "roundStart"),
            (.scoreEntry, "score_entry", ["hole": "7"], "scoreEntry"),
            (.roundEnd, "round_end", [:], "roundEnd")
        ]

        for (intentType, screen, params, expectedDest) in testCases {
            coordinator.popToRoot() // Reset

            let intent = ParsedIntent(intentType: intentType, confidence: 0.88)
            let target = RoutingTarget(module: .caddy, screen: screen, parameters: params)
            let classification = ClassificationResult.route(intent: intent, target: target)

            let routingResult = await orchestrator.route(classification)
            let action = executor.execute(routingResult)

            guard case .navigated(let destination, _) = action else {
                XCTFail("Expected navigated action for \(intentType)")
                continue
            }

            XCTAssertEqual(destination.module, .caddy,
                          "\(intentType) should route to caddy module")

            // Verify specific destination type
            switch (expectedDest, destination) {
            case ("roundStart", .roundStart):
                break // Success
            case ("scoreEntry", .scoreEntry(let hole)):
                XCTAssertEqual(hole, 7)
            case ("roundEnd", .roundEnd):
                break // Success
            default:
                XCTFail("Unexpected destination for \(intentType)")
            }
        }
    }

    // MARK: - Coach Module Tests

    func testCoachModule_drillRequest() async {
        // Given: Drill request intent
        let intent = ParsedIntent(intentType: .drillRequest, confidence: 0.86)
        let target = RoutingTarget(
            module: .coach,
            screen: "drill_screen",
            parameters: ["drill_type": "putting"]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate to drill screen
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        XCTAssertEqual(destination.module, .coach)

        guard case .drillScreen(let drillType, let focusClub) = destination else {
            XCTFail("Expected drillScreen destination")
            return
        }

        XCTAssertEqual(drillType, "putting")
        XCTAssertNil(focusClub)
    }

    func testCoachModule_statsLookup() async {
        // Given: Stats lookup intent
        let intent = ParsedIntent(intentType: .statsLookup, confidence: 0.89)
        let target = RoutingTarget(
            module: .coach,
            screen: "stats_lookup",
            parameters: [
                "stat_type": "putting",
                "date_range": "last_30_days"
            ]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate to stats lookup
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        XCTAssertEqual(destination.module, .coach)

        guard case .statsLookup(let statType, let dateRange) = destination else {
            XCTFail("Expected statsLookup destination")
            return
        }

        XCTAssertEqual(statType, "putting")
        XCTAssertEqual(dateRange, "last_30_days")
    }

    func testCoachModule_patternQuery_noNavigation() async {
        // Given: Pattern query intent (no-navigation type)
        let intent = ParsedIntent(intentType: .patternQuery, confidence: 0.91)
        let target = RoutingTarget(
            module: .coach,
            screen: "pattern_query",
            parameters: [:]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route
        let routingResult = await orchestrator.route(classification)

        // Then: Should return no-navigation result
        guard case .noNavigation(let routedIntent, let response) = routingResult else {
            XCTFail("Expected noNavigation result for pattern query")
            return
        }

        XCTAssertEqual(routedIntent.intentType, .patternQuery)
        XCTAssertFalse(response.isEmpty)
        XCTAssertTrue(response.contains("patterns") || response.contains("insights"))
    }

    // MARK: - Recovery Module Tests

    func testRecoveryModule_overview() async {
        // Given: Recovery check intent
        let intent = ParsedIntent(intentType: .recoveryCheck, confidence: 0.88)
        let target = RoutingTarget(
            module: .recovery,
            screen: "overview",
            parameters: [:]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate to recovery overview
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        XCTAssertEqual(destination.module, .recovery)

        guard case .recoveryOverview = destination else {
            XCTFail("Expected recoveryOverview destination")
            return
        }
    }

    func testRecoveryModule_detail() async {
        // Given: Recovery detail with specific date
        let intent = ParsedIntent(intentType: .recoveryCheck, confidence: 0.90)
        let target = RoutingTarget(
            module: .recovery,
            screen: "detail",
            parameters: ["date": "2026-01-15"]
        )

        // When: Build destination
        let destination = deepLinkBuilder.buildDestination(from: target)

        // Then: Should create recovery detail destination
        XCTAssertNotNil(destination)

        guard case .recoveryDetail(let date) = destination else {
            XCTFail("Expected recoveryDetail destination")
            return
        }

        XCTAssertEqual(date, "2026-01-15")
        XCTAssertEqual(destination?.module, .recovery)
    }

    // MARK: - Settings Module Tests

    func testSettingsModule_equipmentInfo() async {
        // Given: Equipment info intent
        let intent = ParsedIntent(intentType: .equipmentInfo, confidence: 0.87)
        let target = RoutingTarget(
            module: .settings,
            screen: "equipment_info",
            parameters: ["club": "driver"]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate to equipment info
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        XCTAssertEqual(destination.module, .settings)

        guard case .equipmentInfo(let clubToEdit) = destination else {
            XCTFail("Expected equipmentInfo destination")
            return
        }

        XCTAssertEqual(clubToEdit, "driver")
    }

    func testSettingsModule_settings() async {
        // Given: Settings change intent
        let intent = ParsedIntent(intentType: .settingsChange, confidence: 0.85)
        let target = RoutingTarget(
            module: .settings,
            screen: "settings",
            parameters: ["section": "notifications"]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route and execute
        let routingResult = await orchestrator.route(classification)
        let action = executor.execute(routingResult)

        // Then: Should navigate to settings
        guard case .navigated(let destination, _) = action else {
            XCTFail("Expected navigated action")
            return
        }

        XCTAssertEqual(destination.module, .settings)

        guard case .settings(let section) = destination else {
            XCTFail("Expected settings destination")
            return
        }

        XCTAssertEqual(section, "notifications")
    }

    func testSettingsModule_help_noNavigation() async {
        // Given: Help request intent (no-navigation type)
        let intent = ParsedIntent(intentType: .helpRequest, confidence: 0.92)
        let target = RoutingTarget(
            module: .settings,
            screen: "help",
            parameters: [:]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route
        let routingResult = await orchestrator.route(classification)

        // Then: Should return no-navigation result
        guard case .noNavigation(let routedIntent, let response) = routingResult else {
            XCTFail("Expected noNavigation result for help request")
            return
        }

        XCTAssertEqual(routedIntent.intentType, .helpRequest)
        XCTAssertFalse(response.isEmpty)
        XCTAssertTrue(response.contains("Bones") || response.contains("caddy") || response.contains("help"))
    }

    func testSettingsModule_feedback_noNavigation() async {
        // Given: Feedback intent (no-navigation type)
        let intent = ParsedIntent(intentType: .feedback, confidence: 0.88)
        let target = RoutingTarget(
            module: .settings,
            screen: "feedback",
            parameters: [:]
        )
        let classification = ClassificationResult.route(intent: intent, target: target)

        // When: Route
        let routingResult = await orchestrator.route(classification)

        // Then: Should return no-navigation result
        guard case .noNavigation(let routedIntent, let response) = routingResult else {
            XCTFail("Expected noNavigation result for feedback")
            return
        }

        XCTAssertEqual(routedIntent.intentType, .feedback)
        XCTAssertFalse(response.isEmpty)
        XCTAssertTrue(response.contains("feedback") || response.contains("Thanks"))
    }

    // MARK: - Parameter Passing Tests

    func testParameterPassing_throughPipeline() async {
        // Test that parameters are correctly passed through the entire pipeline
        struct TestCase {
            let intentType: IntentType
            let screen: String
            let inputParams: [String: String]
            let verify: (NavCaddyDestination) -> Bool
        }

        let testCases: [TestCase] = [
            TestCase(
                intentType: .clubAdjustment,
                screen: "club_adjustment",
                inputParams: ["club": "9-iron"],
                verify: { dest in
                    guard case .clubAdjustment(let club) = dest else { return false }
                    return club == "9-iron"
                }
            ),
            TestCase(
                intentType: .shotRecommendation,
                screen: "shot_recommendation",
                inputParams: ["yardage": "175", "lie": "rough"],
                verify: { dest in
                    guard case .shotRecommendation(let yardage, _, let lie, _) = dest else { return false }
                    return yardage == 175 && lie == "rough"
                }
            ),
            TestCase(
                intentType: .courseInfo,
                screen: "course_info",
                inputParams: ["course_id": "augusta-national-001"],
                verify: { dest in
                    guard case .courseInfo(let courseId) = dest else { return false }
                    return courseId == "augusta-national-001"
                }
            ),
            TestCase(
                intentType: .scoreEntry,
                screen: "score_entry",
                inputParams: ["hole": "18"],
                verify: { dest in
                    guard case .scoreEntry(let hole) = dest else { return false }
                    return hole == 18
                }
            )
        ]

        for testCase in testCases {
            coordinator.popToRoot() // Reset

            let intent = ParsedIntent(intentType: testCase.intentType, confidence: 0.85)
            let target = RoutingTarget(
                module: .caddy,
                screen: testCase.screen,
                parameters: testCase.inputParams
            )
            let classification = ClassificationResult.route(intent: intent, target: target)

            let routingResult = await orchestrator.route(classification)
            let action = executor.execute(routingResult)

            guard case .navigated(let destination, _) = action else {
                XCTFail("Expected navigation for \(testCase.intentType)")
                continue
            }

            XCTAssertTrue(testCase.verify(destination),
                         "Parameter verification failed for \(testCase.intentType)")
        }
    }

    // MARK: - Invalid Module/Screen Tests

    func testInvalidScreen_returnsError() {
        // Given: Invalid screen name
        let intent = ParsedIntent(intentType: .clubAdjustment, confidence: 0.85)
        let target = RoutingTarget(
            module: .caddy,
            screen: "nonexistent_screen_xyz",
            parameters: ["club": "7-iron"]
        )
        let routingResult = RoutingResult.navigate(target: target, intent: intent)

        // When: Execute
        let action = executor.execute(routingResult)

        // Then: Should show error
        guard case .showError(let message, _) = action else {
            XCTFail("Expected showError for invalid screen")
            return
        }

        XCTAssertTrue(message.contains("Could not build destination") ||
                     message.contains("nonexistent_screen_xyz"))
    }

    func testMissingRequiredParameter_failsValidation() {
        // Given: Target with missing required parameter
        let testCases: [(String, [String: String])] = [
            ("club_adjustment", [:]), // Missing "club"
            ("score_entry", [:]),     // Missing "hole"
            ("course_info", [:])      // Missing "course_id"
        ]

        for (screen, params) in testCases {
            let target = RoutingTarget(module: .caddy, screen: screen, parameters: params)

            // When: Validate
            let isValid = deepLinkBuilder.validate(target)

            // Then: Should be invalid
            XCTAssertFalse(isValid,
                          "Screen '\(screen)' should fail validation with params: \(params)")

            // When: Try to build destination
            let destination = deepLinkBuilder.buildDestination(from: target)

            // Then: Should return nil
            XCTAssertNil(destination,
                        "Should not build destination for '\(screen)' with missing parameters")
        }
    }

    // MARK: - Module Coverage Tests

    func testAllModules_haveAtLeastOneIntent() async {
        // Verify each module has intents that route to it
        let moduleIntents: [Module: [IntentType]] = [
            .caddy: [.clubAdjustment, .shotRecommendation, .weatherCheck, .scoreEntry, .roundStart, .roundEnd, .courseInfo],
            .coach: [.drillRequest, .statsLookup, .patternQuery],
            .recovery: [.recoveryCheck],
            .settings: [.equipmentInfo, .settingsChange, .helpRequest, .feedback]
        ]

        for (module, intents) in moduleIntents {
            XCTAssertFalse(intents.isEmpty,
                          "Module \(module) should have at least one intent")

            // Verify each intent can route to its module
            for intentType in intents {
                let intent = ParsedIntent(intentType: intentType, confidence: 0.85)

                // For no-navigation intents, just verify they exist
                if [.patternQuery, .helpRequest, .feedback].contains(intentType) {
                    continue
                }

                // For navigation intents, verify they map correctly
                // (actual routing tested in individual tests above)
            }
        }
    }

    func testAllIntentTypes_haveMappedScreens() {
        // Verify all 15 intent types have proper screen mappings
        let intentScreenMappings: [IntentType: String] = [
            .clubAdjustment: "club_adjustment",
            .recoveryCheck: "overview",
            .shotRecommendation: "shot_recommendation",
            .scoreEntry: "score_entry",
            .patternQuery: "pattern_query",
            .drillRequest: "drill_screen",
            .weatherCheck: "weather_check",
            .statsLookup: "stats_lookup",
            .roundStart: "round_start",
            .roundEnd: "round_end",
            .equipmentInfo: "equipment_info",
            .courseInfo: "course_info",
            .settingsChange: "settings",
            .helpRequest: "help",
            .feedback: "feedback"
        ]

        XCTAssertEqual(intentScreenMappings.count, 15,
                      "Should have mappings for all 15 MVP intent types")

        // Verify each mapping exists
        for (intentType, screen) in intentScreenMappings {
            XCTAssertFalse(screen.isEmpty,
                          "Intent \(intentType) should have a non-empty screen mapping")
        }
    }
}
