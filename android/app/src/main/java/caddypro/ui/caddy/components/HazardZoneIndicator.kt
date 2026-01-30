package caddypro.ui.caddy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caddypro.domain.caddy.models.HazardLocation
import caddypro.domain.caddy.models.HazardType
import caddypro.domain.caddy.models.HazardZone
import caddypro.domain.navcaddy.models.MissDirection

/**
 * Displays a single hazard zone with icon, location, and distance range.
 *
 * Uses appropriate icons for each hazard type and displays distance range
 * in a compact format suitable for outdoor visibility.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 16
 *
 * @param zone The hazard zone to display
 * @param modifier Optional modifier for the component
 */
@Composable
fun HazardZoneIndicator(
    zone: HazardZone,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: icon and hazard type with location
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = hazardIcon(zone.type),
                contentDescription = zone.type.name,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "${zone.type.displayName} ${zone.location.side}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }

        // Right side: distance range
        Text(
            text = "${zone.location.distanceFromTee.first}-${zone.location.distanceFromTee.last}m",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

/**
 * Maps hazard type to appropriate Material icon.
 */
private fun hazardIcon(type: HazardType): ImageVector {
    return when (type) {
        HazardType.WATER -> Icons.Default.WaterDrop
        HazardType.OB -> Icons.Default.Block
        HazardType.BUNKER -> Icons.Default.Terrain
        HazardType.PENALTY_ROUGH -> Icons.Default.Landscape
        HazardType.TREES -> Icons.Default.Park
    }
}

/**
 * Human-readable display name for hazard types.
 */
private val HazardType.displayName: String
    get() = when (this) {
        HazardType.WATER -> "Water"
        HazardType.OB -> "OB"
        HazardType.BUNKER -> "Bunker"
        HazardType.PENALTY_ROUGH -> "Rough"
        HazardType.TREES -> "Trees"
    }

// Preview composables

@Preview(name = "Water Hazard", showBackground = true)
@Composable
private fun HazardZoneIndicatorWaterPreview() {
    MaterialTheme {
        HazardZoneIndicator(
            zone = HazardZone(
                type = HazardType.WATER,
                location = HazardLocation("right", 180..220),
                penaltyStrokes = 1.0,
                affectedMisses = listOf(MissDirection.RIGHT)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "OB Hazard", showBackground = true)
@Composable
private fun HazardZoneIndicatorOBPreview() {
    MaterialTheme {
        HazardZoneIndicator(
            zone = HazardZone(
                type = HazardType.OB,
                location = HazardLocation("left", 200..250),
                penaltyStrokes = 2.0,
                affectedMisses = listOf(MissDirection.LEFT)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Bunker Hazard", showBackground = true)
@Composable
private fun HazardZoneIndicatorBunkerPreview() {
    MaterialTheme {
        HazardZoneIndicator(
            zone = HazardZone(
                type = HazardType.BUNKER,
                location = HazardLocation("center", 240..260),
                penaltyStrokes = 0.5,
                affectedMisses = listOf(MissDirection.SHORT)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Trees Hazard", showBackground = true)
@Composable
private fun HazardZoneIndicatorTreesPreview() {
    MaterialTheme {
        HazardZoneIndicator(
            zone = HazardZone(
                type = HazardType.TREES,
                location = HazardLocation("long", 280..320),
                penaltyStrokes = 1.5,
                affectedMisses = listOf(MissDirection.LONG)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Penalty Rough Hazard", showBackground = true)
@Composable
private fun HazardZoneIndicatorRoughPreview() {
    MaterialTheme {
        HazardZoneIndicator(
            zone = HazardZone(
                type = HazardType.PENALTY_ROUGH,
                location = HazardLocation("short", 150..170),
                penaltyStrokes = 0.8,
                affectedMisses = listOf(MissDirection.SHORT)
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
