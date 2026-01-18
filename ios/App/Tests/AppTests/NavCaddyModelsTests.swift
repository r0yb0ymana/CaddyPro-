import XCTest
@testable import App

/// Comprehensive tests for NavCaddy domain models
///
/// Verifies:
/// - All enums have expected cases
/// - Struct equality works correctly
/// - Codable encoding/decoding roundtrip works
final class NavCaddyModelsTests: XCTestCase {

    // MARK: - Enum Tests

    func testIntentTypeHasAllExpectedCases() {
        let expectedCases: Set<IntentType> = [
            .clubAdjustment,
            .recoveryCheck,
            .shotRecommendation,
            .scoreEntry,
            .patternQuery,
            .drillRequest,
            .weatherCheck,
            .statsLookup,
            .roundStart,
            .roundEnd,
            .equipmentInfo,
            .courseInfo,
            .settingsChange,
            .helpRequest,
            .feedback
        ]

        XCTAssertEqual(Set(IntentType.allCases), expectedCases)
        XCTAssertEqual(IntentType.allCases.count, 15, "Should have exactly 15 MVP intents")
    }

    func testModuleHasAllExpectedCases() {
        let expectedCases: Set<Module> = [
            .caddy,
            .coach,
            .recovery,
            .settings
        ]

        XCTAssertEqual(Set(Module.allCases), expectedCases)
        XCTAssertEqual(Module.allCases.count, 4)
    }

    func testClubTypeHasAllExpectedCases() {
        let expectedCases: Set<ClubType> = [
            .driver,
            .wood,
            .hybrid,
            .iron,
            .wedge,
            .putter
        ]

        XCTAssertEqual(Set(ClubType.allCases), expectedCases)
        XCTAssertEqual(ClubType.allCases.count, 6)
    }

    func testMissDirectionHasAllExpectedCases() {
        let expectedCases: Set<MissDirection> = [
            .push,
            .pull,
            .slice,
            .hook,
            .fat,
            .thin,
            .straight
        ]

        XCTAssertEqual(Set(MissDirection.allCases), expectedCases)
        XCTAssertEqual(MissDirection.allCases.count, 7)
    }

    func testLieHasAllExpectedCases() {
        let expectedCases: Set<Lie> = [
            .tee,
            .fairway,
            .rough,
            .bunker,
            .green,
            .fringe,
            .hazard
        ]

        XCTAssertEqual(Set(Lie.allCases), expectedCases)
        XCTAssertEqual(Lie.allCases.count, 7)
    }

    func testConversationTurnRoleHasExpectedCases() {
        let expectedCases: Set<ConversationTurn.Role> = [
            .user,
            .assistant
        ]

        XCTAssertEqual(Set(ConversationTurn.Role.allCases), expectedCases)
        XCTAssertEqual(ConversationTurn.Role.allCases.count, 2)
    }

    // MARK: - Struct Equality Tests

    func testClubEquality() {
        let club1 = Club(id: "1", name: "7-Iron", type: .iron, loft: 34.0, distance: 150)
        let club2 = Club(id: "1", name: "7-Iron", type: .iron, loft: 34.0, distance: 150)
        let club3 = Club(id: "2", name: "7-Iron", type: .iron, loft: 34.0, distance: 150)

        XCTAssertEqual(club1, club2)
        XCTAssertNotEqual(club1, club3)
    }

    func testPressureContextEquality() {
        let context1 = PressureContext(isUserTagged: true, isInferred: false, scoringContext: "leading by 1")
        let context2 = PressureContext(isUserTagged: true, isInferred: false, scoringContext: "leading by 1")
        let context3 = PressureContext(isUserTagged: false, isInferred: true, scoringContext: nil)

        XCTAssertEqual(context1, context2)
        XCTAssertNotEqual(context1, context3)
    }

    func testPressureContextHasPressure() {
        let noPressure = PressureContext(isUserTagged: false, isInferred: false)
        let userTagged = PressureContext(isUserTagged: true, isInferred: false)
        let inferred = PressureContext(isUserTagged: false, isInferred: true)
        let both = PressureContext(isUserTagged: true, isInferred: true)

        XCTAssertFalse(noPressure.hasPressure)
        XCTAssertTrue(userTagged.hasPressure)
        XCTAssertTrue(inferred.hasPressure)
        XCTAssertTrue(both.hasPressure)
    }

    func testShotEquality() {
        let club = Club(name: "Driver", type: .driver)
        let timestamp = Date()
        let context = PressureContext(isUserTagged: true)

        let shot1 = Shot(
            id: "1",
            timestamp: timestamp,
            club: club,
            missDirection: .slice,
            lie: .tee,
            pressureContext: context,
            holeNumber: 1,
            notes: "Test"
        )
        let shot2 = Shot(
            id: "1",
            timestamp: timestamp,
            club: club,
            missDirection: .slice,
            lie: .tee,
            pressureContext: context,
            holeNumber: 1,
            notes: "Test"
        )

        XCTAssertEqual(shot1, shot2)
    }

