package caddypro.domain.navcaddy.normalizer

/**
 * Result of input normalization pipeline.
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 7
 *
 * @property normalizedInput The normalized input text ready for classification
 * @property originalInput The original input text before normalization
 * @property wasModified Whether any modifications were made
 * @property modifications List of all modifications applied
 */
data class NormalizationResult(
    val normalizedInput: String,
    val originalInput: String,
    val wasModified: Boolean,
    val modifications: List<Modification>
) {
    companion object {
        /**
         * Create a result with no modifications.
         */
        fun unchanged(input: String): NormalizationResult {
            return NormalizationResult(
                normalizedInput = input,
                originalInput = input,
                wasModified = false,
                modifications = emptyList()
            )
        }
    }
}
