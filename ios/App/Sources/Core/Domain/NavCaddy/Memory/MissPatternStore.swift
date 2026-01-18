import Foundation

/// Main interface for miss pattern storage and retrieval.
///
/// Spec R5: Miss Pattern Store tracks shot history and aggregates patterns
/// with time-based decay, filtering by club and pressure context.
///
/// This actor provides thread-safe access to miss pattern operations.
actor MissPatternStore {
    private let recorder: ShotRecorder
    private let aggregator: MissPatternAggregator
    private let repository: NavCaddyRepository

    init(
        recorder: ShotRecorder,
        aggregator: MissPatternAggregator,
        repository: NavCaddyRepository
    ) {
        self.recorder = recorder
        self.aggregator = aggregator
        self.repository = repository
    }

    /// Convenience initializer with repository dependency.
    init(repository: NavCaddyRepository) {
        let recorder = ShotRecorder(repository: repository)
        let aggregator = MissPatternAggregator(repository: repository)

        self.init(
            recorder: recorder,
            aggregator: aggregator,
            repository: repository
        )
    }

    // MARK: - Recording Misses

    /// Records a miss with full shot context.
    ///
    /// Spec Q3: Manual entry only for MVP.
    ///
    /// - Parameters:
    ///   - clubId: ID of the club used
    ///   - clubName: Name of the club (e.g., "7-iron")
    ///   - clubType: Type of club
    ///   - direction: Miss direction (push, pull, slice, hook, fat, thin)
    ///   - lie: Lie condition (tee, fairway, rough, etc.)
    ///   - pressure: Pressure context (user tagged or inferred)
    ///   - holeNumber: Optional hole number
    ///   - notes: Optional notes about the shot
    /// - Throws: Database error if recording fails
    func recordMiss(
        clubId: String,
        clubName: String,
        clubType: ClubType,
        direction: MissDirection,
        lie: Lie,
        pressure: PressureContext = PressureContext(),
        holeNumber: Int? = nil,
        notes: String? = nil
    ) async throws {
        let club = Club(
            id: clubId,
            name: clubName,
            type: clubType
        )

        let shot = Shot(
            timestamp: Date(),
            club: club,
            missDirection: direction,
            lie: lie,
            pressureContext: pressure,
            holeNumber: holeNumber,
            notes: notes
        )

        try await recorder.recordShot(shot)
    }

    /// Records a miss using an existing Club instance.
    ///
    /// - Parameters:
    ///   - club: The club used for the shot
    ///   - direction: Miss direction
    ///   - lie: Lie condition
    ///   - pressure: Pressure context
    ///   - holeNumber: Optional hole number
    ///   - notes: Optional notes
    /// - Throws: Database error if recording fails
    func recordMiss(
        club: Club,
        direction: MissDirection,
        lie: Lie,
        pressure: PressureContext = PressureContext(),
        holeNumber: Int? = nil,
        notes: String? = nil
    ) async throws {
        let shot = Shot(
            timestamp: Date(),
            club: club,
            missDirection: direction,
            lie: lie,
            pressureContext: pressure,
            holeNumber: holeNumber,
            notes: notes
        )

        try await recorder.recordShot(shot)
    }

    /// Records a successful shot (straight, no miss).
    ///
    /// - Parameters:
    ///   - club: The club used
    ///   - lie: Lie condition
    ///   - pressure: Pressure context
    ///   - holeNumber: Optional hole number
    ///   - notes: Optional notes
    /// - Throws: Database error if recording fails
    func recordSuccess(
        club: Club,
        lie: Lie,
        pressure: PressureContext = PressureContext(),
        holeNumber: Int? = nil,
        notes: String? = nil
    ) async throws {
        let shot = Shot(
            timestamp: Date(),
            club: club,
            missDirection: .straight,
            lie: lie,
            pressureContext: pressure,
            holeNumber: holeNumber,
            notes: notes
        )

        try await recorder.recordShot(shot)
    }

    // MARK: - Pattern Retrieval

    /// Gets miss patterns with optional filtering.
    ///
    /// - Parameters:
    ///   - clubId: Optional club ID to filter by
    ///   - pressure: Optional pressure context to filter by
    /// - Returns: Array of miss patterns sorted by confidence
    /// - Throws: Database error if fetch fails
    func getPatterns(
        clubId: String? = nil,
        pressure: PressureContext? = nil
    ) async throws -> [MissPattern] {
        if let clubId = clubId {
            return try await aggregator.getPatternsForClub(clubId)
        } else if let pressure = pressure, pressure.hasPressure {
            return try await aggregator.getPatternsForContext(pressure)
        } else {
            return try await aggregator.getAllPatterns()
        }
    }

    /// Gets patterns for a specific miss direction.
    ///
    /// - Parameter direction: The miss direction to filter by
    /// - Returns: Array of matching patterns
    /// - Throws: Database error if fetch fails
    func getPatternsByDirection(_ direction: MissDirection) async throws -> [MissPattern] {
        return try await aggregator.getPatternsByDirection(direction)
    }

    /// Gets the most significant patterns (highest confidence).
    ///
    /// - Parameter limit: Maximum number of patterns to return (default: 5)
    /// - Returns: Top patterns by confidence
    /// - Throws: Database error if fetch fails
    func getTopPatterns(limit: Int = 5) async throws -> [MissPattern] {
        let allPatterns = try await aggregator.getAllPatterns()
        return Array(allPatterns.prefix(limit))
    }

    /// Gets statistical summary of miss patterns.
    ///
    /// - Parameter windowDays: Rolling window in days (default: 30)
    /// - Returns: Pattern statistics
    /// - Throws: Database error if fetch fails
    func getStatistics(windowDays: Int = 30) async throws -> MissPatternAggregator.PatternStatistics {
        let shots = try await recorder.getShotsInWindow(days: windowDays)
        return aggregator.calculateStatistics(from: shots)
    }

    // MARK: - Shot History

    /// Gets recent shot history.
    ///
    /// - Parameter count: Maximum number of shots to return (default: 50)
    /// - Returns: Array of recent shots
    /// - Throws: Database error if fetch fails
    func getRecentShots(count: Int = 50) async throws -> [Shot] {
        return try await recorder.getRecentShots(count: count)
    }

    /// Gets shots for a specific club.
    ///
    /// - Parameters:
    ///   - clubId: The club ID
    ///   - limit: Maximum number of shots (default: 50)
    /// - Returns: Array of shots for the club
    /// - Throws: Database error if fetch fails
    func getShotsByClub(_ clubId: String, limit: Int = 50) async throws -> [Shot] {
        return try await recorder.getShotsByClub(clubId, limit: limit)
    }

    /// Gets shots that occurred under pressure.
    ///
    /// - Parameter limit: Maximum number of shots (default: 50)
    /// - Returns: Array of pressure shots
    /// - Throws: Database error if fetch fails
    func getPressureShots(limit: Int = 50) async throws -> [Shot] {
        return try await recorder.getShotsWithPressure(limit: limit)
    }

    // MARK: - Memory Management

    /// Clears all shot history and patterns.
    ///
    /// Spec C4: User-facing control to wipe memory.
    ///
    /// - Throws: Database error if clear fails
    func clearHistory() async throws {
        try await repository.clearMemory()
    }

    /// Enforces the retention policy by deleting old data.
    ///
    /// Spec Q5: 90-day retention window.
    ///
    /// - Throws: Database error if cleanup fails
    func enforceRetentionPolicy() async throws {
        try await repository.enforceRetentionPolicy()
    }
}

// MARK: - Convenience Extensions

extension MissPatternStore {
    /// Checks if a pattern exists for a specific club and direction.
    ///
    /// - Parameters:
    ///   - clubId: The club ID
    ///   - direction: The miss direction
    /// - Returns: True if pattern exists with confidence > 0.3
    /// - Throws: Database error if fetch fails
    func hasSignificantPattern(clubId: String, direction: MissDirection) async throws -> Bool {
        let patterns = try await getPatterns(clubId: clubId)
        return patterns.contains { $0.direction == direction && $0.confidence > 0.3 }
    }

    /// Gets a human-readable summary of miss patterns for a club.
    ///
    /// - Parameter clubId: The club ID
    ///   - Returns: Textual summary of patterns
    /// - Throws: Database error if fetch fails
    func getPatternSummary(clubId: String) async throws -> String {
        let patterns = try await getPatterns(clubId: clubId)

        guard !patterns.isEmpty else {
            return "No miss patterns found for this club."
        }

        let topPattern = patterns[0]
        let confidence = Int(topPattern.confidence * 100)

        return "Most common miss: \(topPattern.direction.rawValue) (\(confidence)% confidence)"
    }
}
