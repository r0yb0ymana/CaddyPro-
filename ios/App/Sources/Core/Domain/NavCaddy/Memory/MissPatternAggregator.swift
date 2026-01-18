import Foundation

/// Aggregates miss patterns from shot history with rolling windows and decay.
///
/// Spec R5: Aggregates patterns by club, lie, and pressure context with time-based decay.
/// Q5: Rolling window with 14-day decay half-life.
@MainActor
final class MissPatternAggregator {
    private let repository: NavCaddyRepository
    private let decayCalculator: PatternDecayCalculator

    /// Default rolling window in days (spec: last 30 days)
    static let defaultWindowDays = 30

    /// Minimum shots required to form a pattern
    static let minimumShotsForPattern = 3

    /// Minimum confidence threshold for pattern inclusion
    static let minimumConfidence = 0.1

    init(
        repository: NavCaddyRepository,
        decayCalculator: PatternDecayCalculator = .default
    ) {
        self.repository = repository
        self.decayCalculator = decayCalculator
    }

    // MARK: - Pattern Aggregation

    /// Aggregates miss patterns from a collection of shots.
    ///
    /// - Parameters:
    ///   - shots: Array of shots to analyze
    ///   - applyDecay: Whether to apply time-based decay to confidence (default: true)
    /// - Returns: Array of aggregated miss patterns sorted by confidence
    func aggregatePatterns(from shots: [Shot], applyDecay: Bool = true) -> [MissPattern] {
        // Filter to only shots with miss directions (excluding straight)
        let missedShots = shots.filter { shot in
            guard let direction = shot.missDirection else { return false }
            return direction != .straight
        }

        // Group by direction
        let groupedByDirection = Dictionary(grouping: missedShots) { $0.missDirection! }

        var patterns: [MissPattern] = []

        for (direction, directionShots) in groupedByDirection {
            // Overall pattern for this direction
            if let pattern = createPattern(
                direction: direction,
                shots: directionShots,
                club: nil,
                pressureContext: nil,
                applyDecay: applyDecay
            ) {
                patterns.append(pattern)
            }

            // Patterns by club
            let byClub = Dictionary(grouping: directionShots) { $0.club.id }
            for (clubId, clubShots) in byClub {
                if let pattern = createPattern(
                    direction: direction,
                    shots: clubShots,
                    club: clubShots.first?.club,
                    pressureContext: nil,
                    applyDecay: applyDecay
                ) {
                    patterns.append(pattern)
                }
            }

            // Patterns under pressure
            let pressureShots = directionShots.filter { $0.pressureContext.hasPressure }
            if !pressureShots.isEmpty {
                if let pattern = createPattern(
                    direction: direction,
                    shots: pressureShots,
                    club: nil,
                    pressureContext: pressureShots.first?.pressureContext,
                    applyDecay: applyDecay
                ) {
                    patterns.append(pattern)
                }
            }
        }

        // Sort by confidence descending
        return patterns
            .filter { $0.confidence >= Self.minimumConfidence }
            .sorted { $0.confidence > $1.confidence }
    }

    /// Gets patterns for a specific club with decay applied.
    ///
    /// - Parameter clubId: The club ID to filter by
    /// - Returns: Array of miss patterns for the club
    func getPatternsForClub(_ clubId: String) async throws -> [MissPattern] {
        // Get recent shots for the club
        let shots = try await repository.getRecentShots(days: Self.defaultWindowDays)
        let clubShots = shots.filter { $0.club.id == clubId }

        return aggregatePatterns(from: clubShots, applyDecay: true)
    }

    /// Gets patterns for a specific pressure context.
    ///
    /// - Parameter context: The pressure context to filter by
    /// - Returns: Array of miss patterns under pressure
    func getPatternsForContext(_ context: PressureContext) async throws -> [MissPattern] {
        guard context.hasPressure else {
            return []
        }

        let pressureShots = try await repository.getShotsWithPressure()
        let recentShots = pressureShots.filter { shot in
            let daysAgo = Date().timeIntervalSince(shot.timestamp) / (24 * 60 * 60)
            return daysAgo <= Double(Self.defaultWindowDays)
        }

        return aggregatePatterns(from: recentShots, applyDecay: true)
    }

    /// Gets patterns filtered by miss direction.
    ///
    /// - Parameter direction: The miss direction to filter by
    /// - Returns: Array of patterns with the specified direction
    func getPatternsByDirection(_ direction: MissDirection) async throws -> [MissPattern] {
        let shots = try await repository.getRecentShots(days: Self.defaultWindowDays)
        let directionShots = shots.filter { $0.missDirection == direction }

        return aggregatePatterns(from: directionShots, applyDecay: true)
    }

    /// Gets all recent patterns across all contexts.
    ///
    /// - Parameter windowDays: Rolling window in days (default: 30)
    /// - Returns: Array of all miss patterns
    func getAllPatterns(windowDays: Int = defaultWindowDays) async throws -> [MissPattern] {
        let shots = try await repository.getRecentShots(days: windowDays)
        return aggregatePatterns(from: shots, applyDecay: true)
    }

    // MARK: - Private Helpers

    /// Creates a miss pattern from a group of shots.
    private func createPattern(
        direction: MissDirection,
        shots: [Shot],
        club: Club?,
        pressureContext: PressureContext?,
        applyDecay: Bool
    ) -> MissPattern? {
        guard shots.count >= Self.minimumShotsForPattern else {
            return nil
        }

        let frequency = shots.count
        let lastOccurrence = shots.map(\.timestamp).max() ?? Date()

        // Calculate base confidence from frequency
        // More shots = higher confidence, with diminishing returns
        let baseConfidence = min(1.0, Double(frequency) / 10.0)

        // Apply decay if requested
        let confidence = applyDecay
            ? baseConfidence * decayCalculator.calculateDecay(for: lastOccurrence)
            : baseConfidence

        return MissPattern(
            direction: direction,
            club: club,
            frequency: frequency,
            confidence: confidence,
            pressureContext: pressureContext,
            lastOccurrence: lastOccurrence
        )
    }
}

// MARK: - Pattern Statistics

extension MissPatternAggregator {
    /// Statistical summary of miss patterns.
    struct PatternStatistics {
        let totalShots: Int
        let totalMisses: Int
        let mostCommonDirection: MissDirection?
        let averageConfidence: Double
        let patternsFound: Int
    }

    /// Calculates statistics for a set of shots.
    ///
    /// - Parameter shots: Array of shots to analyze
    /// - Returns: Statistical summary
    func calculateStatistics(from shots: [Shot]) -> PatternStatistics {
        let patterns = aggregatePatterns(from: shots, applyDecay: true)

        let missedShots = shots.filter { shot in
            guard let direction = shot.missDirection else { return false }
            return direction != .straight
        }

        let directionFrequencies = Dictionary(grouping: missedShots) { $0.missDirection! }
            .mapValues { $0.count }

        let mostCommon = directionFrequencies.max { $0.value < $1.value }?.key

        let avgConfidence = patterns.isEmpty
            ? 0.0
            : patterns.map(\.confidence).reduce(0, +) / Double(patterns.count)

        return PatternStatistics(
            totalShots: shots.count,
            totalMisses: missedShots.count,
            mostCommonDirection: mostCommon,
            averageConfidence: avgConfidence,
            patternsFound: patterns.count
        )
    }
}
