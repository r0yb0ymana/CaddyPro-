package caddypro.ui.navigation

import androidx.navigation.NavController
import caddypro.domain.navcaddy.navigation.NavCaddyDestination
import caddypro.domain.navcaddy.navigation.NavCaddyNavigator

/**
 * NavController-based implementation of NavCaddyNavigator.
 *
 * Bridges the domain layer navigation interface with the actual NavController
 * from Jetpack Navigation Compose.
 *
 * This allows the domain layer to trigger navigation without depending on
 * Android-specific navigation components.
 */
class NavCaddyNavigatorImpl(
    private val navController: NavController
) : NavCaddyNavigator {

    override fun navigate(destination: NavCaddyDestination) {
        val route = destination.toRoute()
        navController.navigate(route)
    }

    override fun navigateBack(): Boolean {
        return navController.popBackStack()
    }

    override fun navigateAndPopUpTo(
        destination: NavCaddyDestination,
        popUpTo: String,
        inclusive: Boolean
    ) {
        val route = destination.toRoute()
        navController.navigate(route) {
            this.popUpTo(popUpTo) {
                this.inclusive = inclusive
            }
        }
    }

    override fun navigateAndClearBackStack(destination: NavCaddyDestination) {
        val route = destination.toRoute()
        navController.navigate(route) {
            // Clear entire back stack using graph's start destination
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }
}
