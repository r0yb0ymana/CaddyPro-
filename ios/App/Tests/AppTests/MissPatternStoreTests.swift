import XCTest
import SwiftData
@testable import App

/// Unit tests for Miss Pattern Store (Task 14).
///
/// Spec R5: Verifies pattern aggregation, decay, filtering, and recording.
@MainActor
final class MissPatternStoreTests: XCTestCase {
    var container: ModelContainer!
    var repository: NavCaddyRepository!
    var store: MissPatternStore!

    override func setUp() async throws {
        // Create in-memory container for testing
        let schema = Schema([
            ShotRecord.self,
            MissPatternRecord.self,
            SessionRecord.self,
            ConversationTurnRecord.self
        ])

        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        container = try ModelContainer(for: schema, configurations: config)

        repository = NavCaddyRepositoryImpl(modelContainer: container)
        store = await MissPatternStore(repository: repository)
    }

    override func tearDown() async throws {
        try await repository.clearMemory()
        container = nil
        repository = nil
        store = nil
    }

    // MARK: - Recording Tests

    func testRecordMiss() async throws {
        // Given: A club and miss details
        let club = Club(id: "7iron", name: "7-iron", type: .iron)

        // When: Recording a miss
        try await store.recordMiss(
            club: club,
            direction: .slice,
            lie: .fairway,
            pressure: PressureContext()
        )

        // Then: Shot is recorded
        let shots = try await store.getRecentShots(count: 10)
        XCTAssertEqual(shots.count, 1)
        XCTAssertEqual(shots[0].club.id, "7iron")
        XCTAssertEqual(shots[0].missDirection, .slice)
        XCTAssertEqual(shots[0].lie, .fairway)
    }

    func testRecordMissWithParameters() async throws {
        // When: Recording a miss with parameters
        try await store.recordMiss(
            clubId: "driver",
            clubName: "Driver",
            clubType: .driver,
            direction: .hook,
            lie: .tee,
            pressure: PressureContext(isUserTagged: true),
            holeNumber: 1,
            notes: "Strong wind"
        )

        // Then: Shot is recorded with all details
        let shots = try await store.getRecentShots(count: 10)
        XCTAssertEqual(shots.count, 1)
        XCTAssertEqual(shots[0].club.name, "Driver")
        XCTAssertEqual(shots[0].missDirection, .hook)
        XCTAssertEqual(shots[0].holeNumber, 1)
        XCTAssertEqual(shots[0].notes, "Strong wind")
        XCTAssertTrue(shots[0].pressureContext.isUserTagged)
    }

    func testRecordSuccess() async throws {
        // Given: A club
        let club = Club(id: "pw", name: "Pitching Wedge", type: .wedge)

        // When: Recording a successful shot
        try await store.recordSuccess(
            club: club,
            lie: .fairway
        )

        // Then: Shot is recorded as straight
        let shots = try await store.getRecentShots(count: 10)
        XCTAssertEqual(shots.count, 1)
        XCTAssertEqual(shots[0].missDirection, .straight)
    }

    // MARK: - Pattern Aggregation Tests

    func testAggregatePatterns() async throws {
        // Given: Multiple shots with a pattern (slice with 7-iron)
        let club = Club(id: "7iron", name: "7-iron", type: .iron)

        for _ in 0..<5 {
            try await store.recordMiss(
                club: club,
                direction: .slice,
                lie: .fairway
            )
        }

        // Add a hook shot (noise)
        try await store.recordMiss(
            club: club,
            direction: .hook,
            lie: .fairway
        )

        // When: Getting patterns for the club
        let patterns = try await store.getPatterns(clubId: "7iron")

        // Then: Slice pattern has higher confidence than hook
        XCTAssertGreaterThan(patterns.count, 0)
        let slicePattern = patterns.first { $0.direction == .slice }
        let hookPattern = patterns.first { $0.direction == .hook }

        XCTAssertNotNil(slicePattern)
        XCTAssertEqual(slicePattern?.frequency, 5)
        XCTAssertGreaterThan(slicePattern?.confidence ?? 0, hookPattern?.confidence ?? 0)
    }

