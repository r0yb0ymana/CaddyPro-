import SwiftData
import Foundation

/// SwiftData-based implementation of NavCaddyRepository.
///
/// Manages persistence for shots, patterns, session context, and bag management.
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

    // MARK: - Bag Profile Operations

    func createBag(profile: BagProfile) async throws -> BagProfile {
        let record = BagProfileRecord(from: profile)
        modelContext.insert(record)
        try modelContext.save()
        return profile
    }

    func updateBag(profile: BagProfile) async throws {
        let descriptor = FetchDescriptor<BagProfileRecord>(
            predicate: #Predicate { $0.id == profile.id }
        )

        guard let existing = try modelContext.fetch(descriptor).first else {
            throw RepositoryError.bagNotFound(profile.id)
        }

        existing.name = profile.name
        existing.isActive = profile.isActive
        existing.isArchived = profile.isArchived
        existing.updatedAt = profile.updatedAt

        try modelContext.save()
    }

    func archiveBag(bagId: String) async throws {
        let descriptor = FetchDescriptor<BagProfileRecord>(
            predicate: #Predicate { $0.id == bagId }
        )

        guard let bag = try modelContext.fetch(descriptor).first else {
            throw RepositoryError.bagNotFound(bagId)
        }

        bag.isArchived = true
        bag.isActive = false
        bag.updatedAt = Date()

        try modelContext.save()
    }

    func duplicateBag(bagId: String, newName: String) async throws -> BagProfile {
        // Fetch original bag
        let bagDescriptor = FetchDescriptor<BagProfileRecord>(
            predicate: #Predicate { $0.id == bagId }
        )

        guard let originalBag = try modelContext.fetch(bagDescriptor).first else {
            throw RepositoryError.bagNotFound(bagId)
        }

        // Create new bag profile
        let newBag = BagProfile(
            id: UUID().uuidString,
            name: newName,
            isActive: false,
            isArchived: false,
            createdAt: Date(),
            updatedAt: Date()
        )

        let newBagRecord = BagProfileRecord(from: newBag)
        modelContext.insert(newBagRecord)

        // Duplicate clubs from original bag
        for clubRecord in originalBag.clubs {
            let newClubRecord = BagClubRecord(
                id: UUID().uuidString,
                clubId: clubRecord.clubId,
                position: clubRecord.position,
                name: clubRecord.name,
                type: clubRecord.type,
                loft: clubRecord.loft,
                estimatedCarry: clubRecord.estimatedCarry,
                inferredCarry: clubRecord.inferredCarry,
                inferredConfidence: clubRecord.inferredConfidence,
                missBiasDirection: clubRecord.missBiasDirection,
                missBiasType: clubRecord.missBiasType,
                missBiasIsUserDefined: clubRecord.missBiasIsUserDefined,
                missBiasConfidence: clubRecord.missBiasConfidence,
                missBiasLastUpdated: clubRecord.missBiasLastUpdated,
                shaft: clubRecord.shaft,
                flex: clubRecord.flex,
                notes: clubRecord.notes,
                bagProfile: newBagRecord
            )
            modelContext.insert(newClubRecord)
        }

        try modelContext.save()
        return newBag
    }

    func getActiveBag() async throws -> BagProfile? {
        let descriptor = FetchDescriptor<BagProfileRecord>(
            predicate: #Predicate { $0.isActive == true }
        )

        let records = try modelContext.fetch(descriptor)
        return records.first?.toDomain()
    }

    func getAllBags() async throws -> [BagProfile] {
        let descriptor = FetchDescriptor<BagProfileRecord>(
            predicate: #Predicate { $0.isArchived == false },
            sortBy: [SortDescriptor(\.createdAt, order: .forward)]
        )

        let records = try modelContext.fetch(descriptor)
        return records.map { $0.toDomain() }
    }

    func switchActiveBag(bagId: String) async throws {
        // Deactivate all bags
        let allBagsDescriptor = FetchDescriptor<BagProfileRecord>()
        let allBags = try modelContext.fetch(allBagsDescriptor)

        for bag in allBags {
            bag.isActive = false
        }

        // Activate the target bag
        let targetDescriptor = FetchDescriptor<BagProfileRecord>(
            predicate: #Predicate { $0.id == bagId }
        )

        guard let targetBag = try modelContext.fetch(targetDescriptor).first else {
            throw RepositoryError.bagNotFound(bagId)
        }

        targetBag.isActive = true
        targetBag.updatedAt = Date()

        try modelContext.save()
    }

    // MARK: - Club Operations (bag-scoped)

    func getClubsForBag(bagId: String) async throws -> [Club] {
        let descriptor = FetchDescriptor<BagClubRecord>(
            predicate: #Predicate { $0.bagProfile?.id == bagId },
            sortBy: [SortDescriptor(\.position, order: .forward)]
        )

        let records = try modelContext.fetch(descriptor)
        return records.compactMap { $0.toDomain() }
    }

    func updateClubDistance(bagId: String, clubId: String, estimated: Int) async throws {
        let descriptor = FetchDescriptor<BagClubRecord>(
            predicate: #Predicate { $0.bagProfile?.id == bagId && $0.clubId == clubId }
        )

        guard let clubRecord = try modelContext.fetch(descriptor).first else {
            throw RepositoryError.clubNotFoundInBag(clubId: clubId, bagId: bagId)
        }

        clubRecord.estimatedCarry = estimated

        try modelContext.save()
    }

    func updateClubMissBias(bagId: String, clubId: String, bias: MissBias) async throws {
        let descriptor = FetchDescriptor<BagClubRecord>(
            predicate: #Predicate { $0.bagProfile?.id == bagId && $0.clubId == clubId }
        )

        guard let clubRecord = try modelContext.fetch(descriptor).first else {
            throw RepositoryError.clubNotFoundInBag(clubId: clubId, bagId: bagId)
        }

        clubRecord.missBiasDirection = bias.dominantDirection.rawValue
        clubRecord.missBiasType = bias.missType?.rawValue
        clubRecord.missBiasIsUserDefined = bias.isUserDefined
        clubRecord.missBiasConfidence = bias.confidence
        clubRecord.missBiasLastUpdated = bias.lastUpdated

        try modelContext.save()
    }

    func addClubToBag(bagId: String, club: Club, position: Int) async throws {
        let bagDescriptor = FetchDescriptor<BagProfileRecord>(
            predicate: #Predicate { $0.id == bagId }
        )

        guard let bagRecord = try modelContext.fetch(bagDescriptor).first else {
            throw RepositoryError.bagNotFound(bagId)
        }

        let clubRecord = BagClubRecord(from: club, bagId: bagId, position: position)
        clubRecord.bagProfile = bagRecord
        modelContext.insert(clubRecord)

        try modelContext.save()
    }

    func removeClubFromBag(bagId: String, clubId: String) async throws {
        let descriptor = FetchDescriptor<BagClubRecord>(
            predicate: #Predicate { $0.bagProfile?.id == bagId && $0.clubId == clubId }
        )

        guard let clubRecord = try modelContext.fetch(descriptor).first else {
            throw RepositoryError.clubNotFoundInBag(clubId: clubId, bagId: bagId)
        }

        modelContext.delete(clubRecord)
        try modelContext.save()
    }

    // MARK: - Audit Trail

    func recordDistanceAudit(entry: DistanceAuditEntry) async throws {
        let record = DistanceAuditRecord(from: entry)
        modelContext.insert(record)
        try modelContext.save()
    }

    func getAuditHistory(clubId: String) async throws -> [DistanceAuditEntry] {
        let descriptor = FetchDescriptor<DistanceAuditRecord>(
            predicate: #Predicate { $0.clubId == clubId },
            sortBy: [SortDescriptor(\.timestamp, order: .reverse)]
        )

        let records = try modelContext.fetch(descriptor)
        return records.map { $0.toDomain() }
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

// MARK: - Repository Errors

enum RepositoryError: LocalizedError {
    case bagNotFound(String)
    case clubNotFoundInBag(clubId: String, bagId: String)

    var errorDescription: String? {
        switch self {
        case .bagNotFound(let bagId):
            return "Bag with ID \(bagId) not found"
        case .clubNotFoundInBag(let clubId, let bagId):
            return "Club \(clubId) not found in bag \(bagId)"
        }
    }
}
