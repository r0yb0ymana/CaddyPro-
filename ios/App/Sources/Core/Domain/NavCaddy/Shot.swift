import Foundation

/// Represents a recorded golf shot with context.
///
/// Spec R5: Shots are tracked for miss pattern analysis.
struct Shot: Codable, Hashable, Identifiable {
    let id: String
    let timestamp: Date
    let club: Club
    let missDirection: MissDirection?
    let lie: Lie
    let pressureContext: PressureContext
    let holeNumber: Int?
    let notes: String?

    init(
        id: String = UUID().uuidString,
        timestamp: Date = Date(),
        club: Club,
        missDirection: MissDirection? = nil,
        lie: Lie,
        pressureContext: PressureContext = PressureContext(),
        holeNumber: Int? = nil,
        notes: String? = nil
    ) {
        self.id = id
        self.timestamp = timestamp
        self.club = club
        self.missDirection = missDirection
        self.lie = lie
        self.pressureContext = pressureContext
        self.holeNumber = holeNumber
        self.notes = notes
    }
}
