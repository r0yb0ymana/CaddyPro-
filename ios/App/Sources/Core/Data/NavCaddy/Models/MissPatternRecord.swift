import SwiftData
import Foundation

/// SwiftData model for persisting MissPattern aggregations.
///
/// Stores aggregated patterns with decay information.
/// Spec R5: Patterns decay over time (14-day half-life).
@Model
final class MissPatternRecord {
    @Attribute(.unique) var id: String
    var direction: String // MissDirection as raw value

    // Optional club filter (nil means pattern applies to all clubs)
    var clubId: String?

    var frequency: Int

    /// Confidence score (0-1) that decays over time
    var confidence: Double

    // Optional pressure context filter
    var isUserTaggedPressure: Bool?
    var isInferredPressure: Bool?
    var scoringContext: String?

    var lastOccurrence: Date

    init(
        id: String,
        direction: String,
        clubId: String?,
        frequency: Int,
        confidence: Double,
        isUserTaggedPressure: Bool?,
        isInferredPressure: Bool?,
        scoringContext: String?,
        lastOccurrence: Date
    ) {
        self.id = id
        self.direction = direction
        self.clubId = clubId
        self.frequency = frequency
        self.confidence = confidence
        self.isUserTaggedPressure = isUserTaggedPressure
        self.isInferredPressure = isInferredPressure
        self.scoringContext = scoringContext
        self.lastOccurrence = lastOccurrence
    }
}

// MARK: - Domain Conversion

extension MissPatternRecord {
    /// Creates a SwiftData record from a domain MissPattern
    convenience init(from pattern: MissPattern) {
        self.init(
            id: pattern.id,
            direction: pattern.direction.rawValue,
            clubId: pattern.club?.id,
            frequency: pattern.frequency,
            confidence: pattern.confidence,
            isUserTaggedPressure: pattern.pressureContext?.isUserTagged,
            isInferredPressure: pattern.pressureContext?.isInferred,
            scoringContext: pattern.pressureContext?.scoringContext,
            lastOccurrence: pattern.lastOccurrence
        )
    }

    /// Converts this record to a domain MissPattern
    func toDomain() -> MissPattern? {
        guard let direction = MissDirection(rawValue: direction) else {
            return nil
        }

        // Reconstruct club if clubId exists (simplified - may need enrichment)
        let club: Club? = clubId.map { id in
            Club(id: id, name: "Club \(id)", type: .iron) // Placeholder
        }

        let pressureContext: PressureContext? = {
            if let isUserTagged = isUserTaggedPressure,
               let isInferred = isInferredPressure {
                return PressureContext(
                    isUserTagged: isUserTagged,
                    isInferred: isInferred,
                    scoringContext: scoringContext
                )
            }
            return nil
        }()

        return MissPattern(
            id: id,
            direction: direction,
            club: club,
            frequency: frequency,
            confidence: confidence,
            pressureContext: pressureContext,
            lastOccurrence: lastOccurrence
        )
    }
}
