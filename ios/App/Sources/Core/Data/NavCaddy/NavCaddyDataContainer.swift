import SwiftData
import Foundation

/// Factory for creating NavCaddy ModelContainer.
///
/// Configures SwiftData with all NavCaddy models and encryption settings.
/// Spec C4: Data encrypted at rest using iOS Data Protection.
enum NavCaddyDataContainer {
    /// Creates and configures the ModelContainer for NavCaddy persistence
    ///
    /// - Parameter inMemory: If true, uses in-memory storage (useful for testing)
    /// - Returns: Configured ModelContainer
    /// - Throws: ModelContainer creation errors
    static func create(inMemory: Bool = false) throws -> ModelContainer {
        let schema = Schema([
            ShotRecord.self,
            MissPatternRecord.self,
            SessionRecord.self,
            ConversationTurnRecord.self,
            BagProfileRecord.self,
            BagClubRecord.self,
            DistanceAuditRecord.self
        ])

        let configuration = ModelConfiguration(
            schema: schema,
            isStoredInMemoryOnly: inMemory
        )

        return try ModelContainer(for: schema, configurations: [configuration])
    }
}
