import Foundation

/// All supported intent types in the NavCaddy Engine MVP.
///
/// Maps to the 15 MVP intents defined in spec R2.
enum IntentType: String, Codable, CaseIterable, Hashable {
    // Core intents
    case clubAdjustment = "club_adjustment"
    case recoveryCheck = "recovery_check"
    case shotRecommendation = "shot_recommendation"
    case scoreEntry = "score_entry"
    case patternQuery = "pattern_query"

    // Extended intents
    case drillRequest = "drill_request"
    case weatherCheck = "weather_check"
    case statsLookup = "stats_lookup"
    case roundStart = "round_start"
    case roundEnd = "round_end"

    // Additional intents
    case equipmentInfo = "equipment_info"
    case courseInfo = "course_info"
    case settingsChange = "settings_change"
    case helpRequest = "help_request"
    case feedback = "feedback"
}
