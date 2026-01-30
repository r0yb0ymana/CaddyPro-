package caddypro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import caddypro.domain.navcaddy.navigation.NavCaddyDestination
import caddypro.ui.caddy.LiveCaddyScreen
import caddypro.ui.screens.HomeScreen

/**
 * Main navigation graph for the CaddyPro application.
 *
 * Defines all composable routes and navigation structure using Jetpack Navigation Compose.
 * Integrates with NavCaddyNavigator for domain-driven navigation.
 *
 * Features:
 * - Home screen with Live Caddy entry points (Task 25)
 * - Live Caddy screen with full HUD integration
 * - Round start flow (placeholder)
 * - Round end summary (placeholder)
 *
 * Spec reference: live-caddy-mode.md R1, R5, live-caddy-mode-plan.md Task 23, Task 25
 *
 * @param navController Navigation controller for managing the back stack
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home screen with Live Caddy entry points (Task 25)
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { id ->
                    // navController.navigate(Screen.Detail.createRoute(id))
                },
                onNavigateToLiveCaddy = {
                    navController.navigate(NavCaddyDestination.LiveCaddy.toRoute())
                },
                onStartRound = {
                    navController.navigate(NavCaddyDestination.RoundStart().toRoute())
                }
            )
        }

        // Live Caddy Mode screen
        // Spec: live-caddy-mode.md R1-R7
        composable(NavCaddyDestination.LiveCaddy.toRoute()) {
            LiveCaddyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Round End Summary screen with roundId parameter
        // Spec: live-caddy-mode.md R1 (Live Round Context)
        composable(
            route = NavCaddyDestination.RoundEndSummary.ROUTE_PATTERN,
            arguments = listOf(
                navArgument(NavCaddyDestination.RoundEndSummary.ARG_ROUND_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val roundId = backStackEntry.arguments?.getLong(
                NavCaddyDestination.RoundEndSummary.ARG_ROUND_ID
            ) ?: return@composable

            // TODO: Implement RoundEndSummaryScreen when created in Task 24+
            // RoundEndSummaryScreen(
            //     roundId = roundId,
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }

        // Start Round screen
        // Spec: live-caddy-mode.md R1 (Live Round Context)
        composable(NavCaddyDestination.RoundStart().toRoute()) {
            // TODO: Implement StartRoundScreen when created
            // For now, this is a placeholder that navigates directly to Live Caddy
            // In a real implementation, this would show a form to:
            // - Select course
            // - Set starting hole
            // - Configure round settings
            // Then call StartRoundUseCase before navigating to Live Caddy

            // Placeholder: Navigate directly to Live Caddy
            // This will be replaced with actual StartRoundScreen implementation
            navController.navigate(NavCaddyDestination.LiveCaddy.toRoute()) {
                popUpTo(Screen.Home.route)
            }

            // Future implementation:
            // StartRoundScreen(
            //     onRoundStarted = {
            //         navController.navigate(NavCaddyDestination.LiveCaddy.toRoute()) {
            //             // Clear back stack to home when starting round
            //             popUpTo(Screen.Home.route)
            //         }
            //     },
            //     onNavigateBack = { navController.popBackStack() }
            // )
        }

        // Add more destinations here
        // composable(Screen.Detail.route) { backStackEntry ->
        //     val id = backStackEntry.arguments?.getString("id") ?: return@composable
        //     DetailScreen(id = id, onBack = { navController.popBackStack() })
        // }
    }
}

/**
 * Simple screen definitions for basic navigation.
 *
 * For more complex navigation with parameters, use NavCaddyDestination directly.
 * This sealed class is kept for backwards compatibility and simple routes.
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Detail : Screen("detail/{id}") {
        fun createRoute(id: String) = "detail/$id"
    }
    // Add more screens here
}
