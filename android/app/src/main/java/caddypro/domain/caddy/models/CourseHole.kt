package caddypro.domain.caddy.models

/**
 * Domain model representing a golf hole with hazard data.
 *
 * This is a placeholder for MVP - future versions will integrate with
 * course data providers (GolfNow, GHIN, manual import).
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 4
 *
 * @property number Hole number (1-18)
 * @property par Par rating for the hole (3-5)
 * @property lengthMeters Total length from back tees in meters
 * @property hazards List of hazard zones on this hole
 * @property pinPosition Current pin placement if known (null for MVP)
 */
data class CourseHole(
    val number: Int,
    val par: Int,
    val lengthMeters: Int,
    val hazards: List<HazardZone>,
    val pinPosition: PinPosition?
) {
    init {
        require(number in 1..18) {
            "Invalid hole number: $number. Must be between 1 and 18"
        }
        require(par in 3..5) {
            "Invalid par: $par. Must be between 3 and 5"
        }
        require(lengthMeters > 0) {
            "Invalid length: $lengthMeters. Must be greater than 0 meters"
        }
    }
}

/**
 * Represents pin position on the green using a thirds grid.
 *
 * Divides the green into 9 zones (front/middle/back × left/center/right).
 * Multiple flags can be true if pin is on a border.
 *
 * @property front Pin is in front third of green depth
 * @property middle Pin is in middle third of green depth
 * @property back Pin is in back third of green depth
 * @property left Pin is in left third of green width
 * @property center Pin is in center third of green width
 * @property right Pin is in right third of green width
 */
data class PinPosition(
    val front: Boolean,
    val middle: Boolean,
    val back: Boolean,
    val left: Boolean,
    val center: Boolean,
    val right: Boolean
) {
    init {
        require(front || middle || back) {
            "At least one depth flag (front/middle/back) must be true"
        }
        require(left || center || right) {
            "At least one width flag (left/center/right) must be true"
        }
    }

    /**
     * Returns a human-readable description of the pin position.
     *
     * Examples: "Front left", "Middle center", "Back right"
     */
    fun describe(): String {
        val depthPart = when {
            front -> "Front"
            middle -> "Middle"
            back -> "Back"
            else -> "Unknown"
        }
        val widthPart = when {
            left -> "left"
            center -> "center"
            right -> "right"
            else -> "unknown"
        }
        return "$depthPart $widthPart"
    }
}
