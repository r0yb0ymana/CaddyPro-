import XCTest
import SwiftData
@testable import App

@MainActor
final class NavCaddyRepositoryTests: XCTestCase {
    var repository: NavCaddyRepositoryImpl!
    var modelContainer: ModelContainer!

    override func setUp() async throws {
        try await super.setUp()

        // Create in-memory container for testing
        modelContainer = try NavCaddyDataContainer.create(inMemory: true)
        repository = NavCaddyRepositoryImpl(modelContainer: modelContainer)
    }

    override func tearDown() async throws {
        repository = nil
        modelContainer = nil
        try await super.tearDown()
    }

    // MARK: - Shot Tests

    func testRecordShot() async throws {
        // Given
        let club = Club(name: "7 Iron", type: .iron)
        let shot = Shot(
            club: club,
            missDirection: .slice,
            lie: .fairway,
            pressureContext: PressureContext(isUserTagged: true),
            holeNumber: 5
        )

        // When
        try await repository.recordShot(shot)

        // Then
        let shots = try await repository.getRecentShots(days: 90)
        XCTAssertEqual(shots.count, 1)
        XCTAssertEqual(shots.first?.id, shot.id)
        XCTAssertEqual(shots.first?.club.name, "7 Iron")
        XCTAssertEqual(shots.first?.missDirection, .slice)
    }

    func testGetRecentShotsFiltersByDate() async throws {
        // Given - Create shots with different timestamps
        let oldClub = Club(name: "Driver", type: .driver)
        let oldShot = Shot(
            id: "old",
            timestamp: Date().addingTimeInterval(-100 * 24 * 60 * 60), // 100 days ago
            club: oldClub,
            lie: .tee
        )

        let recentClub = Club(name: "8 Iron", type: .iron)
        let recentShot = Shot(
            id: "recent",
            timestamp: Date().addingTimeInterval(-10 * 24 * 60 * 60), // 10 days ago
            club: recentClub,
            lie: .fairway
        )

        try await repository.recordShot(oldShot)
        try await repository.recordShot(recentShot)

        // When
        let shots = try await repository.getRecentShots(days: 30)

        // Then - Only recent shot should be returned
        XCTAssertEqual(shots.count, 1)
        XCTAssertEqual(shots.first?.id, "recent")
    }

    func testGetShotsWithPressure() async throws {
        // Given
        let club = Club(name: "9 Iron", type: .iron)

        let pressureShot = Shot(
            id: "pressure",
            club: club,
            lie: .fairway,
            pressureContext: PressureContext(isUserTagged: true)
        )

        let normalShot = Shot(
            id: "normal",
            club: club,
            lie: .fairway,
            pressureContext: PressureContext()
        )

        try await repository.recordShot(pressureShot)
        try await repository.recordShot(normalShot)

        // When
        let shots = try await repository.getShotsWithPressure()

        // Then
        XCTAssertEqual(shots.count, 1)
        XCTAssertEqual(shots.first?.id, "pressure")
    }

    // MARK: - Pattern Tests

    func testUpdateAndGetPattern() async throws {
        // Given
        let club = Club(name: "Driver", type: .driver)
        let pattern = MissPattern(
            direction: .slice,
            club: club,
            frequency: 5,
            confidence: 0.8,
            pressureContext: PressureContext(isUserTagged: true),
            lastOccurrence: Date()
        )

        // When
        try await repository.updatePattern(pattern)

        // Then
        let patterns = try await repository.getMissPatterns()
        XCTAssertEqual(patterns.count, 1)
        XCTAssertEqual(patterns.first?.direction, .slice)
        XCTAssertEqual(patterns.first?.frequency, 5)
        XCTAssertEqual(patterns.first?.confidence, 0.8)
    }

    func testUpdateExistingPattern() async throws {
        // Given
        let club = Club(name: "Driver", type: .driver)
        let pattern = MissPattern(
            id: "pattern1",
            direction: .slice,
            club: club,
            frequency: 5,
            confidence: 0.8,
            lastOccurrence: Date()
        )

        try await repository.updatePattern(pattern)

        // When - Update the same pattern
        let updatedPattern = MissPattern(
            id: "pattern1",
            direction: .slice,
            club: club,
            frequency: 10,
            confidence: 0.9,
            lastOccurrence: Date()
        )

        try await repository.updatePattern(updatedPattern)

        // Then
        let patterns = try await repository.getMissPatterns()
        XCTAssertEqual(patterns.count, 1)
        XCTAssertEqual(patterns.first?.frequency, 10)
        XCTAssertEqual(patterns.first?.confidence, 0.9)
    }

    func testGetPatternsByDirection() async throws {
        // Given
        let club = Club(name: "Driver", type: .driver)
        let slicePattern = MissPattern(
            direction: .slice,
            club: club,
            frequency: 5,
            confidence: 0.8,
            lastOccurrence: Date()
        )

        let hookPattern = MissPattern(
            direction: .hook,
            club: club,
            frequency: 3,
            confidence: 0.6,
            lastOccurrence: Date()
        )

        try await repository.updatePattern(slicePattern)
        try await repository.updatePattern(hookPattern)

        // When
        let patterns = try await repository.getPatternsByDirection(.slice)

        // Then
        XCTAssertEqual(patterns.count, 1)
        XCTAssertEqual(patterns.first?.direction, .slice)
    }

