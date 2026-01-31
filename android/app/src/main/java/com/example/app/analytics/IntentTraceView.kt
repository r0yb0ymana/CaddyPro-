package caddypro.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caddypro.BuildConfig
import kotlinx.coroutines.flow.runningFold

/**
 * Debug trace view for QA builds.
 *
 * Shows recent analytics events with visual latency breakdown.
 * Only displayed when BuildConfig.DEBUG is true.
 *
 * Spec reference: navcaddy-engine.md R8
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
@Composable
fun IntentTraceView(
    analytics: NavCaddyAnalytics,
    modifier: Modifier = Modifier
) {
    // Only show in debug builds
    if (!BuildConfig.DEBUG) return

    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Floating debug button
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.tertiary
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.BugReport,
                contentDescription = if (isExpanded) "Close trace" else "Open trace"
            )
        }

        // Trace panel
        AnimatedVisibility(
            visible = isExpanded,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            TracePanel(analytics)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TracePanel(
    analytics: NavCaddyAnalytics,
    modifier: Modifier = Modifier
) {
    val events by remember(analytics) {
        analytics.eventStream.runningFold(emptyList<AnalyticsEvent>()) { acc, event ->
            (acc + event).takeLast(100)
        }
    }.collectAsState(initial = emptyList())

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Intent Trace",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Session: ${analytics.getSessionId()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Events list
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events.takeLast(20).reversed()) { event ->
                    EventItem(event)
                }

                if (events.isEmpty()) {
                    item {
                        Text(
                            text = "No events yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItem(
    event: AnalyticsEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header with event type and indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(getEventColor(event))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Event type
                Text(
                    text = event::class.simpleName ?: "Unknown",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.weight(1f))

                // Timestamp
                Text(
                    text = formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Event details
            when (event) {
                is AnalyticsEvent.InputReceived -> {
                    DetailRow("Type", event.inputType.name)
                    DetailRow("Length", "${event.inputLength} chars")
                }

                is AnalyticsEvent.IntentClassified -> {
                    DetailRow("Intent", event.intent)
                    DetailRow("Confidence", "${(event.confidence * 100).toInt()}%")
                    LatencyBar(event.latencyMs)
                }

                is AnalyticsEvent.RouteExecuted -> {
                    DetailRow("Module", event.module)
                    DetailRow("Screen", event.screen)
                    LatencyBar(event.latencyMs)
                }

                is AnalyticsEvent.ErrorOccurred -> {
                    DetailRow("Type", event.errorType.name)
                    DetailRow("Message", event.message)
                    DetailRow("Recoverable", if (event.isRecoverable) "Yes" else "No")
                }

                is AnalyticsEvent.VoiceTranscription -> {
                    DetailRow("Words", event.wordCount.toString())
                    LatencyBar(event.latencyMs)
                }

                is AnalyticsEvent.ClarificationRequested -> {
                    DetailRow("Confidence", "${(event.confidence * 100).toInt()}%")
                    DetailRow("Suggestions", event.suggestionsCount.toString())
                }

                is AnalyticsEvent.SuggestionSelected -> {
                    DetailRow("Intent", event.intentType)
                    DetailRow("Index", event.suggestionIndex.toString())
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LatencyBar(
    latencyMs: Long,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Latency: ${latencyMs}ms",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Visual bar
        val maxLatency = 3000f // 3 seconds max for visualization
        val fraction = (latencyMs / maxLatency).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(getLatencyColor(latencyMs))
            )
        }
    }
}

@Composable
private fun getEventColor(event: AnalyticsEvent): Color {
    return when (event) {
        is AnalyticsEvent.InputReceived -> Color(0xFF4CAF50) // Green
        is AnalyticsEvent.IntentClassified -> Color(0xFF2196F3) // Blue
        is AnalyticsEvent.RouteExecuted -> Color(0xFF9C27B0) // Purple
        is AnalyticsEvent.ErrorOccurred -> Color(0xFFF44336) // Red
        is AnalyticsEvent.VoiceTranscription -> Color(0xFFFF9800) // Orange
        is AnalyticsEvent.ClarificationRequested -> Color(0xFFFFEB3B) // Yellow
        is AnalyticsEvent.SuggestionSelected -> Color(0xFF00BCD4) // Cyan
    }
}

private fun getLatencyColor(latencyMs: Long): Color {
    return when {
        latencyMs < 1000 -> Color(0xFF4CAF50) // Green - fast
        latencyMs < 2000 -> Color(0xFFFFEB3B) // Yellow - acceptable
        latencyMs < 3000 -> Color(0xFFFF9800) // Orange - slow
        else -> Color(0xFFF44336) // Red - very slow
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return android.text.format.DateFormat.format("HH:mm:ss", timestamp).toString()
}
