import SwiftData
import Foundation

/// SwiftData model for persisting Shot records.
///
/// Maps to domain model `Shot` with flattened relationships for SwiftData compatibility.
/// Spec C5: Stores full shot records for attributable memory.
@Model
final class ShotRecord {
    @Attribute(.unique) var id: String
    var timestamp: Date

    // Club fields (flattened)
    var clubId: String
    var clubName: String
    var clubType: String // ClubType as raw value

    // Shot context
    var missDirection: String? // MissDirection as raw value (nullable)
    var lie: String // Lie as raw value

    // Pressure context (flattened)
    var isUserTaggedPressure: Bool
    var isInferredPressure: Bool
    var scoringContext: String?

    // Optional metadata
    var holeNumber: Int?
    var notes: String?

    init(
        id: String,
        timestamp: Date,
        clubId: String,
        clubName: String,
        clubType: String,
        missDirection: String?,
        lie: String,
        isUserTaggedPressure: Bool,
        isInferredPressure: Bool,
        scoringContext: String?,
        holeNumber: Int?,
        notes: String?
    ) {
        self.id = id
        self.timestamp = timestamp
        self.clubId = clubId
        self.clubName = clubName
        self.clubType = clubType
        self.missDirection = missDirection
        self.lie = lie
        self.isUserTaggedPressure = isUserTaggedPressure
        self.isInferredPressure = isInferredPressure
        self.scoringContext = scoringContext
        self.holeNumber = holeNumber
        self.notes = notes
    }
}

// MARK: - Domain Conversion

extension ShotRecord {
    /// Creates a SwiftData record from a domain Shot
    convenience init(from shot: Shot) {
        self.init(
            id: shot.id,
            timestamp: shot.timestamp,
            clubId: shot.club.id,
            clubName: shot.club.name,
            clubType: shot.club.type.rawValue,
            missDirection: shot.missDirection?.rawValue,
            lie: shot.lie.rawValue,
            isUserTaggedPressure: shot.pressureContext.isUserTagged,
            isInferredPressure: shot.pressureContext.isInferred,
            scoringContext: shot.pressureContext.scoringContext,
            holeNumber: shot.holeNumber,
            notes: shot.notes
        )
    }

    /// Converts this record to a domain Shot
    func toDomain() -> Shot? {
        // Parse enums safely
        guard let clubType = ClubType(rawValue: clubType),
              let lie = Lie(rawValue: lie) else {
            return nil
        }

        let missDir = missDirection.flatMap { MissDirection(rawValue: $0) }

        let club = Club(
            id: clubId,
            name: clubName,
            type: clubType
        )

        let pressureContext = PressureContext(
            isUserTagged: isUserTaggedPressure,
            isInferred: isInferredPressure,
            scoringContext: scoringContext
        )

        return Shot(
            id: id,
            timestamp: timestamp,
            club: club,
            missDirection: missDir,
            lie: lie,
            pressureContext: pressureContext,
            holeNumber: holeNumber,
            notes: notes
        )
    }
}
