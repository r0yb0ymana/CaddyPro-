package caddypro.domain.navcaddy.routing

import caddypro.domain.navcaddy.models.ParsedIntent
import caddypro.domain.navcaddy.models.RoutingTarget

/**
 * Result of routing orchestration.
 *
 * Based on intent classification, prerequisite validation, and intent type,
 * routing can result in:
 * - Navigate: Route user to target screen with state
 * - NoNavigation: Answer without screen change (query, help)
 * - PrerequisiteMissing: Required data missing, prompt user
 * - ConfirmationRequired: Mid-confidence intent needs confirmation
 *
 * Spec reference: navcaddy-engine.md R3
 */
sealed class RoutingResult {
    /**
     * Direct navigation to a target screen.
     * Used for high-confidence intents with all prerequisites met.
     *
     * @property target Routing destination with parameters
     * @property intent Original parsed intent for context
     */
    data class Navigate(
        val target: RoutingTarget,
        val intent: ParsedIntent
    ) : RoutingResult()

    /**
     * Answer without navigation.
     * Used for intents that don't require screen changes (pattern_query, help_request).
     *
     * @property intent Original parsed intent
     * @property response Answer to display to user
     */
    data class NoNavigation(
        val intent: ParsedIntent,
        val response: String
    ) : RoutingResult()

    /**
     * Prerequisites missing before routing can proceed.
     * User must provide required data first.
     *
     * @property intent Original parsed intent
     * @property missing List of missing prerequisites
     * @property message User-friendly explanation of what's needed
     */
    data class PrerequisiteMissing(
        val intent: ParsedIntent,
        val missing: List<Prerequisite>,
        val message: String
    ) : RoutingResult() {
        init {
            require(missing.isNotEmpty()) { "Must have at least one missing prerequisite" }
        }
    }

    /**
     * User confirmation required before routing.
     * Used for mid-confidence intents (0.50-0.74).
     *
     * @property intent Original parsed intent
     * @property message Confirmation question to present
     */
    data class ConfirmationRequired(
        val intent: ParsedIntent,
        val message: String
    ) : RoutingResult()
}
