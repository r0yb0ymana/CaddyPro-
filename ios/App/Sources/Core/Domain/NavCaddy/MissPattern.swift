import Foundation

/// Represents an aggregated miss pattern with decay over time.
///
/// Spec R5: Patterns are aggregated from shots with rolling windows and decay.
struct MissPattern: Codable, Hashable, Identifiable {
    let id: String
    let direction: MissDirection
    let club: Club?
    let frequency: Int

    /// Confidence score (0-1) that decays over time
    /// Spec Q5: 14-day decay half-life
    let confidence: Double

    let pressureContext: PressureContext?
    let lastOccurrence: Date

    /// Decay half-life in days (Q5: 14-day decay)
    static let decayHalfLifeDays: Double = 14.0

    init(
        id: String = UUID().uuidString,
        direction: MissDirection,
        club: Club? = nil,
        frequency: Int,
        confidence: Double,
        pressureContext: PressureContext? = nil,
        lastOccurrence: Date
    ) {
        self.id = id
        self.direction = direction
        self.club = club
        self.frequency = frequency
        self.confidence = min(max(confidence, 0.0), 1.0) // Clamp to [0, 1]
        self.pressureContext = pressureContext
        self.lastOccurrence = lastOccurrence
    }

    /// Calculate the decayed confidence based on time elapsed.
    ///
    /// Confidence decays with a 14-day half-life per Q5 resolution.
    ///
    /// - Parameter currentDate: The current date for decay calculation
    /// - Returns: Decayed confidence value (0-1)
    func decayedConfidence(at currentDate: Date = Date()) -> Double {
        let daysSinceOccurrence = currentDate.timeIntervalSince(lastOccurrence) / (24 * 60 * 60)
        return confidence * pow(0.5, daysSinceOccurrence / Self.decayHalfLifeDays)
    }
}
