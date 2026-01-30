package caddypro.ui.caddy.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.ui.theme.CaddyProTheme
import kotlin.math.roundToInt

/**
 * Horizontal bar chart showing readiness metric breakdown.
 *
 * Displays individual components of the readiness score:
 * - HRV (Heart Rate Variability) - 40% weight
 * - Sleep Quality - 40% weight
 * - Stress Level - 20% weight
 *
 * Each metric is shown as:
 * - Metric name and weight percentage
 * - Horizontal bar with value/100 fill
 * - Numeric score value
 * - Color coding based on metric value
 *
 * Metrics that are unavailable (null) are gracefully hidden.
 * If all metrics are null (manual entry), displays a message.
 *
 * The chart provides transparency into how the overall readiness
 * score is calculated, meeting spec requirement R3 for transparent
 * contributors to the readiness calculation.
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 15
 * Acceptance criteria: A2 (Readiness impacts strategy)
 *
 * @param breakdown Readiness metric breakdown with individual scores
 * @param modifier Modifier for the chart container
 */
@Composable
fun ReadinessBreakdownChart(
    breakdown: ReadinessBreakdown,
    modifier: Modifier = Modifier
) {
    val hasMetrics = breakdown.hrv != null ||
            breakdown.sleepQuality != null ||
            breakdown.stressLevel != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Breakdown",
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (!hasMetrics) {
            // Show message when no metrics are available
            Text(
                text = "No wearable data available. Using manual entry.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            // Show metric bars
            breakdown.hrv?.let { metric ->
                MetricBar(
                    label = "HRV",
                    metric = metric,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            breakdown.sleepQuality?.let { metric ->
                MetricBar(
                    label = "Sleep",
                    metric = metric,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            breakdown.stressLevel?.let { metric ->
                MetricBar(
                    label = "Stress",
                    metric = metric,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

/**
 * Individual metric bar with label, progress bar, and value.
 *
 * Displays a single readiness metric as:
 * - Label with weight percentage (e.g., "HRV (40%)")
 * - Horizontal progress bar showing value/100
 * - Numeric score on the right
 *
 * Bar color matches the readiness color coding.
 *
 * @param label Metric name (HRV, Sleep, Stress)
 * @param metric Metric score with value and weight
 * @param modifier Modifier for the bar container
 */
@Composable
private fun MetricBar(
    label: String,
    metric: MetricScore,
    modifier: Modifier = Modifier
) {
    val scoreInt = metric.value.roundToInt()
    val weightPercent = (metric.weight * 100).roundToInt()
    val barColor = readinessColor(scoreInt)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$label: $scoreInt out of 100, weight $weightPercent percent"
            }
    ) {
        // Label and value row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Metric label with weight
            Text(
                text = "$label ($weightPercent%)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Numeric score
            Text(
                text = "$scoreInt",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (metric.value / 100f).coerceIn(0f, 1f))
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }
    }
}

// Preview functions for development and testing

@Preview(name = "All Metrics Available - High Readiness", showBackground = true)
@Composable
private fun PreviewReadinessBreakdownChartComplete() {
    CaddyProTheme {
        ReadinessBreakdownChart(
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(value = 90.0, weight = 0.4),
                sleepQuality = MetricScore(value = 85.0, weight = 0.4),
                stressLevel = MetricScore(value = 75.0, weight = 0.2)
            )
        )
    }
}

@Preview(name = "All Metrics Available - Moderate Readiness", showBackground = true)
@Composable
private fun PreviewReadinessBreakdownChartModerate() {
    CaddyProTheme {
        ReadinessBreakdownChart(
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(value = 60.0, weight = 0.4),
                sleepQuality = MetricScore(value = 50.0, weight = 0.4),
                stressLevel = MetricScore(value = 55.0, weight = 0.2)
            )
        )
    }
}

@Preview(name = "All Metrics Available - Low Readiness", showBackground = true)
@Composable
private fun PreviewReadinessBreakdownChartLow() {
    CaddyProTheme {
        ReadinessBreakdownChart(
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(value = 40.0, weight = 0.4),
                sleepQuality = MetricScore(value = 30.0, weight = 0.4),
                stressLevel = MetricScore(value = 35.0, weight = 0.2)
            )
        )
    }
}

@Preview(name = "Partial Metrics - HRV Only", showBackground = true)
@Composable
private fun PreviewReadinessBreakdownChartHrvOnly() {
    CaddyProTheme {
        ReadinessBreakdownChart(
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(value = 75.0, weight = 1.0),
                sleepQuality = null,
                stressLevel = null
            )
        )
    }
}

@Preview(name = "Partial Metrics - Sleep and Stress", showBackground = true)
@Composable
private fun PreviewReadinessBreakdownChartSleepStress() {
    CaddyProTheme {
        ReadinessBreakdownChart(
            breakdown = ReadinessBreakdown(
                hrv = null,
                sleepQuality = MetricScore(value = 65.0, weight = 0.67),
                stressLevel = MetricScore(value = 55.0, weight = 0.33)
            )
        )
    }
}

@Preview(name = "No Metrics - Manual Entry", showBackground = true)
@Composable
private fun PreviewReadinessBreakdownChartEmpty() {
    CaddyProTheme {
        ReadinessBreakdownChart(
            breakdown = ReadinessBreakdown(
                hrv = null,
                sleepQuality = null,
                stressLevel = null
            )
        )
    }
}

@Preview(name = "Mixed Values - Wide Range", showBackground = true)
@Composable
private fun PreviewReadinessBreakdownChartMixed() {
    CaddyProTheme {
        ReadinessBreakdownChart(
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(value = 95.0, weight = 0.4),
                sleepQuality = MetricScore(value = 45.0, weight = 0.4),
                stressLevel = MetricScore(value = 20.0, weight = 0.2)
            )
        )
    }
}
