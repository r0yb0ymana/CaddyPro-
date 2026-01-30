package caddypro.data.caddy.local

import caddypro.domain.caddy.models.CourseHole
import caddypro.domain.caddy.models.HazardLocation
import caddypro.domain.caddy.models.HazardType
import caddypro.domain.caddy.models.HazardZone
import caddypro.domain.navcaddy.models.MissDirection

/**
 * Placeholder course data for MVP testing and development.
 *
 * Provides 3 sample holes demonstrating different strategic scenarios:
 * - Hole 3: Simple par 3 with minimal hazards
 * - Hole 7: Par 4 with water right punishing slices
 * - Hole 14: Par 5 with OB left punishing hooks
 *
 * Future: Replace with integration to course data providers.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 4
 */
object PlaceholderCourseData {

    /**
     * Returns a list of sample holes for testing and development.
     */
    fun getSampleHoles(): List<CourseHole> = listOf(
        hole3_SimplePar3(),
        hole7_Par4WaterRight(),
        hole14_Par5OBLeft()
    )

    /**
     * Returns a specific hole by number, or null if not found.
     */
    fun getHoleByNumber(number: Int): CourseHole? {
        return getSampleHoles().find { it.number == number }
    }

    /**
     * Hole 3: Simple par 3 with minimal hazards.
     *
     * Strategic considerations:
     * - Short par 3 at 150 meters
     * - Front bunker guards short misses
     * - Relatively safe green with room for error
     * - Good birdie opportunity for mid-handicappers
     */
    private fun hole3_SimplePar3() = CourseHole(
        number = 3,
        par = 3,
        lengthMeters = 150,
        hazards = listOf(
            HazardZone(
                type = HazardType.BUNKER,
                location = HazardLocation(
                    side = "short",
                    distanceFromTee = 135..145
                ),
                penaltyStrokes = 0.3,
                affectedMisses = listOf(MissDirection.THIN, MissDirection.FAT)
            )
        ),
        pinPosition = null // Unknown for MVP
    )

    /**
     * Hole 7: Par 4 with water right (punishes slices).
     *
     * Strategic considerations:
     * - Classic risk-reward par 4 at 360 meters
     * - Water hazard runs right side from 200-330 meters
     * - Right-side bunker at 180 meters protects aggressive lines
     * - Slicers must play conservative left-center
     * - Long hitters can challenge the water for short approach
     */
    private fun hole7_Par4WaterRight() = CourseHole(
        number = 7,
        par = 4,
        lengthMeters = 360,
        hazards = listOf(
            HazardZone(
                type = HazardType.WATER,
                location = HazardLocation(
                    side = "right",
                    distanceFromTee = 200..330
                ),
                penaltyStrokes = 1.0,
                affectedMisses = listOf(
                    MissDirection.SLICE,
                    MissDirection.PUSH
                )
            ),
            HazardZone(
                type = HazardType.BUNKER,
                location = HazardLocation(
                    side = "right",
                    distanceFromTee = 175..185
                ),
                penaltyStrokes = 0.4,
                affectedMisses = listOf(
                    MissDirection.SLICE,
                    MissDirection.PUSH
                )
            ),
            HazardZone(
                type = HazardType.BUNKER,
                location = HazardLocation(
                    side = "long",
                    distanceFromTee = 355..365
                ),
                penaltyStrokes = 0.3,
                affectedMisses = listOf(MissDirection.STRAIGHT)
            )
        ),
        pinPosition = null
    )

    /**
     * Hole 14: Par 5 with OB left (punishes hooks).
     *
     * Strategic considerations:
     * - Long par 5 at 520 meters requiring 3 shots for most players
     * - OB left from 180-400 meters (penalty area with trees)
     * - Fairway bunker right at 240 meters (safe but blocks green approach)
     * - Deep penalty rough left at 380-420 meters guards layup zone
     * - Hookers must favor right side or risk severe penalty
     * - Three-shot strategy recommended for mid-handicappers
     */
    private fun hole14_Par5OBLeft() = CourseHole(
        number = 14,
        par = 5,
        lengthMeters = 520,
        hazards = listOf(
            HazardZone(
                type = HazardType.OB,
                location = HazardLocation(
                    side = "left",
                    distanceFromTee = 180..400
                ),
                penaltyStrokes = 2.0, // Stroke and distance
                affectedMisses = listOf(
                    MissDirection.HOOK,
                    MissDirection.PULL
                )
            ),
            HazardZone(
                type = HazardType.BUNKER,
                location = HazardLocation(
                    side = "right",
                    distanceFromTee = 235..245
                ),
                penaltyStrokes = 0.5,
                affectedMisses = listOf(
                    MissDirection.SLICE,
                    MissDirection.PUSH
                )
            ),
            HazardZone(
                type = HazardType.PENALTY_ROUGH,
                location = HazardLocation(
                    side = "left",
                    distanceFromTee = 380..420
                ),
                penaltyStrokes = 0.7,
                affectedMisses = listOf(
                    MissDirection.HOOK,
                    MissDirection.PULL
                )
            ),
            HazardZone(
                type = HazardType.TREES,
                location = HazardLocation(
                    side = "right",
                    distanceFromTee = 400..520
                ),
                penaltyStrokes = 0.8,
                affectedMisses = listOf(
                    MissDirection.SLICE,
                    MissDirection.PUSH
                )
            )
        ),
        pinPosition = null
    )
}
