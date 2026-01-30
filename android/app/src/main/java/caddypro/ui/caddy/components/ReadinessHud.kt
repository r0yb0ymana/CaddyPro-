package caddypro.ui.caddy.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import caddypro.ui.theme.CaddyProTheme

/**
 * BodyCaddy Readiness HUD component displaying biometric readiness score.
 *
 * Provides a compact, outdoor-optimized readiness display with:
 * - Overall readiness score (0-100) with color coding
 * - Circular progress indicator
 * - Expandable breakdown of HRV, sleep, and stress components
 * - Manual override button for when wearables are unavailable
 * - Source indicator (wearable vs manual)
 * - High contrast for outdoor visibility
 * - Large touch targets (48dp minimum)
 *
 * The HUD supports expansion to show detailed metric breakdown,
 * providing transparency into how the overall score is calculated.
 *
 * Color coding:
 * - 0-40: Red (low readiness, maximum conservatism)
 * - 41-60: Yellow (moderate readiness, increased safety margins)
 * - 61+: Green (high readiness, normal risk tolerance)
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 15
 * Acceptance criteria: A2 (Readiness impacts strategy)
 *
 * @param readiness Current readiness score with breakdown
 * @param showDetails True to show expanded metric breakdown
 * @param onToggleDetails Callback when user toggles details visibility
 * @param onManualOverride Callback when user sets manual readiness score
 * @param modifier Modifier for the HUD container
 */
@Composable
fun ReadinessHud(
    readiness: ReadinessScore?,
    showDetails: Boolean,
    onToggleDetails: (Boolean) -> Unit,
    onManualOverride: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleDetails(!showDetails) }
            .semantics {
                contentDescription = "Readiness score: ${readiness?.overall ?: "loading"} out of 100"
            },
        colors = CardDefaults.cardColors(
            containerColor = readiness?.let { readinessColor(it.overall) }?.copy(alpha = 0.15f)
                ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (readiness == null) {
            LoadingReadinessIndicator()
        } else {
            ReadinessContent(
                readiness = readiness,
                showDetails = showDetails,
                onManualOverrideClick = { showManualDialog = true }
            )
        }
    }

    // Manual override dialog
    if (showManualDialog && readiness != null) {
        ManualReadinessDialog(
            currentScore = readiness.overall,
            onDismiss = { showManualDialog = false },
            onConfirm = { score ->
                onManualOverride(score)
                showManualDialog = false
            }
        )
    }
}

/**
 * Loading indicator shown while readiness data is being fetched.
 *
 * Displays a centered progress indicator with message.
 */
