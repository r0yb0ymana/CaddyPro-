import Foundation

/// Represents the lie or position of the ball on the course.
enum Lie: String, Codable, CaseIterable, Hashable {
    case tee
    case fairway
    case rough
    case bunker
    case green
    case fringe
    case hazard
}
