import SwiftData
import Foundation

/// SwiftData model for persisting bag profiles.
///
/// Represents a collection of clubs that can be activated for use in rounds.
///
/// Spec reference: player-profile-bag-management.md R1
/// Plan reference: player-profile-bag-management-plan.md Task 4
@Model
final class BagProfileRecord {
    @Attribute(.unique) var id: String
    var name: String
    var isActive: Bool
    var isArchived: Bool
    var createdAt: Date
    var updatedAt: Date

    @Relationship(deleteRule: .cascade, inverse: \BagClubRecord.bagProfile)
    var clubs: [BagClubRecord] = []

    init(
        id: String,
        name: String,
        isActive: Bool,
        isArchived: Bool,
        createdAt: Date,
        updatedAt: Date,
        clubs: [BagClubRecord] = []
    ) {
        self.id = id
        self.name = name
        self.isActive = isActive
        self.isArchived = isArchived
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.clubs = clubs
    }
}

// MARK: - Domain Conversion

extension BagProfileRecord {
    /// Creates a SwiftData record from a domain BagProfile
    convenience init(from profile: BagProfile) {
        self.init(
            id: profile.id,
            name: profile.name,
            isActive: profile.isActive,
            isArchived: profile.isArchived,
            createdAt: profile.createdAt,
            updatedAt: profile.updatedAt,
            clubs: []
        )
    }

    /// Converts this record to a domain BagProfile
    func toDomain() -> BagProfile {
        BagProfile(
            id: id,
            name: name,
            isActive: isActive,
            isArchived: isArchived,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }
}
