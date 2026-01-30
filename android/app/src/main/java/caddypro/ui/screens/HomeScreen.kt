package caddypro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caddypro.R
import caddypro.domain.navcaddy.context.CourseConditions
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.context.SessionContextManager
import caddypro.ui.theme.AppTheme

/**
 * Home screen for CaddyPro application.
 *
 * Provides navigation entry points to main features including Live Caddy Mode.
 * Shows active round status with resume capability, or start round prompt when no round is active.
 *
 * Features:
 * - Resume Round card when round is in progress (Task 25)
 * - Start Round button when no active round (Task 25)
 * - Quick action cards for Weather and Strategy when round is active (Task 25)
 * - Material 3 design with clear visual hierarchy
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context - Start/Resume/End a round)
 * Plan reference: live-caddy-mode-plan.md Task 25
 *
 * @param sessionContextManager Session context manager for round state
 * @param onNavigateToDetail Callback to navigate to detail screen (legacy)
 * @param onNavigateToLiveCaddy Callback to navigate to Live Caddy Mode
 * @param onStartRound Callback to navigate to start round flow
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessionContextManager: SessionContextManager = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToLiveCaddy: () -> Unit = {},
    onStartRound: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context by sessionContextManager.context.collectAsStateWithLifecycle()
    val roundState = sessionContextManager.getCurrentRoundState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome section
            Text(
                text = "Welcome to CaddyPro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your intelligent golf caddy",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Hero section - Resume Round or Start Round
            if (roundState != null) {
                ResumeRoundCard(
                    roundState = roundState,
                    onResume = onNavigateToLiveCaddy
                )
            } else {
                StartRoundCard(
                    onStartRound = onStartRound
                )
            }

            // Quick actions (only shown when round is active)
            if (roundState != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                QuickActionGrid(
                    onNavigateToLiveCaddy = onNavigateToLiveCaddy,
                    hasActiveRound = true
                )
            }
        }
    }
}

/**
 * Card showing active round information with resume button.
 *
 * Displays:
 * - Course name
 * - Current hole and total holes
 * - Total score
 * - Resume Round button
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 25
 *
 * @param roundState Active round state
 * @param onResume Callback when user taps Resume Round
 * @param modifier Optional modifier for the card
 */
@Composable
private fun ResumeRoundCard(
    roundState: RoundState,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Round in Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = roundState.courseName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Show hole progress and score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hole ${roundState.currentHole} of 18",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (roundState.holesCompleted > 0) {
                    Text(
                        text = "${roundState.totalScore} total",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Show course conditions if available
            roundState.conditions?.let { conditions ->
                val conditionsText = conditions.toDescription()
                if (conditionsText.isNotBlank()) {
                    Text(
                        text = conditionsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Resume round at ${roundState.courseName} on hole ${roundState.currentHole}"
                    }
            ) {
                Text("Resume Round")
            }
        }
    }
}

/**
 * Card prompting user to start a new round.
 *
 * Shown when no active round is in progress. Provides a prominent CTA to start a round
 * and access Live Caddy features.
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 25
 *
 * @param onStartRound Callback when user taps Start Round
 * @param modifier Optional modifier for the card
 */
@Composable
private fun StartRoundCard(
    onStartRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Start a Round",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = "Access Live Caddy features including weather forecasting, readiness tracking, and hole strategy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onStartRound,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Start a new round to access Live Caddy features"
                    }
            ) {
                Text("Start Round")
            }
        }
    }
}

/**
 * Grid of quick action cards for Live Caddy features.
 *
 * Shows contextual quick actions when a round is active:
 * - Weather: Navigate to Live Caddy weather HUD
 * - Strategy: Navigate to Live Caddy hole strategy
 *
 * Spec reference: live-caddy-mode.md R2 (Forecaster HUD), R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 25
 *
 * @param onNavigateToLiveCaddy Callback to navigate to Live Caddy screen
 * @param hasActiveRound True if a round is currently active
 * @param modifier Optional modifier for the grid
 */
@Composable
private fun QuickActionGrid(
    onNavigateToLiveCaddy: () -> Unit,
    hasActiveRound: Boolean,
    modifier: Modifier = Modifier
) {
    if (!hasActiveRound) return

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.height(120.dp)
    ) {
        item {
            QuickActionCard(
                icon = Icons.Default.Cloud,
                label = "Weather",
                onClick = onNavigateToLiveCaddy
            )
        }

        item {
            QuickActionCard(
                icon = Icons.Default.Map,
                label = "Strategy",
                onClick = onNavigateToLiveCaddy
            )
        }
    }
}

/**
 * Individual quick action card.
 *
 * A compact card with an icon and label for quick navigation to Live Caddy features.
 * Uses Material 3 outlined card style with minimum 48dp touch target.
 *
 * @param icon Icon for the action
 * @param label Label text
 * @param onClick Callback when card is tapped
 * @param modifier Optional modifier for the card
 */
@Composable
private fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ============================================================================
// Preview Composables
// ============================================================================

@Preview(name = "Home Screen - No Active Round", showBackground = true)
@Composable
private fun HomeScreenNoRoundPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to CaddyPro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your intelligent golf caddy",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            StartRoundCard(onStartRound = {})
        }
    }
}

@Preview(name = "Home Screen - Active Round", showBackground = true)
@Composable
private fun HomeScreenActiveRoundPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Welcome to CaddyPro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Your intelligent golf caddy",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            ResumeRoundCard(
                roundState = RoundState(
                    roundId = "round-123",
                    courseName = "Pebble Beach",
                    currentHole = 7,
                    currentPar = 4,
                    totalScore = 38,
                    holesCompleted = 6,
                    conditions = CourseConditions(
                        weather = "Sunny",
                        windSpeed = 12,
                        windDirection = "NW",
                        temperature = 72
                    )
                ),
                onResume = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            QuickActionGrid(
                onNavigateToLiveCaddy = {},
                hasActiveRound = true
            )
        }
    }
}

@Preview(name = "Resume Round Card", showBackground = true)
@Composable
private fun ResumeRoundCardPreview() {
    AppTheme {
        ResumeRoundCard(
            roundState = RoundState(
                roundId = "round-123",
                courseName = "Augusta National",
                currentHole = 14,
                currentPar = 4,
                totalScore = 82,
                holesCompleted = 13,
                conditions = CourseConditions(
                    weather = "Partly cloudy",
                    windSpeed = 8,
                    windDirection = "SW",
                    temperature = 78
                )
            ),
            onResume = {}
        )
    }
}

@Preview(name = "Start Round Card", showBackground = true)
@Composable
private fun StartRoundCardPreview() {
    AppTheme {
        StartRoundCard(onStartRound = {})
    }
}

@Preview(name = "Quick Action Card", showBackground = true)
@Composable
private fun QuickActionCardPreview() {
    AppTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.Cloud,
                label = "Weather",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                icon = Icons.Default.Map,
                label = "Strategy",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}
