package caddypro.ui.caddy.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Displays a list of risk callouts with warning icons.
 *
 * Risk callouts are personalized warnings about hazards based on the player's
 * dominant miss pattern and tendencies. Maximum of 3 callouts displayed per spec.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 16
 * Acceptance criteria: A3 (Hazard-aware landing zone)
 *
 * @param callouts List of risk warning strings (max 3)
 * @param modifier Optional modifier for the component
 */
@Composable
fun RiskCalloutsList(
    callouts: List<String>,
    modifier: Modifier = Modifier
) {
    if (callouts.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Risk Callouts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        callouts.forEach { callout ->
            RiskCalloutItem(callout)
        }
    }
}

/**
 * Individual risk callout item with warning icon.
 */
@Composable
private fun RiskCalloutItem(
    callout: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Risk",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = callout,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// Preview composables

@Preview(name = "Risk Callouts - Multiple", showBackground = true)
@Composable
private fun RiskCalloutsListPreview() {
    MaterialTheme {
        RiskCalloutsList(
            callouts = listOf(
                "Right miss brings water into play at 180-220m",
                "Left miss is OB stroke and distance",
                "Aggressive carry over bunker at 240m"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Risk Callouts - Single", showBackground = true)
@Composable
private fun RiskCalloutsListSinglePreview() {
    MaterialTheme {
        RiskCalloutsList(
            callouts = listOf(
                "Long miss finds water - play conservative"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Risk Callouts - Two", showBackground = true)
@Composable
private fun RiskCalloutsListTwoPreview() {
    MaterialTheme {
        RiskCalloutsList(
            callouts = listOf(
                "Short miss leaves difficult uphill pitch",
                "Wind at 3 o'clock - aim left edge"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Risk Callouts - Empty", showBackground = true)
@Composable
private fun RiskCalloutsListEmptyPreview() {
    MaterialTheme {
        RiskCalloutsList(
            callouts = emptyList(),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Risk Callouts - Long Text", showBackground = true)
@Composable
private fun RiskCalloutsListLongTextPreview() {
    MaterialTheme {
        RiskCalloutsList(
            callouts = listOf(
                "Your dominant right miss pattern brings the water hazard on the right side into play between 180-220 meters from the tee",
                "OB left is in play for any shot missing more than 15 yards left of center line",
                "Consider laying up short of fairway bunker complex at 240m"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
