package caddypro.domain.navcaddy.navigation

import caddypro.domain.navcaddy.models.Module
import caddypro.domain.navcaddy.models.RoutingTarget

/**
 * Sealed class representing all navigable destinations in the NavCaddy system.
 *
 * Each destination corresponds to a specific screen within a module and holds
 * the necessary parameters for that screen. Destinations generate route strings
 * compatible with Jetpack Navigation Compose.
 *
 * Spec reference: navcaddy-engine.md R3, navcaddy-engine-plan.md Task 11
 */
sealed class NavCaddyDestination(
    val module: Module,
    val screen: String
) {
    /**
     * Generate the route string for Navigation Compose.
     * Base implementation returns "module/screen", subclasses add parameters.
     */
    abstract fun toRoute(): String

    // CADDY Module Destinations

    /**
     * Club adjustment screen with optional preselected club.
     * @property clubId Optional club identifier to preselect (e.g., "7i", "PW")
     */
    data class ClubAdjustment(
        val clubId: String? = null
    ) : NavCaddyDestination(Module.CADDY, "club_adjustment") {
        override fun toRoute(): String {
            return if (clubId != null) {
                "caddy/club_adjustment?clubId=$clubId"
            } else {
                "caddy/club_adjustment"
            }
        }
    }

    /**
     * Shot recommendation screen with optional contextual parameters.
     * @property yardage Distance to target
     * @property lie Ball lie type (fairway, rough, sand, etc.)
     * @property wind Wind conditions
     */
    data class ShotRecommendation(
        val yardage: Int? = null,
        val lie: String? = null,
        val wind: String? = null
    ) : NavCaddyDestination(Module.CADDY, "shot_recommendation") {
        override fun toRoute(): String {
            val params = mutableListOf<String>()
            yardage?.let { params.add("yardage=$it") }
            lie?.let { params.add("lie=$it") }
            wind?.let { params.add("wind=$it") }

            return if (params.isNotEmpty()) {
                "caddy/shot_recommendation?${params.joinToString("&")}"
            } else {
                "caddy/shot_recommendation"
            }
        }
    }

    /**
     * Live Caddy HUD screen for on-course strategy.
     *
     * Main interface for Live Caddy Mode with:
     * - Forecaster HUD (weather integration)
     * - BodyCaddy (readiness scoring)
     * - PinSeeker AI Map (hole strategy)
     * - Real-time shot logger
     *
     * Spec reference: live-caddy-mode.md R1-R7, live-caddy-mode-plan.md Task 23
     */
    data object LiveCaddy : NavCaddyDestination(Module.CADDY, "live_caddy") {
        override fun toRoute(): String = "caddy/live_caddy"
    }

    /**
     * Round start screen to initialize a new round.
     * @property courseName Optional course name to preselect
     */
    data class RoundStart(
        val courseName: String? = null
    ) : NavCaddyDestination(Module.CADDY, "round_start") {
        override fun toRoute(): String {
            return if (courseName != null) {
                "caddy/round_start?course=${courseName.encodeUrl()}"
            } else {
                "caddy/round_start"
            }
        }
    }

    /**
     * Score entry screen for current hole.
     * @property hole Hole number to enter score for
     */
    data class ScoreEntry(
        val hole: Int? = null
    ) : NavCaddyDestination(Module.CADDY, "score_entry") {
        override fun toRoute(): String {
            return if (hole != null) {
                "caddy/score_entry?hole=$hole"
            } else {
                "caddy/score_entry"
            }
        }
    }

    /**
     * Round end summary screen to finalize and review round.
     * @property roundId Round identifier to display summary for
     */
    data class RoundEndSummary(
        val roundId: Long
    ) : NavCaddyDestination(Module.CADDY, "round_end_summary") {
        override fun toRoute(): String = "caddy/round_end_summary/$roundId"

        companion object {
            /**
             * Route pattern for navigation graph definition.
             * Use this in NavHost composable() route parameter.
             */
            const val ROUTE_PATTERN = "caddy/round_end_summary/{roundId}"

            /**
             * Navigation argument key for round ID.
             */
            const val ARG_ROUND_ID = "roundId"
        }
    }

    /**
     * Round end screen to finalize round.
     * Deprecated: Use RoundEndSummary instead.
     */
    @Deprecated(
        message = "Use RoundEndSummary for better round context",
        replaceWith = ReplaceWith("RoundEndSummary(roundId)")
    )
    data object RoundEnd : NavCaddyDestination(Module.CADDY, "round_end") {
        override fun toRoute(): String = "caddy/round_end"
    }

    /**
     * Weather check screen for course conditions.
     */
    data object WeatherCheck : NavCaddyDestination(Module.CADDY, "weather") {
        override fun toRoute(): String = "caddy/weather"
    }

    /**
     * Stats lookup screen for performance metrics.
     * @property statType Optional stat category to display (e.g., "driving", "putting")
     */
    data class StatsLookup(
        val statType: String? = null
    ) : NavCaddyDestination(Module.CADDY, "stats") {
        override fun toRoute(): String {
            return if (statType != null) {
                "caddy/stats?type=$statType"
            } else {
                "caddy/stats"
            }
        }
    }

    /**
     * Course information screen.
     * @property courseId Course identifier
     */
    data class CourseInfo(
        val courseId: String
    ) : NavCaddyDestination(Module.CADDY, "course_info") {
        override fun toRoute(): String {
            return "caddy/course_info?id=$courseId"
        }
    }

    // COACH Module Destinations

    /**
     * Drill screen with optional preselected drill.
     * @property drillId Optional drill identifier
     * @property focusArea Optional focus area (e.g., "driver", "putting", "short_game")
     */
    data class DrillScreen(
        val drillId: String? = null,
        val focusArea: String? = null
    ) : NavCaddyDestination(Module.COACH, "drill") {
        override fun toRoute(): String {
            val params = mutableListOf<String>()
            drillId?.let { params.add("drillId=$it") }
            focusArea?.let { params.add("focusArea=$it") }

            return if (params.isNotEmpty()) {
                "coach/drill?${params.joinToString("&")}"
            } else {
                "coach/drill"
            }
        }
    }

    /**
     * Practice session screen.
     */
    data object PracticeSession : NavCaddyDestination(Module.COACH, "practice") {
        override fun toRoute(): String = "coach/practice"
    }

    // RECOVERY Module Destinations

    /**
     * Recovery overview screen showing current readiness.
     */
    data object RecoveryOverview : NavCaddyDestination(Module.RECOVERY, "overview") {
        override fun toRoute(): String = "recovery/overview"
    }

    /**
     * Recovery data entry screen.
     * @property dataType Type of data to enter (e.g., "sleep", "hrv", "readiness")
     */
    data class RecoveryDataEntry(
        val dataType: String? = null
    ) : NavCaddyDestination(Module.RECOVERY, "data_entry") {
        override fun toRoute(): String {
            return if (dataType != null) {
                "recovery/data_entry?type=$dataType"
            } else {
                "recovery/data_entry"
            }
        }
    }

    // SETTINGS Module Destinations

    /**
     * Equipment/bag management screen.
     */
    data object EquipmentManagement : NavCaddyDestination(Module.SETTINGS, "equipment") {
        override fun toRoute(): String = "settings/equipment"
    }

    /**
     * Specific settings screen.
     * @property settingKey Setting to navigate to (e.g., "profile", "notifications", "privacy")
     */
    data class SettingsScreen(
        val settingKey: String? = null
    ) : NavCaddyDestination(Module.SETTINGS, "settings") {
        override fun toRoute(): String {
            return if (settingKey != null) {
                "settings/settings?key=$settingKey"
            } else {
                "settings/settings"
            }
        }
    }

    /**
     * Help and support screen.
     */
    data object HelpScreen : NavCaddyDestination(Module.SETTINGS, "help") {
        override fun toRoute(): String = "settings/help"
    }

    companion object {
        /**
         * Convert a RoutingTarget to a NavCaddyDestination.
         *
         * Maps the module, screen, and parameters from the intent routing system
         * to the appropriate destination type.
         */
        fun fromRoutingTarget(target: RoutingTarget): NavCaddyDestination {
            return when (target.module) {
                Module.CADDY -> when (target.screen) {
                    "club_adjustment" -> ClubAdjustment(
                        clubId = target.parameters["clubId"] as? String
                    )
                    "shot_recommendation" -> ShotRecommendation(
                        yardage = (target.parameters["yardage"] as? Number)?.toInt(),
                        lie = target.parameters["lie"] as? String,
                        wind = target.parameters["wind"] as? String
                    )
                    "live_caddy" -> LiveCaddy
                    "round_start" -> RoundStart(
                        courseName = target.parameters["courseName"] as? String
                    )
                    "score_entry" -> ScoreEntry(
                        hole = (target.parameters["hole"] as? Number)?.toInt()
                    )
                    "round_end_summary" -> RoundEndSummary(
                        roundId = (target.parameters["roundId"] as? Number)?.toLong()
                            ?: error("roundId required for RoundEndSummary destination")
                    )
                    "round_end" -> RoundEnd
                    "weather" -> WeatherCheck
                    "stats" -> StatsLookup(
                        statType = target.parameters["statType"] as? String
                    )
                    "course_info" -> CourseInfo(
                        courseId = target.parameters["courseId"] as? String
                            ?: error("courseId required for CourseInfo destination")
                    )
                    else -> error("Unknown CADDY screen: ${target.screen}")
                }

                Module.COACH -> when (target.screen) {
                    "drill" -> DrillScreen(
                        drillId = target.parameters["drillId"] as? String,
                        focusArea = target.parameters["focusArea"] as? String
                    )
                    "practice" -> PracticeSession
                    else -> error("Unknown COACH screen: ${target.screen}")
                }

                Module.RECOVERY -> when (target.screen) {
                    "overview" -> RecoveryOverview
                    "data_entry" -> RecoveryDataEntry(
                        dataType = target.parameters["dataType"] as? String
                    )
                    else -> error("Unknown RECOVERY screen: ${target.screen}")
                }

                Module.SETTINGS -> when (target.screen) {
                    "equipment" -> EquipmentManagement
                    "settings" -> SettingsScreen(
                        settingKey = target.parameters["settingKey"] as? String
                    )
                    "help" -> HelpScreen
                    else -> error("Unknown SETTINGS screen: ${target.screen}")
                }
            }
        }
    }
}

/**
 * URL encode a string for safe use in navigation routes.
 * Uses proper URL encoding for all special characters.
 */
private fun String.encodeUrl(): String {
    return java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
        .replace("+", "%20") // URLEncoder uses + for spaces, we want %20
}
