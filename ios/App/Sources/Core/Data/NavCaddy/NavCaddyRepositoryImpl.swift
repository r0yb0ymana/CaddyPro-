import SwiftData
import Foundation

/// SwiftData-based implementation of NavCaddyRepository.
///
/// Manages persistence for shots, patterns, and session context.
/// Spec C4: Data encrypted at rest via SwiftData/Data Protection.
@MainActor
final class NavCaddyRepositoryImpl: NavCaddyRepository {
    private let modelContainer: ModelContainer
    private let modelContext: ModelContext

    /// Retention period in days (Spec Q5: 90-day retention)
    static let retentionDays = 90

    /// Maximum conversation turns to keep (Spec R6: last 10 turns)
    static let maxConversationTurns = 10

    init(modelContainer: ModelContainer) {
        self.modelContainer = modelContainer
        self.modelContext = modelContainer.mainContext
    }

    // MARK: - Shots

    func recordShot(_ shot: Shot) async throws {
        let record = ShotRecord(from: shot)
        modelContext.insert(record)
        try modelContext.save()
    }

    func getRecentShots(days: Int = retentionDays) async throws -> [Shot] {
        let cutoffDate = Calendar.current.date(byAdding: .day, value: -days, to: Date()) ?? Date()

        let descriptor = FetchDescriptor<ShotRecord>(
            predicate: #Predicate { $0.timestamp > cutoffDate },
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )

        let records = try modelContext.fetch(descriptor)
        return records.compactMap { $0.toDomain() }
    }

    func getShotsWithPressure() async throws -> [Shot] {
        let descriptor = FetchDescriptor<ShotRecord>(
            predicate: #Predicate { $0.isUserTaggedPressure == true || $0.isInferredPressure == true },
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )

        let records = try modelContext.fetch(descriptor)
        return records.compactMap { $0.toDomain() }
    }

    // MARK: - Patterns

    func getMissPatterns() async throws -> [MissPattern] {
        let descriptor = FetchDescriptor<MissPatternRecord>(
            sortBy: [SortDescriptor(\.confidence, order: .reverse)]
        )

        let records = try modelContext.fetch(descriptor)
        return records.compactMap { $0.toDomain() }
    }

    func updatePattern(_ pattern: MissPattern) async throws {
        // Try to find existing pattern by ID
        let descriptor = FetchDescriptor<MissPatternRecord>(
            predicate: #Predicate { $0.id == pattern.id }
        )

        let existing = try modelContext.fetch(descriptor).first

        if let existing = existing {
            // Update existing record
            existing.direction = pattern.direction.rawValue
            existing.clubId = pattern.club?.id
            existing.frequency = pattern.frequency
            existing.confidence = pattern.confidence
            existing.isUserTaggedPressure = pattern.pressureContext?.isUserTagged
            existing.isInferredPressure = pattern.pressureContext?.isInferred
            existing.scoringContext = pattern.pressureContext?.scoringContext
            existing.lastOccurrence = pattern.lastOccurrence
        } else {
            // Insert new record
            let record = MissPatternRecord(from: pattern)
            modelContext.insert(record)
        }

        try modelContext.save()
    }

    func getPatternsByDirection(_ direction: MissDirection) async throws -> [MissPattern] {
        let directionRaw = direction.rawValue

        let descriptor = FetchDescriptor<MissPatternRecord>(
            predicate: #Predicate { $0.direction == directionRaw },
            sortBy: [SortDescriptor(\.confidence, order: .reverse)]
        )

        let records = try modelContext.fetch(descriptor)
        return records.compactMap { $0.toDomain() }
    }

    func getPatternsByClub(_ clubId: String) async throws -> [MissPattern] {
        let descriptor = FetchDescriptor<MissPatternRecord>(
            predicate: #Predicate { $0.clubId == clubId },
            sortBy: [SortDescriptor(\.confidence, order: .reverse)]
        )

        let records = try modelContext.fetch(descriptor)
        return records.compactMap { $0.toDomain() }
    }

    // MARK: - Session

    func getSession() async throws -> SessionContext {
        let descriptor = FetchDescriptor<SessionRecord>(
            predicate: #Predicate { $0.id == "current" }
        )

        let records = try modelContext.fetch(descriptor)

        if let record = records.first {
            return record.toDomain()
        } else {
            // Return empty session if none exists
            return SessionContext.empty
        }
    }

    func saveSession(_ context: SessionContext) async throws {
        // Fetch or create singleton session record
        let descriptor = FetchDescriptor<SessionRecord>(
            predicate: #Predicate { $0.id == "current" }
        )

        let existing = try modelContext.fetch(descriptor).first

        if let existing = existing {
            // Update existing
            existing.roundId = context.currentRound?.id
            existing.currentHole = context.currentHole
            existing.lastRecommendation = context.lastRecommendation
            existing.updatedAt = Date()

            // Replace conversation turns (limit to last 10)
            existing.conversationTurns.removeAll()
            let limitedTurns = Array(context.conversationHistory.suffix(Self.maxConversationTurns))
            existing.conversationTurns = limitedTurns.map { ConversationTurnRecord(from: $0) }
        } else {
            // Create new session
            let record = SessionRecord(from: context)
            modelContext.insert(record)
        }

        try modelContext.save()
    }

    func addConversationTurn(_ turn: ConversationTurn) async throws {
        // Get or create session
        var session = try await getSession()
        session = session.addingTurn(turn)
        try await saveSession(session)
    }

    // MARK: - Memory Management

    func clearMemory() async throws {
        // Delete all records
        try modelContext.delete(model: ShotRecord.self)
        try modelContext.delete(model: MissPatternRecord.self)
        try modelContext.delete(model: SessionRecord.self)
        try modelContext.delete(model: ConversationTurnRecord.self)

        try modelContext.save()
    }

    func enforceRetentionPolicy() async throws {
        let cutoffDate = Calendar.current.date(byAdding: .day, value: -Self.retentionDays, to: Date()) ?? Date()

        // Delete old shots
        let shotDescriptor = FetchDescriptor<ShotRecord>(
            predicate: #Predicate { $0.timestamp < cutoffDate }
        )

        let oldShots = try modelContext.fetch(shotDescriptor)
        for shot in oldShots {
            modelContext.delete(shot)
        }

        // Delete old patterns
        let patternDescriptor = FetchDescriptor<MissPatternRecord>(
            predicate: #Predicate { $0.lastOccurrence < cutoffDate }
        )

        let oldPatterns = try modelContext.fetch(patternDescriptor)
        for pattern in oldPatterns {
            modelContext.delete(pattern)
        }

        try modelContext.save()
    }
}