    func testPatternMinimumThreshold() async throws {
        // Given: Only 2 shots (below minimum of 3)
        let club = Club(id: "driver", name: "Driver", type: .driver)

        try await store.recordMiss(club: club, direction: .slice, lie: .tee)
        try await store.recordMiss(club: club, direction: .slice, lie: .tee)

        // When: Getting patterns
        let patterns = try await store.getPatterns(clubId: "driver")

        // Then: No patterns formed (below threshold)
        XCTAssertTrue(patterns.isEmpty)
    }

    func testPatternsByDirection() async throws {
        // Given: Multiple directions
        let club = Club(id: "5iron", name: "5-iron", type: .iron)

        for _ in 0..<4 {
            try await store.recordMiss(club: club, direction: .push, lie: .fairway)
        }

        for _ in 0..<3 {
            try await store.recordMiss(club: club, direction: .pull, lie: .fairway)
        }

        // When: Getting patterns by direction
        let pushPatterns = try await store.getPatternsByDirection(.push)

        // Then: Only push patterns returned
        XCTAssertGreaterThan(pushPatterns.count, 0)
        XCTAssertTrue(pushPatterns.allSatisfy { $0.direction == .push })
    }

    // MARK: - Pressure Context Tests

    func testPatternsByPressure() async throws {
        // Given: Pressure and non-pressure shots
        let club = Club(id: "pw", name: "Pitching Wedge", type: .wedge)
        let pressure = PressureContext(isUserTagged: true, scoringContext: "birdie putt")

        // Record pressure misses
        for _ in 0..<4 {
            try await store.recordMiss(
                club: club,
                direction: .thin,
                lie: .green,
                pressure: pressure
            )
        }

        // Record non-pressure misses
        for _ in 0..<3 {
            try await store.recordMiss(
                club: club,
                direction: .fat,
                lie: .green
            )
        }

        // When: Getting patterns with pressure filter
        let pressurePatterns = try await store.getPatterns(pressure: pressure)

        // Then: Only pressure patterns returned
        XCTAssertGreaterThan(pressurePatterns.count, 0)
        XCTAssertTrue(pressurePatterns.allSatisfy { $0.pressureContext?.hasPressure == true })
    }

    func testGetPressureShots() async throws {
        // Given: Mix of pressure and non-pressure shots
        let club = Club(id: "driver", name: "Driver", type: .driver)

        try await store.recordMiss(
            club: club,
            direction: .slice,
            lie: .tee,
            pressure: PressureContext(isUserTagged: true)
        )

        try await store.recordMiss(club: club, direction: .hook, lie: .tee)

        // When: Getting pressure shots
        let pressureShots = try await store.getPressureShots()

        // Then: Only pressure shot returned
        XCTAssertEqual(pressureShots.count, 1)
        XCTAssertTrue(pressureShots[0].pressureContext.hasPressure)
    }

    // MARK: - Decay Tests

    func testPatternDecayCalculator() {
        // Given: A decay calculator with 14-day half-life
        let calculator = PatternDecayCalculator()

        // When: Calculating decay for various ages
        let now = Date()
        let recent = calculator.calculateDecay(for: now)
        let halfLife = calculator.calculateDecay(for: now.addingTimeInterval(-14 * 24 * 60 * 60))
        let old = calculator.calculateDecay(for: now.addingTimeInterval(-60 * 24 * 60 * 60))

        // Then: Decay follows exponential curve
        XCTAssertEqual(recent, 1.0, accuracy: 0.01)
        XCTAssertEqual(halfLife, 0.5, accuracy: 0.01)
        XCTAssertLessThan(old, 0.2)
    }

