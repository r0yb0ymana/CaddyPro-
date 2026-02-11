import Foundation

/// Post-processing and formatting for Bones responses.
///
/// Spec R4: Bones Response Formatter
/// - Applies Bones voice to responses
/// - Adds disclaimers where needed
/// - Includes pattern references when relevant
/// - Formats for conversational display
///
/// This is the final layer before presenting responses to users.
@MainActor
final class BonesResponseFormatter {
    private let guardrails = PersonaGuardrails.self

    /// Format options for response generation
    struct FormatOptions {
        /// Current module context
        let module: Module

        /// Current intent type
        let intent: IntentType

        /// Relevant miss patterns to include
        let patterns: [MissPattern]

        /// Session context for personalization
        let sessionContext: SessionContext?

        /// Whether to force a disclaimer (e.g., user explicitly mentioned pain)
        let forceDisclaimer: Bool

        init(
            module: Module = .caddy,
            intent: IntentType,
            patterns: [MissPattern] = [],
            sessionContext: SessionContext? = nil,
            forceDisclaimer: Bool = false
        ) {
            self.module = module
            self.intent = intent
            self.patterns = patterns
            self.sessionContext = sessionContext
            self.forceDisclaimer = forceDisclaimer
        }
    }

    /// Formatted response with all enhancements
    struct FormattedResponse {
        /// The main response text
        let text: String

        /// Optional disclaimers to display
        let disclaimers: [String]

        /// Referenced patterns (for UI display)
        let referencedPatterns: [MissPattern]

        /// Suggested follow-up actions (chips)
        let suggestedActions: [String]

        /// Full text including disclaimers
        var fullText: String {
            var result = text
            if !disclaimers.isEmpty {
                result += "\n\n" + disclaimers.joined(separator: "\n\n")
            }
            return result
        }
    }

    init() {}

    /// Formats a raw LLM response with Bones persona enhancements.
    ///
    /// - Parameters:
    ///   - response: Raw LLM response text
    ///   - options: Format options
    /// - Returns: Formatted response with enhancements
    func format(_ response: String, options: FormatOptions) -> FormattedResponse {
        // Step 1: Sanitize response
        var processedText = guardrails.sanitizeResponse(response)

        // Step 2: Detect forbidden patterns and collect disclaimers
        let detectionResult = guardrails.detectPatterns(in: processedText)
        var disclaimers = detectionResult.disclaimers

        // Step 3: Add forced disclaimer if requested
        if options.forceDisclaimer {
            let medicalDisclaimer = PersonaGuardrails.ForbiddenPattern.medicalDiagnosis.disclaimer
            if !disclaimers.contains(medicalDisclaimer) {
                disclaimers.append(medicalDisclaimer)
            }
        }

        // Step 4: Inject pattern references if appropriate
        let patternsToReference = selectPatternsToReference(
            from: options.patterns,
            intent: options.intent
        )

        if !patternsToReference.isEmpty {
            processedText = injectPatternReferences(
                into: processedText,
                patterns: patternsToReference,
                intent: options.intent
            )
        }

        // Step 5: Apply voice polishing
        processedText = applyVoicePolish(processedText)

        // Step 6: Generate suggested follow-up actions
        let actions = generateSuggestedActions(
            intent: options.intent,
            module: options.module,
            patterns: patternsToReference
        )

        return FormattedResponse(
            text: processedText,
            disclaimers: disclaimers,
            referencedPatterns: patternsToReference,
            suggestedActions: actions
        )
    }

    // MARK: - Private Helpers

    /// Selects which patterns to reference in the response.
    ///
    /// - Parameters:
    ///   - patterns: Available patterns
    ///   - intent: Current intent
    /// - Returns: Patterns to include (max 2)
    private func selectPatternsToReference(
        from patterns: [MissPattern],
        intent: IntentType
    ) -> [MissPattern] {
        guard BonesPersona.shouldIncludePatterns(for: intent, hasPatterns: !patterns.isEmpty) else {
            return []
        }

        // Take top 2 most confident patterns
        return Array(patterns.prefix(2))
    }

    /// Injects pattern references into the response text.
    ///
    /// - Parameters:
    ///   - text: Original response text
    ///   - patterns: Patterns to reference
    ///   - intent: Current intent
    /// - Returns: Text with pattern references
    private func injectPatternReferences(
        into text: String,
        patterns: [MissPattern],
        intent: IntentType
    ) -> String {
        guard !patterns.isEmpty else { return text }

        // Generate pattern summary
        let patternSummary = formatPatternSummary(patterns)

        // Inject based on intent type
        switch intent {
        case .shotRecommendation, .clubAdjustment:
            // Add pattern context to the response
            return "\(text)\n\nContext: \(patternSummary)"

        case .patternQuery:
            // Pattern query - patterns are the main content
            return text // LLM should already have included pattern details

        default:
            // For other intents, subtly mention if relevant
            if text.contains("pattern") || text.contains("tend to") {
                // Pattern already mentioned, don't add redundant info
                return text
            } else {
                return "\(text) \(patternSummary)"
            }
        }
    }

