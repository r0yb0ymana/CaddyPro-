import Foundation

/**
 * Utility for redacting Personally Identifiable Information (PII) from logs.
 *
 * Redacts common PII patterns:
 * - Email addresses
 * - Phone numbers
 * - Names (basic detection)
 * - Credit card numbers
 * - Social Security Numbers
 *
 * Spec reference: navcaddy-engine.md R8, R9 (privacy)
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
struct PIIRedactor {
    // MARK: - Regex Patterns

    /// Email pattern: name@domain.com
    private static let emailPattern = #"[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}"#

    /// Phone pattern: various formats including (555) 555-5555, 555-555-5555, +1-555-555-5555
    private static let phonePattern = #"(\+\d{1,3}[\s-]?)?\(?\d{3}\)?[\s.-]?\d{3}[\s.-]?\d{4}"#

    /// Credit card pattern: 16 digits with optional spaces or dashes
    private static let creditCardPattern = #"\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b"#

    /// SSN pattern: 123-45-6789
    private static let ssnPattern = #"\b\d{3}-\d{2}-\d{4}\b"#

    /// Name pattern (simple heuristic): capitalized words 2-3 words long
    /// This is a basic pattern and may have false positives
    private static let namePattern = #"\b[A-Z][a-z]{2,}\s+[A-Z][a-z]{2,}(?:\s+[A-Z][a-z]{2,})?\b"#

    // MARK: - Redaction

    /// Redact all PII from the given text.
    /// - Parameter text: Text to redact
    /// - Returns: Text with PII replaced by [REDACTED]
    static func redact(_ text: String) -> String {
        var redacted = text

        // Redact emails
        redacted = redacted.replacingOccurrences(
            of: emailPattern,
            with: "[REDACTED_EMAIL]",
            options: .regularExpression
        )

        // Redact phone numbers
        redacted = redacted.replacingOccurrences(
            of: phonePattern,
            with: "[REDACTED_PHONE]",
            options: .regularExpression
        )

        // Redact credit cards
        redacted = redacted.replacingOccurrences(
            of: creditCardPattern,
            with: "[REDACTED_CARD]",
            options: .regularExpression
        )

        // Redact SSNs
        redacted = redacted.replacingOccurrences(
            of: ssnPattern,
            with: "[REDACTED_SSN]",
            options: .regularExpression
        )

        // Redact names (disabled by default to avoid false positives in golf context)
        // Uncomment if needed:
        // redacted = redacted.replacingOccurrences(
        //     of: namePattern,
        //     with: "[REDACTED_NAME]",
        //     options: .regularExpression
        // )

        return redacted
    }

    /// Redact specific PII types from text.
    /// - Parameters:
    ///   - text: Text to redact
    ///   - types: Types of PII to redact
    /// - Returns: Text with specified PII types redacted
    static func redact(_ text: String, types: Set<PIIType>) -> String {
        var redacted = text

        if types.contains(.email) {
            redacted = redacted.replacingOccurrences(
                of: emailPattern,
                with: "[REDACTED_EMAIL]",
                options: .regularExpression
            )
        }

        if types.contains(.phone) {
            redacted = redacted.replacingOccurrences(
                of: phonePattern,
                with: "[REDACTED_PHONE]",
                options: .regularExpression
            )
        }

        if types.contains(.creditCard) {
            redacted = redacted.replacingOccurrences(
                of: creditCardPattern,
                with: "[REDACTED_CARD]",
                options: .regularExpression
            )
        }

        if types.contains(.ssn) {
            redacted = redacted.replacingOccurrences(
                of: ssnPattern,
                with: "[REDACTED_SSN]",
                options: .regularExpression
            )
        }

        if types.contains(.name) {
            redacted = redacted.replacingOccurrences(
                of: namePattern,
                with: "[REDACTED_NAME]",
                options: .regularExpression
            )
        }

        return redacted
    }
}

// MARK: - PII Types

/// Types of PII that can be redacted
enum PIIType {
    case email
    case phone
    case creditCard
    case ssn
    case name
}
