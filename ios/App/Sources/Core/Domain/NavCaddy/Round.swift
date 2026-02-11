import Foundation

/// Represents a golf round.
struct Round: Codable, Hashable, Identifiable {
    let id: String
    let startTime: Date
    let courseName: String?

    /// Map of hole number to score
    let scores: [Int: Int]

    init(
        id: String = UUID().uuidString,
        startTime: Date = Date(),
        courseName: String? = nil,
        scores: [Int: Int] = [:]
    ) {
        self.id = id
        self.startTime = startTime
        self.courseName = courseName
        self.scores = scores
    }
}