    func testGetPatternsByClub() async throws {
        // Given
        let driver = Club(id: "driver1", name: "Driver", type: .driver)
        let iron = Club(id: "iron1", name: "7 Iron", type: .iron)

        let driverPattern = MissPattern(
            direction: .slice,
            club: driver,
            frequency: 5,
            confidence: 0.8,
            lastOccurrence: Date()
        )

        let ironPattern = MissPattern(
            direction: .pull,
            club: iron,
            frequency: 3,
            confidence: 0.6,
            lastOccurrence: Date()
        )

        try await repository.updatePattern(driverPattern)
        try await repository.updatePattern(ironPattern)

        // When
        let patterns = try await repository.getPatternsByClub("driver1")

        // Then
        XCTAssertEqual(patterns.count, 1)
        XCTAssertEqual(patterns.first?.club?.id, "driver1")
    }

    // MARK: - Session Tests

    func testGetEmptySession() async throws {
        // When
        let session = try await repository.getSession()

        // Then
        XCTAssertNil(session.currentRound)
        XCTAssertNil(session.currentHole)
        XCTAssertNil(session.lastRecommendation)
        XCTAssertEqual(session.conversationHistory.count, 0)
    }

    func testSaveAndGetSession() async throws {
        // Given
        let round = Round(id: "round1", courseName: "Pebble Beach", scores: [1: 4, 2: 5])
        let turn = ConversationTurn(role: .user, content: "What club should I hit?")

        let session = SessionContext(
            currentRound: round,
            currentHole: 3,
            lastRecommendation: "Hit a 7-iron",
            conversationHistory: [turn]
        )

        // When
        try await repository.saveSession(session)
        let retrieved = try await repository.getSession()

        // Then
        XCTAssertEqual(retrieved.currentRound?.id, "round1")
        XCTAssertEqual(retrieved.currentHole, 3)
        XCTAssertEqual(retrieved.lastRecommendation, "Hit a 7-iron")
        XCTAssertEqual(retrieved.conversationHistory.count, 1)
        XCTAssertEqual(retrieved.conversationHistory.first?.content, "What club should I hit?")
    }

    func testAddConversationTurn() async throws {
        // Given - Create initial session
        let turn1 = ConversationTurn(role: .user, content: "First message")
        let session = SessionContext(conversationHistory: [turn1])
        try await repository.saveSession(session)

        // When - Add another turn
        let turn2 = ConversationTurn(role: .assistant, content: "Response")
        try await repository.addConversationTurn(turn2)

        // Then
        let retrieved = try await repository.getSession()
        XCTAssertEqual(retrieved.conversationHistory.count, 2)
        XCTAssertEqual(retrieved.conversationHistory.last?.content, "Response")
    }

    func testConversationHistoryLimitedTo10Turns() async throws {
        // Given - Create session with 12 turns
        var turns: [ConversationTurn] = []
        for i in 1...12 {
            turns.append(ConversationTurn(role: .user, content: "Message \(i)"))
        }

        let session = SessionContext(conversationHistory: turns)

        // When
        try await repository.saveSession(session)
        let retrieved = try await repository.getSession()

        // Then - Only last 10 should be retained
        XCTAssertEqual(retrieved.conversationHistory.count, 10)
        XCTAssertEqual(retrieved.conversationHistory.first?.content, "Message 3")
        XCTAssertEqual(retrieved.conversationHistory.last?.content, "Message 12")
    }

    // MARK: - Memory Management Tests

    func testClearMemory() async throws {
        // Given - Add data
        let club = Club(name: "Driver", type: .driver)
        let shot = Shot(club: club, lie: .tee)
        try await repository.recordShot(shot)

        let pattern = MissPattern(
            direction: .slice,
            club: club,
            frequency: 5,
            confidence: 0.8,
            lastOccurrence: Date()
        )
        try await repository.updatePattern(pattern)

        let session = SessionContext(currentHole: 5)
        try await repository.saveSession(session)

        // When
        try await repository.clearMemory()

        // Then
        let shots = try await repository.getRecentShots(days: 90)
        let patterns = try await repository.getMissPatterns()
        let retrievedSession = try await repository.getSession()

        XCTAssertEqual(shots.count, 0)
        XCTAssertEqual(patterns.count, 0)
        XCTAssertNil(retrievedSession.currentHole)
    }

    func testEnforceRetentionPolicy() async throws {
        // Given - Create old and recent data
        let club = Club(name: "Driver", type: .driver)

        let oldShot = Shot(
            id: "old",
            timestamp: Date().addingTimeInterval(-100 * 24 * 60 * 60), // 100 days ago
            club: club,
            lie: .tee
        )

        let recentShot = Shot(
            id: "recent",
            timestamp: Date(),
            club: club,
            lie: .fairway
        )

        try await repository.recordShot(oldShot)
        try await repository.recordShot(recentShot)

        let oldPattern = MissPattern(
            id: "oldpattern",
            direction: .slice,
            club: club,
            frequency: 5,
            confidence: 0.8,
            lastOccurrence: Date().addingTimeInterval(-100 * 24 * 60 * 60)
        )

        let recentPattern = MissPattern(
            id: "recentpattern",
            direction: .hook,
            club: club,
            frequency: 3,
            confidence: 0.6,
            lastOccurrence: Date()
        )

        try await repository.updatePattern(oldPattern)
        try await repository.updatePattern(recentPattern)

        // When
        try await repository.enforceRetentionPolicy()

        // Then
        let shots = try await repository.getRecentShots(days: 90)
        let patterns = try await repository.getMissPatterns()

        XCTAssertEqual(shots.count, 1)
        XCTAssertEqual(shots.first?.id, "recent")

        XCTAssertEqual(patterns.count, 1)
        XCTAssertEqual(patterns.first?.id, "recentpattern")
    }
}
