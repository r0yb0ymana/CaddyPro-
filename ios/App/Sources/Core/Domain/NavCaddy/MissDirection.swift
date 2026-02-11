import Foundation

/// Represents the direction or type of shot miss.
enum MissDirection: String, Codable, CaseIterable, Hashable {
    case push
    case pull
    case slice
    case hook
    case fat
    case thin
    case straight
}
