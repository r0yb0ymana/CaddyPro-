import Foundation

/// Repository protocol for NavCaddy persistence operations.
///
/// Provides access to shots, patterns, session context, and bag management with memory management.
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

    // MARK: - Bag Profile Operations

    /// Creates a new bag profile
    /// - Parameter profile: The bag profile to create
    /// - Returns: The created bag profile
    func createBag(profile: BagProfile) async throws -> BagProfile

    /// Updates an existing bag profile
    /// - Parameter profile: The bag profile to update
    func updateBag(profile: BagProfile) async throws

    /// Archives a bag profile
    /// - Parameter bagId: The ID of the bag to archive
    func archiveBag(bagId: String) async throws

    /// Duplicates a bag profile with a new name
    /// - Parameters:
    ///   - bagId: The ID of the bag to duplicate
    ///   - newName: The name for the duplicated bag
    /// - Returns: The newly created bag profile
    func duplicateBag(bagId: String, newName: String) async throws -> BagProfile

    /// Retrieves the currently active bag profile
    /// - Returns: The active bag profile, or nil if none is active
    func getActiveBag() async throws -> BagProfile?

    /// Retrieves all bag profiles (excluding archived)
    /// - Returns: Array of bag profiles sorted by creation date
    func getAllBags() async throws -> [BagProfile]

    /// Switches the active bag to a different bag
    /// - Parameter bagId: The ID of the bag to make active
    func switchActiveBag(bagId: String) async throws

    // MARK: - Club Operations (bag-scoped)

    /// Retrieves all clubs for a specific bag
    /// - Parameter bagId: The ID of the bag
    /// - Returns: Array of clubs sorted by position
    func getClubsForBag(bagId: String) async throws -> [Club]

    /// Updates the estimated carry distance for a club in a bag
    /// - Parameters:
    ///   - bagId: The ID of the bag
    ///   - clubId: The ID of the club
    ///   - estimated: The new estimated carry distance in meters
    func updateClubDistance(bagId: String, clubId: String, estimated: Int) async throws

    /// Updates the miss bias for a club in a bag
    /// - Parameters:
    ///   - bagId: The ID of the bag
    ///   - clubId: The ID of the club
    ///   - bias: The miss bias to set
    func updateClubMissBias(bagId: String, clubId: String, bias: MissBias) async throws

    /// Adds a club to a bag at a specific position
    /// - Parameters:
    ///   - bagId: The ID of the bag
    ///   - club: The club to add
    ///   - position: The position in the bag (0-based)
    func addClubToBag(bagId: String, club: Club, position: Int) async throws

    /// Removes a club from a bag
    /// - Parameters:
    ///   - bagId: The ID of the bag
    ///   - clubId: The ID of the club
    func removeClubFromBag(bagId: String, clubId: String) async throws

    // MARK: - Audit Trail

    /// Records a distance change audit entry
    /// - Parameter entry: The audit entry to record
    func recordDistanceAudit(entry: DistanceAuditEntry) async throws

    /// Retrieves the audit history for a specific club
    /// - Parameter clubId: The ID of the club
    /// - Returns: Array of audit entries sorted by timestamp descending
    func getAuditHistory(clubId: String) async throws -> [DistanceAuditEntry]

    // MARK: - Memory Management

    /// Clears all memory (shots, patterns, session)
    /// Spec C4: User-facing control to wipe memory
    func clearMemory() async throws

    /// Enforces the 90-day retention policy by deleting old data
    /// Spec R5, Q5: 90-day retention window
    func enforceRetentionPolicy() async throws
}
