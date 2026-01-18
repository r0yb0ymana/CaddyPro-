import XCTest
@testable import App

/// Integration tests for MissPatternAggregator with ShotRecorder.
///
/// Tests real component interactions for pattern aggregation logic.
///
/// Spec reference: navcaddy-engine.md R5, A4
/// Task reference: navcaddy-engine-plan.md Task 17
///
/// Tests verify:
/// - Pattern threshold behavior (minimum shots required)
/// - Frequency accumulation across multiple shots
/// - Pattern filtering by confidence
/// - Interaction with ShotRecorder
@MainActor
final class MissPatternAggregatorIntegrationTests: XCTestCase {
    var repository: MockNavCaddyRepository!
    var recorder: ShotRecorder!
    var aggregator: MissPatternAggregator!

    override func setUp() {
        super.setUp()
        repository = MockNavCaddyRepository()
        recorder = ShotRecorder(repository: repository)
        aggregator = MissPatternAggregator(repository: repository)
    }

    override func tearDown() {
        aggregator = nil
        recorder = nil
        repository = nil
        super.tearDown()
    }

    // MARK: - Threshold Behavior Tests

    func testPatternsNotAggregated_WhenBelowMinimumThreshold() async throws {
        // GIVEN: Only 2 shots with same miss direction (below minimum of 3)
        let club = Club(id: "7iron", name: "7-iron", type: .iron)

        let shot1 = Shot(
            timestamp: Date(),
            club: club,
            missDirection: .push,
            lie: .fairway
        )

        let shot2 = Shot(
            timestamp: Date(),
            club: club,
            missDirection: .push,
            lie: .fairway
        )

        try await recorder.recordShot(shot1)
        try await recorder.recordShot(shot2)

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: No patterns should be created (below threshold)
        XCTAssertEqual(patterns.count, 0, "Should not create pattern with only 2 shots")
    }

    func testPatternAggregated_WhenMeetsMinimumThreshold() async throws {
        // GIVEN: Exactly 3 shots with same miss direction (meets minimum)
        let club = Club(id: "7iron", name: "7-iron", type: .iron)

        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .push,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Should create pattern (meets threshold)
        XCTAssertEqual(patterns.count, 2, "Should create overall pattern and club-specific pattern")

        let pushPatterns = patterns.filter { $0.direction == .push }
        XCTAssertEqual(pushPatterns.count, 2, "Should have push patterns")

        // Verify pattern has correct frequency
        let clubPattern = pushPatterns.first { $0.club?.id == "7iron" }
        XCTAssertNotNil(clubPattern, "Should have club-specific pattern")
        XCTAssertEqual(clubPattern?.frequency, 3)
    }

