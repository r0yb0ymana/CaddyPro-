package com.example.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.app.ui.screens.HomeScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { id ->
                    // navController.navigate(Screen.Detail.createRoute(id))
                }
            )
        }
        
        // Add more destinations here
        // composable(Screen.Detail.route) { backStackEntry ->
        //     val id = backStackEntry.arguments?.getString("id") ?: return@composable
        //     DetailScreen(id = id, onBack = { navController.popBackStack() })
        // }
    }
}

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Detail : Screen("detail/{id}") {
        fun createRoute(id: String) = "detail/$id"
    }
    // Add more screens here
}