    func testMissPatternConfidenceClamping() {
        let pattern1 = MissPattern(
            direction: .slice,
            frequency: 5,
            confidence: 1.5, // Should clamp to 1.0
            lastOccurrence: Date()
        )
        let pattern2 = MissPattern(
            direction: .hook,
            frequency: 3,
            confidence: -0.5, // Should clamp to 0.0
            lastOccurrence: Date()
        )

        XCTAssertEqual(pattern1.confidence, 1.0)
        XCTAssertEqual(pattern2.confidence, 0.0)
    }

    func testExtractedEntitiesFatigueClamps() {
        let entities1 = ExtractedEntities(fatigue: 15) // Should clamp to 10
        let entities2 = ExtractedEntities(fatigue: -5) // Should clamp to 1
        let entities3 = ExtractedEntities(fatigue: 5)  // Should stay 5

        XCTAssertEqual(entities1.fatigue, 10)
        XCTAssertEqual(entities2.fatigue, 1)
        XCTAssertEqual(entities3.fatigue, 5)
    }

    func testParsedIntentConfidenceClamping() {
        let intent1 = ParsedIntent(
            intentType: .clubAdjustment,
            confidence: 1.5 // Should clamp to 1.0
        )
        let intent2 = ParsedIntent(
            intentType: .recoveryCheck,
            confidence: -0.3 // Should clamp to 0.0
        )

        XCTAssertEqual(intent1.confidence, 1.0)
        XCTAssertEqual(intent2.confidence, 0.0)
    }

    func testSessionContextAddingTurn() {
        let context = SessionContext()
        let turn1 = ConversationTurn(role: .user, content: "Hello")
        let turn2 = ConversationTurn(role: .assistant, content: "Hi there")

        let newContext = context
            .addingTurn(turn1)
            .addingTurn(turn2)

        XCTAssertEqual(newContext.conversationHistory.count, 2)
        XCTAssertEqual(newContext.conversationHistory[0].content, "Hello")
        XCTAssertEqual(newContext.conversationHistory[1].content, "Hi there")
    }

    func testSessionContextLimitsHistoryTo10Turns() {
        var context = SessionContext()

        // Add 15 turns
        for i in 1...15 {
            let turn = ConversationTurn(role: .user, content: "Message \(i)")
            context = context.addingTurn(turn)
        }

        XCTAssertEqual(context.conversationHistory.count, 10, "Should keep only last 10 turns")
        XCTAssertEqual(context.conversationHistory.first?.content, "Message 6")
        XCTAssertEqual(context.conversationHistory.last?.content, "Message 15")
    }

    func testSessionContextEmpty() {
        let empty = SessionContext.empty

        XCTAssertNil(empty.currentRound)
        XCTAssertNil(empty.currentHole)
        XCTAssertNil(empty.lastShot)
        XCTAssertNil(empty.lastRecommendation)
        XCTAssertTrue(empty.conversationHistory.isEmpty)
    }

    // MARK: - Codable Roundtrip Tests

    func testIntentTypeCodable() throws {
        let intent = IntentType.clubAdjustment
        let encoded = try JSONEncoder().encode(intent)
        let decoded = try JSONDecoder().decode(IntentType.self, from: encoded)

        XCTAssertEqual(intent, decoded)
    }

    func testModuleCodable() throws {
        let module = Module.caddy
        let encoded = try JSONEncoder().encode(module)
        let decoded = try JSONDecoder().decode(Module.self, from: encoded)

        XCTAssertEqual(module, decoded)
    }

    func testClubCodable() throws {
        let club = Club(name: "7-Iron", type: .iron, loft: 34.0, distance: 150)
        let encoded = try JSONEncoder().encode(club)
        let decoded = try JSONDecoder().decode(Club.self, from: encoded)

        XCTAssertEqual(club, decoded)
    }

    func testPressureContextCodable() throws {
        let context = PressureContext(
            isUserTagged: true,
            isInferred: false,
            scoringContext: "tournament mode"
        )
        let encoded = try JSONEncoder().encode(context)
        let decoded = try JSONDecoder().decode(PressureContext.self, from: encoded)

        XCTAssertEqual(context, decoded)
    }

    func testShotCodable() throws {
        let club = Club(name: "Driver", type: .driver)
        let shot = Shot(
            timestamp: Date(),
            club: club,
            missDirection: .slice,
            lie: .tee,
            pressureContext: PressureContext(isUserTagged: true),
            holeNumber: 1,
            notes: "Test shot"
        )

        let encoded = try JSONEncoder().encode(shot)
        let decoded = try JSONDecoder().decode(Shot.self, from: encoded)

        XCTAssertEqual(shot, decoded)
    }

    func testMissPatternCodable() throws {
        let club = Club(name: "Driver", type: .driver)
        let pattern = MissPattern(
            direction: .slice,
            club: club,
            frequency: 10,
            confidence: 0.85,
            pressureContext: PressureContext(isInferred: true),
            lastOccurrence: Date()
        )

        let encoded = try JSONEncoder().encode(pattern)
        let decoded = try JSONDecoder().decode(MissPattern.self, from: encoded)

        XCTAssertEqual(pattern, decoded)
    }

