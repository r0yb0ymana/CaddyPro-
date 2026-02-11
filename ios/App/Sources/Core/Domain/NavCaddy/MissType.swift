import Foundation

/// Represents detailed miss types for club shots.
enum MissType: String, Codable, CaseIterable, Hashable {
    case slice
    case hook
    case push
    case pull
    case fat
    case thin
}
