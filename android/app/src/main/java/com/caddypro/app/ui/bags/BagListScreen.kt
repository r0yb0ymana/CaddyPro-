package com.caddypro.app.ui.bags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caddypro.app.domain.model.Bag
import com.caddypro.app.ui.theme.Spacing

/**
 * BagListScreen
 *
 * Displays list of bags with:
 * - Active badge on the active bag
 * - Tap to view/edit clubs
 * - FAB to create new bag
 * - Swipe to delete with confirmation
 * - Active bag toggle
 */
@Composable
fun BagListScreen(
    viewModel: BagListViewModel = hiltViewModel(),
    onNavigateToClubEditor: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Delete confirmation dialog
    if (state.showDeleteConfirmation && state.bagToDelete != null) {
        DeleteConfirmationDialog(
            bag = state.bagToDelete!!,
            onConfirm = { viewModel.onAction(BagListAction.ConfirmDelete) },
            onDismiss = { viewModel.onAction(BagListAction.DismissDeleteConfirmation) }
        )
    }

    // Error snackbar
    if (state.errorMessage != null) {
        androidx.compose.runtime.LaunchedEffect(state.errorMessage) {
            // Could show a Snackbar here
            // For now, error is displayed inline
        }
    }

    Scaffold(
        topBar = {
            BagListTopBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(BagListAction.CreateNewBag) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new bag"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                state.bags.isEmpty() -> {
                    EmptyBagList(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    BagListContent(
                        bags = state.bags,
                        onBagClick = { onNavigateToClubEditor(it.id) },
                        onSetActive = { viewModel.onAction(BagListAction.SetActiveBag(it)) },
                        onDelete = { viewModel.onAction(BagListAction.ShowDeleteConfirmation(it)) }
                    )
                }
            }

            // Error message
            state.errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Spacing.Medium),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BagListTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "My Bags",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun BagListContent(
    bags: List<Bag>,
    onBagClick: (Bag) -> Unit,
    onSetActive: (Bag) -> Unit,
    onDelete: (Bag) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.Medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
    ) {
        items(
            items = bags,
            key = { it.id }
        ) { bag ->
            SwipeToDismissBagItem(
                bag = bag,
                onBagClick = onBagClick,
                onSetActive = onSetActive,
                onDelete = onDelete
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissBagItem(
    bag: Bag,
    onBagClick: (Bag) -> Unit,
    onSetActive: (Bag) -> Unit,
    onDelete: (Bag) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(bag)
                false // Don't actually dismiss, show confirmation dialog
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            DismissBackground(dismissState)
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        BagItem(
            bag = bag,
            onBagClick = onBagClick,
            onSetActive = onSetActive
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DismissBackground(dismissState: SwipeToDismissBoxState) {
    val color = when (dismissState.targetValue) {
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = Spacing.Large),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun BagItem(
    bag: Bag,
    onBagClick: (Bag) -> Unit,
    onSetActive: (Bag) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onBagClick(bag) },
        colors = CardDefaults.cardColors(
            containerColor = if (bag.isActive) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bag info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Text(
                        text = bag.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (bag.isActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(
                                    horizontal = Spacing.Small,
                                    vertical = 2.dp
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = "${bag.clubCount} clubs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Active toggle
            if (!bag.isActive) {
                IconButton(
                    onClick = { onSetActive(bag) }
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Set as active",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active bag",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(Spacing.Medium)
                )
            }
        }
    }
}

@Composable
private fun EmptyBagList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No bags yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = "Tap the + button to create your first bag",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    bag: Bag,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete \"${bag.name}\"?")
        },
        text = {
            Column {
                Text("This will permanently delete this bag and all ${bag.clubCount} clubs in it.")
                if (bag.isActive) {
                    Spacer(modifier = Modifier.height(Spacing.Small))
                    Text(
                        "The next bag will be set as active.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
