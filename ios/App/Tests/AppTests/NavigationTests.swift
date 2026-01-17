import XCTest
@testable import App

@MainActor
final class NavigationTests: XCTestCase {
    // MARK: - DeepLinkBuilder Tests

    func testDeepLinkBuilder_clubAdjustment_success() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: ["club": "7-iron"]
        )

        let destination = builder.buildDestination(from: target)

        XCTAssertNotNil(destination)
        if case .clubAdjustment(let club) = destination {
            XCTAssertEqual(club, "7-iron")
        } else {
            XCTFail("Expected clubAdjustment destination")
        }
    }

    func testDeepLinkBuilder_clubAdjustment_missingClub_returnsNil() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: [:]
        )

        let destination = builder.buildDestination(from: target)

        XCTAssertNil(destination, "Should return nil when required parameter is missing")
    }

    func testDeepLinkBuilder_shotRecommendation_allParameters() {
        let builder = DeepLinkBuilder()
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

        let destination = builder.buildDestination(from: target)

        XCTAssertNotNil(destination)
        if case .shotRecommendation(let yardage, let club, let lie, let wind) = destination {
            XCTAssertEqual(yardage, 150)
            XCTAssertEqual(club, "7-iron")
            XCTAssertEqual(lie, "fairway")
            XCTAssertEqual(wind, "10mph headwind")
        } else {
            XCTFail("Expected shotRecommendation destination")
        }
    }

    func testDeepLinkBuilder_shotRecommendation_optionalParameters() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "shot_recommendation",
            parameters: [:] // All parameters are optional
        )

        let destination = builder.buildDestination(from: target)

        XCTAssertNotNil(destination)
        if case .shotRecommendation(let yardage, let club, let lie, let wind) = destination {
            XCTAssertNil(yardage)
            XCTAssertNil(club)
            XCTAssertNil(lie)
            XCTAssertNil(wind)
        } else {
            XCTFail("Expected shotRecommendation destination")
        }
    }

    func testDeepLinkBuilder_recoveryOverview() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .recovery,
            screen: "overview",
            parameters: [:]
        )

        let destination = builder.buildDestination(from: target)

        XCTAssertNotNil(destination)
        if case .recoveryOverview = destination {
            // Success
        } else {
            XCTFail("Expected recoveryOverview destination")
        }
    }

    func testDeepLinkBuilder_drillScreen_withParameters() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .coach,
            screen: "drill_screen",
            parameters: [
                "drill_type": "putting",
                "club": "putter"
            ]
        )

        let destination = builder.buildDestination(from: target)

        XCTAssertNotNil(destination)
        if case .drillScreen(let drillType, let focusClub) = destination {
            XCTAssertEqual(drillType, "putting")
            XCTAssertEqual(focusClub, "putter")
        } else {
            XCTFail("Expected drillScreen destination")
        }
    }

    func testDeepLinkBuilder_scoreEntry_validHole() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "score_entry",
            parameters: ["hole": "5"]
        )

        let destination = builder.buildDestination(from: target)

        XCTAssertNotNil(destination)
        if case .scoreEntry(let hole) = destination {
            XCTAssertEqual(hole, 5)
        } else {
            XCTFail("Expected scoreEntry destination")
        }
    }

    func testDeepLinkBuilder_scoreEntry_missingHole_returnsNil() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "score_entry",
            parameters: [:]
        )

        let destination = builder.buildDestination(from: target)

        XCTAssertNil(destination, "Should return nil when hole parameter is missing")
    }

    func testDeepLinkBuilder_unknownScreen_returnsNil() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "unknown_screen",
            parameters: [:]
        )

        let destination = builder.buildDestination(from: target)

        XCTAssertNil(destination, "Should return nil for unknown screens")
    }

    func testDeepLinkBuilder_validate_clubAdjustment_valid() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: ["club": "7-iron"]
        )

        XCTAssertTrue(builder.validate(target))
    }

    func testDeepLinkBuilder_validate_clubAdjustment_invalid() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: [:]
        )

        XCTAssertFalse(builder.validate(target))
    }

    func testDeepLinkBuilder_validate_scoreEntry_validHole() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "score_entry",
            parameters: ["hole": "10"]
        )

        XCTAssertTrue(builder.validate(target))
    }

    func testDeepLinkBuilder_validate_scoreEntry_invalidHole() {
        let builder = DeepLinkBuilder()
        let target = RoutingTarget(
            module: .caddy,
            screen: "score_entry",
            parameters: ["hole": "25"] // Invalid: > 18
        )

        XCTAssertFalse(builder.validate(target))
    }

    // MARK: - NavigationCoordinator Tests

    func testNavigationCoordinator_initialState() {
        let coordinator = NavigationCoordinator()

        XCTAssertTrue(coordinator.isEmpty)
        XCTAssertEqual(coordinator.stackDepth, 0)
        XCTAssertNil(coordinator.currentDestination)
    }

    func testNavigationCoordinator_navigate_addsToStack() {
        let coordinator = NavigationCoordinator()
        let destination = NavCaddyDestination.clubAdjustment(club: "7-iron")

        coordinator.navigate(to: destination)

        XCTAssertFalse(coordinator.isEmpty)
        XCTAssertEqual(coordinator.stackDepth, 1)
        XCTAssertEqual(coordinator.currentDestination, destination)
    }

    func testNavigationCoordinator_navigateMultiple() {
        let coordinator = NavigationCoordinator()
        let dest1 = NavCaddyDestination.clubAdjustment(club: "7-iron")
        let dest2 = NavCaddyDestination.recoveryOverview

        coordinator.navigate(to: dest1)
        coordinator.navigate(to: dest2)

        XCTAssertEqual(coordinator.stackDepth, 2)
        XCTAssertEqual(coordinator.currentDestination, dest2)
    }

    func testNavigationCoordinator_navigateBack() {
        let coordinator = NavigationCoordinator()
        let dest1 = NavCaddyDestination.clubAdjustment(club: "7-iron")
        let dest2 = NavCaddyDestination.recoveryOverview

        coordinator.navigate(to: dest1)
        coordinator.navigate(to: dest2)
        coordinator.navigateBack()

        XCTAssertEqual(coordinator.stackDepth, 1)
        XCTAssertEqual(coordinator.currentDestination, dest1)
    }

    func testNavigationCoordinator_navigateBack_emptyStack() {
        let coordinator = NavigationCoordinator()

        coordinator.navigateBack() // Should not crash

        XCTAssertTrue(coordinator.isEmpty)
    }

    func testNavigationCoordinator_popToRoot() {
        let coordinator = NavigationCoordinator()
        coordinator.navigate(to: .clubAdjustment(club: "7-iron"))
        coordinator.navigate(to: .recoveryOverview)
        coordinator.navigate(to: .drillScreen(drillType: "putting", focusClub: nil))

        coordinator.popToRoot()

        XCTAssertTrue(coordinator.isEmpty)
        XCTAssertEqual(coordinator.stackDepth, 0)
        XCTAssertNil(coordinator.currentDestination)
    }

    func testNavigationCoordinator_replace() {
        let coordinator = NavigationCoordinator()
        let dest1 = NavCaddyDestination.clubAdjustment(club: "7-iron")
        let dest2 = NavCaddyDestination.recoveryOverview

        coordinator.navigate(to: dest1)
        coordinator.replace(with: dest2)

        XCTAssertEqual(coordinator.stackDepth, 1)
        XCTAssertEqual(coordinator.currentDestination, dest2)
    }

    func testNavigationCoordinator_replace_emptyStack() {
        let coordinator = NavigationCoordinator()
        let destination = NavCaddyDestination.recoveryOverview

        coordinator.replace(with: destination)

        XCTAssertEqual(coordinator.stackDepth, 1)
        XCTAssertEqual(coordinator.currentDestination, destination)
    }

    // MARK: - NavigationExecutor Tests

    func testNavigationExecutor_navigate_success() {
        let coordinator = NavigationCoordinator()
        let executor = NavigationExecutor(coordinator: coordinator)

        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: ["club": "7-iron"]
        )
        let intent = ParsedIntent(
            intentType: .clubAdjustment,
            confidence: 0.9
        )
        let result = RoutingResult.navigate(target: target, intent: intent)

        let action = executor.execute(result)

        // Verify navigation was executed
        XCTAssertEqual(coordinator.stackDepth, 1)

        // Verify action is correct
        if case .navigated(let destination, let returnedIntent) = action {
            if case .clubAdjustment(let club) = destination {
                XCTAssertEqual(club, "7-iron")
            } else {
                XCTFail("Expected clubAdjustment destination")
            }
            XCTAssertEqual(returnedIntent.intentType, .clubAdjustment)
        } else {
            XCTFail("Expected navigated action")
        }
    }

    func testNavigationExecutor_navigate_invalidTarget() {
        let coordinator = NavigationCoordinator()
        let executor = NavigationExecutor(coordinator: coordinator)

        // Missing required parameter
        let target = RoutingTarget(
            module: .caddy,
            screen: "club_adjustment",
            parameters: [:]
        )
        let intent = ParsedIntent(
            intentType: .clubAdjustment,
            confidence: 0.9
        )
        let result = RoutingResult.navigate(target: target, intent: intent)

        let action = executor.execute(result)

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)

        // Verify error action
        if case .showError(let message, _) = action {
            XCTAssertTrue(message.contains("Invalid"))
        } else {
            XCTFail("Expected showError action")
        }
    }

    func testNavigationExecutor_noNavigation() {
        let coordinator = NavigationCoordinator()
        let executor = NavigationExecutor(coordinator: coordinator)

        let intent = ParsedIntent(
            intentType: .helpRequest,
            confidence: 0.9
        )
        let result = RoutingResult.noNavigation(
            intent: intent,
            response: "I can help you with that."
        )

        let action = executor.execute(result)

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)

        // Verify response action
        if case .showResponse(let response, let returnedIntent) = action {
            XCTAssertEqual(response, "I can help you with that.")
            XCTAssertEqual(returnedIntent.intentType, .helpRequest)
        } else {
            XCTFail("Expected showResponse action")
        }
    }

    func testNavigationExecutor_prerequisiteMissing() {
        let coordinator = NavigationCoordinator()
        let executor = NavigationExecutor(coordinator: coordinator)

        let intent = ParsedIntent(
            intentType: .recoveryCheck,
            confidence: 0.9
        )
        let result = RoutingResult.prerequisiteMissing(
            intent: intent,
            missing: [.recoveryData],
            message: "You need to log recovery data first."
        )

        let action = executor.execute(result)

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)

        // Verify prerequisite prompt action
        if case .showPrerequisitePrompt(let message, let prerequisites, let returnedIntent) = action {
            XCTAssertEqual(message, "You need to log recovery data first.")
            XCTAssertEqual(prerequisites.count, 1)
            XCTAssertEqual(prerequisites.first, .recoveryData)
            XCTAssertEqual(returnedIntent.intentType, .recoveryCheck)
        } else {
            XCTFail("Expected showPrerequisitePrompt action")
        }
    }

    func testNavigationExecutor_confirmationRequired() {
        let coordinator = NavigationCoordinator()
        let executor = NavigationExecutor(coordinator: coordinator)

        let intent = ParsedIntent(
            intentType: .clubAdjustment,
            confidence: 0.6 // Medium confidence
        )
        let result = RoutingResult.confirmationRequired(
            intent: intent,
            message: "Did you mean to adjust your 7-iron?"
        )

        let action = executor.execute(result)

        // Verify no navigation occurred
        XCTAssertEqual(coordinator.stackDepth, 0)

        // Verify confirmation action
        if case .requestConfirmation(let message, let returnedIntent) = action {
            XCTAssertEqual(message, "Did you mean to adjust your 7-iron?")
            XCTAssertEqual(returnedIntent.intentType, .clubAdjustment)
        } else {
            XCTFail("Expected requestConfirmation action")
        }
    }

    func testNavigationExecutor_navigateBack() {
        let coordinator = NavigationCoordinator()
        let executor = NavigationExecutor(coordinator: coordinator)

        coordinator.navigate(to: .clubAdjustment(club: "7-iron"))
        coordinator.navigate(to: .recoveryOverview)

        executor.navigateBack()

        XCTAssertEqual(coordinator.stackDepth, 1)
    }

    func testNavigationExecutor_popToRoot() {
        let coordinator = NavigationCoordinator()
        let executor = NavigationExecutor(coordinator: coordinator)

        coordinator.navigate(to: .clubAdjustment(club: "7-iron"))
        coordinator.navigate(to: .recoveryOverview)

        executor.popToRoot()

        XCTAssertTrue(coordinator.isEmpty)
    }

    // MARK: - NavCaddyDestination Tests

    func testNavCaddyDestination_module_caddy() {
        XCTAssertEqual(NavCaddyDestination.clubAdjustment(club: "7-iron").module, .caddy)
        XCTAssertEqual(NavCaddyDestination.shotRecommendation(yardage: nil, club: nil, lie: nil, wind: nil).module, .caddy)
        XCTAssertEqual(NavCaddyDestination.weatherCheck(location: nil).module, .caddy)
    }

    func testNavCaddyDestination_module_coach() {
        XCTAssertEqual(NavCaddyDestination.drillScreen(drillType: nil, focusClub: nil).module, .coach)
        XCTAssertEqual(NavCaddyDestination.statsLookup(statType: nil, dateRange: nil).module, .coach)
        XCTAssertEqual(NavCaddyDestination.patternQuery(club: nil, pressureContext: nil).module, .coach)
    }

    func testNavCaddyDestination_module_recovery() {
        XCTAssertEqual(NavCaddyDestination.recoveryOverview.module, .recovery)
        XCTAssertEqual(NavCaddyDestination.recoveryDetail(date: "2026-01-01").module, .recovery)
    }

    func testNavCaddyDestination_module_settings() {
        XCTAssertEqual(NavCaddyDestination.equipmentInfo(clubToEdit: nil).module, .settings)
        XCTAssertEqual(NavCaddyDestination.settings(section: nil).module, .settings)
        XCTAssertEqual(NavCaddyDestination.help.module, .settings)
    }

    func testNavCaddyDestination_description() {
        let dest1 = NavCaddyDestination.clubAdjustment(club: "7-iron")
        XCTAssertTrue(dest1.description.contains("Club Adjustment"))
        XCTAssertTrue(dest1.description.contains("7-iron"))

        let dest2 = NavCaddyDestination.shotRecommendation(yardage: 150, club: "7-iron", lie: "fairway", wind: nil)
        XCTAssertTrue(dest2.description.contains("Shot Recommendation"))
        XCTAssertTrue(dest2.description.contains("150"))
        XCTAssertTrue(dest2.description.contains("7-iron"))
    }

    // MARK: - NavigationAction Tests

    func testNavigationAction_intent() {
        let intent = ParsedIntent(intentType: .clubAdjustment, confidence: 0.9)

        let action1 = NavigationAction.navigated(
            destination: .clubAdjustment(club: "7-iron"),
            intent: intent
        )
        XCTAssertEqual(action1.intent.intentType, .clubAdjustment)

        let action2 = NavigationAction.showResponse(response: "Here you go", intent: intent)
        XCTAssertEqual(action2.intent.intentType, .clubAdjustment)

        let action3 = NavigationAction.showError(message: "Error", intent: intent)
        XCTAssertEqual(action3.intent.intentType, .clubAdjustment)
    }

    func testNavigationAction_message() {
        let intent = ParsedIntent(intentType: .helpRequest, confidence: 0.9)

        let action1 = NavigationAction.navigated(
            destination: .help,
            intent: intent
        )
        XCTAssertNil(action1.message)

        let action2 = NavigationAction.showResponse(response: "Here you go", intent: intent)
        XCTAssertEqual(action2.message, "Here you go")

        let action3 = NavigationAction.showError(message: "Error occurred", intent: intent)
        XCTAssertEqual(action3.message, "Error occurred")
    }

    func testNavigationAction_requiresUIFeedback() {
        let intent = ParsedIntent(intentType: .helpRequest, confidence: 0.9)

        let action1 = NavigationAction.navigated(destination: .help, intent: intent)
        XCTAssertFalse(action1.requiresUIFeedback)

        let action2 = NavigationAction.showResponse(response: "Here you go", intent: intent)
        XCTAssertTrue(action2.requiresUIFeedback)

        let action3 = NavigationAction.showError(message: "Error", intent: intent)
        XCTAssertTrue(action3.requiresUIFeedback)
    }
}
