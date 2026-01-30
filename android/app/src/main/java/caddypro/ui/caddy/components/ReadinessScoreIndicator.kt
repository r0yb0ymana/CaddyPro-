package caddypro.ui.caddy.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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

/**
 * Circular readiness score indicator with centered score value.
 *
 * Displays a circular progress indicator showing the readiness score
 * as a percentage (0-100) with the numeric score in the center.
 * The progress indicator uses the readiness color coding:
 * - 0-40: Red
 * - 41-60: Yellow/Orange
 * - 61+: Green
 *
 * The component is sized for outdoor visibility with large, bold text
 * and a thick stroke width for the progress indicator.
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 15
 * Acceptance criteria: A2 (Readiness impacts strategy)
 *
 * @param score Readiness score (0-100)
 * @param modifier Modifier for the indicator container
 */
@Composable
fun ReadinessScoreIndicator(
    score: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            // Background track
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(80.dp),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            )

            // Progress indicator with readiness color
            CircularProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.size(80.dp),
                strokeWidth = 8.dp,
                color = readinessColor(score),
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            )

            // Centered score text
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = readinessColor(score)
            )
        }

        // "Readiness" label below indicator
        Text(
            text = "Readiness",
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Preview functions for development and testing

@Preview(name = "High Readiness (85)", showBackground = true)
@Composable
private fun PreviewReadinessScoreIndicatorHigh() {
    CaddyProTheme {
        ReadinessScoreIndicator(score = 85)
    }
}

@Preview(name = "Moderate Readiness (55)", showBackground = true)
@Composable
private fun PreviewReadinessScoreIndicatorModerate() {
    CaddyProTheme {
        ReadinessScoreIndicator(score = 55)
    }
}

@Preview(name = "Low Readiness (35)", showBackground = true)
@Composable
private fun PreviewReadinessScoreIndicatorLow() {
    CaddyProTheme {
        ReadinessScoreIndicator(score = 35)
    }
}

@Preview(name = "Perfect Readiness (100)", showBackground = true)
@Composable
private fun PreviewReadinessScoreIndicatorPerfect() {
    CaddyProTheme {
        ReadinessScoreIndicator(score = 100)
    }
}

@Preview(name = "Very Low Readiness (15)", showBackground = true)
@Composable
private fun PreviewReadinessScoreIndicatorVeryLow() {
    CaddyProTheme {
        ReadinessScoreIndicator(score = 15)
    }
}

@Preview(name = "Threshold - Yellow to Green (61)", showBackground = true)
@Composable
private fun PreviewReadinessScoreIndicatorThresholdGreen() {
    CaddyProTheme {
        ReadinessScoreIndicator(score = 61)
    }
}

@Preview(name = "Threshold - Red to Yellow (41)", showBackground = true)
@Composable
private fun PreviewReadinessScoreIndicatorThresholdYellow() {
    CaddyProTheme {
        ReadinessScoreIndicator(score = 41)
    }
}
