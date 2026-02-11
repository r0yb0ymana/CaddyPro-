import Foundation

/// Handles clarification generation for low-confidence intent classification.
///
/// When intent confidence is below the clarification threshold (< 0.50),
/// this handler generates targeted clarification questions and suggests
/// relevant intents to help users refine their request.
///
/// Spec reference: navcaddy-engine.md R2, G6, A3
/// Task reference: navcaddy-engine-plan.md Task 8
struct ClarificationHandler {
    /// Generates a clarification response for ambiguous user input.
    ///
    /// - Parameters:
    ///   - input: The original ambiguous user input
    ///   - parsedIntent: The low-confidence parsed intent (may be used for similarity)
    ///   - context: Optional session context for contextual suggestions
    /// - Returns: Clarification response with message and up to 3 suggestions
    func generateClarification(
        for input: String,
        parsedIntent: ParsedIntent?,
        context: SessionContext?
    ) -> ClarificationResponse {
        // Find relevant intents based on input keywords and context
        let suggestions = findRelevantIntents(
            input: input,
            parsedIntent: parsedIntent,
            context: context,
            maxCount: 3
        )

        // Generate contextual clarification message
        let message = generateClarificationMessage(
            input: input,
            suggestions: suggestions
        )

        return ClarificationResponse(
            message: message,
            suggestions: suggestions,
            originalInput: input
        )
    }

    // MARK: - Intent Similarity & Relevance

    /// Finds relevant intents based on input keywords and context.
    private func findRelevantIntents(
        input: String,
        parsedIntent: ParsedIntent?,
        context: SessionContext?,
        maxCount: Int
    ) -> [IntentSuggestion] {
        let normalizedInput = input.lowercased()
        var scoredIntents: [(IntentType, Double)] = []

        // Score each intent based on keyword matching
        for intentType in IntentType.allCases {
            let score = calculateRelevanceScore(
                intentType: intentType,
                input: normalizedInput,
                parsedIntent: parsedIntent,
                context: context
            )
            scoredIntents.append((intentType, score))
        }

        // Sort by score descending and take top N
        let topIntents = scoredIntents
            .sorted { $0.1 > $1.1 }
            .prefix(maxCount)
            .map { $0.0 }

        // Convert to suggestions with labels
        return topIntents.map { intentType in
            createSuggestion(for: intentType)
        }
    }

    /// Calculates relevance score for an intent based on input keywords.
    private func calculateRelevanceScore(
        intentType: IntentType,
        input: String,
        parsedIntent: ParsedIntent?,
        context: SessionContext?
    ) -> Double {
        var score = 0.0
        let schema = IntentRegistry.getSchema(for: intentType)

        // Boost score if this matches the parsed intent (even if low confidence)
        if parsedIntent?.intentType == intentType {
            score += 2.0
        }

        // Score based on keyword matching with example phrases
        for phrase in schema.examplePhrases {
            let phraseWords = phrase.lowercased().split(separator: " ")
            let matchingWords = phraseWords.filter { input.contains($0) }
            score += Double(matchingWords.count) * 0.5
        }

        // Score based on keywords in intent description
        let descriptionWords = schema.description.lowercased().split(separator: " ")
        let matchingDescWords = descriptionWords.filter { input.contains($0) }
        score += Double(matchingDescWords.count) * 0.3

        // Context-based scoring
        if let context = context {
            score += contextualBoost(for: intentType, context: context)
        }

        // Keyword-based boosting
        score += keywordBoost(for: intentType, input: input)

        return score
    }

    /// Provides contextual boost for intents based on session state.
    private func contextualBoost(for intentType: IntentType, context: SessionContext) -> Double {
        var boost = 0.0

        // If in active round, boost round-related intents
        if context.currentRound != nil {
            switch intentType {
            case .shotRecommendation, .scoreEntry, .patternQuery, .weatherCheck:
                boost += 1.0
            case .roundEnd:
                boost += 0.5
            default:
                break
            }
        } else {
            // Not in round, boost round start
            if intentType == .roundStart {
                boost += 1.0
            }
        }

        return boost
    }

    /// Provides keyword-based boost for specific intent types.
    private func keywordBoost(for intentType: IntentType, input: String) -> Double {
        let keywords = getKeywords(for: intentType)
        var boost = 0.0

        for keyword in keywords {
            if input.contains(keyword) {
                boost += 1.5
            }
        }

        return boost
    }

