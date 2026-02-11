import Foundation

/// Forbidden pattern detection and disclaimer injection.
///
/// Spec R4: Persona Guardrails
/// - Detects sensitive topics requiring disclaimers
/// - No medical diagnoses without disclaimer
/// - No betting advice
/// - No swing technique guarantees without disclaimer
///
/// Spec R9: Safety and Compliance
/// - Clear user controls and disclaimers for sensitive topics
struct PersonaGuardrails {
    /// Pattern types requiring special handling
    enum ForbiddenPattern: String, CaseIterable {
        case medicalDiagnosis
        case bettingAdvice
        case swingGuarantee
        case unsafeDirective

        /// Detection keywords for each pattern
        var detectionKeywords: [String] {
            switch self {
            case .medicalDiagnosis:
                return [
                    "diagnose", "diagnosis", "injury", "pain", "hurt", "strain",
                    "sprain", "inflammation", "tendinitis", "arthritis",
                    "medical condition", "see a doctor", "consult a physician"
                ]
            case .bettingAdvice:
                return [
                    "bet", "betting", "wager", "gamble", "gambling", "odds",
                    "put money on", "place a bet"
                ]
            case .swingGuarantee:
                return [
                    "will fix", "guaranteed to", "definitely will", "always works",
                    "never fail", "100%", "promise", "cure your slice",
                    "eliminate your hook"
                ]
            case .unsafeDirective:
                return [
                    "ignore the pain", "push through", "play injured",
                    "don't worry about", "it's nothing serious"
                ]
            }
        }

        /// Disclaimer text for each pattern
        var disclaimer: String {
            switch self {
            case .medicalDiagnosis:
                return "Note: This is informational only. For pain or injury concerns, please consult a healthcare professional."
            case .bettingAdvice:
                return "Note: This app does not provide gambling or betting advice."
            case .swingGuarantee:
                return "Note: Swing improvements vary by individual. These are suggestions based on common patterns."
            case .unsafeDirective:
                return "Important: Do not play through pain or injury. Consult a healthcare professional if you're experiencing discomfort."
            }
        }
    }

    /// Result of pattern detection
    struct DetectionResult {
        /// Detected forbidden patterns
        let patterns: Set<ForbiddenPattern>

        /// Whether any patterns were detected
        var hasIssues: Bool {
            !patterns.isEmpty
        }

        /// All disclaimers needed
        var disclaimers: [String] {
            patterns.map { $0.disclaimer }
        }
    }

    /// Detects forbidden patterns in text.
    ///
    /// - Parameters:
    ///   - text: The text to check
    ///   - caseSensitive: Whether to perform case-sensitive matching (default: false)
    /// - Returns: Detection result with found patterns
    static func detectPatterns(in text: String, caseSensitive: Bool = false) -> DetectionResult {
        let searchText = caseSensitive ? text : text.lowercased()
        var detected = Set<ForbiddenPattern>()

        for pattern in ForbiddenPattern.allCases {
            let keywords = caseSensitive ? pattern.detectionKeywords : pattern.detectionKeywords.map { $0.lowercased() }

            for keyword in keywords {
                if searchText.contains(keyword) {
                    detected.insert(pattern)
                    break
                }
            }
        }

        return DetectionResult(patterns: detected)
    }

    /// Sanitizes a response by removing or replacing forbidden content.
    ///
    /// This is a defensive layer - the LLM should follow instructions,
    /// but this catches edge cases.
    ///
    /// - Parameter text: The response text to sanitize
    /// - Returns: Sanitized text
    static func sanitizeResponse(_ text: String) -> String {
        var sanitized = text

        // Replace absolute guarantees with softer language
        let guaranteeReplacements: [String: String] = [
            "will fix": "should help address",
            "guaranteed to": "likely to",
            "definitely will": "should",
            "always works": "often helps",
            "never fails": "is typically effective",
            "100% effective": "highly effective",
            "cure your": "help improve your",
            "eliminate your": "reduce your"
        ]

        for (bad, good) in guaranteeReplacements {
            sanitized = sanitized.replacingOccurrences(
                of: bad,
                with: good,
                options: .caseInsensitive
            )
        }

        // Remove unsafe directives
        let unsafePatterns = [
            "ignore the pain",
            "push through the pain",
            "play injured",
            "it's nothing serious"
        ]

        for pattern in unsafePatterns {
            if sanitized.lowercased().contains(pattern) {
                // Remove the sentence containing the unsafe directive
                // This is a simple implementation - could be more sophisticated
                sanitized = sanitized.replacingOccurrences(
                    of: pattern,
                    with: "consult a professional if concerned",
                    options: .caseInsensitive
                )
            }
        }

        return sanitized
    }

    /// Checks if user input contains sensitive topics.
    ///
    /// Used to preemptively inject context into LLM prompts.
    ///
    /// - Parameter input: User's input text
    /// - Returns: Detection result
    static func checkUserInput(_ input: String) -> DetectionResult {
        return detectPatterns(in: input)
    }

    /// Generates a safety reminder for sensitive topics.
    ///
    /// - Parameter patterns: Detected patterns
    /// - Returns: Combined safety reminder
    static func generateSafetyReminder(for patterns: Set<ForbiddenPattern>) -> String? {
        guard !patterns.isEmpty else { return nil }

        if patterns.contains(.medicalDiagnosis) || patterns.contains(.unsafeDirective) {
            return """
            IMPORTANT: The user mentioned pain or injury. Do not diagnose or give medical advice. \
            Acknowledge their concern and recommend consulting a healthcare professional. \
            Keep golf-related suggestions informational only.
            """
        }

        if patterns.contains(.bettingAdvice) {
            return """
            IMPORTANT: Do not provide betting or gambling advice. \
            Focus on golf performance and strategy only.
            """
        }

        if patterns.contains(.swingGuarantee) {
            return """
            REMINDER: Frame swing advice as suggestions, not guarantees. \
            Use phrases like "should help", "typically", "often works" rather than absolute claims.
            """
        }

        return nil
    }

    /// Validates a response meets safety guidelines.
    ///
    /// - Parameter response: The LLM response to validate
    /// - Returns: True if response passes safety checks
    static func validateResponse(_ response: String) -> Bool {
        let result = detectPatterns(in: response)

        // Unsafe directives are never allowed
        if result.patterns.contains(.unsafeDirective) {
            return false
        }

        // Betting advice is never allowed
        if result.patterns.contains(.bettingAdvice) {
            return false
        }

        // Medical diagnosis and swing guarantees are allowed only with proper framing
        // This is handled by sanitization and disclaimer injection
        return true
    }
}
