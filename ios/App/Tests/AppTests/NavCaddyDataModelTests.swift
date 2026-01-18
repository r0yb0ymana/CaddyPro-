import XCTest
import SwiftData
@testable import App

@MainActor
final class NavCaddyDataModelTests: XCTestCase {

    // MARK: - ShotRecord Mapping Tests

    func testShotRecordFromDomain() {
        // Given
        let club = Club(id: "club1", name: "7 Iron", type: .iron, distance: 150)
        let pressureContext = PressureContext(
            isUserTagged: true,
            isInferred: false,
            scoringContext: "Tournament mode"
        )

        let shot = Shot(
            id: "shot1",
            timestamp: Date(),
            club: club,
            missDirection: .slice,
            lie: .fairway,
            pressureContext: pressureContext,
            holeNumber: 5,
            notes: "Windy conditions"
        )

        // When
        let record = ShotRecord(from: shot)

        // Then
        XCTAssertEqual(record.id, "shot1")
        XCTAssertEqual(record.clubId, "club1")
        XCTAssertEqual(record.clubName, "7 Iron")
        XCTAssertEqual(record.clubType, "iron")
        XCTAssertEqual(record.missDirection, "slice")
        XCTAssertEqual(record.lie, "fairway")
        XCTAssertTrue(record.isUserTaggedPressure)
        XCTAssertFalse(record.isInferredPressure)
        XCTAssertEqual(record.scoringContext, "Tournament mode")
        XCTAssertEqual(record.holeNumber, 5)
        XCTAssertEqual(record.notes, "Windy conditions")
    }

    func testShotRecordToDomain() {
        // Given
        let record = ShotRecord(
            id: "shot1",
            timestamp: Date(),
            clubId: "club1",
            clubName: "Driver",
            clubType: "driver",
            missDirection: "hook",
            lie: "tee",
            isUserTaggedPressure: false,
            isInferredPressure: true,
            scoringContext: "Leading by 1",
            holeNumber: 18,
            notes: "Pressure shot"
        )

        // When
        let shot = record.toDomain()

        // Then
        XCTAssertNotNil(shot)
        XCTAssertEqual(shot?.id, "shot1")
        XCTAssertEqual(shot?.club.id, "club1")
        XCTAssertEqual(shot?.club.name, "Driver")
        XCTAssertEqual(shot?.club.type, .driver)
        XCTAssertEqual(shot?.missDirection, .hook)
        XCTAssertEqual(shot?.lie, .tee)
        XCTAssertFalse(shot?.pressureContext.isUserTagged ?? true)
        XCTAssertTrue(shot?.pressureContext.isInferred ?? false)
        XCTAssertEqual(shot?.pressureContext.scoringContext, "Leading by 1")
        XCTAssertEqual(shot?.holeNumber, 18)
        XCTAssertEqual(shot?.notes, "Pressure shot")
    }

    func testShotRecordWithNullMissDirection() {
        // Given
        let record = ShotRecord(
            id: "shot1",
            timestamp: Date(),
            clubId: "club1",
            clubName: "Putter",
            clubType: "putter",
            missDirection: nil, // No miss
            lie: "green",
            isUserTaggedPressure: false,
            isInferredPressure: false,
            scoringContext: nil,
            holeNumber: 1,
            notes: nil
        )

        // When
        let shot = record.toDomain()

        // Then
        XCTAssertNotNil(shot)
        XCTAssertNil(shot?.missDirection)
        XCTAssertEqual(shot?.lie, .green)
    }

    func testShotRecordWithInvalidEnumReturnsNil() {
        // Given
        let record = ShotRecord(
            id: "shot1",
            timestamp: Date(),
            clubId: "club1",
            clubName: "Invalid",
            clubType: "invalid_type", // Invalid enum
            missDirection: nil,
            lie: "fairway",
            isUserTaggedPressure: false,
            isInferredPressure: false,
            scoringContext: nil,
            holeNumber: 1,
            notes: nil
        )

        // When
        let shot = record.toDomain()

        // Then
        XCTAssertNil(shot)
    }

    // MARK: - MissPatternRecord Mapping Tests

    func testMissPatternRecordFromDomain() {
        // Given
        let club = Club(id: "club1", name: "Driver", type: .driver)
        let pressureContext = PressureContext(
            isUserTagged: true,
            isInferred: true,
            scoringContext: "Tournament"
        )

        let pattern = MissPattern(
            id: "pattern1",
            direction: .slice,
            club: club,
            frequency: 10,
            confidence: 0.85,
            pressureContext: pressureContext,
            lastOccurrence: Date()
        )

        // When
        let record = MissPatternRecord(from: pattern)

        // Then
        XCTAssertEqual(record.id, "pattern1")
        XCTAssertEqual(record.direction, "slice")
        XCTAssertEqual(record.clubId, "club1")
        XCTAssertEqual(record.frequency, 10)
        XCTAssertEqual(record.confidence, 0.85)
        XCTAssertEqual(record.isUserTaggedPressure, true)
        XCTAssertEqual(record.isInferredPressure, true)
        XCTAssertEqual(record.scoringContext, "Tournament")
    }

    func testMissPatternRecordToDomain() {
        // Given
        let record = MissPatternRecord(
            id: "pattern1",
            direction: "hook",
            clubId: "club1",
            frequency: 5,
            confidence: 0.7,
            isUserTaggedPressure: true,
            isInferredPressure: false,
            scoringContext: "Leading",
            lastOccurrence: Date()
        )

        // When
        let pattern = record.toDomain()

        // Then
        XCTAssertNotNil(pattern)
        XCTAssertEqual(pattern?.id, "pattern1")
        XCTAssertEqual(pattern?.direction, .hook)
        XCTAssertEqual(pattern?.club?.id, "club1")
        XCTAssertEqual(pattern?.frequency, 5)
        XCTAssertEqual(pattern?.confidence, 0.7)
        XCTAssertEqual(pattern?.pressureContext?.isUserTagged, true)
        XCTAssertEqual(pattern?.pressureContext?.isInferred, false)
    }

