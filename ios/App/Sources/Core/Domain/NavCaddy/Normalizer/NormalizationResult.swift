import Foundation

/// Result of input normalization.
///
/// Spec R2: Input normalization result with tracking of modifications.
struct NormalizationResult {
    /// The normalized input string after all transformations.
    let normalizedInput: String

    /// The original input before normalization.
    let originalInput: String

    /// Whether any modifications were made during normalization.
    let wasModified: Bool

    /// List of all modifications applied.
    let modifications: [Modification]
}

/// A single modification made during normalization.
struct Modification: Equatable {
    /// The type of modification.
    let type: ModificationType

    /// The original text that was replaced.
    let original: String

    /// The replacement text.
    let replacement: String

    /// Optional range of the modification in the original string.
    let range: NSRange?

    init(type: ModificationType, original: String, replacement: String, range: NSRange? = nil) {
        self.type = type
        self.original = original
        self.replacement = replacement
        self.range = range
    }
}

/// Types of normalization modifications.
enum ModificationType: String, Equatable {
    /// Golf slang expansion (e.g., "7i" -> "7-iron").
    case slang

    /// Number normalization (e.g., "one fifty" -> "150").
    case number

    /// Profanity filtering (e.g., "damn" -> "****").
    case profanity

    /// General text cleanup (whitespace, punctuation).
    case cleanup
}
