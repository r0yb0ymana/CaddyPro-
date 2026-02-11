import Foundation

/// Represents the pressure context during a shot.
///
/// Spec R5, Q4: Pressure detection combines user toggle + scoring inference.
struct PressureContext: Codable, Hashable {
    /// User manually tagged this shot as high pressure
    let isUserTagged: Bool

    /// System inferred pressure from scoring context (e.g., close match, tournament)
    let isInferred: Bool

    /// Optional description of the scoring context
    /// Examples: "leading by 1", "tournament mode", "trailing by 2"
    let scoringContext: String?

    init(
        isUserTagged: Bool = false,
        isInferred: Bool = false,
        scoringContext: String? = nil
    ) {
        self.isUserTagged = isUserTagged
        self.isInferred = isInferred
        self.scoringContext = scoringContext
    }

    /// Returns true if there is any pressure context (user or inferred)
    var hasPressure: Bool {
        isUserTagged || isInferred
    }
}