    func testMultiplePatterns_WithDifferentDirections() async throws {
        // GIVEN: 3 push shots and 3 slice shots
        let club = Club(id: "driver", name: "Driver", type: .driver)

        // Record push shots
        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .push,
                lie: .tee
            )
            try await recorder.recordShot(shot)
        }

        // Record slice shots
        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .slice,
                lie: .tee
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Should create patterns for both directions
        let pushPatterns = patterns.filter { $0.direction == .push }
        let slicePatterns = patterns.filter { $0.direction == .slice }

        XCTAssertFalse(pushPatterns.isEmpty, "Should have push patterns")
        XCTAssertFalse(slicePatterns.isEmpty, "Should have slice patterns")

        // Verify each has correct frequency
        XCTAssertEqual(pushPatterns.first?.frequency, 3)
        XCTAssertEqual(slicePatterns.first?.frequency, 3)
    }

    // MARK: - Frequency Accumulation Tests

    func testFrequencyAccumulatesCorrectly_WithMultipleShots() async throws {
        // GIVEN: 7 shots with same miss direction
        let club = Club(id: "5iron", name: "5-iron", type: .iron)

        for _ in 0..<7 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .pull,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Should accumulate frequency correctly
        let pullPatterns = patterns.filter { $0.direction == .pull }
        XCTAssertFalse(pullPatterns.isEmpty, "Should have pull patterns")

        let overallPattern = pullPatterns.first { $0.club == nil }
        XCTAssertNotNil(overallPattern, "Should have overall pattern")
        XCTAssertEqual(overallPattern?.frequency, 7, "Frequency should accumulate to 7")
    }

    func testFrequencyAffectsConfidence() async throws {
        // GIVEN: Two patterns with different frequencies
        let club1 = Club(id: "pw", name: "Pitching Wedge", type: .wedge)
        let club2 = Club(id: "sw", name: "Sand Wedge", type: .wedge)

        // Pattern 1: 3 shots (minimum threshold)
        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date(),
                club: club1,
                missDirection: .short,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // Pattern 2: 8 shots (higher frequency)
        for _ in 0..<8 {
            let shot = Shot(
                timestamp: Date(),
                club: club2,
                missDirection: .short,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Higher frequency should yield higher confidence
        let pattern1 = patterns.first { $0.club?.id == "pw" }
        let pattern2 = patterns.first { $0.club?.id == "sw" }

        XCTAssertNotNil(pattern1)
        XCTAssertNotNil(pattern2)

        XCTAssertLessThan(pattern1!.confidence, pattern2!.confidence,
                         "Higher frequency should yield higher confidence")
    }

    // MARK: - Confidence Filtering Tests

    func testPatternsFilteredByMinimumConfidence() async throws {
        // GIVEN: Aggregator with minimum confidence threshold
        let minimumConfidence = MissPatternAggregator.minimumConfidence

        // Create patterns by recording shots
        let club = Club(id: "3wood", name: "3-wood", type: .wood)

        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .slice,
                lie: .tee
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Getting patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: All returned patterns should meet minimum confidence
        for pattern in patterns {
            XCTAssertGreaterThanOrEqual(pattern.confidence, minimumConfidence,
                                       "Pattern confidence should meet minimum threshold")
        }
    }

    func testPatternsOrderedByConfidence() async throws {
        // GIVEN: Multiple patterns with different frequencies
        let club = Club(id: "9iron", name: "9-iron", type: .iron)

        // Pattern 1: 3 shots
        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date().addingTimeInterval(-86400), // 1 day ago
                club: club,
                missDirection: .thin,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // Pattern 2: 6 shots (higher frequency)
        for _ in 0..<6 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .fat,
                lie: .rough
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Getting patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Patterns should be ordered by confidence (descending)
        for i in 0..<(patterns.count - 1) {
            XCTAssertGreaterThanOrEqual(patterns[i].confidence, patterns[i + 1].confidence,
                                       "Patterns should be sorted by confidence descending")
        }
    }

    // MARK: - ShotRecorder Interaction Tests

    func testAggregatorUsesRecordedShots() async throws {
        // GIVEN: Shots recorded through ShotRecorder
        let club = Club(id: "6iron", name: "6-iron", type: .iron)

        // Record via ShotRecorder
        for _ in 0..<4 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .hook,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregator reads from same repository
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Should find patterns from recorded shots
        let hookPatterns = patterns.filter { $0.direction == .hook }
        XCTAssertFalse(hookPatterns.isEmpty, "Should find hook patterns from recorded shots")
        XCTAssertEqual(hookPatterns.first?.frequency, 4)
    }

    func testAggregatorIgnoresStraightShots() async throws {
        // GIVEN: Mix of straight and miss shots
        let club = Club(id: "4iron", name: "4-iron", type: .iron)

        // Record 2 straight shots
        for _ in 0..<2 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .straight,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // Record 3 push shots
        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .push,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Should only create patterns for misses (not straight shots)
        XCTAssertTrue(patterns.allSatisfy { $0.direction != .straight },
                     "Should not create patterns for straight shots")

        let pushPatterns = patterns.filter { $0.direction == .push }
        XCTAssertEqual(pushPatterns.first?.frequency, 3, "Should only count miss shots")
    }

    func testPressureContextFiltering() async throws {
        // GIVEN: Mix of pressure and non-pressure shots
        let club = Club(id: "driver", name: "Driver", type: .driver)

        // Record 4 non-pressure shots
        for _ in 0..<4 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .slice,
                lie: .tee,
                pressureContext: PressureContext()
            )
            try await recorder.recordShot(shot)
        }

        // Record 3 pressure shots
        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .slice,
                lie: .tee,
                pressureContext: PressureContext(isUnderPressure: true, isUserTagged: true)
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Getting patterns for pressure context
        let pressureContext = PressureContext(isUnderPressure: true, isUserTagged: true)
        let pressurePatterns = try await aggregator.getPatternsForContext(pressureContext)

        // THEN: Should have separate pressure pattern
        XCTAssertFalse(pressurePatterns.isEmpty, "Should have pressure patterns")

        // Overall patterns should include all 7 shots
        let allPatterns = try await aggregator.getAllPatterns()
        let overallPattern = allPatterns.first { $0.direction == .slice && $0.club == nil && $0.pressureContext == nil }
        XCTAssertEqual(overallPattern?.frequency, 7, "Overall pattern should include all shots")
    }

    func testClubSpecificAggregation() async throws {
        // GIVEN: Same miss direction with different clubs
        let driver = Club(id: "driver", name: "Driver", type: .driver)
        let threeWood = Club(id: "3wood", name: "3-wood", type: .wood)

        // Record 4 driver slices
        for _ in 0..<4 {
            let shot = Shot(
                timestamp: Date(),
                club: driver,
                missDirection: .slice,
                lie: .tee
            )
            try await recorder.recordShot(shot)
        }

        // Record 3 3-wood slices
        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date(),
                club: threeWood,
                missDirection: .slice,
                lie: .tee
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Getting club-specific patterns
        let driverPatterns = try await aggregator.getPatternsForClub("driver")
        let threeWoodPatterns = try await aggregator.getPatternsForClub("3wood")

        // THEN: Should have separate patterns per club
        XCTAssertFalse(driverPatterns.isEmpty, "Should have driver patterns")
        XCTAssertFalse(threeWoodPatterns.isEmpty, "Should have 3-wood patterns")

        let driverPattern = driverPatterns.first { $0.club?.id == "driver" }
        let threeWoodPattern = threeWoodPatterns.first { $0.club?.id == "3wood" }

        XCTAssertEqual(driverPattern?.frequency, 4)
        XCTAssertEqual(threeWoodPattern?.frequency, 3)
    }
}

