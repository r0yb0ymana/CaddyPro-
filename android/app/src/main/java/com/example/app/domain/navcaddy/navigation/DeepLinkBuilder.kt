package caddypro.domain.navcaddy.navigation

import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.Module
import caddypro.domain.navcaddy.models.RoutingTarget
import javax.inject.Inject

/**
 * Builds navigation routes from RoutingTargets.
 *
 * Converts the generic RoutingTarget (from intent classification) into
 * a concrete NavCaddyDestination with properly typed parameters.
 *
 * Validates target before building to ensure all required parameters are present.
 *
 * Spec reference: navcaddy-engine.md R3, navcaddy-engine-plan.md Task 11
 */
class DeepLinkBuilder @Inject constructor() {

    /**
     * Build a navigation route string from a RoutingTarget.
     *
     * @param target The routing target from intent classification
     * @return Navigation route string for NavController
     * @throws IllegalArgumentException if target is invalid or missing required parameters
     */
    fun buildRoute(target: RoutingTarget): String {
        validateTarget(target)
        val destination = buildDestination(target)
        return destination.toRoute()
    }

    /**
     * Build a typed NavCaddyDestination from a RoutingTarget.
     *
     * @param target The routing target from intent classification
     * @return Typed destination with parameters
     * @throws IllegalArgumentException if screen name is unknown
     */
    fun buildDestination(target: RoutingTarget): NavCaddyDestination {
        return when (target.module) {
            Module.CADDY -> buildCaddyDestination(target)
            Module.COACH -> buildCoachDestination(target)
            Module.RECOVERY -> buildRecoveryDestination(target)
            Module.SETTINGS -> buildSettingsDestination(target)
        }
    }

    private fun buildCaddyDestination(target: RoutingTarget): NavCaddyDestination {
        return when (target.screen) {
            "club_adjustment" -> NavCaddyDestination.ClubAdjustment(
                clubId = target.parameters["clubId"] as? String
            )

            "shot_recommendation" -> NavCaddyDestination.ShotRecommendation(
                yardage = (target.parameters["yardage"] as? Number)?.toInt(),
                lie = target.parameters["lie"] as? String,
                wind = target.parameters["wind"] as? String
            )

            "round_start" -> NavCaddyDestination.RoundStart(
                courseName = target.parameters["courseName"] as? String
            )

            "score_entry" -> NavCaddyDestination.ScoreEntry(
                hole = (target.parameters["hole"] as? Number)?.toInt()
            )

            "round_end" -> NavCaddyDestination.RoundEnd

            "weather" -> NavCaddyDestination.WeatherCheck

            "stats" -> NavCaddyDestination.StatsLookup(
                statType = target.parameters["statType"] as? String
            )

            "course_info" -> {
                val courseId = target.parameters["courseId"] as? String
                    ?: throw IllegalArgumentException("CourseInfo requires courseId parameter")
                NavCaddyDestination.CourseInfo(courseId)
            }

            else -> throw IllegalArgumentException("Unknown CADDY screen: ${target.screen}")
        }
    }

    private fun buildCoachDestination(target: RoutingTarget): NavCaddyDestination {
        return when (target.screen) {
            "drill" -> NavCaddyDestination.DrillScreen(
                drillId = target.parameters["drillId"] as? String,
                focusArea = target.parameters["focusArea"] as? String
            )

            "practice" -> NavCaddyDestination.PracticeSession

            else -> throw IllegalArgumentException("Unknown COACH screen: ${target.screen}")
        }
    }

    private fun buildRecoveryDestination(target: RoutingTarget): NavCaddyDestination {
        return when (target.screen) {
            "overview" -> NavCaddyDestination.RecoveryOverview

            "data_entry" -> NavCaddyDestination.RecoveryDataEntry(
                dataType = target.parameters["dataType"] as? String
            )

            else -> throw IllegalArgumentException("Unknown RECOVERY screen: ${target.screen}")
        }
    }

    private fun buildSettingsDestination(target: RoutingTarget): NavCaddyDestination {
        return when (target.screen) {
            "equipment" -> NavCaddyDestination.EquipmentManagement

            "settings" -> NavCaddyDestination.SettingsScreen(
                settingKey = target.parameters["settingKey"] as? String
            )

            "help" -> NavCaddyDestination.HelpScreen

            else -> throw IllegalArgumentException("Unknown SETTINGS screen: ${target.screen}")
        }
    }

    /**
     * Validate that target has valid module and screen.
     * Throws IllegalArgumentException if invalid.
     */
    private fun validateTarget(target: RoutingTarget) {
        require(target.screen.isNotBlank()) {
            "RoutingTarget must have a non-blank screen name"
        }
    }

    companion object {
        /**
         * Quick helper to validate if a target is buildable without throwing.
         * Useful for testing/debugging.
         */
        fun canBuild(target: RoutingTarget): Boolean {
            return try {
                DeepLinkBuilder().buildDestination(target)
                true
            } catch (e: IllegalArgumentException) {
                false
            }
        }
    }
}