    /// Formats a summary of miss patterns.
    ///
    /// - Parameter patterns: Patterns to summarize
    /// - Returns: Human-readable summary
    private func formatPatternSummary(_ patterns: [MissPattern]) -> String {
        guard !patterns.isEmpty else { return "" }

        if patterns.count == 1 {
            let pattern = patterns[0]
            let confidence = Int(pattern.confidence * 100)
            let clubInfo = pattern.club.map { " with \($0.name)" } ?? ""
            return "You tend to \(pattern.direction.rawValue)\(clubInfo) (\(confidence)% confidence)."
        } else {
            let summaries = patterns.map { pattern -> String in
                let confidence = Int(pattern.confidence * 100)
                let clubInfo = pattern.club.map { " with \($0.name)" } ?? ""
                return "\(pattern.direction.rawValue)\(clubInfo) (\(confidence)%)"
            }
            return "Your common misses: \(summaries.joined(separator: ", "))."
        }
    }

    /// Applies final voice polishing to match Bones persona.
    ///
    /// - Parameter text: Text to polish
    /// - Returns: Polished text
    private func applyVoicePolish(_ text: String) -> String {
        var polished = text

        // Remove generic AI phrases if they slipped through
        for phrase in BonesPersona.VoiceCharacteristics.avoidPhrases {
            polished = polished.replacingOccurrences(
                of: phrase,
                with: "",
                options: .caseInsensitive
            )
        }

        // Replace formal language with natural alternatives (aligned with Android)
        let formalToNatural: [String: String] = [
            "utilize": "use",
            "approximately": "about",
            "in order to": "to",
            "it is recommended that you": "I'd recommend",
            "you should consider": "consider",
            "it would be beneficial to": "it'll help to"
        ]

        for (formal, natural) in formalToNatural {
            polished = polished.replacingOccurrences(
                of: formal,
                with: natural,
                options: .caseInsensitive
            )
        }

        // Trim excessive whitespace
        polished = polished.trimmingCharacters(in: .whitespacesAndNewlines)
        polished = polished.replacingOccurrences(of: "\n\n\n+", with: "\n\n", options: .regularExpression)

        return polished
    }

    /// Generates suggested follow-up actions based on context.
    ///
    /// - Parameters:
    ///   - intent: Current intent
    ///   - module: Current module
    ///   - patterns: Referenced patterns
    /// - Returns: Array of suggested action phrases
    private func generateSuggestedActions(
        intent: IntentType,
        module: Module,
        patterns: [MissPattern]
    ) -> [String] {
        var actions: [String] = []

        switch intent {
        case .shotRecommendation:
            actions = ["What if the wind picks up?", "Alternative club?", "More conservative line?"]

        case .clubAdjustment:
            actions = ["Show my distances", "Compare to last round", "Record this yardage"]

        case .patternQuery:
            if !patterns.isEmpty {
                actions = ["Show me drills for this", "When does this happen most?", "How to fix this?"]
            } else {
                actions = ["Record a miss", "View my stats", "Start a round"]
            }

        case .recoveryCheck:
            actions = ["What should I focus on?", "Am I ready to play?", "Track today's wellness"]

        case .drillRequest:
            actions = ["Show me how", "Add to practice plan", "Related drills"]

        case .scoreEntry:
            actions = ["View scorecard", "Stats for this round", "Next hole"]

        case .statsLookup:
            actions = ["Compare to last week", "Export my data", "View trends"]

        default:
            // Generic actions
            actions = ["Help", "Start a round", "View my stats"]
        }

        return Array(actions.prefix(3)) // Max 3 suggestions
    }

    // MARK: - Preemptive Safety Checks

    /// Checks user input and prepares safety context for LLM.
    ///
    /// Use this before sending to LLM to inject safety reminders.
    ///
    /// - Parameter userInput: The user's input text
    /// - Returns: Optional safety reminder to inject into system prompt
    func prepareSafetyContext(for userInput: String) -> String? {
        let detectionResult = PersonaGuardrails.checkUserInput(userInput)
        return PersonaGuardrails.generateSafetyReminder(for: detectionResult.patterns)
    }

    /// Validates a response before formatting.
    ///
    /// - Parameter response: Raw LLM response
    /// - Returns: True if response is safe to format
    func validateResponse(_ response: String) -> Bool {
        return PersonaGuardrails.validateResponse(response)
    }
}

// MARK: - Convenience Extensions

extension BonesResponseFormatter {
    /// Quick format for simple responses without context.
    ///
    /// - Parameters:
    ///   - response: Raw response text
    ///   - intent: User's intent
    /// - Returns: Formatted response
    func formatSimple(_ response: String, intent: IntentType) -> FormattedResponse {
        let options = FormatOptions(intent: intent)
        return format(response, options: options)
    }

    /// Format with pattern context.
    ///
    /// - Parameters:
    ///   - response: Raw response text
    ///   - intent: User's intent
    ///   - patterns: Relevant patterns
    /// - Returns: Formatted response with pattern references
    func formatWithPatterns(
        _ response: String,
        intent: IntentType,
        patterns: [MissPattern]
    ) -> FormattedResponse {
        let options = FormatOptions(intent: intent, patterns: patterns)
        return format(response, options: options)
    }
}
