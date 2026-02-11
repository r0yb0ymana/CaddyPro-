package caddypro.domain.navcaddy.routing

import caddypro.domain.navcaddy.classifier.ClassificationResult
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.ParsedIntent
import caddypro.domain.navcaddy.models.RoutingTarget
import javax.inject.Inject

/**
 * Orchestrates routing decisions based on classification results.
 *
 * Takes ClassificationResult from IntentClassifier and:
 * 1. Validates prerequisites based on intent type
 * 2. Determines if intent requires navigation or can be answered inline
 * 3. Returns appropriate RoutingResult
 *
 * Spec reference: navcaddy-engine.md R3, navcaddy-engine-plan.md Task 10
 */
class RoutingOrchestrator @Inject constructor(
    private val prerequisiteChecker: PrerequisiteChecker
) {
    /**
     * Process classification result into routing decision.
     *
     * @param classificationResult Result from intent classifier
     * @return Routing decision (Navigate, NoNavigation, PrerequisiteMissing, or ConfirmationRequired)
     */
    suspend fun route(classificationResult: ClassificationResult): RoutingResult {
        return when (classificationResult) {
            is ClassificationResult.Route -> {
                handleRouteClassification(classificationResult.intent, classificationResult.target)
            }

            is ClassificationResult.Confirm -> {
                // Mid-confidence intents need confirmation before routing
                RoutingResult.ConfirmationRequired(
                    intent = classificationResult.intent,
                    message = classificationResult.message
                )
            }

            is ClassificationResult.Clarify -> {
                // Clarification is handled by UI layer, but if we receive it here,
                // we should not route anywhere
                RoutingResult.NoNavigation(
                    intent = createPlaceholderIntent(classificationResult.originalInput),
                    response = classificationResult.message
                )
            }

            is ClassificationResult.Error -> {
                // Error is handled by UI layer
                RoutingResult.NoNavigation(
                    intent = createPlaceholderIntent("error"),
                    response = classificationResult.message
                )
            }
        }
    }

    /**
     * Handle high-confidence route classification.
     * Check prerequisites and determine final routing.
     */
    private suspend fun handleRouteClassification(
        intent: ParsedIntent,
        target: RoutingTarget
    ): RoutingResult {
        // Check if this is a no-navigation intent
        if (isNoNavigationIntent(intent.intentType)) {
            return RoutingResult.NoNavigation(
                intent = intent,
                response = generateNoNavigationResponse(intent)
            )
        }

        // Check prerequisites for intents that require data
        val requiredPrerequisites = getRequiredPrerequisites(intent.intentType)
        if (requiredPrerequisites.isNotEmpty()) {
            val missingPrerequisites = prerequisiteChecker.checkAll(requiredPrerequisites)
            if (missingPrerequisites.isNotEmpty()) {
                return RoutingResult.PrerequisiteMissing(
                    intent = intent,
                    missing = missingPrerequisites,
                    message = generatePrerequisiteMessage(intent.intentType, missingPrerequisites)
                )
            }
        }

        // All checks passed, navigate to target
        return RoutingResult.Navigate(
            target = target,
            intent = intent
        )
    }

    /**
     * Determine if intent type should not navigate to a new screen.
     */
    private fun isNoNavigationIntent(intentType: IntentType): Boolean {
        return when (intentType) {
            IntentType.PATTERN_QUERY,
            IntentType.HELP_REQUEST,
            IntentType.FEEDBACK -> true
            else -> false
        }
    }

    /**
     * Get required prerequisites for an intent type.
     */
    private fun getRequiredPrerequisites(intentType: IntentType): List<Prerequisite> {
        return when (intentType) {
            IntentType.RECOVERY_CHECK -> listOf(Prerequisite.RECOVERY_DATA)

            IntentType.SCORE_ENTRY,
            IntentType.ROUND_END -> listOf(Prerequisite.ROUND_ACTIVE)

            IntentType.CLUB_ADJUSTMENT -> listOf(Prerequisite.BAG_CONFIGURED)

            IntentType.SHOT_RECOMMENDATION -> listOf(Prerequisite.BAG_CONFIGURED)

            IntentType.COURSE_INFO -> listOf(Prerequisite.COURSE_SELECTED)

            else -> emptyList()
        }
    }

    /**
     * Generate user-facing message for missing prerequisites.
     */
    private fun generatePrerequisiteMessage(
        intentType: IntentType,
        missingPrerequisites: List<Prerequisite>
    ): String {
        return when {
            missingPrerequisites.contains(Prerequisite.RECOVERY_DATA) -> {
                "I don't have any recovery data yet. Log your sleep, HRV, or readiness score first, and I'll give you insights."
            }

            missingPrerequisites.contains(Prerequisite.ROUND_ACTIVE) -> {
                "You need to start a round first. Would you like to start a new round now?"
            }

            missingPrerequisites.contains(Prerequisite.BAG_CONFIGURED) -> {
                "Your bag isn't configured yet. Set up your clubs and distances so I can give you better recommendations."
            }

            missingPrerequisites.contains(Prerequisite.COURSE_SELECTED) -> {
                "Which course are you playing? Select a course to get specific information."
            }

            else -> {
                "Some required information is missing. Please complete your profile first."
            }
        }
    }

    /**
     * Generate response for no-navigation intents.
     */
    private fun generateNoNavigationResponse(intent: ParsedIntent): String {
        return when (intent.intentType) {
            IntentType.PATTERN_QUERY -> {
                "Let me check your miss patterns. Based on your recent shots, I'll give you insights."
            }

            IntentType.HELP_REQUEST -> {
                "I'm Bones, your digital caddy. Ask me about club selection, check your recovery, " +
                        "enter scores, or get coaching tips. What can I help you with?"
            }

            IntentType.FEEDBACK -> {
                "Thanks for the feedback! I'm always learning to serve you better."
            }

            else -> {
                "I understand. Let me help you with that."
            }
        }
    }

    /**
     * Create placeholder intent for non-intent results (clarify/error).
     */
    private fun createPlaceholderIntent(input: String): ParsedIntent {
        return ParsedIntent(
            intentId = "placeholder_${System.currentTimeMillis()}",
            intentType = IntentType.HELP_REQUEST,
            confidence = 0f,
            entities = caddypro.domain.navcaddy.models.ExtractedEntities(),
            userGoal = input,
            routingTarget = null
        )
    }
}
