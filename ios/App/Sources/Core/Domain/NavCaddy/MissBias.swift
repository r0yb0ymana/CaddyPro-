import Foundation

/// Represents the miss bias tendency for a club.
///
/// Spec reference: player-profile-bag-management.md R4
/// Plan reference: player-profile-bag-management-plan.md Task 2
struct MissBias: Codable, Hashable {
    let dominantDirection: MissBiasDirection
    let missType: MissType?
    let isUserDefined: Bool
    /// Confidence level (0.0-1.0).
    let confidence: Double
    let lastUpdated: Date

    init(
        dominantDirection: MissBiasDirection,
        missType: MissType? = nil,
        isUserDefined: Bool,
        confidence: Double,
        lastUpdated: Date = Date()
    ) {
        precondition(
            (0.0...1.0).contains(confidence),
            "Confidence must be between 0 and 1"
        )
        self.dominantDirection = dominantDirection
        self.missType = missType
        self.isUserDefined = isUserDefined
        self.confidence = confidence
        self.lastUpdated = lastUpdated
    }
}

/// Represents the directional miss bias (simplified for club bias).
/// Matches Android enum: MissBiasDirection (LEFT, STRAIGHT, RIGHT).
enum MissBiasDirection: String, Codable, CaseIterable, Hashable {
    case left
    case straight
    case right
}
