import Foundation

/// Tracks the current round state for session context.
///
/// Provides a more detailed view of round state than the basic Round model,
/// including current hole, par, total score, and conditions.
///
/// Spec R6: Tracks current round state (hole, score, conditions)
struct RoundState: Codable, Hashable {
    /// Unique identifier for the round
    let roundId: String

    /// Name of the golf course
    let courseName: String

    /// Current hole being played (1-18)
    let currentHole: Int

    /// Par for the current hole
    let currentPar: Int

    /// Total score so far
    let totalScore: Int

    /// Number of holes completed
    let holesCompleted: Int

    /// Current weather and playing conditions
    let conditions: PlayingConditions

    init(
        roundId: String,
        courseName: String,
        currentHole: Int,
        currentPar: Int = 4,
        totalScore: Int = 0,
        holesCompleted: Int = 0,
        conditions: PlayingConditions = PlayingConditions()
    ) {
        self.roundId = roundId
        self.courseName = courseName
        self.currentHole = currentHole
        self.currentPar = currentPar
        self.totalScore = totalScore
        self.holesCompleted = holesCompleted
        self.conditions = conditions
    }

    /// Returns whether the player is currently under or over par.
    var scoreToPar: Int {
        let expectedPar = holesCompleted * 4 // Simplified - assumes par 4 average
        return totalScore - expectedPar
    }

    /// Returns whether the round is complete.
    var isComplete: Bool {
        return holesCompleted >= 18
    }
}

/// Represents current playing conditions.
struct PlayingConditions: Codable, Hashable {
    /// Weather description (e.g., "Sunny", "Cloudy", "Rain")
    let weather: String?

    /// Wind description (e.g., "Light breeze", "Strong headwind")
    let wind: String?

    /// Temperature in Fahrenheit
    let temperature: Int?

    /// Course condition (e.g., "Wet", "Fast greens")
    let courseCondition: String?

    init(
        weather: String? = nil,
        wind: String? = nil,
        temperature: Int? = nil,
        courseCondition: String? = nil
    ) {
        self.weather = weather
        self.wind = wind
        self.temperature = temperature
        self.courseCondition = courseCondition
    }

    /// Returns a human-readable description of conditions.
    var description: String {
        var parts: [String] = []

        if let weather = weather {
            parts.append(weather)
        }

        if let wind = wind {
            parts.append(wind)
        }

        if let temperature = temperature {
            parts.append("\(temperature)°F")
        }

        if let courseCondition = courseCondition {
            parts.append(courseCondition)
        }

        return parts.isEmpty ? "Normal conditions" : parts.joined(separator: ", ")
    }
}
