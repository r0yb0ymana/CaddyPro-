package com.caddypro.app.ui.clubs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.ui.theme.Spacing

/**
 * Club List Screen
 *
 * AC11: Quick Add button for standard 14-club set
 * AC13: Clubs grouped by type (Driver, Woods, Hybrids, Irons, Wedges, Putter)
 * AC15: Warning at 14 clubs, block at 15
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubListScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClubListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onAction(ClubListAction.ClearError)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.bagName)
                        Text(
                            text = "${state.clubCount}/14 clubs",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.clubCount == 14) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!state.isAtMaxClubs()) {
                FloatingActionButton(
                    onClick = { viewModel.onAction(ClubListAction.ShowAddClub) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Add Club")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.clubs.isEmpty() -> {
                    EmptyClubsState(
                        onQuickAdd = { viewModel.onAction(ClubListAction.QuickAddStandardSet) },
                        onAddManually = { viewModel.onAction(ClubListAction.ShowAddClub) }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.ScreenPadding)
                    ) {
                        // AC15: Show warning at 14 clubs
                        if (state.showMaxClubsWarning) {
                            item {
                                MaxClubsWarningCard(
                                    onDismiss = {
                                        viewModel.onAction(ClubListAction.DismissMaxClubsWarning)
                                    }
                                )
                                Spacer(modifier = Modifier.height(Spacing.Medium))
                            }
                        }

                        // AC13: Group clubs by type (Driver first, Putter last)
                        state.clubsByType.forEach { (type, clubs) ->
                            item {
                                ClubTypeHeader(type = type)
                                Spacer(modifier = Modifier.height(Spacing.Small))
                            }

                            items(clubs, key = { it.id }) { club ->
                                ClubItem(
                                    club = club,
                                    onEdit = { viewModel.onAction(ClubListAction.EditClub(club)) },
                                    onDelete = { viewModel.onAction(ClubListAction.DeleteClub(club.id)) }
                                )
                                Spacer(modifier = Modifier.height(Spacing.Small))
                            }

                            item {
                                Spacer(modifier = Modifier.height(Spacing.Medium))
                            }
                        }
                    }
                }
            }
        }
    }

    // Show club detail sheet
    if (state.showClubDetail) {
        ClubDetailSheet(
            bagId = state.bagId,
            clubToEdit = state.clubToEdit,
            onDismiss = { viewModel.onAction(ClubListAction.DismissClubDetail) }
        )
    }
}

/**
 * Empty state with Quick Add option
 * AC11: Quick Add button for standard 14-club set
 */
@Composable
private fun EmptyClubsState(
    onQuickAdd: () -> Unit,
    onAddManually: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No clubs in this bag",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.Small))

        Text(
            text = "Add clubs to start tracking your distances",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

        // AC11: Quick Add standard 14-club set
        OutlinedButton(
            onClick = onQuickAdd,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Quick Add Standard 14-Club Set")
        }

        Spacer(modifier = Modifier.height(Spacing.Medium))

        TextButton(
            onClick = onAddManually,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Clubs Manually")
        }
    }
}

/**
 * AC15: Warning card shown when bag has 14 clubs (max limit)
 */
@Composable
private fun MaxClubsWarningCard(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(Spacing.Medium))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Maximum clubs reached",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "You have 14 clubs (tournament maximum). Delete a club to add another.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(Spacing.Small))

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * AC13: Club type header (Driver, Woods, Hybrids, etc.)
 */
@Composable
private fun ClubTypeHeader(type: ClubType) {
    Text(
        text = type.displayName,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = Spacing.Small)
    )
}

/**
 * Individual club item card
 */
@Composable
private fun ClubItem(
    club: Club,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Club type indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = club.type.displayName.take(1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(Spacing.Medium))

            // Club info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = club.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    // AC22: Distance values rendered in JetBrains Mono
                    Text(
                        text = "Carry: ${club.carryDistance}y",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = com.caddypro.app.ui.theme.JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        text = "Total: ${club.totalDistance}y",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = com.caddypro.app.ui.theme.JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (club.missBias != com.caddypro.app.domain.model.MissBias.STRAIGHT) {
                        Spacer(modifier = Modifier.width(Spacing.Small))
                        Text(
                            text = "• ${club.missBias.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete ${club.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
