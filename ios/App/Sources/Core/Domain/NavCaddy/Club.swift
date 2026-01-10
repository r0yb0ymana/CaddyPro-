import Foundation

/// Represents a golf club with its characteristics.
struct Club: Codable, Hashable, Identifiable {
    let id: String
    let name: String
    let type: ClubType
    let loft: Double?
    let distance: Int?

    init(
        id: String = UUID().uuidString,
        name: String,
        type: ClubType,
        loft: Double? = nil,
        distance: Int? = nil
    ) {
        self.id = id
        self.name = name
        self.type = type
        self.loft = loft
        self.distance = distance
    }
}
