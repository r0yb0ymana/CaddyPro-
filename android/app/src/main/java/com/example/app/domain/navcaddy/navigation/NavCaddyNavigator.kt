package caddypro.domain.navcaddy.navigation

/**
 * Interface for navigation execution in the NavCaddy system.
 *
 * Abstracts the actual navigation implementation (NavController) from the domain layer.
 * The UI layer will provide the concrete implementation that interacts with NavController.
 *
 * This allows domain logic to remain platform-agnostic while still triggering navigation.
 *
 * Spec reference: navcaddy-engine.md R3, navcaddy-engine-plan.md Task 11
 */
interface NavCaddyNavigator {

    /**
     * Navigate to a specific destination.
     *
     * @param destination The typed destination to navigate to
     */
    fun navigate(destination: NavCaddyDestination)

    /**
     * Navigate back to the previous screen.
     * Returns true if back navigation was successful, false if at root.
     */
    fun navigateBack(): Boolean

    /**
     * Navigate to a destination, clearing the back stack up to a specific route.
     * Useful for flows like "start round" that should replace certain history.
     *
     * @param destination Target destination
     * @param popUpTo Route to pop up to (exclusive)
     * @param inclusive Whether to also pop the popUpTo route
     */
    fun navigateAndPopUpTo(
        destination: NavCaddyDestination,
        popUpTo: String,
        inclusive: Boolean = false
    )

    /**
     * Navigate to a destination, clearing all back stack.
     * Useful for major state changes (e.g., logout, round completion).
     *
     * @param destination Target destination (new root)
     */
    fun navigateAndClearBackStack(destination: NavCaddyDestination)
}

/**
 * No-op implementation for testing or contexts where navigation is not needed.
 */
class NoOpNavCaddyNavigator : NavCaddyNavigator {
    override fun navigate(destination: NavCaddyDestination) {
        // No-op
    }

    override fun navigateBack(): Boolean = false

    override fun navigateAndPopUpTo(
        destination: NavCaddyDestination,
        popUpTo: String,
        inclusive: Boolean
    ) {
        // No-op
    }

    override fun navigateAndClearBackStack(destination: NavCaddyDestination) {
        // No-op
    }
}
