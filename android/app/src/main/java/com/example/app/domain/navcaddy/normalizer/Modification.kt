package caddypro.domain.navcaddy.normalizer

/**
 * Represents a single modification made during input normalization.
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 7
 *
 * @property type Type of modification
 * @property original Original text before modification
 * @property replacement Replacement text after modification
 */
data class Modification(
    val type: ModificationType,
    val original: String,
    val replacement: String
)
