package caddypro.ui.caddy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caddypro.ui.theme.CaddyProTheme
import kotlin.math.roundToInt

/**
 * Temperature and humidity indicator component.
 *
 * Displays:
 * - Temperature in Fahrenheit (converted from Celsius) with large, high-contrast text
 * - Humidity percentage
 * - Icons for visual recognition (thermometer and water drop)
 *
 * Design optimized for outdoor visibility with:
 * - Large typography (18sp+ for primary values)
 * - High contrast colors
 * - Clear icons for quick recognition
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 14
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds)
 *
 * @param celsius Temperature in degrees Celsius
 * @param humidity Relative humidity percentage (0-100)
 * @param modifier Modifier for the container
 */
@Composable
fun TemperatureIndicator(
    celsius: Double,
    humidity: Int,
    modifier: Modifier = Modifier
) {
    // Convert Celsius to Fahrenheit: F = C * 9/5 + 32
    val fahrenheit = (celsius * 9.0 / 5.0 + 32.0).roundToInt()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center
    ) {
        // Temperature row
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Thermostat,
                contentDescription = "Temperature",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Temperature value - large, bold text for outdoor visibility
            Text(
                text = "$fahrenheit°F",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Humidity row
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = "Humidity",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.width(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Humidity percentage
            Text(
                text = "$humidity%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

// Preview functions for development and testing

@Preview(name = "Warm Weather", showBackground = true)
@Composable
private fun PreviewTemperatureIndicatorWarm() {
    CaddyProTheme {
        TemperatureIndicator(
            celsius = 28.0,
            humidity = 65
        )
    }
}

@Preview(name = "Cold Weather", showBackground = true)
@Composable
private fun PreviewTemperatureIndicatorCold() {
    CaddyProTheme {
        TemperatureIndicator(
            celsius = 5.0,
            humidity = 45
        )
    }
}

@Preview(name = "Hot Humid Weather", showBackground = true)
@Composable
private fun PreviewTemperatureIndicatorHotHumid() {
    CaddyProTheme {
        TemperatureIndicator(
            celsius = 35.0,
            humidity = 85
        )
    }
}

@Preview(name = "Mild Weather", showBackground = true)
@Composable
private fun PreviewTemperatureIndicatorMild() {
    CaddyProTheme {
        TemperatureIndicator(
            celsius = 20.0,
            humidity = 50
        )
    }
}

@Preview(name = "Freezing Weather", showBackground = true)
@Composable
private fun PreviewTemperatureIndicatorFreezing() {
    CaddyProTheme {
        TemperatureIndicator(
            celsius = 0.0,
            humidity = 30
        )
    }
}
