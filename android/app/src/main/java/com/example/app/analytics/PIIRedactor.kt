package caddypro.analytics

/**
 * PII redaction utility to remove sensitive information from logged text.
 *
 * Spec reference: navcaddy-engine.md R8, R9
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
object PIIRedactor {
    // Common name patterns (simple first/last name detection)
    private val namePattern = Regex(
        "\\b[A-Z][a-z]+\\s+[A-Z][a-z]+\\b"
    )

    // Email pattern
    private val emailPattern = Regex(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"
    )

    // Phone number patterns (US and international)
    private val phonePattern = Regex(
        "(?:(?:\\+?1\\s*(?:[.-]\\s*)?)?(?:\\(\\s*([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9])\\s*\\)|([2-9]1[02-9]|[2-9][02-8]1|[2-9][02-8][02-9]))\\s*(?:[.-]\\s*)?)?([2-9]1[02-9]|[2-9][02-9]1|[2-9][02-9]{2})\\s*(?:[.-]\\s*)?([0-9]{4})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?|\\+?\\d{1,3}[-.\\s]?\\(\\d{1,4}\\)[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}"
    )

    // Credit card pattern (basic)
    private val creditCardPattern = Regex(
        "\\b(?:\\d{4}[-\\s]?){3}\\d{4}\\b"
    )

    // SSN pattern (US)
    private val ssnPattern = Regex(
        "\\b\\d{3}-\\d{2}-\\d{4}\\b"
    )

    // Street address pattern (basic)
    private val addressPattern = Regex(
        "\\b\\d+\\s+[A-Z][a-z]+\\s+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr|Court|Ct|Circle|Cir|Way)\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * Redact PII from text.
     *
     * @param text The text to redact
     * @param redactNames Whether to redact potential names (default: true)
     * @return Redacted text with [REDACTED] placeholders
     */
    fun redact(text: String, redactNames: Boolean = true): String {
        var redacted = text

        // Redact email addresses
        redacted = emailPattern.replace(redacted, "[EMAIL_REDACTED]")

        // Redact phone numbers
        redacted = phonePattern.replace(redacted, "[PHONE_REDACTED]")

        // Redact credit cards
        redacted = creditCardPattern.replace(redacted, "[CC_REDACTED]")

        // Redact SSN
        redacted = ssnPattern.replace(redacted, "[SSN_REDACTED]")

        // Redact addresses
        redacted = addressPattern.replace(redacted, "[ADDRESS_REDACTED]")

        // Optionally redact names
        if (redactNames) {
            redacted = namePattern.replace(redacted, "[NAME_REDACTED]")
        }

        return redacted
    }

    /**
     * Redact PII from a map of key-value pairs.
     *
     * @param data The data to redact
     * @param redactNames Whether to redact potential names (default: true)
     * @return Map with redacted values
     */
    fun redactMap(data: Map<String, String>, redactNames: Boolean = true): Map<String, String> {
        return data.mapValues { (_, value) ->
            redact(value, redactNames)
        }
    }

    /**
     * Check if text contains potential PII.
     *
     * @param text The text to check
     * @return true if PII patterns are detected
     */
    fun containsPII(text: String): Boolean {
        return emailPattern.containsMatchIn(text) ||
            phonePattern.containsMatchIn(text) ||
            creditCardPattern.containsMatchIn(text) ||
            ssnPattern.containsMatchIn(text) ||
            addressPattern.containsMatchIn(text)
    }
}
