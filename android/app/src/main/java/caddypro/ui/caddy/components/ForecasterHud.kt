package caddypro.ui.caddy.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.WeatherData
import caddypro.ui.theme.CaddyProTheme

/**
 * Forecaster HUD component displaying real-time weather conditions.
 *
 * Provides a compact, outdoor-optimized weather display with:
 * - Wind speed/direction indicator
 * - Temperature and humidity
 * - Conditions adjustment percentage (when expanded)
 * - High contrast for outdoor visibility
 * - Semi-transparent card overlay
 * - Loading state handling
 *
 * The HUD supports expansion to show detailed conditions adjustment,
 * which computes the effect of wind and air density on ball flight.
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 14
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds)
 *
 * @param weather Current weather data (null while loading)
 * @param isExpanded True to show expanded details view
 * @param onToggleExpanded Callback when user toggles expansion state
 * @param modifier Modifier for the HUD container
 * @param targetBearing Optional target bearing for conditions adjustment (0-360 degrees, 0 = North)
 * @param baseCarryMeters Optional base carry distance for conditions adjustment
 */
@Composable
fun ForecasterHud(
    weather: WeatherData?,
    isExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    targetBearing: Int? = null,
    baseCarryMeters: Int? = null
) {
    Card(
        onClick = { onToggleExpanded(!isExpanded) },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (weather == null) {
            LoadingWeatherIndicator()
        } else {
            WeatherContent(
                weather = weather,
                isExpanded = isExpanded,
                targetBearing = targetBearing,
                baseCarryMeters = baseCarryMeters
            )
        }
    }
}

/**
 * Loading indicator shown while weather data is being fetched.
 *
 * Displays a centered progress indicator with message to meet the
 * 2-second render requirement from acceptance criteria A1.
 */
@Composable
private fun LoadingWeatherIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = "Loading weather...",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Main weather content display with compact and expanded modes.
 *
 * Compact mode (always visible):
 * - Wind indicator with speed/direction
 * - Temperature and humidity
 *
 * Expanded mode (when toggled):
 * - Conditions adjustment chip showing carry modifier percentage
 * - Detailed explanation of adjustment factors
 *
 * @param weather Current weather data
 * @param isExpanded True to show expanded details
 * @param targetBearing Optional target bearing for conditions adjustment
 * @param baseCarryMeters Optional base carry distance for conditions adjustment
 */
@Composable
private fun WeatherContent(
    weather: WeatherData,
    isExpanded: Boolean,
    targetBearing: Int?,
    baseCarryMeters: Int?
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Compact view (always visible)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wind indicator - left side
            WindIndicator(
                speedMps = weather.windSpeedMps,
                degrees = weather.windDegrees,
                modifier = Modifier.weight(1f)
            )

            // Temperature and humidity - right side
            TemperatureIndicator(
                celsius = weather.temperatureCelsius,
                humidity = weather.humidity,
                modifier = Modifier.weight(1f)
            )
        }

        // Expanded view with conditions adjustment
        AnimatedVisibility(
            visible = isExpanded && targetBearing != null && baseCarryMeters != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            if (targetBearing != null && baseCarryMeters != null) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Compute conditions adjustment
                    val adjustment = weather.conditionsAdjustment(
                        targetBearing = targetBearing,
                        baseCarryMeters = baseCarryMeters
                    )

                    ConditionsChip(
                        carryModifier = adjustment.carryModifier,
                        reason = adjustment.reason
                    )
                }
            }
        }
    }
}

// Preview functions for development and testing

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun PreviewForecasterHudLoading() {
    CaddyProTheme {
        ForecasterHud(
            weather = null,
            isExpanded = false,
            onToggleExpanded = {}
        )
    }
}

@Preview(name = "Compact View", showBackground = true)
@Composable
private fun PreviewForecasterHudCompact() {
    CaddyProTheme {
        ForecasterHud(
            weather = WeatherData(
                windSpeedMps = 5.5,
                windDegrees = 270,
                temperatureCelsius = 22.0,
                humidity = 65,
                timestamp = System.currentTimeMillis(),
                location = Location(
                    latitude = 37.7749,
                    longitude = -122.4194,
                    name = "Test Course"
                )
            ),
            isExpanded = false,
            onToggleExpanded = {}
        )
    }
}

@Preview(name = "Expanded View", showBackground = true)
@Composable
private fun PreviewForecasterHudExpanded() {
    CaddyProTheme {
        ForecasterHud(
            weather = WeatherData(
                windSpeedMps = 5.5,
                windDegrees = 270,
                temperatureCelsius = 22.0,
                humidity = 65,
                timestamp = System.currentTimeMillis(),
                location = Location(
                    latitude = 37.7749,
                    longitude = -122.4194,
                    name = "Test Course"
                )
            ),
            isExpanded = true,
            onToggleExpanded = {},
            targetBearing = 0,
            baseCarryMeters = 150
        )
    }
}

@Preview(name = "Cold Headwind Conditions", showBackground = true)
@Composable
private fun PreviewForecasterHudColdHeadwind() {
    CaddyProTheme {
        ForecasterHud(
            weather = WeatherData(
                windSpeedMps = 8.0,
                windDegrees = 0,  // Wind from North
                temperatureCelsius = 5.0,  // Cold
                humidity = 45,
                timestamp = System.currentTimeMillis(),
                location = Location(
                    latitude = 37.7749,
                    longitude = -122.4194,
                    name = "Test Course"
                )
            ),
            isExpanded = true,
            onToggleExpanded = {},
            targetBearing = 0,  // Hitting North into the wind
            baseCarryMeters = 150
        )
    }
}

@Preview(name = "Hot Tailwind Conditions", showBackground = true)
@Composable
private fun PreviewForecasterHudHotTailwind() {
    CaddyProTheme {
        ForecasterHud(
            weather = WeatherData(
                windSpeedMps = 6.5,
                windDegrees = 180,  // Wind from South
                temperatureCelsius = 32.0,  // Hot
                humidity = 75,
                timestamp = System.currentTimeMillis(),
                location = Location(
                    latitude = 37.7749,
                    longitude = -122.4194,
                    name = "Test Course"
                )
            ),
            isExpanded = true,
            onToggleExpanded = {},
            targetBearing = 0,  // Hitting North with tailwind
            baseCarryMeters = 150
        )
    }
}
