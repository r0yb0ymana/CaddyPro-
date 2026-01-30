import SwiftData
import Foundation

/// SwiftData model for persisting distance change audit trail.
///
/// Tracks all changes to club distances, whether manual or accepted from suggestions.
///
/// All distances are in meters (metric units).
///
/// Spec reference: player-profile-bag-management.md R3
/// Plan reference: player-profile-bag-management-plan.md Task 4
@Model
final class DistanceAuditRecord {
    @Attribute(.unique) var id: String
    var clubId: String
    var oldEstimated: Int
    var newEstimated: Int
    var inferredValue: Int?
    var confidence: Double?
    var reason: String
    var timestamp: Date
    var wasAccepted: Bool

    init(
        id: String,
        clubId: String,
        oldEstimated: Int,
        newEstimated: Int,
        inferredValue: Int?,
        confidence: Double?,
        reason: String,
        timestamp: Date,
        wasAccepted: Bool
    ) {
        self.id = id
        self.clubId = clubId
        self.oldEstimated = oldEstimated
        self.newEstimated = newEstimated
        self.inferredValue = inferredValue
        self.confidence = confidence
        self.reason = reason
        self.timestamp = timestamp
        self.wasAccepted = wasAccepted
    }
}

// MARK: - Domain Conversion

extension DistanceAuditRecord {
    /// Creates a SwiftData record from a domain DistanceAuditEntry
    convenience init(from entry: DistanceAuditEntry) {
        self.init(
            id: entry.id,
            clubId: entry.clubId,
            oldEstimated: entry.oldEstimated,
            newEstimated: entry.newEstimated,
            inferredValue: entry.inferredValue,
            confidence: entry.confidence,
            reason: entry.reason,
            timestamp: entry.timestamp,
            wasAccepted: entry.wasAccepted
        )
    }

    /// Converts this record to a domain DistanceAuditEntry
    func toDomain() -> DistanceAuditEntry {
        DistanceAuditEntry(
            id: id,
            clubId: clubId,
            oldEstimated: oldEstimated,
            newEstimated: newEstimated,
            inferredValue: inferredValue,
            confidence: confidence,
            reason: reason,
            timestamp: timestamp,
            wasAccepted: wasAccepted
        )
    }
}