    func testPatternConfidenceDecaysOverTime() async throws {
        // Given: Old shots (30 days ago)
        let club = Club(id: "6iron", name: "6-iron", type: .iron)
        let oldDate = Date().addingTimeInterval(-30 * 24 * 60 * 60)

        // Create shots with old timestamps (need to use repository directly)
        for _ in 0..<5 {
            let shot = Shot(
                timestamp: oldDate,
                club: club,
                missDirection: .push,
                lie: .fairway
            )
            try await repository.recordShot(shot)
        }

        // When: Getting patterns
        let patterns = try await store.getPatterns(clubId: "6iron")

        // Then: Confidence is reduced due to decay
        guard let pattern = patterns.first else {
            XCTFail("Expected pattern to be found")
            return
        }

        // With 30 days and 14-day half-life, decay factor ≈ 0.25
        // Base confidence from 5 shots ≈ 0.5, so final ≈ 0.125
        XCTAssertLessThan(pattern.confidence, 0.2)
    }

    // MARK: - Filtering Tests

    func testGetShotsByClub() async throws {
        // Given: Shots from different clubs
        let club7 = Club(id: "7iron", name: "7-iron", type: .iron)
        let club8 = Club(id: "8iron", name: "8-iron", type: .iron)

        try await store.recordMiss(club: club7, direction: .slice, lie: .fairway)
        try await store.recordMiss(club: club8, direction: .hook, lie: .fairway)
        try await store.recordMiss(club: club7, direction: .push, lie: .rough)

        // When: Getting shots by club
        let club7Shots = try await store.getShotsByClub("7iron")

        // Then: Only club7 shots returned
        XCTAssertEqual(club7Shots.count, 2)
        XCTAssertTrue(club7Shots.allSatisfy { $0.club.id == "7iron" })
    }

    func testGetTopPatterns() async throws {
        // Given: Multiple patterns with different frequencies
        let club = Club(id: "pw", name: "Pitching Wedge", type: .wedge)

        for _ in 0..<5 {
            try await store.recordMiss(club: club, direction: .thin, lie: .green)
        }

        for _ in 0..<3 {
            try await store.recordMiss(club: club, direction: .fat, lie: .green)
        }

        // When: Getting top patterns
        let topPatterns = try await store.getTopPatterns(limit: 2)

        // Then: Patterns sorted by confidence
        XCTAssertEqual(topPatterns.count, 2)
        XCTAssertGreaterThanOrEqual(topPatterns[0].confidence, topPatterns[1].confidence)
    }

    // MARK: - Statistics Tests

    func testGetStatistics() async throws {
        // Given: A mix of shots
        let club = Club(id: "driver", name: "Driver", type: .driver)

        for _ in 0..<5 {
            try await store.recordMiss(club: club, direction: .slice, lie: .tee)
        }

        for _ in 0..<2 {
            try await store.recordSuccess(club: club, lie: .tee)
        }

        // When: Getting statistics
        let stats = try await store.getStatistics()

        // Then: Statistics reflect shot history
        XCTAssertEqual(stats.totalShots, 7)
        XCTAssertEqual(stats.totalMisses, 5)
        XCTAssertEqual(stats.mostCommonDirection, .slice)
        XCTAssertGreaterThan(stats.averageConfidence, 0)
        XCTAssertGreaterThan(stats.patternsFound, 0)
    }

    // MARK: - Memory Management Tests

    func testClearHistory() async throws {
        // Given: Some recorded shots
        let club = Club(id: "7iron", name: "7-iron", type: .iron)

        for _ in 0..<5 {
            try await store.recordMiss(club: club, direction: .slice, lie: .fairway)
        }

        XCTAssertGreaterThan(try await store.getRecentShots().count, 0)

        // When: Clearing history
        try await store.clearHistory()

        // Then: All shots are deleted
        let shots = try await store.getRecentShots()
        XCTAssertEqual(shots.count, 0)
    }

