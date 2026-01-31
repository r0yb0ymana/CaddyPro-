package caddypro.ui.caddy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caddypro.ui.theme.CaddyProTheme

/**
 * Conditions adjustment chip showing carry distance modifier.
 *
 * Displays:
 * - Percentage adjustment to carry distance (e.g., "+5%" or "-8%")
 * - Visual indicator (up/down arrow) for positive/negative adjustment
 * - Human-readable explanation of adjustment factors
 * - Color-coded chip (green for helping conditions, red for hurting)
 *
 * This chip appears in the expanded Forecaster HUD view to show how
 * current weather conditions (wind, temperature, humidity) affect ball flight.
 *
 * Design optimized for outdoor visibility with:
 * - Large typography (16sp+ for primary values)
 * - High contrast colors with semantic meaning
 * - Clear icons for quick recognition
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 14
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds)
 *
 * @param carryModifier Multiplier for carry distance (e.g., 0.95 = 5% less, 1.05 = 5% more)
 * @param reason Human-readable explanation of the adjustment
 * @param modifier Modifier for the container
 */
@Composable
fun ConditionsChip(
    carryModifier: Double,
    reason: String,
    modifier: Modifier = Modifier
) {
    // Calculate percentage change from 1.0 baseline
    val percentChange = ((carryModifier - 1.0) * 100).toInt()
    val isPositive = percentChange > 0
    val isNeutral = percentChange == 0

    // Determine chip color based on adjustment direction
    val chipColor = when {
        isNeutral -> MaterialTheme.colorScheme.surfaceVariant
        isPositive -> Color(0xFF4CAF50).copy(alpha = 0.2f) // Green background for helping
        else -> Color(0xFFF44336).copy(alpha = 0.2f) // Red background for hurting
    }

    val contentColor = when {
        isNeutral -> MaterialTheme.colorScheme.onSurfaceVariant
        isPositive -> Color(0xFF2E7D32) // Dark green text
        else -> Color(0xFFC62828) // Dark red text
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Chip with percentage and icon
        AssistChip(
            onClick = { /* Chip is informational only */ },
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Icon indicating direction
                    if (!isNeutral) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = if (isPositive) "Helping conditions" else "Hurting conditions",
                            tint = contentColor
                        )
                    }

                    // Percentage text
                    Text(
                        text = when {
                            isNeutral -> "Standard conditions"
                            isPositive -> "+$percentChange% carry"
                            else -> "$percentChange% carry"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = contentColor
                    )
                }
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = chipColor,
                labelColor = contentColor
            ),
            border = AssistChipDefaults.assistChipBorder(
                borderColor = contentColor.copy(alpha = 0.5f),
                enabled = true
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Explanation text
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// Preview functions for development and testing

@Preview(name = "Helping Conditions", showBackground = true)
@Composable
private fun PreviewConditionsChipHelping() {
    CaddyProTheme {
        ConditionsChip(
            carryModifier = 1.05,
            reason = "Tailwind (+3 m/s) and warm air (+5°C above standard)",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Hurting Conditions", showBackground = true)
@Composable
private fun PreviewConditionsChipHurting() {
    CaddyProTheme {
        ConditionsChip(
            carryModifier = 0.92,
            reason = "Strong headwind (-5 m/s) and cold dense air (-10°C below standard)",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Neutral Conditions", showBackground = true)
@Composable
private fun PreviewConditionsChipNeutral() {
    CaddyProTheme {
        ConditionsChip(
            carryModifier = 1.0,
            reason = "Minimal wind and near-standard temperature",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Strong Helping Wind", showBackground = true)
@Composable
private fun PreviewConditionsChipStrongHelping() {
    CaddyProTheme {
        ConditionsChip(
            carryModifier = 1.12,
            reason = "Strong tailwind (+8 m/s) with hot thin air (+15°C)",
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Moderate Hurting", showBackground = true)
@Composable
private fun PreviewConditionsChipModerateHurting() {
    CaddyProTheme {
        ConditionsChip(
            carryModifier = 0.95,
            reason = "Light headwind (-2 m/s) with standard temperature",
            modifier = Modifier.padding(16.dp)
        )
    }
}