    /// Gets common keywords for an intent type.
    private func getKeywords(for intentType: IntentType) -> [String] {
        switch intentType {
        case .clubAdjustment:
            return ["club", "adjust", "distance", "feels", "long", "short", "iron", "driver", "wedge"]
        case .recoveryCheck:
            return ["recovery", "ready", "tired", "sore", "fatigue", "readiness", "rest"]
        case .shotRecommendation:
            return ["shot", "play", "club", "hit", "recommend", "what", "advice", "yards", "yardage"]
        case .scoreEntry:
            return ["score", "enter", "log", "made", "par", "bogey", "birdie", "got"]
        case .patternQuery:
            return ["pattern", "miss", "tendency", "slice", "hook", "push", "pull"]
        case .drillRequest:
            return ["drill", "practice", "exercise", "work", "improve", "training"]
        case .weatherCheck:
            return ["weather", "wind", "rain", "forecast", "conditions"]
        case .statsLookup:
            return ["stats", "statistics", "performance", "average", "percentage"]
        case .roundStart:
            return ["start", "begin", "new", "round", "tee"]
        case .roundEnd:
            return ["end", "finish", "done", "complete", "summary"]
        case .equipmentInfo:
            return ["equipment", "bag", "clubs", "gear", "setup"]
        case .courseInfo:
            return ["course", "hole", "yardage", "layout", "map"]
        case .settingsChange:
            return ["settings", "change", "update", "preferences", "options"]
        case .helpRequest:
            return ["help", "how", "what", "explain", "understand"]
        case .feedback:
            return ["feedback", "bug", "report", "problem", "issue"]
        }
    }

    // MARK: - Suggestion Creation

    /// Creates a user-friendly suggestion for an intent type.
    private func createSuggestion(for intentType: IntentType) -> IntentSuggestion {
        let schema = IntentRegistry.getSchema(for: intentType)

        // Create concise, actionable labels
        let label = createChipLabel(for: intentType)
        let description = schema.description

        return IntentSuggestion(
            intentType: intentType,
            label: label,
            description: description
        )
    }

    /// Creates a concise chip label for an intent type.
    private func createChipLabel(for intentType: IntentType) -> String {
        switch intentType {
        case .clubAdjustment:
            return "Adjust Club"
        case .recoveryCheck:
            return "Check Recovery"
        case .shotRecommendation:
            return "Get Shot Advice"
        case .scoreEntry:
            return "Enter Score"
        case .patternQuery:
            return "View Patterns"
        case .drillRequest:
            return "Get Drill"
        case .weatherCheck:
            return "Check Weather"
        case .statsLookup:
            return "View Stats"
        case .roundStart:
            return "Start Round"
        case .roundEnd:
            return "End Round"
        case .equipmentInfo:
            return "View Equipment"
        case .courseInfo:
            return "Course Info"
        case .settingsChange:
            return "Change Settings"
        case .helpRequest:
            return "Get Help"
        case .feedback:
            return "Send Feedback"
        }
    }

    // MARK: - Message Generation

    /// Generates a contextual clarification message.
    private func generateClarificationMessage(
        input: String,
        suggestions: [IntentSuggestion]
    ) -> String {
        // Analyze input to provide targeted questions
        if input.count < 5 {
            return "I'm not sure what you need. Could you tell me more?"
        }

        if containsVagueWords(input) {
            return "I'm not quite sure what you mean. Did you want to:"
        }

        // Check for specific patterns
        if containsFeelWords(input) {
            return "Could you clarify? Are you looking to:"
        }

        if containsQuestionWords(input) {
            return "I can help with that. Did you want to:"
        }

        // Default message
        return "I'm not quite sure. Did you want to:"
    }

    /// Checks if input contains vague words.
    private func containsVagueWords(_ input: String) -> Bool {
        let vagueWords = ["it", "this", "that", "something", "help", "off", "wrong"]
        let normalizedInput = input.lowercased()
        return vagueWords.contains { normalizedInput.contains($0) }
    }

    /// Checks if input contains feel-related words.
    private func containsFeelWords(_ input: String) -> Bool {
        let feelWords = ["feel", "feels", "feeling"]
        let normalizedInput = input.lowercased()
        return feelWords.contains { normalizedInput.contains($0) }
    }

    /// Checks if input contains question words.
    private func containsQuestionWords(_ input: String) -> Bool {
        let questionWords = ["what", "how", "when", "where", "why", "which", "should", "can", "could"]
        let normalizedInput = input.lowercased()
        return questionWords.contains { normalizedInput.contains($0) }
    }
}
