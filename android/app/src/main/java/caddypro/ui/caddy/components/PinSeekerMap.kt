package caddypro.ui.caddy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caddypro.domain.caddy.models.HazardLocation
import caddypro.domain.caddy.models.HazardType
import caddypro.domain.caddy.models.HazardZone
import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.LandingZone
import caddypro.domain.caddy.models.PersonalizationContext
import caddypro.domain.navcaddy.models.MissDirection

/**
 * Displays the PinSeeker AI strategy map for a hole.
 *
 * Shows hazard zones, recommended landing zones, and risk callouts
 * personalized to the player's profile and readiness.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 16
 * Acceptance criteria: A3 (Hazard-aware landing zone)
 *
 * @param strategy The computed hole strategy, or null if course data unavailable
 * @param modifier Optional modifier for the component
 */
@Composable
fun PinSeekerMap(
    strategy: HoleStrategy?,
    modifier: Modifier = Modifier
) {
    if (strategy == null) {
        EmptyStrategyPlaceholder(modifier)
        return
    }

    Column(modifier = modifier.padding(16.dp)) {
        // Hole header
        HoleHeader(
            holeNumber = strategy.holeNumber,
            personalizedFor = strategy.personalizedFor
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Simplified overhead view (MVP: no course rendering, just zones)
        HazardZonesList(strategy.dangerZones)

        Spacer(modifier = Modifier.height(12.dp))

        // Recommended landing zone
        LandingZoneCard(strategy.recommendedLandingZone)

        Spacer(modifier = Modifier.height(12.dp))

        // Risk callouts (max 3 per spec)
        RiskCalloutsList(strategy.riskCallouts.take(3))
    }
}

/**
 * Displays hole number and personalization context.
 */
@Composable
private fun HoleHeader(
    holeNumber: Int,
    personalizedFor: PersonalizationContext,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Hole $holeNumber Strategy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "HCP ${personalizedFor.handicap} • ${personalizedFor.dominantMiss.displayName} miss • Readiness ${personalizedFor.readinessScore}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Displays danger zones in an error-colored card.
 */
@Composable
private fun HazardZonesList(
    zones: List<HazardZone>,
    modifier: Modifier = Modifier
) {
    if (zones.isEmpty()) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Danger Zones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            zones.forEach { zone ->
                HazardZoneIndicator(zone)
            }
        }
    }
}

/**
 * Displays recommended landing zone in a primary-colored card.
 */
@Composable
private fun LandingZoneCard(
    zone: LandingZone,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Recommended Target",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            LandingZoneIndicator(zone)
        }
    }
}

/**
 * Placeholder shown when course data is not available.
 */
@Composable
private fun EmptyStrategyPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Course Data Not Available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Strategy map requires course hazard data. Please ensure course data is downloaded or available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Extension property to provide user-friendly display names for miss directions.
 */
private val MissDirection.displayName: String
    get() = when (this) {
        MissDirection.PUSH -> "Push"
        MissDirection.PULL -> "Pull"
        MissDirection.SLICE -> "Slice"
        MissDirection.HOOK -> "Hook"
        MissDirection.FAT -> "Fat"
        MissDirection.THIN -> "Thin"
        MissDirection.STRAIGHT -> "Straight"
    }

// Preview composables

@Preview(name = "PinSeeker Map with Strategy", showBackground = true)
@Composable
private fun PinSeekerMapPreview() {
    MaterialTheme {
        PinSeekerMap(
            strategy = HoleStrategy(
                holeNumber = 7,
                dangerZones = listOf(
                    HazardZone(
                        type = HazardType.WATER,
                        location = HazardLocation("right", 180..220),
                        penaltyStrokes = 1.0,
                        affectedMisses = listOf(MissDirection.SLICE)
                    ),
                    HazardZone(
                        type = HazardType.OB,
                        location = HazardLocation("left", 200..250),
                        penaltyStrokes = 2.0,
                        affectedMisses = listOf(MissDirection.HOOK)
                    ),
                    HazardZone(
                        type = HazardType.BUNKER,
                        location = HazardLocation("center", 240..260),
                        penaltyStrokes = 0.5,
                        affectedMisses = listOf(MissDirection.FAT)
                    )
                ),
                recommendedLandingZone = LandingZone(
                    targetLine = 275,
                    idealDistance = 230,
                    safetyMargin = 15,
                    visualCue = "Aim 10 yards left of fairway bunker"
                ),
                riskCallouts = listOf(
                    "Right miss brings water into play at 180-220m",
                    "Left miss is OB stroke and distance",
                    "Aggressive carry over bunker at 240m"
                ),
                personalizedFor = PersonalizationContext(
                    handicap = 9,
                    dominantMiss = MissDirection.SLICE,
                    clubDistances = mapOf("Driver" to 240, "3W" to 220),
                    readinessScore = 78
                )
            )
        )
    }
}

@Preview(name = "PinSeeker Map - No Strategy", showBackground = true)
@Composable
private fun PinSeekerMapEmptyPreview() {
    MaterialTheme {
        PinSeekerMap(strategy = null)
    }
}

@Preview(name = "PinSeeker Map - Low Readiness", showBackground = true)
@Composable
private fun PinSeekerMapLowReadinessPreview() {
    MaterialTheme {
        PinSeekerMap(
            strategy = HoleStrategy(
                holeNumber = 15,
                dangerZones = listOf(
                    HazardZone(
                        type = HazardType.WATER,
                        location = HazardLocation("long", 160..180),
                        penaltyStrokes = 1.0,
                        affectedMisses = listOf(MissDirection.THIN)
                    )
                ),
                recommendedLandingZone = LandingZone(
                    targetLine = 90,
                    idealDistance = 150,
                    safetyMargin = 25,
                    visualCue = "Play short of water hazard"
                ),
                riskCallouts = listOf(
                    "Long miss finds water - play conservative"
                ),
                personalizedFor = PersonalizationContext(
                    handicap = 15,
                    dominantMiss = MissDirection.THIN,
                    clubDistances = mapOf("7I" to 155, "8I" to 145),
                    readinessScore = 42
                )
            )
        )
    }
}

@Preview(name = "PinSeeker Map - Hook Pattern", showBackground = true)
@Composable
private fun PinSeekerMapHookPreview() {
    MaterialTheme {
        PinSeekerMap(
            strategy = HoleStrategy(
                holeNumber = 12,
                dangerZones = listOf(
                    HazardZone(
                        type = HazardType.TREES,
                        location = HazardLocation("left", 190..240),
                        penaltyStrokes = 1.5,
                        affectedMisses = listOf(MissDirection.HOOK, MissDirection.PULL)
                    ),
                    HazardZone(
                        type = HazardType.PENALTY_ROUGH,
                        location = HazardLocation("left", 180..200),
                        penaltyStrokes = 0.8,
                        affectedMisses = listOf(MissDirection.HOOK)
                    )
                ),
                recommendedLandingZone = LandingZone(
                    targetLine = 15,
                    idealDistance = 210,
                    safetyMargin = 20,
                    visualCue = "Favor right side to avoid left trouble"
                ),
                riskCallouts = listOf(
                    "Hook pattern brings trees into play left",
                    "Heavy rough on left side at landing zone"
                ),
                personalizedFor = PersonalizationContext(
                    handicap = 12,
                    dominantMiss = MissDirection.HOOK,
                    clubDistances = mapOf("Driver" to 235, "3W" to 215, "5W" to 205),
                    readinessScore = 65
                )
            )
        )
    }
}
