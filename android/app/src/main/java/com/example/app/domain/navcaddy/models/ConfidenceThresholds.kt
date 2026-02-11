package caddypro.domain.navcaddy.models

/**
 * Confidence thresholds for intent routing decisions.
 *
 * These thresholds determine whether to route directly, confirm with user,
 * or request clarification.
 *
 * Spec reference: navcaddy-engine.md Q2 (RESOLVED)
 */
object ConfidenceThresholds {
    /**
     * Minimum confidence to route directly to a destination.
     * Confidence >= 0.75 routes immediately without confirmation.
     */
    const val ROUTE_THRESHOLD = 0.75f

    /**
     * Minimum confidence to confirm intent before routing.
     * Confidence in range [0.50, 0.75) prompts user confirmation.
     */
    const val CONFIRM_THRESHOLD = 0.50f

    /**
     * Below this threshold, request clarification.
     * Confidence < 0.50 triggers clarification with suggested intents.
     */
    const val CLARIFY_THRESHOLD = 0.50f

    /**
     * Determines the appropriate action based on confidence level.
     *
     * @param confidence The intent classification confidence (0-1)
     * @return The threshold action to take
     */
    fun actionForConfidence(confidence: Float): ThresholdAction = when {
        confidence >= ROUTE_THRESHOLD -> ThresholdAction.ROUTE
        confidence >= CONFIRM_THRESHOLD -> ThresholdAction.CONFIRM
        else -> ThresholdAction.CLARIFY
    }
}

/**
 * Actions that can be taken based on confidence thresholds.
 */
enum class ThresholdAction {
    /** Navigate directly to the destination without confirmation */
    ROUTE,
    /** Ask user to confirm the detected intent before navigating */
    CONFIRM,
    /** Request clarification with suggested intents */
    CLARIFY
}
