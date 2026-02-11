import Foundation

/// Represents a bag profile containing a collection of clubs.
///
/// Spec reference: player-profile-bag-management.md R1
/// Plan reference: player-profile-bag-management-plan.md Task 2
struct BagProfile: Codable, Identifiable, Hashable {
    let id: String
    let name: String
    let isActive: Bool
    let isArchived: Bool
    let createdAt: Date
    let updatedAt: Date

    init(
        id: String = UUID().uuidString,
        name: String,
        isActive: Bool = false,
        isArchived: Bool = false,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        precondition(
            !name.trimmingCharacters(in: .whitespaces).isEmpty,
            "Bag name cannot be blank"
        )
        self.id = id
        self.name = name
        self.isActive = isActive
        self.isArchived = isArchived
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}
