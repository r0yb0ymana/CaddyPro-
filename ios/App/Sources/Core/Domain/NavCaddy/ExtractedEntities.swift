import Foundation

/// Represents entities extracted from user input during intent classification.
///
/// Spec R2: Entity extraction includes club, yardage, lie, wind, fatigue, pain, etc.
struct ExtractedEntities: Codable, Hashable {
    let club: Club?
    let yardage: Int?
    let lie: Lie?
    let wind: String?

    /// Fatigue level on a scale of 1-10
    let fatigue: Int?

    let pain: String?
    let scoreContext: String?
    let holeNumber: Int?

    init(
        club: Club? = nil,
        yardage: Int? = nil,
        lie: Lie? = nil,
        wind: String? = nil,
        fatigue: Int? = nil,
        pain: String? = nil,
        scoreContext: String? = nil,
        holeNumber: Int? = nil
    ) {
        self.club = club
        // Reject invalid yardage (must be positive)
        self.yardage = yardage.flatMap { $0 > 0 ? $0 : nil }
        self.lie = lie
        self.wind = wind
        // Clamp fatigue to valid range [1, 10]
        self.fatigue = fatigue.map { min(max($0, 1), 10) }
        self.pain = pain
        self.scoreContext = scoreContext
        // Reject invalid hole numbers (must be 1-18)
        self.holeNumber = holeNumber.flatMap { (1...18).contains($0) ? $0 : nil }
    }
}