// MARK: - Mock Repository

@MainActor
private class MockNavCaddyRepository: NavCaddyRepository {
    private var shots: [Shot] = []
    private var patterns: [MissPattern] = []
    private var session = SessionContext.empty

    func recordShot(_ shot: Shot) async throws {
        shots.append(shot)
    }

    func getRecentShots(days: Int) async throws -> [Shot] {
        let cutoffDate = Date().addingTimeInterval(-Double(days) * 24 * 60 * 60)
        return shots.filter { $0.timestamp >= cutoffDate }
            .sorted { $0.timestamp > $1.timestamp }
    }

    func getShotsWithPressure() async throws -> [Shot] {
        return shots.filter { $0.pressureContext.hasPressure }
    }

    func getMissPatterns() async throws -> [MissPattern] {
        return patterns
    }

    func updatePattern(_ pattern: MissPattern) async throws {
        if let index = patterns.firstIndex(where: { $0.id == pattern.id }) {
            patterns[index] = pattern
        } else {
            patterns.append(pattern)
        }
    }

    func getPatternsByDirection(_ direction: MissDirection) async throws -> [MissPattern] {
        return patterns.filter { $0.direction == direction }
    }

    func getPatternsByClub(_ clubId: String) async throws -> [MissPattern] {
        return patterns.filter { $0.club?.id == clubId }
    }

    func getSession() async throws -> SessionContext {
        return session
    }

    func saveSession(_ context: SessionContext) async throws {
        session = context
    }

    func addConversationTurn(_ turn: ConversationTurn) async throws {
        // Not needed for these tests
    }

    func clearMemory() async throws {
        shots.removeAll()
        patterns.removeAll()
        session = SessionContext.empty
    }

    func enforceRetentionPolicy() async throws {
        let cutoffDate = Date().addingTimeInterval(-90 * 24 * 60 * 60)
        shots = shots.filter { $0.timestamp >= cutoffDate }
    }
}
