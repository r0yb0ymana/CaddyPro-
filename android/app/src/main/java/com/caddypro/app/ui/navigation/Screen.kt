package com.caddypro.app.ui.navigation

/**
 * Sealed class representing all navigation destinations in the app
 *
 * Routes are defined according to the spec in r1-player-profile-bag.md
 */
sealed class Screen(val route: String) {

    // Profile & Bag Management Screens
    data object ProfileSetup : Screen("profile/setup")
    data object BagList : Screen("profile/bags")
    data object ClubEditor : Screen("profile/bags/{bagId}/clubs") {
        fun createRoute(bagId: String) = "profile/bags/$bagId/clubs"
    }

    // Shot Logger screens
    data object RoundStart : Screen("round/start")
    data object ShotLogger : Screen("round/{roundId}/log") {
        fun createRoute(roundId: String) = "round/$roundId/log"
    }
    data object RoundSummary : Screen("round/{roundId}/summary") {
        fun createRoute(roundId: String) = "round/$roundId/summary"
    }

    // Forecaster & Map
    data object ForecasterHud : Screen("forecaster-hud")
    data object HoleMap : Screen("hole-map")

    // Settings
    data object Settings : Screen("settings")
}
