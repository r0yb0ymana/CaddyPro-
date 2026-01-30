package caddypro.domain.caddy.models

import caddypro.domain.navcaddy.models.MissDirection

/**
 * Domain model representing a personalized hole strategy.
 *
 * Combines course hazards, player tendencies, and readiness to provide
 * hazard-aware landing zones and risk callouts.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 4
 *
 * @property holeNumber The hole number on the course (1-18)
 * @property dangerZones List of hazards that affect shot strategy
 * @property recommendedLandingZone Personalized target zone based on player profile
 * @property riskCallouts Human-readable warnings about hazards (e.g., "Right miss brings water into play")
 * @property personalizedFor Context used to generate this strategy
 */
data class HoleStrategy(
    val holeNumber: Int,
    val dangerZones: List<HazardZone>,
    val recommendedLandingZone: LandingZone,
    val riskCallouts: List<String>,
    val personalizedFor: PersonalizationContext
) {
    init {
        require(holeNumber in 1..18) {
            "Invalid hole number: $holeNumber. Must be between 1 and 18"
        }
    }
}

/**
 * Represents a hazard zone on the hole with penalty assessment.
 *
 * @property type Category of hazard (water, OB, etc.)
 * @property location Where the hazard is positioned relative to tee
 * @property penaltyStrokes Expected penalty for finding this hazard
 * @property affectedMisses Which miss patterns bring this hazard into play
 */
data class HazardZone(
    val type: HazardType,
    val location: HazardLocation,
    val penaltyStrokes: Double,
    val affectedMisses: List<MissDirection>
) {
    init {
        require(penaltyStrokes >= 0.0) {
            "Invalid penalty strokes: $penaltyStrokes. Must be non-negative"
        }
    }
}

/**
 * Types of course hazards that affect strategy.
 */
enum class HazardType {
    /** Water hazard (1 stroke penalty) */
    WATER,

    /** Out of bounds (stroke and distance) */
    OB,

    /** Sand bunker (difficult lie, no penalty) */
    BUNKER,

    /** Deep rough with significant penalty (e.g., native areas) */
    PENALTY_ROUGH,

    /** Trees or wooded areas */
    TREES
}

/**
 * Describes where a hazard is positioned on the hole.
 *
 * @property side Lateral position: "right", "left", "center", "long", "short"
 * @property distanceFromTee Range of distances where hazard is in play (meters)
 */
data class HazardLocation(
    val side: String,
    val distanceFromTee: IntRange
) {
    init {
        val validSides = setOf("right", "left", "center", "long", "short")
        require(side in validSides) {
            "Invalid side: $side. Must be one of: ${validSides.joinToString()}"
        }
        require(!distanceFromTee.isEmpty()) {
            "Distance range cannot be empty"
        }
        require(distanceFromTee.first >= 0) {
            "Distance range must start at 0 or greater"
        }
    }
}

/**
 * Recommended landing zone personalized to player profile and readiness.
 *
 * @property targetLine Bearing in degrees (0-359) for aim direction
 * @property idealDistance Target carry distance from tee in meters
 * @property safetyMargin Buffer zone in meters (increases with low readiness)
 * @property visualCue Human-readable aim instruction (e.g., "Aim 10 yards left of fairway bunker")
 */
data class LandingZone(
    val targetLine: Int,
    val idealDistance: Int,
    val safetyMargin: Int,
    val visualCue: String
) {
    init {
        require(targetLine in 0..359) {
            "Invalid target line: $targetLine. Must be between 0 and 359 degrees"
        }
        require(idealDistance > 0) {
            "Invalid ideal distance: $idealDistance. Must be greater than 0 meters"
        }
        require(safetyMargin >= 0) {
            "Invalid safety margin: $safetyMargin. Must be non-negative"
        }
        require(visualCue.isNotBlank()) {
            "Visual cue cannot be blank"
        }
    }
}

/**
 * Player profile context used for strategy personalization.
 *
 * @property handicap Player's USGA handicap index
 * @property dominantMiss Primary miss pattern from historical data
 * @property clubDistances Map of club names to carry distances in meters
 * @property readinessScore Current readiness score (0-100)
 */
data class PersonalizationContext(
    val handicap: Int,
    val dominantMiss: MissDirection,
    val clubDistances: Map<String, Int>,
    val readinessScore: Int
) {
    init {
        require(handicap >= 0) {
            "Invalid handicap: $handicap. Must be non-negative"
        }
        require(readinessScore in 0..100) {
            "Invalid readiness score: $readinessScore. Must be between 0 and 100"
        }
        clubDistances.forEach { (club, distance) ->
            require(distance > 0) {
                "Invalid distance for club $club: $distance. Must be greater than 0 meters"
            }
        }
    }
}
