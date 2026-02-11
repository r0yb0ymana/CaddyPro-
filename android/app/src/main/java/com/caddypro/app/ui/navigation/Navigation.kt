package com.caddypro.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.caddypro.app.ui.clubs.ClubListScreen
import com.caddypro.app.ui.forecaster.ForecasterScreen
import com.caddypro.app.ui.holemap.HoleMapScreen
import com.caddypro.app.ui.shotlogger.RoundStartScreen
import com.caddypro.app.ui.shotlogger.RoundSummaryScreen
import com.caddypro.app.ui.shotlogger.ShotLoggerScreen

/**
 * CaddyPro Navigation Graph
 *
 * Defines the navigation structure for the app.
 * Currently a shell - screens will be implemented in subsequent tasks.
 */
@Composable
fun CaddyProNavigation(
    navController: NavHostController,
    startDestination: String = Screen.ProfileSetup.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Profile Setup Screen
        composable(Screen.ProfileSetup.route) {
            com.caddypro.app.ui.profile.ProfileSetupScreen(
                onProfileCreated = {
                    navController.navigate(Screen.BagList.route) {
                        // Clear back stack so user can't go back to setup
                        popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }

        // Bag List Screen
        composable(Screen.BagList.route) {
            com.caddypro.app.ui.bags.BagListScreen(
                onNavigateToClubEditor = { bagId ->
                    navController.navigate(Screen.ClubEditor.createRoute(bagId))
                }
            )
        }

        // Club Editor Screen
        composable(
            route = Screen.ClubEditor.route,
            arguments = listOf(
                navArgument("bagId") { type = NavType.StringType }
            )
        ) {
            ClubListScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // Round Start Screen
        composable(Screen.RoundStart.route) {
            RoundStartScreen(
                onNavigateBack = { navController.navigateUp() },
                onRoundStarted = {
                    navController.navigate(Screen.ShotLogger.createRoute("active")) {
                        popUpTo(Screen.RoundStart.route) { inclusive = true }
                    }
                }
            )
        }

        // Shot Logger Screen (active round)
        composable(
            route = Screen.ShotLogger.route,
            arguments = listOf(
                navArgument("roundId") { type = NavType.StringType }
            )
        ) {
            ShotLoggerScreen(
                onRoundEnded = { roundId ->
                    navController.navigate(Screen.RoundSummary.createRoute(roundId)) {
                        popUpTo(Screen.RoundStart.route) { inclusive = true }
                    }
                },
                onNavigateToMap = {
                    navController.navigate(Screen.HoleMap.route)
                }
            )
        }

        // Round Summary Screen
        composable(
            route = Screen.RoundSummary.route,
            arguments = listOf(
                navArgument("roundId") { type = NavType.StringType }
            )
        ) {
            RoundSummaryScreen(
                onNavigateHome = {
                    navController.navigate(Screen.BagList.route) {
                        popUpTo(Screen.RoundSummary.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ForecasterHud.route) {
            ForecasterScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.HoleMap.route) {
            HoleMapScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        composable(Screen.Settings.route) {
            PlaceholderScreen(title = "Settings")
        }
    }
}

/**
 * Placeholder screen for navigation testing
 * Will be replaced with actual screens in subsequent tasks
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
