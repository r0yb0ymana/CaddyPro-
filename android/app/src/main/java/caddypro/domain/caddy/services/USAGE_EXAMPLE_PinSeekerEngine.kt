package caddypro.domain.caddy.services

import caddypro.domain.caddy.models.CourseHole
import caddypro.domain.caddy.models.HazardLocation
import caddypro.domain.caddy.models.HazardType
import caddypro.domain.caddy.models.HazardZone
import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import caddypro.domain.navcaddy.models.MissDirection

/**
 * Usage example for PinSeekerEngine.
 *
 * Demonstrates how to use the strategy engine to compute personalized hole strategies
 * based on player profile, readiness, and hole geometry.
 *
 * This is NOT production code - it's a reference for developers.
 */
@Suppress("unused")
object PinSeekerEngineUsageExample {

    /**
     * Example: Compute strategy for a par 4 with water right.
     *
     * Scenario:
     * - 9 handicap player with dominant slice
     * - Low readiness (40) from poor sleep
     * - Par 4, 360m with water hazard on the right
     *
     * Expected outcome:
     * - Identifies water right as danger zone (affected by slice)
     * - Aims left to compensate for slice
     * - Doubles safety margin due to low readiness (36m vs 18m)
     * - Generates risk callout about slice bringing water into play
     */
    suspend fun exampleSlicePlayerWithWaterRight(engine: PinSeekerEngine) {
        // Setup: Define the hole
        val hole = CourseHole(
            number = 5,
            par = 4,
            lengthMeters = 360,
            hazards = listOf(
                HazardZone(
                    type = HazardType.WATER,
                    location = HazardLocation("right", 200..320),
                    penaltyStrokes = 1.0,
                    affectedMisses = listOf(MissDirection.SLICE, MissDirection.PUSH)
                ),
                HazardZone(
                    type = HazardType.BUNKER,
                    location = HazardLocation("left", 180..200),
                    penaltyStrokes = 0.5,
                    affectedMisses = listOf(MissDirection.HOOK, MissDirection.PULL)
                )
            ),
            pinPosition = null
        )

        // Setup: Player readiness (low due to poor sleep)
        val readiness = ReadinessScore(
            overall = 40,
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(35.0, 0.4),
                sleepQuality = MetricScore(30.0, 0.4),  // Poor sleep
                stressLevel = MetricScore(55.0, 0.2)
            ),
            timestamp = System.currentTimeMillis(),
            source = ReadinessSource.WEARABLE_SYNC
        )

        // Setup: Player profile
        val handicap = 9

        // Compute strategy
        val strategy: HoleStrategy = engine.computeStrategy(hole, handicap, readiness)

        // Results:
        // strategy.dangerZones = [water right]  (bunker left filtered out - not affected by slice)
        // strategy.recommendedLandingZone.targetLine = 170  (10 degrees left of center)
        // strategy.recommendedLandingZone.idealDistance = 252m  (70% of 360m)
        // strategy.recommendedLandingZone.safetyMargin = 36m  (doubled due to low readiness)
        // strategy.recommendedLandingZone.visualCue = "Aim left side of fairway - your slice brings right hazards into play"
        // strategy.riskCallouts = ["Right miss brings water into play - Slice tendency increases risk"]
        // strategy.personalizedFor.dominantMiss = SLICE
        // strategy.personalizedFor.readinessScore = 40

