package caddypro.domain.caddy.services

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.caddy.models.CourseHole
import caddypro.domain.caddy.models.HazardZone
import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.LandingZone
import caddypro.domain.caddy.models.PersonalizationContext
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.navcaddy.models.MissDirection
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that computes hole strategy based on player profile and readiness.
 *
 * Analyzes hole geometry, hazards, player tendencies (handicap, miss bias, club distances),
 * and current readiness to provide personalized strategy recommendations.
 *
 * Core algorithm:
 * 1. Identify dominant miss pattern from historical data
 * 2. Filter hazards that affect player's dominant miss
 * 3. Compute safety margin based on readiness and handicap (lower readiness = bigger margins)
 * 4. Generate safe landing zone avoiding dominant miss
 * 5. Create risk callouts for relevant hazards
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 8
 * Acceptance criteria: A3 (Hazard-aware landing zone)
 *
 * @see HoleStrategy
 * @see ReadinessScore
 * @see CourseHole
 */
@Singleton
class PinSeekerEngine @Inject constructor(
    private val navCaddyRepository: NavCaddyRepository
) {

    /**
     * Compute personalized hole strategy.
     *
     * Combines hole data, player profile, and readiness to generate a strategy
     * that accounts for hazards, miss patterns, and current performance state.
     *
     * @param hole Course hole with geometry and hazards
     * @param handicap Player's USGA handicap index
     * @param readinessScore Current readiness score (impacts safety margins)
     * @return Personalized hole strategy with landing zones and risk callouts
     */
    suspend fun computeStrategy(
        hole: CourseHole,
        handicap: Int,
        readinessScore: ReadinessScore
    ): HoleStrategy {
        // Get dominant miss bias from repository
        val missPatterns = navCaddyRepository.getMissPatterns().firstOrNull() ?: emptyList()
        val dominantMiss = missPatterns
            .maxByOrNull { it.decayedConfidence(System.currentTimeMillis()) }
            ?.direction ?: MissDirection.STRAIGHT

        // Get club distances from active bag
        val activeBag = navCaddyRepository.getActiveBag().firstOrNull()
        val clubDistances = activeBag?.let { bag ->
            navCaddyRepository.getClubsForBag(bag.id).firstOrNull()
                ?.associate { it.name to it.estimatedCarry }
                ?: emptyMap()
        } ?: emptyMap()

        // Identify danger zones for this player's miss
        val personalizedHazards = hole.hazards.filter { hazard ->
            hazard.affectedMisses.contains(dominantMiss)
        }

        // Compute safe landing zone
        val safetyMargin = computeSafetyMargin(readinessScore, handicap)
        val landingZone = computeLandingZone(
            hole = hole,
            hazards = personalizedHazards,
            safetyMargin = safetyMargin,
            clubDistances = clubDistances,
            dominantMiss = dominantMiss
        )

        // Generate risk callouts
        val riskCallouts = generateRiskCallouts(dominantMiss, personalizedHazards)

        return HoleStrategy(
            holeNumber = hole.number,
            dangerZones = personalizedHazards,
            recommendedLandingZone = landingZone,
            riskCallouts = riskCallouts,
            personalizedFor = PersonalizationContext(
                handicap = handicap,
                dominantMiss = dominantMiss,
                clubDistances = clubDistances,
                readinessScore = readinessScore.overall
            )
        )
    }

    /**
     * Compute safety margin based on readiness and handicap.
     *
     * Lower readiness increases margins for more conservative play.
     * Higher handicap gets more conservative base margins.
     *
     * Algorithm:
     * - Base margin = handicap * 2 (e.g., 18m for 9 handicap)
     * - Readiness factor = 0.5 to 1.0 (from ReadinessScore.adjustmentFactor())
     * - Final margin = base / readiness factor
     *
     * Example:
     * - 9 handicap, 80 readiness (factor=1.0): margin = 18m
     * - 9 handicap, 40 readiness (factor=0.5): margin = 36m (doubled)
     *
     * @param readiness Current readiness score
     * @param handicap Player's handicap index
     * @return Safety margin in meters
     */
    private fun computeSafetyMargin(readiness: ReadinessScore, handicap: Int): Int {
        // Low readiness → bigger margins (more conservative)
        val readinessFactor = readiness.adjustmentFactor()  // 0.5-1.0
        val handicapMargin = handicap * 2  // ~18m for 9 handicap
        return (handicapMargin / readinessFactor).toInt()
    }

    /**
     * Compute recommended landing zone avoiding hazards and player's dominant miss.
     *
     * For MVP, uses simplified geometry:
     * - Target line compensates away from dominant miss side
     * - Ideal distance aims for center of fairway zone
     * - Visual cue provides actionable aim point
     *
     * @param hole Course hole data
     * @param hazards Personalized hazards that affect this player
     * @param safetyMargin Buffer zone in meters
     * @param clubDistances Map of club names to carry distances
     * @param dominantMiss Player's dominant miss pattern
     * @return Landing zone with target line and visual cue
     */
    private fun computeLandingZone(
        hole: CourseHole,
        hazards: List<HazardZone>,
        safetyMargin: Int,
        clubDistances: Map<String, Int>,
        dominantMiss: MissDirection
    ): LandingZone {
        // Determine safe target line based on dominant miss
        // Aim away from miss side (e.g., slice right → aim left)
        val targetLineBias = when (dominantMiss) {
            MissDirection.SLICE, MissDirection.PUSH -> -10  // Aim left
            MissDirection.HOOK, MissDirection.PULL -> 10    // Aim right
            else -> 0  // Straight
        }

        // Center of fairway as baseline (180 degrees = straight ahead in simplified model)
        val targetLine = (180 + targetLineBias).coerceIn(0, 359)

        // Ideal distance: conservative target (70% of hole length for par 4/5, green for par 3)
        val idealDistance = when (hole.par) {
            3 -> hole.lengthMeters  // Aim for green on par 3
            else -> (hole.lengthMeters * 0.7).toInt()  // Layup position on par 4/5
        }

        // Generate visual cue based on miss pattern and hazards
        val visualCue = buildVisualCue(dominantMiss, hazards, hole.par)

        return LandingZone(
            targetLine = targetLine,
            idealDistance = idealDistance,
            safetyMargin = safetyMargin,
            visualCue = visualCue
        )
    }

    /**
     * Build actionable visual aim cue for the player.
     *
     * Provides clear instruction accounting for miss pattern and hazards.
     *
     * @param dominantMiss Player's dominant miss direction
     * @param hazards Relevant hazards for this player
     * @param par Hole par rating
     * @return Human-readable aim instruction
     */
    private fun buildVisualCue(
        dominantMiss: MissDirection,
        hazards: List<HazardZone>,
        par: Int
    ): String {
        return when {
            // Par 3: focus on avoiding trouble around green
            par == 3 && hazards.any { it.location.side == "right" } ->
                "Aim center-left of green to avoid right-side trouble"

            par == 3 && hazards.any { it.location.side == "left" } ->
                "Aim center-right of green to avoid left-side trouble"

            par == 3 -> "Aim center of green"

            // Par 4/5: layup strategy
            dominantMiss == MissDirection.SLICE && hazards.any { it.location.side == "right" } ->
                "Aim left side of fairway - your slice brings right hazards into play"

            dominantMiss == MissDirection.HOOK && hazards.any { it.location.side == "left" } ->
                "Aim right side of fairway - your hook brings left hazards into play"

            dominantMiss == MissDirection.SLICE ->
                "Aim left third of fairway to counter slice"

            dominantMiss == MissDirection.HOOK ->
                "Aim right third of fairway to counter hook"

            hazards.any { it.location.side == "center" } ->
                "Play safe - avoid center hazard"

            else -> "Aim center of fairway"
        }
    }

    /**
     * Generate risk callouts for relevant hazards.
     *
     * Produces 1-3 callouts highlighting dangers specific to player's miss pattern.
     *
     * @param dominantMiss Player's dominant miss direction
     * @param hazards Personalized hazards that affect this player
     * @return List of risk callout strings
     */
    private fun generateRiskCallouts(
        dominantMiss: MissDirection,
        hazards: List<HazardZone>
    ): List<String> {
        val callouts = mutableListOf<String>()

        // Group hazards by type for prioritized messaging
        val waterHazards = hazards.filter { it.type.name == "WATER" }
        val obHazards = hazards.filter { it.type.name == "OB" }
        val bunkers = hazards.filter { it.type.name == "BUNKER" }

        // Prioritize high-penalty hazards
        waterHazards.forEach { hazard ->
            val missName = getMissName(dominantMiss)
            callouts.add("${hazard.location.side.capitalizeFirstChar()} miss brings water into play - $missName tendency increases risk")
        }

        obHazards.forEach { hazard ->
            val missName = getMissName(dominantMiss)
            callouts.add("OB ${hazard.location.side} - $missName pattern puts this in play")
        }

        // Add bunker warnings if fewer than 3 callouts so far
        if (callouts.size < 3 && bunkers.isNotEmpty()) {
            bunkers.take(3 - callouts.size).forEach { hazard ->
                callouts.add("${hazard.location.side.capitalizeFirstChar()} bunkers guard landing zone")
            }
        }

        return callouts.take(3)  // Max 3 callouts per spec
    }

    /**
     * Convert MissDirection to human-readable name.
     *
     * @param miss Miss direction enum
     * @return Display name
     */
    private fun getMissName(miss: MissDirection): String = when (miss) {
        MissDirection.SLICE -> "Slice"
        MissDirection.HOOK -> "Hook"
        MissDirection.PUSH -> "Push"
        MissDirection.PULL -> "Pull"
        MissDirection.FAT -> "Fat"
        MissDirection.THIN -> "Thin"
        MissDirection.STRAIGHT -> "Straight"
    }

    /**
     * Capitalize first letter of string with explicit locale handling.
     *
     * @return Capitalized string
     */
    private fun String.capitalizeFirstChar(): String =
        replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() }
}
