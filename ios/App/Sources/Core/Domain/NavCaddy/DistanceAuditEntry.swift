import Foundation

/// Represents an audit trail entry for distance changes.
///
/// All distances are in meters (metric units).
///
/// Spec reference: player-profile-bag-management.md R3
/// Plan reference: player-profile-bag-management-plan.md Task 2
struct DistanceAuditEntry: Codable, Identifiable, Hashable {
    let id: String
    let clubId: String
    /// Previous estimated carry distance in meters.
    let oldEstimated: Int
    /// New estimated carry distance in meters.
    let newEstimated: Int
    /// Inferred value that triggered suggestion in meters (nil if manual change).
    let inferredValue: Int?
    /// Confidence level for the inference (0.0-1.0).
    let confidence: Double?
    let reason: String
    let timestamp: Date
    let wasAccepted: Bool

    init(
        id: String = UUID().uuidString,
        clubId: String,
        oldEstimated: Int,
        newEstimated: Int,
        inferredValue: Int? = nil,
        confidence: Double? = nil,
        reason: String,
        timestamp: Date = Date(),
        wasAccepted: Bool
    ) {
        precondition(oldEstimated > 0, "Old estimated distance must be positive")
        precondition(newEstimated > 0, "New estimated distance must be positive")
        if let conf = confidence {
            precondition(
                (0.0...1.0).contains(conf),
                "Confidence must be between 0 and 1"
            )
        }
        precondition(
            !reason.trimmingCharacters(in: .whitespaces).isEmpty,
            "Reason cannot be blank"
        )
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
