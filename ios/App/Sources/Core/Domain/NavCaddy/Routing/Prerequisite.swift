import Foundation

/// Prerequisites that must be satisfied before routing to certain intents.
///
/// Spec R3: Prerequisites like "recovery data required" must be validated before navigation.
enum Prerequisite: String, Codable, CaseIterable, Hashable {
    /// Recovery data must exist for recovery_check intent
    case recoveryData = "recovery_data"

    /// An active round must exist for score_entry, round_end intents
    case roundActive = "round_active"

    /// Bag/club configuration must exist for club_adjustment, shot_recommendation
    case bagConfigured = "bag_configured"

    /// Course must be selected for course_info intent
    case courseSelected = "course_selected"
}

extension Prerequisite {
    /// Human-readable description of the prerequisite
    var description: String {
        switch self {
        case .recoveryData:
            return "Recovery data"
        case .roundActive:
            return "Active round"
        case .bagConfigured:
            return "Bag configuration"
        case .courseSelected:
            return "Course selection"
        }
    }

    /// User-facing message when prerequisite is missing (Bones persona - conversational)
    var missingMessage: String {
        switch self {
        case .recoveryData:
            return "I don't have any recovery data yet. Log your sleep, HRV, or readiness score first, and I'll give you insights."
        case .roundActive:
            return "You need to start a round first. Would you like to start a new round now?"
        case .bagConfigured:
            return "Your bag isn't configured yet. Set up your clubs and distances so I can give you better recommendations."
        case .courseSelected:
            return "Which course are you playing? Select a course to get specific information."
        }
    }
}
