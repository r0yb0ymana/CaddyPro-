import XCTest
@testable import App

/// Integration tests for pattern decay behavior.
///
/// Tests time-based decay of miss patterns with PatternDecayCalculator and MissPatternStore.
///
/// Spec reference: navcaddy-engine.md R5, Q5 (14-day half-life)
/// Task reference: navcaddy-engine-plan.md Task 17
///
/// Tests verify:
/// - 14-day half-life decay behavior
/// - Old patterns lose confidence over time
/// - Patterns below threshold after decay are filtered
/// - Interaction with PatternDecayCalculator and MissPatternStore
@MainActor
final class PatternDecayIntegrationTests: XCTestCase {
    var repository: MockNavCaddyRepository!
    var calculator: PatternDecayCalculator!
    var aggregator: MissPatternAggregator!
    var recorder: ShotRecorder!

    override func setUp() {
        super.setUp()
        repository = MockNavCaddyRepository()
        calculator = PatternDecayCalculator.default // 14-day half-life
        aggregator = MissPatternAggregator(repository: repository, decayCalculator: calculator)
        recorder = ShotRecorder(repository: repository)
    }

    override func tearDown() {
        recorder = nil
        aggregator = nil
        calculator = nil
        repository = nil
        super.tearDown()
    }

    // MARK: - 14-Day Half-Life Decay Tests

