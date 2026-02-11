import Foundation

/// Converts RoutingTarget to NavCaddyDestination for navigation.
///
/// Spec R3: Maps intent -> module route + required parameters
/// Validates targets before building destinations.
struct DeepLinkBuilder {
    /// Builds a NavCaddyDestination from a RoutingTarget
    ///
    /// - Parameter target: The routing target containing module, screen, and parameters
    /// - Returns: A NavCaddyDestination if the target is valid, nil otherwise
    func buildDestination(from target: RoutingTarget) -> NavCaddyDestination? {
        // Match on module and screen to determine destination
        switch (target.module, target.screen) {
        // MARK: - Caddy Module

        case (.caddy, "club_adjustment"):
            guard let club = target.parameters["club"] else {
                return nil // Club parameter is required
            }
            return .clubAdjustment(club: club)

        case (.caddy, "shot_recommendation"):
            let yardage = target.parameters["yardage"].flatMap { Int($0) }
            let club = target.parameters["club"]
            let lie = target.parameters["lie"]
            let wind = target.parameters["wind"]
            return .shotRecommendation(yardage: yardage, club: club, lie: lie, wind: wind)

        case (.caddy, "weather_check"):
            let location = target.parameters["location"]
            return .weatherCheck(location: location)

        case (.caddy, "course_info"):
            guard let courseId = target.parameters["course_id"] else {
                return nil // Course ID is required
            }
            return .courseInfo(courseId: courseId)

        case (.caddy, "round_start"):
            let courseId = target.parameters["course_id"]
            return .roundStart(courseId: courseId)

        case (.caddy, "score_entry"):
            guard let holeStr = target.parameters["hole"],
                  let hole = Int(holeStr) else {
                return nil // Hole number is required
            }
            return .scoreEntry(hole: hole)

        case (.caddy, "round_end"):
            return .roundEnd

        // MARK: - Coach Module

        case (.coach, "drill_screen"):
            let drillType = target.parameters["drill_type"]
            let focusClub = target.parameters["club"]
            return .drillScreen(drillType: drillType, focusClub: focusClub)

        case (.coach, "stats_lookup"):
            let statType = target.parameters["stat_type"]
            let dateRange = target.parameters["date_range"]
            return .statsLookup(statType: statType, dateRange: dateRange)

        case (.coach, "pattern_query"):
            let club = target.parameters["club"]
            let pressureContext = target.parameters["pressure_context"]
            return .patternQuery(club: club, pressureContext: pressureContext)

        // MARK: - Recovery Module

        case (.recovery, "overview"):
            return .recoveryOverview

        case (.recovery, "detail"):
            guard let date = target.parameters["date"] else {
                return nil // Date is required
            }
            return .recoveryDetail(date: date)

        // MARK: - Settings Module

        case (.settings, "equipment_info"):
            let clubToEdit = target.parameters["club"]
            return .equipmentInfo(clubToEdit: clubToEdit)

        case (.settings, "settings"):
            let section = target.parameters["section"]
            return .settings(section: section)

        case (.settings, "help"):
            return .help

        // MARK: - Unknown/Unsupported

        default:
            // Log unknown screen for debugging
            print("[DeepLinkBuilder] Unknown routing target: module=\(target.module.rawValue), screen=\(target.screen)")
            return nil
        }
    }

    /// Validates a routing target before building
    ///
    /// - Parameter target: The routing target to validate
    /// - Returns: true if the target has valid structure
    func validate(_ target: RoutingTarget) -> Bool {
        // Screen name should not be empty
        guard !target.screen.isEmpty else {
            return false
        }

        // Module should be valid (already enforced by enum)
        // Check for required parameters based on screen
        switch (target.module, target.screen) {
        case (.caddy, "club_adjustment"):
            return target.parameters["club"] != nil

        case (.caddy, "course_info"):
            return target.parameters["course_id"] != nil

        case (.caddy, "score_entry"):
            if let holeStr = target.parameters["hole"],
               let hole = Int(holeStr),
               hole >= 1 && hole <= 18 {
                return true
            }
            return false

        case (.recovery, "detail"):
            return target.parameters["date"] != nil

        default:
            // Most screens have optional parameters
            return true
        }
    }
}
