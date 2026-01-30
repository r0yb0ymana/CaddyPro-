package caddypro.ui.caddy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caddypro.domain.caddy.models.LandingZone

/**
 * Displays the recommended landing zone with visual cue and target details.
 *
 * Shows the visual aim instruction prominently with distance and safety margin
 * information. Designed for quick outdoor readability with large text.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 16
 * Acceptance criteria: A3 (Hazard-aware landing zone)
 *
 * @param zone The landing zone recommendation
 * @param modifier Optional modifier for the component
 */
@Composable
fun LandingZoneIndicator(
    zone: LandingZone,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Visual cue - most prominent element
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GolfCourse,
                contentDescription = "Target",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            Text(
                text = zone.visualCue,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Target line and distance details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            DetailItem(
                label = "Target Line",
                value = "${zone.targetLine}°"
            )

            DetailItem(
                label = "Distance",
                value = "${zone.idealDistance}m"
            )

            DetailItem(
                label = "Margin",
                value = "±${zone.safetyMargin}m"
            )
        }
    }
}

/**
 * Individual detail item with label and value.
 */
@Composable
private fun DetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// Preview composables

@Preview(name = "Landing Zone - Standard", showBackground = true)
@Composable
private fun LandingZoneIndicatorPreview() {
    MaterialTheme {
        LandingZoneIndicator(
            zone = LandingZone(
                targetLine = 275,
                idealDistance = 230,
                safetyMargin = 15,
                visualCue = "Aim 10 yards left of fairway bunker"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Landing Zone - Conservative", showBackground = true)
@Composable
private fun LandingZoneIndicatorConservativePreview() {
    MaterialTheme {
        LandingZoneIndicator(
            zone = LandingZone(
                targetLine = 90,
                idealDistance = 150,
                safetyMargin = 25,
                visualCue = "Play short of water hazard"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Landing Zone - Aggressive", showBackground = true)
@Composable
private fun LandingZoneIndicatorAggressivePreview() {
    MaterialTheme {
        LandingZoneIndicator(
            zone = LandingZone(
                targetLine = 180,
                idealDistance = 285,
                safetyMargin = 10,
                visualCue = "Attack flag over bunker"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Landing Zone - Long Cue", showBackground = true)
@Composable
private fun LandingZoneIndicatorLongCuePreview() {
    MaterialTheme {
        LandingZoneIndicator(
            zone = LandingZone(
                targetLine = 45,
                idealDistance = 195,
                safetyMargin = 20,
                visualCue = "Aim at the large tree behind green, favor left side"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
