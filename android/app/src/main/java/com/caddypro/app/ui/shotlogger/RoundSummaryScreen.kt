package com.caddypro.app.ui.shotlogger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caddypro.app.domain.model.ShotType
import com.caddypro.app.ui.theme.CaddyProColors
import com.caddypro.app.ui.theme.DataTextStyles
import com.caddypro.app.ui.theme.JetBrainsMonoFontFamily
import com.caddypro.app.ui.theme.Spacing

/**
 * Round Summary Screen
 *
 * Displayed after ending a round. Shows per-hole grid, club usage, shot type breakdown.
 * AC16: Hole summary shows shots per hole
 * AC17: Round summary shows per-hole breakdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundSummaryScreen(
    viewModel: RoundSummaryViewModel = hiltViewModel(),
    onNavigateHome: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Round Summary", style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (state.errorMessage != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Spacing.Medium),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    Button(onClick = onNavigateHome) {
                        Text("Go Home")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
                ) {
                    // Header card
                    item {
                        SummaryHeaderCard(state)
                    }

                    // Hole-by-hole grid
                    item {
                        HoleShotGrid(
                            holesPlayed = state.holesPlayed,
                            holeShotCounts = state.holeShotCounts
                        )
                    }

                    // Shot type breakdown
                    if (state.shotTypeBreakdown.isNotEmpty()) {
                        item {
                            ShotTypeBreakdownCard(
                                breakdown = state.shotTypeBreakdown,
                                totalShots = state.totalShots
                            )
                        }
                    }

                    // Club usage
                    if (state.clubUsage.isNotEmpty()) {
                        item {
                            ClubUsageCard(
                                clubUsage = state.clubUsage,
                                totalShots = state.totalShots
                            )
                        }
                    }

                    // Done button
                    item {
                        Spacer(modifier = Modifier.height(Spacing.Medium))
                        Button(
                            onClick = onNavigateHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Spacing.MinTouchTarget),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.padding(end = Spacing.Small)
                            )
                            Text("Done", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(Spacing.Medium))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeaderCard(state: RoundSummaryState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SportsGolf,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = state.courseName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatItem(label = "Total Shots", value = "${state.totalShots}")
                StatItem(label = "Holes", value = "${state.holesPlayed}")
                if (state.durationMinutes > 0) {
                    StatItem(
                        label = "Duration",
                        value = "${state.durationMinutes}m"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = DataTextStyles.DataSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoleShotGrid(
    holesPlayed: Int,
    holeShotCounts: Map<Int, Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(
                text = "Shots per Hole",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.Small)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                for (hole in 1..holesPlayed) {
                    val count = holeShotCounts[hole] ?: 0
                    HoleChip(holeNumber = hole, shotCount = count)
                }
            }
        }
    }
}

@Composable
private fun HoleChip(holeNumber: Int, shotCount: Int) {
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 48.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(Spacing.Small)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "H$holeNumber",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$shotCount",
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (shotCount > 0) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
private fun ShotTypeBreakdownCard(
    breakdown: Map<ShotType, Int>,
    totalShots: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(
                text = "Shot Types",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.Small)
            )
            breakdown.entries.sortedByDescending { it.value }.forEach { (type, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.ExtraSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (type == ShotType.PENALTY) CaddyProColors.DangerZone
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$count",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            text = if (totalShots > 0) "${(count * 100 / totalShots)}%" else "0%",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubUsageCard(
    clubUsage: List<ClubUsageStat>,
    totalShots: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(
                text = "Club Usage",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.Small)
            )
            clubUsage.forEach { stat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.ExtraSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stat.clubName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${stat.count}",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            text = if (totalShots > 0) "${(stat.count * 100 / totalShots)}%" else "0%",
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
