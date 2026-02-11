import Foundation

/// Represents the type of golf club.
enum ClubType: String, Codable, CaseIterable, Hashable {
    case driver
    case wood
    case hybrid
    case iron
    case wedge
    case putter
}