    func testRetentionPolicyEnforcement() async throws {
        // Given: Very old shots (100 days ago, beyond 90-day retention)
        let club = Club(id: "driver", name: "Driver", type: .driver)
        let oldDate = Date().addingTimeInterval(-100 * 24 * 60 * 60)

        let oldShot = Shot(
            timestamp: oldDate,
            club: club,
            missDirection: .slice,
            lie: .tee
        )
        try await repository.recordShot(oldShot)

        // Recent shot
        try await store.recordMiss(club: club, direction: .hook, lie: .tee)

        // When: Enforcing retention policy
        try await store.enforceRetentionPolicy()

        // Then: Old shots are deleted, recent shots remain
        let shots = try await store.getRecentShots(count: 100)
        XCTAssertEqual(shots.count, 1)
        XCTAssertEqual(shots[0].missDirection, .hook)
    }

    // MARK: - Convenience Tests

    func testHasSignificantPattern() async throws {
        // Given: A strong pattern
        let club = Club(id: "7iron", name: "7-iron", type: .iron)

        for _ in 0..<5 {
            try await store.recordMiss(club: club, direction: .slice, lie: .fairway)
        }

        // When: Checking for significant pattern
        let hasSlice = try await store.hasSignificantPattern(clubId: "7iron", direction: .slice)
        let hasHook = try await store.hasSignificantPattern(clubId: "7iron", direction: .hook)

        // Then: Slice is significant, hook is not
        XCTAssertTrue(hasSlice)
        XCTAssertFalse(hasHook)
    }

    func testGetPatternSummary() async throws {
        // Given: A pattern
        let club = Club(id: "pw", name: "Pitching Wedge", type: .wedge)

        for _ in 0..<4 {
            try await store.recordMiss(club: club, direction: .thin, lie: .green)
        }

        // When: Getting summary
        let summary = try await store.getPatternSummary(clubId: "pw")

        // Then: Summary describes the pattern
        XCTAssertTrue(summary.contains("thin"))
        XCTAssertTrue(summary.contains("confidence"))
    }

    func testGetPatternSummaryNoPattern() async throws {
        // When: Getting summary for club with no history
        let summary = try await store.getPatternSummary(clubId: "nonexistent")

        // Then: Returns no pattern message
        XCTAssertTrue(summary.contains("No miss patterns"))
    }

    // MARK: - Edge Cases

    func testMultipleClubsPatterns() async throws {
        // Given: Different patterns for different clubs
        let driver = Club(id: "driver", name: "Driver", type: .driver)
        let iron7 = Club(id: "7iron", name: "7-iron", type: .iron)

        for _ in 0..<4 {
            try await store.recordMiss(club: driver, direction: .slice, lie: .tee)
        }

        for _ in 0..<4 {
            try await store.recordMiss(club: iron7, direction: .pull, lie: .fairway)
        }

        // When: Getting patterns for each club
        let driverPatterns = try await store.getPatterns(clubId: "driver")
        let ironPatterns = try await store.getPatterns(clubId: "7iron")

        // Then: Each club has distinct patterns
        XCTAssertTrue(driverPatterns.contains { $0.direction == .slice })
        XCTAssertTrue(ironPatterns.contains { $0.direction == .pull })
    }

    func testEmptyPatterns() async throws {
        // When: Getting patterns with no data
        let patterns = try await store.getPatterns()

        // Then: Empty array returned
        XCTAssertEqual(patterns.count, 0)
    }

    func testRecentShotsLimit() async throws {
        // Given: More shots than limit
        let club = Club(id: "7iron", name: "7-iron", type: .iron)

        for _ in 0..<10 {
            try await store.recordMiss(club: club, direction: .slice, lie: .fairway)
        }

        // When: Getting recent shots with limit
        let shots = try await store.getRecentShots(count: 5)

        // Then: Only 5 shots returned
        XCTAssertEqual(shots.count, 5)
    }
}
