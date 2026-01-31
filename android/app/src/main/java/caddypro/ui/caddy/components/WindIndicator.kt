package caddypro.ui.caddy.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caddypro.ui.theme.CaddyProTheme
import kotlin.math.roundToInt

/**
 * Wind indicator component showing wind speed and direction.
 *
 * Displays:
 * - Wind speed in mph (converted from m/s) with large, high-contrast text
 * - Directional arrow pointing in wind direction
 * - Compass direction label (N, NE, E, SE, S, SW, W, NW)
 *
 * Design optimized for outdoor visibility with:
 * - Large typography (18sp+ for primary values)
 * - High contrast colors
 * - Simple, bold arrow graphics
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD)
 * Plan reference: live-caddy-mode-plan.md Task 14
 * Acceptance criteria: A1 (Weather HUD renders within 2 seconds)
 *
 * @param speedMps Wind speed in meters per second
 * @param degrees Wind direction in degrees (0-359, meteorological convention: from direction, 0 = North)
 * @param modifier Modifier for the container
 */
@Composable
fun WindIndicator(
    speedMps: Double,
    degrees: Int,
    modifier: Modifier = Modifier
) {
    // Convert m/s to mph (1 m/s = 2.237 mph)
    val speedMph = (speedMps * 2.237).roundToInt()
    val direction = degreesToCompassDirection(degrees)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Wind direction arrow
        WindArrow(
            degrees = degrees,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Wind speed and direction text
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            // Wind speed - large, bold text for outdoor visibility
            Text(
                text = "$speedMph mph",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Compass direction label
            Text(
                text = direction,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Directional arrow canvas showing wind direction.
 *
 * Draws a simple arrow pointing in the direction the wind is blowing FROM
 * (meteorological convention). Arrow rotates to match wind direction.
 *
 * @param degrees Wind direction in degrees (0 = North, 90 = East, etc.)
 * @param color Color for the arrow
 * @param modifier Modifier for the canvas
 */
@Composable
private fun WindArrow(
    degrees: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(40.dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val arrowLength = size.width * 0.6f

        // Rotate canvas to match wind direction
        // Note: Canvas rotation is clockwise, 0° is up (North)
        rotate(degrees = degrees.toFloat(), pivot = center) {
            // Draw arrow shaft
            drawLine(
                color = color,
                start = Offset(center.x, center.y + arrowLength / 2),
                end = Offset(center.x, center.y - arrowLength / 2),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Draw arrowhead (pointing up/north before rotation)
            val arrowheadPath = Path().apply {
                // Top point
                moveTo(center.x, center.y - arrowLength / 2)
                // Left barb
                lineTo(center.x - 8.dp.toPx(), center.y - arrowLength / 2 + 12.dp.toPx())
                // Back to top
                moveTo(center.x, center.y - arrowLength / 2)
                // Right barb
                lineTo(center.x + 8.dp.toPx(), center.y - arrowLength / 2 + 12.dp.toPx())
            }

            drawPath(
                path = arrowheadPath,
                color = color,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Convert degrees to compass direction label.
 *
 * Maps wind direction to 8-point compass rose:
 * - N (North): 337.5° - 22.5°
 * - NE (Northeast): 22.5° - 67.5°
 * - E (East): 67.5° - 112.5°
 * - SE (Southeast): 112.5° - 157.5°
 * - S (South): 157.5° - 202.5°
 * - SW (Southwest): 202.5° - 247.5°
 * - W (West): 247.5° - 292.5°
 * - NW (Northwest): 292.5° - 337.5°
 *
 * @param degrees Wind direction in degrees (0-359)
 * @return Compass direction string (N, NE, E, SE, S, SW, W, NW)
 */
private fun degreesToCompassDirection(degrees: Int): String {
    // Normalize to 0-359
    val normalized = ((degrees % 360) + 360) % 360

    return when {
        normalized < 22.5 -> "N"
        normalized < 67.5 -> "NE"
        normalized < 112.5 -> "E"
        normalized < 157.5 -> "SE"
        normalized < 202.5 -> "S"
        normalized < 247.5 -> "SW"
        normalized < 292.5 -> "W"
        normalized < 337.5 -> "NW"
        else -> "N"
    }
}

// Preview functions for development and testing

@Preview(name = "North Wind", showBackground = true)
@Composable
private fun PreviewWindIndicatorNorth() {
    CaddyProTheme {
        WindIndicator(
            speedMps = 5.0,
            degrees = 0
        )
    }
}

@Preview(name = "East Wind", showBackground = true)
@Composable
private fun PreviewWindIndicatorEast() {
    CaddyProTheme {
        WindIndicator(
            speedMps = 8.5,
            degrees = 90
        )
    }
}

@Preview(name = "Southwest Wind", showBackground = true)
@Composable
private fun PreviewWindIndicatorSouthwest() {
    CaddyProTheme {
        WindIndicator(
            speedMps = 3.2,
            degrees = 225
        )
    }
}

@Preview(name = "Light Wind", showBackground = true)
@Composable
private fun PreviewWindIndicatorLight() {
    CaddyProTheme {
        WindIndicator(
            speedMps = 1.5,
            degrees = 180
        )
    }
}

@Preview(name = "Strong Wind", showBackground = true)
@Composable
private fun PreviewWindIndicatorStrong() {
    CaddyProTheme {
        WindIndicator(
            speedMps = 12.0,
            degrees = 315
        )
    }
}
