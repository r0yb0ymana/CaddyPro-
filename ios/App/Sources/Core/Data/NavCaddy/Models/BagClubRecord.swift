import SwiftData
import Foundation

/// SwiftData model for persisting club-bag associations with club data.
///
/// This represents the club within the context of a specific bag.
/// The same physical club can have different settings in different bags.
///
/// All distances are in meters (metric units).
///
/// Spec reference: player-profile-bag-management.md R2
/// Plan reference: player-profile-bag-management-plan.md Task 4
@Model
final class BagClubRecord {
    @Attribute(.unique) var id: String
    var clubId: String
    var position: Int

    // Club data fields
    var name: String
    var type: String // ClubType raw value
    var loft: Double?

    // Distance fields (meters)
    var estimatedCarry: Int
    var inferredCarry: Int?
    var inferredConfidence: Double?

    // Miss bias fields (embedded from MissBias)
    var missBiasDirection: String? // MissBiasDirection raw value
    var missBiasType: String? // MissType raw value
    var missBiasIsUserDefined: Bool?
    var missBiasConfidence: Double?
    var missBiasLastUpdated: Date?

    // Additional club fields
    var shaft: String?
    var flex: String?
    var notes: String?

    // Relationship to bag profile
    var bagProfile: BagProfileRecord?

    init(
        id: String,
        clubId: String,
        position: Int,
        name: String,
        type: String,
        loft: Double?,
        estimatedCarry: Int,
        inferredCarry: Int?,
        inferredConfidence: Double?,
        missBiasDirection: String?,
        missBiasType: String?,
        missBiasIsUserDefined: Bool?,
        missBiasConfidence: Double?,
        missBiasLastUpdated: Date?,
        shaft: String?,
        flex: String?,
        notes: String?,
        bagProfile: BagProfileRecord? = nil
    ) {
        self.id = id
        self.clubId = clubId
        self.position = position
        self.name = name
        self.type = type
        self.loft = loft
        self.estimatedCarry = estimatedCarry
        self.inferredCarry = inferredCarry
        self.inferredConfidence = inferredConfidence
        self.missBiasDirection = missBiasDirection
        self.missBiasType = missBiasType
        self.missBiasIsUserDefined = missBiasIsUserDefined
        self.missBiasConfidence = missBiasConfidence
        self.missBiasLastUpdated = missBiasLastUpdated
        self.shaft = shaft
        self.flex = flex
        self.notes = notes
        self.bagProfile = bagProfile
    }
}

// MARK: - Domain Conversion

extension BagClubRecord {
    /// Creates a SwiftData record from a domain Club
    convenience init(from club: Club, bagId: String, position: Int) {
        self.init(
            id: UUID().uuidString,
            clubId: club.id,
            position: position,
            name: club.name,
            type: club.type.rawValue,
            loft: club.loft,
            estimatedCarry: club.estimatedCarry,
            inferredCarry: club.inferredCarry,
            inferredConfidence: club.inferredConfidence,
            missBiasDirection: club.missBias?.dominantDirection.rawValue,
            missBiasType: club.missBias?.missType?.rawValue,
            missBiasIsUserDefined: club.missBias?.isUserDefined,
            missBiasConfidence: club.missBias?.confidence,
            missBiasLastUpdated: club.missBias?.lastUpdated,
            shaft: club.shaft,
            flex: club.flex,
            notes: club.notes,
            bagProfile: nil
        )
    }

    /// Converts this record to a domain Club
    func toDomain() -> Club? {
        guard let clubType = ClubType(rawValue: type) else {
            return nil
        }

        // Reconstruct MissBias if direction is present
        let missBias: MissBias? = {
            guard let directionRaw = missBiasDirection,
                  let direction = MissBiasDirection(rawValue: directionRaw),
                  let isUserDef = missBiasIsUserDefined,
                  let conf = missBiasConfidence,
                  let lastUpd = missBiasLastUpdated else {
                return nil
            }

            let type = missBiasType.flatMap { MissType(rawValue: $0) }

            return MissBias(
                dominantDirection: direction,
                missType: type,
                isUserDefined: isUserDef,
                confidence: conf,
                lastUpdated: lastUpd
            )
        }()

        return Club(
            id: clubId,
            name: name,
            type: clubType,
            loft: loft,
            distance: nil, // Legacy field, not used
            estimatedCarry: estimatedCarry,
            inferredCarry: inferredCarry,
            inferredConfidence: inferredConfidence,
            missBias: missBias,
            shaft: shaft,
            flex: flex,
            notes: notes
        )
    }
}
