package caddypro.domain.navcaddy.normalizer

/**
 * Type of modification made during input normalization.
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 7
 */
enum class ModificationType {
    /**
     * Golf slang expanded (e.g., "7i" -> "7-iron", "PW" -> "pitching wedge")
     */
    SLANG,

    /**
     * Spoken number converted to digits (e.g., "one fifty" -> "150")
     */
    NUMBER,

    /**
     * Profanity filtered (replaced with asterisks or removed)
     */
    PROFANITY,

    /**
     * Other normalization (whitespace, case, etc.)
     */
    OTHER
}
