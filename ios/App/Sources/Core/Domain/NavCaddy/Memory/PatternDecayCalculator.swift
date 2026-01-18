import Foundation

/// Calculates time-based decay for miss pattern confidence.
///
/// Spec R5: Applies time-based decay to older patterns to avoid permanent anchoring.
/// Q5: 14-day decay half-life.
struct PatternDecayCalculator {
    /// Decay half-life in days (default: 14 days per spec Q5)
    let decayHalfLifeDays: Double

    /// Decay function type
    enum DecayFunction {
        case exponential
        case linear
    }

    let decayFunction: DecayFunction

    init(
        decayHalfLifeDays: Double = 14.0,
        decayFunction: DecayFunction = .exponential
    ) {
        self.decayHalfLifeDays = max(decayHalfLifeDays, 1.0) // Minimum 1 day
        self.decayFunction = decayFunction
    }

    /// Calculates decay factor for a given timestamp.
    ///
    /// - Parameters:
    ///   - timestamp: The timestamp of the original data point
    ///   - currentDate: The current date for decay calculation (default: now)
    /// - Returns: Decay factor between 0.0 (very old) and 1.0 (recent)
    func calculateDecay(for timestamp: Date, at currentDate: Date = Date()) -> Double {
        let daysSinceTimestamp = currentDate.timeIntervalSince(timestamp) / (24 * 60 * 60)

        // Don't decay future timestamps or negative time intervals
        guard daysSinceTimestamp >= 0 else {
            return 1.0
        }

        switch decayFunction {
        case .exponential:
            return exponentialDecay(days: daysSinceTimestamp)
        case .linear:
            return linearDecay(days: daysSinceTimestamp)
        }
    }

    // MARK: - Private Decay Functions

    /// Exponential decay: decay_factor = 0.5^(days / half_life)
    private func exponentialDecay(days: Double) -> Double {
        return pow(0.5, days / decayHalfLifeDays)
    }

    /// Linear decay: decay_factor = max(0, 1 - (days / (2 * half_life)))
    /// Reaches 0 at 2 * half_life
    private func linearDecay(days: Double) -> Double {
        let decayWindow = 2.0 * decayHalfLifeDays
        return max(0.0, 1.0 - (days / decayWindow))
    }
}

// MARK: - Convenience Extensions

extension PatternDecayCalculator {
    /// Default calculator with 14-day exponential decay (spec Q5)
    static let `default` = PatternDecayCalculator()

    /// Short-term calculator with 7-day decay for recent patterns
    static let shortTerm = PatternDecayCalculator(decayHalfLifeDays: 7.0)

    /// Long-term calculator with 30-day decay for persistent patterns
    static let longTerm = PatternDecayCalculator(decayHalfLifeDays: 30.0)
}