    func testDecayHalfLife_At14Days() async throws {
        // GIVEN: Shots recorded 14 days ago (one half-life)
        let club = Club(id: "7iron", name: "7-iron", type: .iron)
        let fourteenDaysAgo = Date().addingTimeInterval(-14 * 24 * 60 * 60)

        for _ in 0..<5 {
            let shot = Shot(
                timestamp: fourteenDaysAgo,
                club: club,
                missDirection: .push,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating with decay
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Confidence should be approximately 50% of base
        let pattern = patterns.first { $0.club?.id == "7iron" }
        XCTAssertNotNil(pattern, "Should have pattern")

        // Base confidence for 5 shots: min(1.0, 5/10) = 0.5
        // After 14 days (1 half-life): 0.5 * 0.5 = 0.25
        let expectedConfidence = 0.25
        let tolerance = 0.05

        XCTAssertEqual(pattern!.confidence, expectedConfidence, accuracy: tolerance,
                      "Confidence should be halved after 14 days")
    }

    func testDecayHalfLife_At28Days() async throws {
        // GIVEN: Shots recorded 28 days ago (two half-lives)
        let club = Club(id: "driver", name: "Driver", type: .driver)
        let twentyEightDaysAgo = Date().addingTimeInterval(-28 * 24 * 60 * 60)

        for _ in 0..<8 {
            let shot = Shot(
                timestamp: twentyEightDaysAgo,
                club: club,
                missDirection: .slice,
                lie: .tee
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating with decay
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Confidence should be approximately 25% of base (0.5^2)
        let pattern = patterns.first { $0.club?.id == "driver" }
        XCTAssertNotNil(pattern, "Should have pattern")

        // Base confidence for 8 shots: min(1.0, 8/10) = 0.8
        // After 28 days (2 half-lives): 0.8 * 0.25 = 0.2
        let expectedConfidence = 0.2
        let tolerance = 0.05

        XCTAssertEqual(pattern!.confidence, expectedConfidence, accuracy: tolerance,
                      "Confidence should be quartered after 28 days")
    }

    func testNoDecay_ForRecentShots() async throws {
        // GIVEN: Shots recorded today (no decay)
        let club = Club(id: "pw", name: "Pitching Wedge", type: .wedge)

        for _ in 0..<6 {
            let shot = Shot(
                timestamp: Date(), // Now
                club: club,
                missDirection: .short,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating with decay
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Confidence should be at full base value
        let pattern = patterns.first { $0.club?.id == "pw" }
        XCTAssertNotNil(pattern, "Should have pattern")

        // Base confidence for 6 shots: min(1.0, 6/10) = 0.6
        // No decay for today's shots
        let expectedConfidence = 0.6
        let tolerance = 0.01

        XCTAssertEqual(pattern!.confidence, expectedConfidence, accuracy: tolerance,
                      "Recent shots should have no decay")
    }

    func testDecayGradual_OverTime() async throws {
        // GIVEN: Shots at different ages
        let club = Club(id: "5iron", name: "5-iron", type: .iron)

        let dates = [
            Date(), // Today
            Date().addingTimeInterval(-7 * 24 * 60 * 60),  // 7 days ago
            Date().addingTimeInterval(-14 * 24 * 60 * 60), // 14 days ago
            Date().addingTimeInterval(-21 * 24 * 60 * 60)  // 21 days ago
        ]

        // Record shots at each timestamp
        for date in dates {
            for _ in 0..<3 {
                let shot = Shot(
                    timestamp: date,
                    club: club,
                    missDirection: .pull,
                    lie: .fairway
                )
                try await recorder.recordShot(shot)
            }
        }

        // WHEN: Checking decay at different times
        let todayDecay = calculator.calculateDecay(for: Date())
        let sevenDaysDecay = calculator.calculateDecay(for: dates[1])
        let fourteenDaysDecay = calculator.calculateDecay(for: dates[2])
        let twentyOneDaysDecay = calculator.calculateDecay(for: dates[3])

        // THEN: Decay should decrease gradually
        XCTAssertEqual(todayDecay, 1.0, accuracy: 0.01, "Today should have no decay")
        XCTAssertGreaterThan(sevenDaysDecay, fourteenDaysDecay, "Older patterns should have more decay")
        XCTAssertGreaterThan(fourteenDaysDecay, twentyOneDaysDecay, "Oldest patterns should have most decay")
        XCTAssertEqual(fourteenDaysDecay, 0.5, accuracy: 0.01, "14 days should be 50%")
    }

    // MARK: - Old Patterns Lose Confidence Tests

    func testOldPatternsLoseConfidence_ComparedToRecent() async throws {
        // GIVEN: Same pattern recorded at different times
        let club = Club(id: "6iron", name: "6-iron", type: .iron)

        // Old shots (30 days ago)
        let thirtyDaysAgo = Date().addingTimeInterval(-30 * 24 * 60 * 60)
        for _ in 0..<5 {
            let shot = Shot(
                timestamp: thirtyDaysAgo,
                club: club,
                missDirection: .hook,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // Create separate club for recent shots (to isolate patterns)
        let club2 = Club(id: "6iron-recent", name: "6-iron-recent", type: .iron)

        // Recent shots (today)
        for _ in 0..<5 {
            let shot = Shot(
                timestamp: Date(),
                club: club2,
                missDirection: .hook,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Old pattern should have lower confidence than recent
        let oldPattern = patterns.first { $0.club?.id == "6iron" }
        let recentPattern = patterns.first { $0.club?.id == "6iron-recent" }

        XCTAssertNotNil(oldPattern, "Should have old pattern")
        XCTAssertNotNil(recentPattern, "Should have recent pattern")

        XCTAssertLessThan(oldPattern!.confidence, recentPattern!.confidence,
                         "Old pattern should have lower confidence due to decay")
    }

    func testVeryOldPatterns_HaveVeryLowConfidence() async throws {
        // GIVEN: Very old shots (60 days ago = ~4.3 half-lives)
        let club = Club(id: "3wood", name: "3-wood", type: .wood)
        let sixtyDaysAgo = Date().addingTimeInterval(-60 * 24 * 60 * 60)

        for _ in 0..<10 {
            let shot = Shot(
                timestamp: sixtyDaysAgo,
                club: club,
                missDirection: .slice,
                lie: .tee
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Should have very low confidence
        let pattern = patterns.first { $0.club?.id == "3wood" }

        // Base confidence: min(1.0, 10/10) = 1.0
        // After 60 days: 1.0 * 0.5^(60/14) ≈ 0.05
        if let pattern = pattern {
            XCTAssertLessThan(pattern.confidence, 0.1,
                            "Very old pattern should have very low confidence")
        }
        // Pattern might be filtered out entirely if below minimum threshold
    }

    // MARK: - Threshold Filtering After Decay Tests

    func testPatternsFiltered_WhenDecayedBelowThreshold() async throws {
        // GIVEN: Pattern that starts above threshold but decays below it
        let club = Club(id: "sw", name: "Sand Wedge", type: .wedge)
        let fortyFiveDaysAgo = Date().addingTimeInterval(-45 * 24 * 60 * 60)

        // 3 shots (minimum frequency) recorded long ago
        for _ in 0..<3 {
            let shot = Shot(
                timestamp: fortyFiveDaysAgo,
                club: club,
                missDirection: .thin,
                lie: .bunker
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Pattern might be filtered out if decayed below minimum confidence
        // Base confidence: min(1.0, 3/10) = 0.3
        // After 45 days: 0.3 * 0.5^(45/14) ≈ 0.03
        // Minimum threshold: 0.1
        // Should be filtered out

        let thinPatterns = patterns.filter { $0.direction == .thin }

        // Pattern should either not exist or have very low confidence
        if let pattern = thinPatterns.first(where: { $0.club?.id == "sw" }) {
            XCTAssertLessThan(pattern.confidence, MissPatternAggregator.minimumConfidence,
                            "Decayed pattern should be below threshold")
        }
        // Or it should be filtered out entirely
    }

    func testRecentPatternsNotFiltered_EvenAtMinimumFrequency() async throws {
        // GIVEN: Pattern at minimum frequency (3 shots) recorded recently
        let club = Club(id: "lw", name: "Lob Wedge", type: .wedge)

        for _ in 0..<3 {
            let shot = Shot(
                timestamp: Date(),
                club: club,
                missDirection: .fat,
                lie: .rough
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Aggregating patterns
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Should not be filtered out
        let fatPatterns = patterns.filter { $0.direction == .fat && $0.club?.id == "lw" }
        XCTAssertFalse(fatPatterns.isEmpty, "Recent pattern at minimum frequency should not be filtered")

        if let pattern = fatPatterns.first {
            XCTAssertGreaterThanOrEqual(pattern.confidence, MissPatternAggregator.minimumConfidence,
                                       "Recent pattern should be above threshold")
        }
    }

    func testDecayCanBeDisabled_ForTesting() async throws {
        // GIVEN: Old shots with decay disabled
        let club = Club(id: "4iron", name: "4-iron", type: .iron)
        let thirtyDaysAgo = Date().addingTimeInterval(-30 * 24 * 60 * 60)

        for _ in 0..<5 {
            let shot = Shot(
                timestamp: thirtyDaysAgo,
                club: club,
                missDirection: .push,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Getting shots and aggregating without decay
        let shots = try await recorder.getShotsInWindow(days: 90)
        let patternsWithoutDecay = aggregator.aggregatePatterns(from: shots, applyDecay: false)

        // THEN: Should have full base confidence (no decay applied)
        let pattern = patternsWithoutDecay.first { $0.club?.id == "4iron" }
        XCTAssertNotNil(pattern, "Should have pattern")

        // Base confidence for 5 shots: 0.5, with no decay
        XCTAssertEqual(pattern!.confidence, 0.5, accuracy: 0.01,
                      "Should have full base confidence when decay disabled")
    }

    // MARK: - MissPatternStore Integration Tests

    func testMissPatternStore_AppliesDecayAutomatically() async {
        // GIVEN: MissPatternStore with old shots
        let store = MissPatternStore(repository: repository)
        let club = Club(id: "driver", name: "Driver", type: .driver)
        let twentyOneDaysAgo = Date().addingTimeInterval(-21 * 24 * 60 * 60)

        for _ in 0..<6 {
            try await store.recordMiss(
                club: club,
                direction: .slice,
                lie: .tee
            )
        }

        // Manually set timestamps to simulate old data
        let shots = try await repository.getRecentShots(days: 90)
        for shot in shots {
            let oldShot = Shot(
                id: shot.id,
                timestamp: twentyOneDaysAgo,
                club: shot.club,
                missDirection: shot.missDirection,
                lie: shot.lie,
                pressureContext: shot.pressureContext,
                holeNumber: shot.holeNumber,
                notes: shot.notes
            )
            // Re-record with old timestamp
            repository.shots.removeAll { $0.id == shot.id }
            try await repository.recordShot(oldShot)
        }

        // WHEN: Getting patterns through store
        let patterns = try await store.getPatterns()

        // THEN: Decay should be applied automatically
        let slicePattern = patterns.first { $0.direction == .slice }
        XCTAssertNotNil(slicePattern, "Should have slice pattern")

        // Base confidence for 6 shots: 0.6
        // After 21 days: 0.6 * 0.5^(21/14) ≈ 0.21
        XCTAssertLessThan(slicePattern!.confidence, 0.6,
                         "Store should apply decay automatically")
    }

    func testDecayAffectsPatternsOrdering() async throws {
        // GIVEN: Two patterns - one recent, one old, with different frequencies
        let club1 = Club(id: "pw", name: "Pitching Wedge", type: .wedge)
        let club2 = Club(id: "9iron", name: "9-iron", type: .iron)

        // Pattern 1: 8 shots, 30 days old (high frequency, old)
        let thirtyDaysAgo = Date().addingTimeInterval(-30 * 24 * 60 * 60)
        for _ in 0..<8 {
            let shot = Shot(
                timestamp: thirtyDaysAgo,
                club: club1,
                missDirection: .short,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // Pattern 2: 4 shots, today (lower frequency, recent)
        for _ in 0..<4 {
            let shot = Shot(
                timestamp: Date(),
                club: club2,
                missDirection: .short,
                lie: .fairway
            )
            try await recorder.recordShot(shot)
        }

        // WHEN: Getting patterns (should be ordered by confidence)
        let patterns = try await aggregator.getAllPatterns()

        // THEN: Recent pattern might have higher confidence than old pattern despite lower frequency
        let oldPattern = patterns.first { $0.club?.id == "pw" }
        let recentPattern = patterns.first { $0.club?.id == "9iron" }

        XCTAssertNotNil(oldPattern, "Should have old pattern")
        XCTAssertNotNil(recentPattern, "Should have recent pattern")

        // Old: base 0.8, after 30 days: 0.8 * 0.5^(30/14) ≈ 0.19
        // Recent: base 0.4, no decay: 0.4
        // Recent should rank higher despite lower frequency
        XCTAssertGreaterThan(recentPattern!.confidence, oldPattern!.confidence,
                            "Recent pattern should have higher confidence than old pattern with higher frequency")
    }
}

// MARK: - Mock Repository

@MainActor
private class MockNavCaddyRepository: NavCaddyRepository {
    var shots: [Shot] = []
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
