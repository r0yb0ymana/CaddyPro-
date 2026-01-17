import Foundation

/// Enum representing all navigable destinations in the NavCaddy system.
///
/// Spec R3: Deep link navigation with module, screen, and state payload.
/// Each case holds its parameters as associated values.
/// Hashable for use with SwiftUI NavigationStack.
enum NavCaddyDestination: Hashable {
    // MARK: - Caddy Module Destinations

    /// Club adjustment workflow with pre-selected club
    case clubAdjustment(club: String)

    /// Shot recommendation screen with optional context
    case shotRecommendation(yardage: Int?, club: String?, lie: String?, wind: String?)

    /// Weather check screen with optional location
    case weatherCheck(location: String?)

    /// Course information screen with course identifier
    case courseInfo(courseId: String)

    // MARK: - Coach Module Destinations

    /// Drill screen with optional drill type filter
    case drillScreen(drillType: String?, focusClub: String?)

    /// Stats lookup screen with optional filters
    case statsLookup(statType: String?, dateRange: String?)

    /// Pattern query results screen showing miss patterns
    case patternQuery(club: String?, pressureContext: String?)

    // MARK: - Recovery Module Destinations

    /// Recovery overview dashboard
    case recoveryOverview

    /// Recovery details for a specific date
    case recoveryDetail(date: String)

    // MARK: - Round Management Destinations

    /// Start new round screen with optional course pre-selection
    case roundStart(courseId: String?)

    /// Score entry screen with hole number pre-selected
    case scoreEntry(hole: Int)

    /// Round end summary screen
    case roundEnd

    // MARK: - Settings Module Destinations

    /// Equipment/bag configuration screen
    case equipmentInfo(clubToEdit: String?)

    /// General settings screen with optional section
    case settings(section: String?)

    // MARK: - Help & Support

    /// Help screen showing NavCaddy guidance
    case help
}

extension NavCaddyDestination {
    /// The module this destination belongs to
    var module: Module {
        switch self {
        case .clubAdjustment, .shotRecommendation, .weatherCheck, .courseInfo:
            return .caddy
        case .drillScreen, .statsLookup, .patternQuery:
            return .coach
        case .recoveryOverview, .recoveryDetail:
            return .recovery
        case .equipmentInfo, .settings:
            return .settings
        case .roundStart, .scoreEntry, .roundEnd:
            return .caddy // Round management is part of caddy module
        case .help:
            return .settings
        }
    }

    /// Human-readable description for debugging
    var description: String {
        switch self {
        case .clubAdjustment(let club):
            return "Club Adjustment (\(club))"
        case .shotRecommendation(let yardage, let club, let lie, let wind):
            var parts = ["Shot Recommendation"]
            if let y = yardage { parts.append("\(y) yds") }
            if let c = club { parts.append(c) }
            if let l = lie { parts.append(l) }
            if let w = wind { parts.append(w) }
            return parts.joined(separator: ", ")
        case .weatherCheck(let location):
            return location.map { "Weather Check (\($0))" } ?? "Weather Check"
        case .courseInfo(let courseId):
            return "Course Info (\(courseId))"
        case .drillScreen(let drillType, let focusClub):
            var parts = ["Drill Screen"]
            if let d = drillType { parts.append(d) }
            if let c = focusClub { parts.append(c) }
            return parts.joined(separator: ", ")
        case .statsLookup(let statType, let dateRange):
            var parts = ["Stats Lookup"]
            if let s = statType { parts.append(s) }
            if let d = dateRange { parts.append(d) }
            return parts.joined(separator: ", ")
        case .patternQuery(let club, let pressureContext):
            var parts = ["Pattern Query"]
            if let c = club { parts.append(c) }
            if let p = pressureContext { parts.append(p) }
            return parts.joined(separator: ", ")
        case .recoveryOverview:
            return "Recovery Overview"
        case .recoveryDetail(let date):
            return "Recovery Detail (\(date))"
        case .roundStart(let courseId):
            return courseId.map { "Round Start (\($0))" } ?? "Round Start"
        case .scoreEntry(let hole):
            return "Score Entry (Hole \(hole))"
        case .roundEnd:
            return "Round End"
        case .equipmentInfo(let clubToEdit):
            return clubToEdit.map { "Equipment (\($0))" } ?? "Equipment"
        case .settings(let section):
            return section.map { "Settings (\($0))" } ?? "Settings"
        case .help:
            return "Help"
        }
    }
}
