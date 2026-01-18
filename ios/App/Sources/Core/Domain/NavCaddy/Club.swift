import Foundation

/// Represents a golf club with its characteristics.
///
/// All distances are in meters (metric units).
///
/// Spec reference: player-profile-bag-management.md R2
/// Plan reference: player-profile-bag-management-plan.md Task 2
struct Club: Codable, Hashable, Identifiable {
    let id: String
    let name: String
    let type: ClubType
    let loft: Double?
    let distance: Int?

    // MARK: - Bag Management Fields (Task 2)

    /// User's estimated carry distance in meters.
    let estimatedCarry: Int
    /// Carry distance inferred from shot data in meters (nil until calculated).
    let inferredCarry: Int?
    /// Confidence level for inferred carry (0.0-1.0).
    let inferredConfidence: Double?
    /// Miss bias tendency for this club.
    let missBias: MissBias?
    /// Shaft specification (e.g., "Graphite", "Steel").
    let shaft: String?
    /// Flex specification (e.g., "Stiff", "Regular").
    let flex: String?
    /// User notes about the club.
    let notes: String?

    init(
        id: String = UUID().uuidString,
        name: String,
        type: ClubType,
        loft: Double? = nil,
        distance: Int? = nil,
        estimatedCarry: Int,
        inferredCarry: Int? = nil,
        inferredConfidence: Double? = nil,
        missBias: MissBias? = nil,
        shaft: String? = nil,
        flex: String? = nil,
        notes: String? = nil
    ) {
        precondition(estimatedCarry > 0, "Estimated carry must be positive")
        if let conf = inferredConfidence {
            precondition(
                (0.0...1.0).contains(conf),
                "Inferred confidence must be between 0 and 1"
            )
        }
        self.id = id
        self.name = name
        self.type = type
        self.loft = loft
        self.distance = distance
        self.estimatedCarry = estimatedCarry
        self.inferredCarry = inferredCarry
        self.inferredConfidence = inferredConfidence
        self.missBias = missBias
        self.shaft = shaft
        self.flex = flex
        self.notes = notes
    }
}
