package com.caddypro.app.ui.shotlogger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.Shot
import com.caddypro.app.domain.model.ShotType
import com.caddypro.app.ui.theme.CaddyProColors
import com.caddypro.app.ui.theme.DataTextStyles
import com.caddypro.app.ui.theme.JetBrainsMonoFontFamily
import com.caddypro.app.ui.theme.Spacing

/**
 * Shot Logger Screen (Active Round)
 *
 * Primary screen during an active round. Minimal taps to log shots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShotLoggerScreen(
    viewModel: ShotLoggerViewModel = hiltViewModel(),
    onRoundEnded: (String) -> Unit,
    onNavigateToMap: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.roundEnded) {
        if (state.roundEnded) {
            onRoundEnded(state.roundId)
        }
    }

    // Undo confirmation dialog
    if (state.showUndoConfirmation) {
        UndoConfirmationDialog(
            onConfirm = { viewModel.onAction(ShotLoggerAction.ConfirmUndo) },
            onDismiss = { viewModel.onAction(ShotLoggerAction.DismissUndo) }
        )
    }

    // End round confirmation dialog
    if (state.showEndRoundConfirmation) {
        EndRoundConfirmationDialog(
            totalShots = state.totalShots,
            onConfirm = { viewModel.onAction(ShotLoggerAction.ConfirmEndRound) },
            onDismiss = { viewModel.onAction(ShotLoggerAction.DismissEndRound) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.courseName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    // Map button
                    IconButton(onClick = onNavigateToMap) {
                        Icon(Icons.Default.Map, contentDescription = "Hole Map",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    // End Round button
                    IconButton(onClick = { viewModel.onAction(ShotLoggerAction.ShowEndRound) }) {
                        Icon(Icons.Default.Stop, contentDescription = "End Round",
                            tint = CaddyProColors.DangerZone)
                    }
                },
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
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Hole header (AC14: prominent)
                    HoleHeader(
                        holeNumber = state.currentHole,
                        shotCount = state.shotCountForHole,
                        totalShots = state.totalShots
                    )

                    // Club quick-select (AC6)
                    ClubQuickSelect(
                        clubs = state.clubs,
                        selectedClub = state.selectedClub,
                        lastSavedClubId = state.lastSavedClubId,
                        onClubSelected = { viewModel.onAction(ShotLoggerAction.SelectClub(it)) }
                    )

                    Spacer(modifier = Modifier.height(Spacing.Medium))

                    // Shot type selector (AC7)
                    ShotTypeSelector(
                        selectedType = state.selectedShotType,
                        onTypeSelected = { viewModel.onAction(ShotLoggerAction.SelectShotType(it)) }
                    )

                    Spacer(modifier = Modifier.height(Spacing.Large))

                    // Log Shot button (AC8, AC28)
                    Button(
                        onClick = { viewModel.onAction(ShotLoggerAction.LogShot) },
                        enabled = state.canLogShot,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = Spacing.Medium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Log Shot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.Medium))

                    // Action bar
                    ActionBar(
                        canUndo = state.canUndo,
                        canAdvance = state.canAdvanceHole,
                        onUndo = { viewModel.onAction(ShotLoggerAction.ShowUndoConfirmation) },
                        onNextHole = { viewModel.onAction(ShotLoggerAction.NextHole) },
                        onToggleSummary = { viewModel.onAction(ShotLoggerAction.ToggleHoleSummary) }
                    )

                    // Hole summary (AC17)
                    if (state.showHoleSummary && state.shotsForCurrentHole.isNotEmpty()) {
                        HoleShotList(shots = state.shotsForCurrentHole)
                    }
                }

                // Error message
                state.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(Spacing.Medium),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(Spacing.Medium),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoleHeader(
    holeNumber: Int,
    shotCount: Int,
    totalShots: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            // AC14: Hole number displayed prominently
            Text(
                text = "Hole $holeNumber",
                style = DataTextStyles.DataMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            // AC15: Shot count visible
            Text(
                text = "Shot ${shotCount + 1}",
                style = DataTextStyles.DataSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Total: $totalShots",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClubQuickSelect(
    clubs: List<Club>,
    selectedClub: Club?,
    lastSavedClubId: String?,
    onClubSelected: (Club) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.Medium)) {
        Text(
            text = "Club",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            contentPadding = PaddingValues(end = Spacing.Medium)
        ) {
            items(clubs.filter { it.type != ClubType.PUTTER || true }) { club ->
                val isSelected = selectedClub?.id == club.id
                val wasLastUsed = lastSavedClubId == club.id

                FilterChip(
                    selected = isSelected,
                    onClick = { onClubSelected(club) },
                    label = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = Spacing.ExtraSmall)
                        ) {
                            Text(
                                text = club.name,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                            if (club.carryDistance > 0) {
                                Text(
                                    text = "${club.carryDistance}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = if (wasLastUsed && !isSelected)
                            MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}

@Composable
private fun ShotTypeSelector(
    selectedType: ShotType,
    onTypeSelected: (ShotType) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = Spacing.Medium)) {
        Text(
            text = "Shot Type",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            items(ShotType.values().toList()) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (type == ShotType.PENALTY)
                            CaddyProColors.DangerZone
                        else MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun ActionBar(
    canUndo: Boolean,
    canAdvance: Boolean,
    onUndo: () -> Unit,
    onNextHole: () -> Unit,
    onToggleSummary: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Undo
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = Modifier.size(Spacing.MinTouchTarget)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo",
                    tint = if (canUndo) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                Text("Undo", style = MaterialTheme.typography.labelSmall,
                    color = if (canUndo) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            }
        }

        // Hole summary
        IconButton(
            onClick = onToggleSummary,
            modifier = Modifier.size(Spacing.MinTouchTarget)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Shots",
                    tint = MaterialTheme.colorScheme.onSurface)
                Text("Shots", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // Next hole
        IconButton(
            onClick = onNextHole,
            enabled = canAdvance,
            modifier = Modifier.size(Spacing.MinTouchTarget)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Hole",
                    tint = if (canAdvance) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                Text("Next", style = MaterialTheme.typography.labelSmall,
                    color = if (canAdvance) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
            }
        }
    }
}

@Composable
private fun HoleShotList(shots: List<Shot>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(Spacing.Medium)) {
            Text(
                text = "Hole Shots",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = Spacing.Small)
            )
            shots.forEach { shot ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.ExtraSmall),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "#${shot.shotNumber}",
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = shot.clubName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = shot.shotType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UndoConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Undo Last Shot?") },
        text = { Text("This will remove the last shot logged on this hole.") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Undo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun EndRoundConfirmationDialog(
    totalShots: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("End Round?") },
        text = { Text("You've logged $totalShots shots. End this round?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CaddyProColors.DangerZone,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text("End Round") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
