package caddypro.domain.navcaddy.navigation

import caddypro.domain.navcaddy.routing.RoutingResult
import javax.inject.Inject

/**
 * Executes navigation actions based on RoutingResult from the orchestrator.
 *
 * Processes the routing decision and:
 * - For Navigate: builds the route and executes navigation
 * - For NoNavigation: returns the response for inline display
 * - For PrerequisiteMissing: returns UI action to prompt user
 * - For ConfirmationRequired: returns UI action to show confirmation
 *
 * Respects the latency budget of <100ms for navigation execution (R3, C1).
 *
 * Spec reference: navcaddy-engine.md R3, navcaddy-engine-plan.md Task 11
 */
class NavigationExecutor @Inject constructor(
    private val deepLinkBuilder: DeepLinkBuilder,
    private val navigator: NavCaddyNavigator
) {

    /**
     * Execute the appropriate action based on routing result.
     *
     * @param result Routing decision from orchestrator
     * @return NavigationAction describing what happened (for UI feedback)
     */
    fun execute(result: RoutingResult): NavigationAction {
        return when (result) {
            is RoutingResult.Navigate -> {
                executeNavigation(result)
            }

            is RoutingResult.NoNavigation -> {
                NavigationAction.ShowInlineResponse(
                    response = result.response
                )
            }

            is RoutingResult.PrerequisiteMissing -> {
                NavigationAction.PromptPrerequisites(
                    message = result.message,
                    missingPrerequisites = result.missing.map { it.name }
                )
            }

            is RoutingResult.ConfirmationRequired -> {
                NavigationAction.RequestConfirmation(
                    message = result.message,
                    intent = result.intent
                )
            }
        }
    }

    /**
     * Execute navigation to a target.
     * Builds route and triggers navigation via navigator.
     */
    private fun executeNavigation(result: RoutingResult.Navigate): NavigationAction {
        return try {
            val destination = deepLinkBuilder.buildDestination(result.target)
            navigator.navigate(destination)

            NavigationAction.Navigated(
                destination = destination,
                route = destination.toRoute()
            )
        } catch (e: IllegalArgumentException) {
            NavigationAction.NavigationFailed(
                error = "Invalid navigation target: ${e.message}",
                target = result.target
            )
        }
    }
}

/**
 * Result of navigation execution.
 * Returned to UI layer for appropriate feedback/state updates.
 */
sealed class NavigationAction {
    /**
     * Successfully navigated to a destination.
     */
    data class Navigated(
        val destination: NavCaddyDestination,
        val route: String
    ) : NavigationAction()

    /**
     * Display response inline without navigation.
     */
    data class ShowInlineResponse(
        val response: String
    ) : NavigationAction()

    /**
     * Prompt user to provide missing prerequisites.
     */
    data class PromptPrerequisites(
        val message: String,
        val missingPrerequisites: List<String>
    ) : NavigationAction()

    /**
     * Request user confirmation before proceeding.
     */
    data class RequestConfirmation(
        val message: String,
        val intent: caddypro.domain.navcaddy.models.ParsedIntent
    ) : NavigationAction()

    /**
     * Navigation failed due to invalid target.
     */
    data class NavigationFailed(
        val error: String,
        val target: caddypro.domain.navcaddy.models.RoutingTarget
    ) : NavigationAction()
}
