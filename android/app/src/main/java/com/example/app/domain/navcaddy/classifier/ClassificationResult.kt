package caddypro.domain.navcaddy.classifier

import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.ParsedIntent
import caddypro.domain.navcaddy.models.RoutingTarget

/**
 * Result of intent classification with routing decision.
 *
 * Based on confidence thresholds, classification can result in:
 * - Route: High confidence, navigate immediately
 * - Confirm: Medium confidence, ask user to confirm
 * - Clarify: Low confidence, request clarification
 * - Error: Classification failed
 *
 * Spec reference: navcaddy-engine.md R2, G6, navcaddy-engine-plan.md Task 6
 */
sealed class ClassificationResult {
    /**
     * High confidence classification (>= 0.75).
     * System will route directly to the target.
     *
     * @property intent Classified intent with entities
     * @property target Routing destination
     */
    data class Route(
        val intent: ParsedIntent,
        val target: RoutingTarget
    ) : ClassificationResult()

    /**
     * Medium confidence classification (0.50 - 0.74).
     * System will ask user to confirm before routing.
     *
     * @property intent Classified intent with entities
     * @property message Confirmation message to present to user
     */
    data class Confirm(
        val intent: ParsedIntent,
        val message: String
    ) : ClassificationResult()

    /**
     * Low confidence classification (< 0.50).
     * System will request clarification with suggestions.
     *
     * @property suggestions List of possible intent types (max 3)
     * @property message Clarification question to present to user
     * @property originalInput Original user input for context
     */
    data class Clarify(
        val suggestions: List<IntentType>,
        val message: String,
        val originalInput: String
    ) : ClassificationResult() {
        init {
            require(suggestions.size <= 3) { "Maximum 3 suggestions allowed" }
        }
    }

    /**
     * Classification failed due to error.
     *
     * @property cause Original exception
     * @property message User-friendly error message
     */
    data class Error(
        val cause: Throwable,
        val message: String = "Unable to understand your request. Please try again."
    ) : ClassificationResult()
}
