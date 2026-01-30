import Foundation

/// Protocol for checking if prerequisites are satisfied.
///
/// Allows dependency injection and mocking in tests.
protocol PrerequisiteChecker: Sendable {
    /// Checks if a prerequisite is satisfied
    ///
    /// - Parameter prerequisite: The prerequisite to check
    /// - Returns: True if the prerequisite is satisfied, false otherwise
    func check(_ prerequisite: Prerequisite) async -> Bool
}

/// Default implementation that checks prerequisites
///
/// This will be implemented with actual data access in a future task.
/// For now, provides a reference implementation that can be injected.
actor DefaultPrerequisiteChecker: PrerequisiteChecker {
    private let recoveryDataExists: () async -> Bool
    private let roundActive: () async -> Bool
    private let bagConfigured: () async -> Bool
    private let courseSelected: () async -> Bool

    init(
        recoveryDataExists: @escaping () async -> Bool = { false },
        roundActive: @escaping () async -> Bool = { false },
        bagConfigured: @escaping () async -> Bool = { false },
        courseSelected: @escaping () async -> Bool = { false }
    ) {
        self.recoveryDataExists = recoveryDataExists
        self.roundActive = roundActive
        self.bagConfigured = bagConfigured
        self.courseSelected = courseSelected
    }

    func check(_ prerequisite: Prerequisite) async -> Bool {
        switch prerequisite {
        case .recoveryData:
            return await recoveryDataExists()
        case .roundActive:
            return await roundActive()
        case .bagConfigured:
            return await bagConfigured()
        case .courseSelected:
            return await courseSelected()
        }
    }
}

/// Mock implementation for testing
actor MockPrerequisiteChecker: PrerequisiteChecker {
    private var satisfiedPrerequisites: Set<Prerequisite> = []

    func setSatisfied(_ prerequisite: Prerequisite, satisfied: Bool = true) {
        if satisfied {
            satisfiedPrerequisites.insert(prerequisite)
        } else {
            satisfiedPrerequisites.remove(prerequisite)
        }
    }

    func check(_ prerequisite: Prerequisite) async -> Bool {
        return satisfiedPrerequisites.contains(prerequisite)
    }
}
