import Foundation

/// Prepares session context for LLM prompt injection.
///
/// Formats round state, hole info, last shot details, last recommendation,
/// and conversation history into a structured prompt for the LLM.
///
/// Spec R6: Provides context to LLM for better understanding
final class ContextInjector {

    /// Builds a context prompt from session context.
    ///
    /// Creates a formatted string that provides the LLM with:
    /// - Current round information
    /// - Current hole being played
    /// - Last shot details
    /// - Last recommendation given
    /// - Recent conversation history
    ///
    /// - Parameter context: The session context to format
    /// - Returns: Formatted context string for LLM injection
    func buildContextPrompt(from context: SessionContext) -> String {
        var prompt = "# Session Context\n\n"

        // Round information
        if let round = context.currentRound {
            prompt += "## Current Round\n"
            if let courseName = round.courseName {
                prompt += "Course: \(courseName)\n"
            }
            prompt += "Round ID: \(round.id)\n"

            if !round.scores.isEmpty {
                let totalScore = round.scores.values.reduce(0, +)
                let holesPlayed = round.scores.count
                prompt += "Score: \(totalScore) after \(holesPlayed) holes\n"
            }
            prompt += "\n"
        }

        // Hole information
        if let holeNumber = context.currentHole {
            prompt += "## Current Hole\n"
            prompt += "Hole: \(holeNumber)\n\n"
        }

        // Last shot
        if let shot = context.lastShot {
            prompt += "## Last Shot\n"
            prompt += "Club: \(shot.club.name)\n"
            prompt += "Lie: \(shot.lie.rawValue)\n"

            if let missDirection = shot.missDirection {
                prompt += "Miss: \(formatMissDirection(missDirection))\n"
            }

            if shot.pressureContext.hasPressure {
                prompt += "Pressure: Yes\n"
            }

            if let notes = shot.notes {
                prompt += "Notes: \(notes)\n"
            }
            prompt += "\n"
        }

        // Last recommendation
        if let recommendation = context.lastRecommendation {
            prompt += "## Last Recommendation\n"
            prompt += "\(recommendation)\n\n"
        }

        // Conversation history
        if !context.conversationHistory.isEmpty {
            prompt += "## Recent Conversation\n"
            for turn in context.conversationHistory {
                let role = turn.role == .user ? "User" : "Assistant"
                prompt += "\(role): \(turn.content)\n"
            }
            prompt += "\n"
        }

        return prompt
    }

    // MARK: - Formatting Helpers

    private func formatMissDirection(_ direction: MissDirection) -> String {
        switch direction {
        case .straight:
            return "straight"
        case .push:
            return "push"
        case .pull:
            return "pull"
        case .slice:
            return "slice"
        case .hook:
            return "hook"
        case .fat:
            return "fat"
        case .thin:
            return "thin"
        }
    }
}
