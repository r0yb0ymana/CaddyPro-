import Foundation

/// Repository protocol for NavCaddy persistence operations.
///
/// Provides access to shots, patterns, and session context with memory management.
/// Spec R5, C4, C5: Supports encrypted, attributable memory with retention policies.
protocol NavCaddyRepository {
    // MARK: - Shots

    /// Records a new shot in the database
    /// - Parameter shot: The shot to record
    func recordShot(_ shot: Shot) async throws

    /// Retrieves recent shots within a specified number of days
    /// - Parameter days: Number of days to look back (default: 90 per spec)
    /// - Returns: Array of shots sorted by timestamp descending
    func getRecentShots(days: Int) async throws -> [Shot]

    /// Retrieves all shots that have pressure context (user tagged or inferred)
    /// - Returns: Array of shots with pressure
    func getShotsWithPressure() async throws -> [Shot]

    // MARK: - Patterns

    /// Retrieves all miss patterns
    /// - Returns: Array of miss patterns
    func getMissPatterns() async throws -> [MissPattern]

    /// Updates or inserts a miss pattern
    /// - Parameter pattern: The pattern to save
    func updatePattern(_ pattern: MissPattern) async throws

    /// Retrieves patterns filtered by miss direction
    /// - Parameter direction: The miss direction to filter by
    /// - Returns: Array of matching patterns
    func getPatternsByDirection(_ direction: MissDirection) async throws -> [MissPattern]

    /// Retrieves patterns filtered by club ID
    /// - Parameter clubId: The club ID to filter by
    /// - Returns: Array of matching patterns
    func getPatternsByClub(_ clubId: String) async throws -> [MissPattern]

    // MARK: - Session

    /// Retrieves the current session context
    /// - Returns: Current session context or empty context if none exists
    func getSession() async throws -> SessionContext

    /// Saves the session context
    /// - Parameter context: The session context to save
    func saveSession(_ context: SessionContext) async throws

    /// Adds a conversation turn to the current session
    /// - Parameter turn: The turn to add
    func addConversationTurn(_ turn: ConversationTurn) async throws

    // MARK: - Memory Management

    /// Clears all memory (shots, patterns, session)
    /// Spec C4: User-facing control to wipe memory
    func clearMemory() async throws

    /// Enforces the 90-day retention policy by deleting old data
    /// Spec R5, Q5: 90-day retention window
    func enforceRetentionPolicy() async throws
}
