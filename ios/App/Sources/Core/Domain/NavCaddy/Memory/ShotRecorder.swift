import Foundation

/// Records shots with miss information for pattern analysis.
///
/// Spec R5: Track shots with miss direction, lie, pressure context, and timestamps.
/// Spec Q3: Manual entry only for MVP (no shot tracker integration).
@MainActor
final class ShotRecorder {
    private let repository: NavCaddyRepository

    init(repository: NavCaddyRepository) {
        self.repository = repository
    }

    /// Records a new shot to the database.
    ///
    /// - Parameter shot: The shot to record
    /// - Throws: Database error if recording fails
    func recordShot(_ shot: Shot) async throws {
        try await repository.recordShot(shot)
    }

    /// Gets the most recent shots.
    ///
    /// - Parameter count: Maximum number of shots to return (default: 50)
    /// - Returns: Array of shots sorted by timestamp descending
    /// - Throws: Database error if fetch fails
    func getRecentShots(count: Int = 50) async throws -> [Shot] {
        // Fetch with large enough window to get count shots
        // Use 90 days to respect retention policy
        let allShots = try await repository.getRecentShots(days: 90)
        return Array(allShots.prefix(count))
    }

    /// Gets shots for a specific club.
    ///
    /// - Parameters:
    ///   - clubId: The club ID to filter by
    ///   - limit: Maximum number of shots to return (default: 50)
    /// - Returns: Array of shots for the specified club
    /// - Throws: Database error if fetch fails
    func getShotsByClub(_ clubId: String, limit: Int = 50) async throws -> [Shot] {
        let allShots = try await repository.getRecentShots(days: 90)
        let clubShots = allShots.filter { $0.club.id == clubId }
        return Array(clubShots.prefix(limit))
    }

    /// Gets shots with specific lie conditions.
    ///
    /// - Parameters:
    ///   - lie: The lie to filter by
    ///   - limit: Maximum number of shots to return (default: 50)
    /// - Returns: Array of shots from the specified lie
    /// - Throws: Database error if fetch fails
    func getShotsByLie(_ lie: Lie, limit: Int = 50) async throws -> [Shot] {
        let allShots = try await repository.getRecentShots(days: 90)
        let lieShots = allShots.filter { $0.lie == lie }
        return Array(lieShots.prefix(limit))
    }

    /// Gets shots that occurred under pressure.
    ///
    /// - Parameter limit: Maximum number of shots to return (default: 50)
    /// - Returns: Array of pressure shots
    /// - Throws: Database error if fetch fails
    func getShotsWithPressure(limit: Int = 50) async throws -> [Shot] {
        let pressureShots = try await repository.getShotsWithPressure()
        return Array(pressureShots.prefix(limit))
    }

    /// Gets shots within a specific time window.
    ///
    /// - Parameters:
    ///   - days: Number of days to look back
    ///   - limit: Maximum number of shots to return
    /// - Returns: Array of shots within the window
    /// - Throws: Database error if fetch fails
    func getShotsInWindow(days: Int, limit: Int? = nil) async throws -> [Shot] {
        let shots = try await repository.getRecentShots(days: days)
        if let limit = limit {
            return Array(shots.prefix(limit))
        }
        return shots
    }

    /// Gets shots that resulted in misses (excludes straight shots).
    ///
    /// - Parameter limit: Maximum number of shots to return (default: 50)
    /// - Returns: Array of shots with miss directions
    /// - Throws: Database error if fetch fails
    func getMissedShots(limit: Int = 50) async throws -> [Shot] {
        let allShots = try await repository.getRecentShots(days: 90)
        let missedShots = allShots.filter { shot in
            guard let direction = shot.missDirection else { return false }
            return direction != .straight
        }
        return Array(missedShots.prefix(limit))
    }
}