@Composable
private fun LoadingReadinessIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.padding(8.dp))
        Text(
            text = "Loading readiness...",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Main readiness content display with compact and expanded modes.
 *
 * Compact mode (always visible):
 * - Circular readiness score indicator
 * - Source indicator (wearable/manual)
 * - Manual override button
 *
 * Expanded mode (when toggled):
 * - Breakdown chart showing HRV, sleep, and stress components
 *
 * @param readiness Current readiness score
 * @param showDetails True to show expanded breakdown
 * @param onManualOverrideClick Callback when manual override button is clicked
 */
@Composable
private fun ReadinessContent(
    readiness: ReadinessScore,
    showDetails: Boolean,
    onManualOverrideClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compact view (always visible)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Readiness score indicator - left side
            ReadinessScoreIndicator(
                score = readiness.overall,
                modifier = Modifier.weight(1f)
            )

            // Manual override button - right side
            IconButton(
                onClick = onManualOverrideClick,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Set readiness manually"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit readiness",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Source indicator
        Text(
            text = when (readiness.source) {
                ReadinessSource.WEARABLE_SYNC -> "From wearable"
                ReadinessSource.MANUAL_ENTRY -> "Manual entry"
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )

        // Expanded view with breakdown
        AnimatedVisibility(
            visible = showDetails,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                ReadinessBreakdownChart(breakdown = readiness.breakdown)
            }
        }
    }
}

/**
 * Manual readiness override dialog.
 *
 * Allows user to manually set their readiness score when wearables
 * are unavailable or when they want to override the automatic score.
 *
 * Uses a slider with 5-point increments (0, 5, 10, ..., 100) for
 * easy selection with large touch targets.
 *
 * @param currentScore Current readiness score to initialize slider
 * @param onDismiss Callback when dialog is dismissed without confirmation
 * @param onConfirm Callback with selected score when confirmed
 */
@Composable
fun ManualReadinessDialog(
    currentScore: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember { mutableFloatStateOf(currentScore.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Set Readiness Manually",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Override wearable data if unavailable or adjust based on how you feel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Large score display
                Text(
                    text = "${sliderValue.toInt()}",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = readinessColor(sliderValue.toInt())
                )

                Text(
                    text = readinessLabel(sliderValue.toInt()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Slider with 5-point increments
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..100f,
                    steps = 19,  // Creates 21 stops: 0, 5, 10, ..., 95, 100
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Readiness score slider from 0 to 100"
                        }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(sliderValue.toInt()) },
                modifier = Modifier.semantics {
                    contentDescription = "Confirm readiness score"
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics {
                    contentDescription = "Cancel manual readiness entry"
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Determine readiness color based on score.
 *
 * Color coding per spec:
 * - 0-40: Red (low readiness)
 * - 41-60: Yellow (moderate readiness)
 * - 61+: Green (high readiness)
 *
 * @param score Readiness score (0-100)
 * @return Color corresponding to the score
 */
internal fun readinessColor(score: Int): Color {
    return when {
        score >= 61 -> Color(0xFF4CAF50)  // Green
        score >= 41 -> Color(0xFFFFA726)  // Orange/Yellow
        else -> Color(0xFFEF5350)          // Red
    }
}

/**
 * Get readiness label text based on score.
 *
 * @param score Readiness score (0-100)
 * @return Human-readable label for the score
 */
private fun readinessLabel(score: Int): String {
    return when {
        score >= 80 -> "Excellent"
        score >= 61 -> "Good"
        score >= 41 -> "Fair"
        score >= 21 -> "Low"
        else -> "Very Low"
    }
}

// Preview functions for development and testing

@Preview(name = "Loading State", showBackground = true)
@Composable
private fun PreviewReadinessHudLoading() {
    CaddyProTheme {
        ReadinessHud(
            readiness = null,
            showDetails = false,
            onToggleDetails = {},
            onManualOverride = {}
        )
    }
}

@Preview(name = "Compact - High Readiness", showBackground = true)
@Composable
private fun PreviewReadinessHudCompactHigh() {
    CaddyProTheme {
        ReadinessHud(
            readiness = ReadinessScore(
                overall = 85,
                breakdown = ReadinessBreakdown(
                    hrv = MetricScore(value = 90.0, weight = 0.4),
                    sleepQuality = MetricScore(value = 85.0, weight = 0.4),
                    stressLevel = MetricScore(value = 75.0, weight = 0.2)
                ),
                timestamp = System.currentTimeMillis(),
                source = ReadinessSource.WEARABLE_SYNC
            ),
            showDetails = false,
            onToggleDetails = {},
            onManualOverride = {}
        )
    }
}

@Preview(name = "Expanded - High Readiness", showBackground = true)
@Composable
private fun PreviewReadinessHudExpandedHigh() {
    CaddyProTheme {
        ReadinessHud(
            readiness = ReadinessScore(
                overall = 85,
                breakdown = ReadinessBreakdown(
                    hrv = MetricScore(value = 90.0, weight = 0.4),
                    sleepQuality = MetricScore(value = 85.0, weight = 0.4),
                    stressLevel = MetricScore(value = 75.0, weight = 0.2)
                ),
                timestamp = System.currentTimeMillis(),
                source = ReadinessSource.WEARABLE_SYNC
            ),
            showDetails = true,
            onToggleDetails = {},
            onManualOverride = {}
        )
    }
}

@Preview(name = "Compact - Moderate Readiness", showBackground = true)
@Composable
private fun PreviewReadinessHudCompactModerate() {
    CaddyProTheme {
        ReadinessHud(
            readiness = ReadinessScore(
                overall = 55,
                breakdown = ReadinessBreakdown(
                    hrv = MetricScore(value = 60.0, weight = 0.4),
                    sleepQuality = MetricScore(value = 50.0, weight = 0.4),
                    stressLevel = MetricScore(value = 55.0, weight = 0.2)
                ),
                timestamp = System.currentTimeMillis(),
                source = ReadinessSource.WEARABLE_SYNC
            ),
            showDetails = false,
            onToggleDetails = {},
            onManualOverride = {}
        )
    }
}

@Preview(name = "Expanded - Low Readiness", showBackground = true)
@Composable
private fun PreviewReadinessHudExpandedLow() {
    CaddyProTheme {
        ReadinessHud(
            readiness = ReadinessScore(
                overall = 35,
                breakdown = ReadinessBreakdown(
                    hrv = MetricScore(value = 40.0, weight = 0.4),
                    sleepQuality = MetricScore(value = 30.0, weight = 0.4),
                    stressLevel = MetricScore(value = 35.0, weight = 0.2)
                ),
                timestamp = System.currentTimeMillis(),
                source = ReadinessSource.WEARABLE_SYNC
            ),
            showDetails = true,
            onToggleDetails = {},
            onManualOverride = {}
        )
    }
}

@Preview(name = "Manual Entry", showBackground = true)
@Composable
private fun PreviewReadinessHudManual() {
    CaddyProTheme {
        ReadinessHud(
            readiness = ReadinessScore(
                overall = 70,
                breakdown = ReadinessBreakdown(
                    hrv = null,
                    sleepQuality = null,
                    stressLevel = null
                ),
                timestamp = System.currentTimeMillis(),
                source = ReadinessSource.MANUAL_ENTRY
            ),
            showDetails = false,
            onToggleDetails = {},
            onManualOverride = {}
        )
    }
}

@Preview(name = "Manual Override Dialog - High", showBackground = true)
@Composable
private fun PreviewManualReadinessDialogHigh() {
    CaddyProTheme {
        ManualReadinessDialog(
            currentScore = 85,
            onDismiss = {},
            onConfirm = {}
        )
    }
}

@Preview(name = "Manual Override Dialog - Low", showBackground = true)
@Composable
private fun PreviewManualReadinessDialogLow() {
    CaddyProTheme {
        ManualReadinessDialog(
            currentScore = 30,
            onDismiss = {},
            onConfirm = {}
        )
    }
}
