import Foundation

/// Represents the main modules in the CaddyPro app.
///
/// Used for routing intents to the correct module.
enum Module: String, Codable, CaseIterable, Hashable {
    case caddy = "CADDY"
    case coach = "COACH"
    case recovery = "RECOVERY"
    case settings = "SETTINGS"
}