    func testMissPatternRecordWithNullPressureContext() {
        // Given
        let record = MissPatternRecord(
            id: "pattern1",
            direction: "push",
            clubId: nil,
            frequency: 3,
            confidence: 0.6,
            isUserTaggedPressure: nil,
            isInferredPressure: nil,
            scoringContext: nil,
            lastOccurrence: Date()
        )

        // When
        let pattern = record.toDomain()

        // Then
        XCTAssertNotNil(pattern)
        XCTAssertNil(pattern?.club)
        XCTAssertNil(pattern?.pressureContext)
    }

    // MARK: - ConversationTurnRecord Mapping Tests

    func testConversationTurnRecordFromDomain() {
        // Given
        let turn = ConversationTurn(
            id: "turn1",
            role: .user,
            content: "What club should I hit?",
            timestamp: Date()
        )

        // When
        let record = ConversationTurnRecord(from: turn)

        // Then
        XCTAssertEqual(record.role, "user")
        XCTAssertEqual(record.content, "What club should I hit?")
    }

    func testConversationTurnRecordToDomain() {
        // Given
        let record = ConversationTurnRecord(
            id: UUID(),
            role: "assistant",
            content: "Try a 7-iron",
            timestamp: Date()
        )

        // When
        let turn = record.toDomain()

        // Then
        XCTAssertNotNil(turn)
        XCTAssertEqual(turn?.role, .assistant)
        XCTAssertEqual(turn?.content, "Try a 7-iron")
    }

    func testConversationTurnRecordWithInvalidRoleReturnsNil() {
        // Given
        let record = ConversationTurnRecord(
            id: UUID(),
            role: "invalid_role",
            content: "Test",
            timestamp: Date()
        )

        // When
        let turn = record.toDomain()

        // Then
        XCTAssertNil(turn)
    }

    // MARK: - SessionRecord Mapping Tests

    func testSessionRecordFromDomain() {
        // Given
        let round = Round(id: "round1", courseName: "Augusta", scores: [1: 4])
        let turn = ConversationTurn(role: .user, content: "Test")

        let context = SessionContext(
            currentRound: round,
            currentHole: 5,
            lastRecommendation: "Hit a fade",
            conversationHistory: [turn]
        )

        // When
        let record = SessionRecord(from: context)

        // Then
        XCTAssertEqual(record.id, "current")
        XCTAssertEqual(record.roundId, "round1")
        XCTAssertEqual(record.currentHole, 5)
        XCTAssertEqual(record.lastRecommendation, "Hit a fade")
        XCTAssertEqual(record.conversationTurns.count, 1)
    }

    func testSessionRecordToDomain() {
        // Given
        let turnRecord = ConversationTurnRecord(
            role: "user",
            content: "Hello",
            timestamp: Date()
        )

        let record = SessionRecord(
            roundId: "round1",
            currentHole: 3,
            lastRecommendation: "Play safe",
            conversationTurns: [turnRecord]
        )

        // When
        let context = record.toDomain()

        // Then
        XCTAssertEqual(context.currentRound?.id, "round1")
        XCTAssertEqual(context.currentHole, 3)
        XCTAssertEqual(context.lastRecommendation, "Play safe")
        XCTAssertEqual(context.conversationHistory.count, 1)
    }

    func testSessionRecordWithEmptyData() {
        // Given
        let record = SessionRecord()

        // When
        let context = record.toDomain()

        // Then
        XCTAssertNil(context.currentRound)
        XCTAssertNil(context.currentHole)
        XCTAssertNil(context.lastRecommendation)
        XCTAssertEqual(context.conversationHistory.count, 0)
    }

    // MARK: - Roundtrip Tests

    func testShotRoundtrip() {
        // Given
        let club = Club(id: "club1", name: "9 Iron", type: .iron)
        let originalShot = Shot(
            id: "shot1",
            club: club,
            missDirection: .pull,
            lie: .rough,
            pressureContext: PressureContext(isUserTagged: true),
            holeNumber: 7
        )

        // When - Convert to record and back
        let record = ShotRecord(from: originalShot)
        let convertedShot = record.toDomain()

        // Then
        XCTAssertNotNil(convertedShot)
        XCTAssertEqual(convertedShot?.id, originalShot.id)
        XCTAssertEqual(convertedShot?.club.name, originalShot.club.name)
        XCTAssertEqual(convertedShot?.missDirection, originalShot.missDirection)
        XCTAssertEqual(convertedShot?.lie, originalShot.lie)
        XCTAssertEqual(convertedShot?.holeNumber, originalShot.holeNumber)
    }

    func testMissPatternRoundtrip() {
        // Given
        let club = Club(id: "club1", name: "Driver", type: .driver)
        let originalPattern = MissPattern(
            id: "pattern1",
            direction: .slice,
            club: club,
            frequency: 8,
            confidence: 0.75,
            lastOccurrence: Date()
        )

        // When - Convert to record and back
        let record = MissPatternRecord(from: originalPattern)
        let convertedPattern = record.toDomain()

        // Then
        XCTAssertNotNil(convertedPattern)
        XCTAssertEqual(convertedPattern?.id, originalPattern.id)
        XCTAssertEqual(convertedPattern?.direction, originalPattern.direction)
        XCTAssertEqual(convertedPattern?.frequency, originalPattern.frequency)
        XCTAssertEqual(convertedPattern?.confidence, originalPattern.confidence)
    }
}