    func testRoundCodable() throws {
        let round = Round(
            startTime: Date(),
            courseName: "Pebble Beach",
            scores: [1: 4, 2: 3, 3: 5]
        )

        let encoded = try JSONEncoder().encode(round)
        let decoded = try JSONDecoder().decode(Round.self, from: encoded)

        XCTAssertEqual(round, decoded)
    }

    func testConversationTurnCodable() throws {
        let turn = ConversationTurn(
            role: .user,
            content: "What club should I use?",
            timestamp: Date()
        )

        let encoded = try JSONEncoder().encode(turn)
        let decoded = try JSONDecoder().decode(ConversationTurn.self, from: encoded)

        XCTAssertEqual(turn, decoded)
    }

    func testSessionContextCodable() throws {
        let round = Round(courseName: "Augusta National")
        let club = Club(name: "7-Iron", type: .iron)
        let shot = Shot(club: club, lie: .fairway)
        let turn1 = ConversationTurn(role: .user, content: "Hello")
        let turn2 = ConversationTurn(role: .assistant, content: "Hi")

        let context = SessionContext(
            currentRound: round,
            currentHole: 5,
            lastShot: shot,
            lastRecommendation: "Use 7-iron",
            conversationHistory: [turn1, turn2]
        )

        let encoded = try JSONEncoder().encode(context)
        let decoded = try JSONDecoder().decode(SessionContext.self, from: encoded)

        XCTAssertEqual(context, decoded)
    }

    func testExtractedEntitiesCodable() throws {
        let club = Club(name: "Pitching Wedge", type: .wedge)
        let entities = ExtractedEntities(
            club: club,
            yardage: 125,
            lie: .fairway,
            wind: "10mph headwind",
            fatigue: 7,
            pain: "lower back",
            scoreContext: "leading by 2",
            holeNumber: 15
        )

        let encoded = try JSONEncoder().encode(entities)
        let decoded = try JSONDecoder().decode(ExtractedEntities.self, from: encoded)

        XCTAssertEqual(entities, decoded)
    }

    func testRoutingTargetCodable() throws {
        let target = RoutingTarget(
            module: .caddy,
            screen: "ClubSelection",
            parameters: ["club": "7-iron", "yardage": "150"]
        )

        let encoded = try JSONEncoder().encode(target)
        let decoded = try JSONDecoder().decode(RoutingTarget.self, from: encoded)

        XCTAssertEqual(target, decoded)
    }

    func testParsedIntentCodable() throws {
        let club = Club(name: "Driver", type: .driver)
        let entities = ExtractedEntities(club: club, yardage: 250)
        let target = RoutingTarget(module: .caddy, screen: "ShotRecommendation")

        let intent = ParsedIntent(
            intentType: .shotRecommendation,
            confidence: 0.92,
            entities: entities,
            userGoal: "Get club recommendation for tee shot",
            routingTarget: target
        )

        let encoded = try JSONEncoder().encode(intent)
        let decoded = try JSONDecoder().decode(ParsedIntent.self, from: encoded)

        XCTAssertEqual(intent, decoded)
    }

    // MARK: - Confidence Threshold Tests

    func testConfidenceThresholdValues() {
        XCTAssertEqual(ConfidenceThresholds.route, 0.75)
        XCTAssertEqual(ConfidenceThresholds.confirm, 0.50)
        XCTAssertEqual(ConfidenceThresholds.clarify, 0.50)
    }

    func testConfidenceThresholdActions() {
        // High confidence: route
        XCTAssertEqual(ConfidenceThresholds.action(for: 0.75), .route)
        XCTAssertEqual(ConfidenceThresholds.action(for: 0.85), .route)
        XCTAssertEqual(ConfidenceThresholds.action(for: 1.0), .route)

        // Medium confidence: confirm
        XCTAssertEqual(ConfidenceThresholds.action(for: 0.50), .confirm)
        XCTAssertEqual(ConfidenceThresholds.action(for: 0.65), .confirm)
        XCTAssertEqual(ConfidenceThresholds.action(for: 0.74), .confirm)

        // Low confidence: clarify
        XCTAssertEqual(ConfidenceThresholds.action(for: 0.0), .clarify)
        XCTAssertEqual(ConfidenceThresholds.action(for: 0.25), .clarify)
        XCTAssertEqual(ConfidenceThresholds.action(for: 0.49), .clarify)
    }

    // MARK: - Edge Cases

    func testEmptyExtractedEntities() {
        let entities = ExtractedEntities()

        XCTAssertNil(entities.club)
        XCTAssertNil(entities.yardage)
        XCTAssertNil(entities.lie)
        XCTAssertNil(entities.wind)
        XCTAssertNil(entities.fatigue)
        XCTAssertNil(entities.pain)
        XCTAssertNil(entities.scoreContext)
        XCTAssertNil(entities.holeNumber)
    }

    func testEmptyRoutingTargetParameters() {
        let target = RoutingTarget(module: .coach, screen: "DrillLibrary")

        XCTAssertTrue(target.parameters.isEmpty)
    }

    func testRoundWithEmptyScores() {
        let round = Round(courseName: "Test Course")

        XCTAssertTrue(round.scores.isEmpty)
    }
}