        println("Strategy for Hole ${strategy.holeNumber}:")
        println("  Danger zones: ${strategy.dangerZones.size}")
        println("  Target line: ${strategy.recommendedLandingZone.targetLine}°")
        println("  Ideal distance: ${strategy.recommendedLandingZone.idealDistance}m")
        println("  Safety margin: ${strategy.recommendedLandingZone.safetyMargin}m")
        println("  Visual cue: ${strategy.recommendedLandingZone.visualCue}")
        println("  Risk callouts:")
        strategy.riskCallouts.forEach { callout ->
            println("    - $callout")
        }
        println("  Personalized for:")
        println("    - Handicap: ${strategy.personalizedFor.handicap}")
        println("    - Dominant miss: ${strategy.personalizedFor.dominantMiss}")
        println("    - Readiness: ${strategy.personalizedFor.readinessScore}")
    }

    /**
     * Example: Compute strategy for a hook player on par 3.
     *
     * Scenario:
     * - 15 handicap player with dominant hook
     * - High readiness (80) - well rested
     * - Par 3, 150m with bunkers left
     *
     * Expected outcome:
     * - Identifies left bunkers as danger zones
     * - Aims right to compensate for hook
     * - Normal safety margin due to high readiness
     * - Suggests aiming center-right of green
     */
    suspend fun exampleHookPlayerOnPar3(engine: PinSeekerEngine) {
        // Setup: Par 3 hole
        val hole = CourseHole(
            number = 12,
            par = 3,
            lengthMeters = 150,
            hazards = listOf(
                HazardZone(
                    type = HazardType.BUNKER,
                    location = HazardLocation("left", 140..160),
                    penaltyStrokes = 0.5,
                    affectedMisses = listOf(MissDirection.HOOK, MissDirection.PULL)
                )
            ),
            pinPosition = null
        )

        // Setup: High readiness
        val readiness = ReadinessScore(
            overall = 80,
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(75.0, 0.4),
                sleepQuality = MetricScore(85.0, 0.4),
                stressLevel = MetricScore(80.0, 0.2)
            ),
            timestamp = System.currentTimeMillis(),
            source = ReadinessSource.WEARABLE_SYNC
        )

        val handicap = 15

        // Compute strategy
        val strategy: HoleStrategy = engine.computeStrategy(hole, handicap, readiness)

        // Results:
        // strategy.dangerZones = [bunker left]
        // strategy.recommendedLandingZone.targetLine = 190  (10 degrees right of center)
        // strategy.recommendedLandingZone.idealDistance = 150m  (aim for green on par 3)
        // strategy.recommendedLandingZone.safetyMargin = 30m  (15 * 2, normal readiness factor)
        // strategy.recommendedLandingZone.visualCue = "Aim center-right of green to avoid left-side trouble"
        // strategy.riskCallouts = ["Left bunkers guard landing zone"]

        println("Strategy for Par 3 Hole ${strategy.holeNumber}:")
        println("  Target: ${strategy.recommendedLandingZone.visualCue}")
        println("  Distance: ${strategy.recommendedLandingZone.idealDistance}m")
        println("  Margin: ${strategy.recommendedLandingZone.safetyMargin}m")
    }

    /**
     * Example: Strategy with no miss pattern data.
     *
     * Scenario:
     * - New player with no historical data
     * - Medium readiness
     * - Par 4 with center bunker
     *
     * Expected outcome:
     * - Defaults to STRAIGHT (no bias)
     * - Conservative center-avoiding strategy
     * - Standard safety margin based on handicap
     */
    suspend fun exampleNewPlayerNoBias(engine: PinSeekerEngine) {
        val hole = CourseHole(
            number = 8,
            par = 4,
            lengthMeters = 380,
            hazards = listOf(
                HazardZone(
                    type = HazardType.BUNKER,
                    location = HazardLocation("center", 220..240),
                    penaltyStrokes = 0.5,
                    affectedMisses = listOf(
                        MissDirection.STRAIGHT,
                        MissDirection.SLICE,
                        MissDirection.HOOK
                    )
                )
            ),
            pinPosition = null
        )

        val readiness = ReadinessScore(
            overall = 60,
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(60.0, 0.4),
                sleepQuality = MetricScore(60.0, 0.4),
                stressLevel = MetricScore(60.0, 0.2)
            ),
            timestamp = System.currentTimeMillis(),
            source = ReadinessSource.WEARABLE_SYNC
        )

        val handicap = 12

        // Compute strategy
        val strategy: HoleStrategy = engine.computeStrategy(hole, handicap, readiness)

        // Results:
        // strategy.personalizedFor.dominantMiss = STRAIGHT  (default)
        // strategy.dangerZones includes center bunker
        // strategy.recommendedLandingZone.visualCue = "Play safe - avoid center hazard"

        println("Strategy for new player:")
        println("  Dominant miss: ${strategy.personalizedFor.dominantMiss}")
        println("  Visual cue: ${strategy.recommendedLandingZone.visualCue}")
    }
}
