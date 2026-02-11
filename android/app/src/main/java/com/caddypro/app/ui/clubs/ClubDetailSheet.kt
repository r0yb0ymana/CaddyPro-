package com.caddypro.app.ui.clubs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias
import com.caddypro.app.ui.theme.Spacing

/**
 * Club Detail Bottom Sheet
 *
 * AC12: Validate carry <= total distance
 * AC14: Miss bias visual selector
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDetailSheet(
    bagId: String,
    clubToEdit: Club?,
    onDismiss: () -> Unit,
    viewModel: ClubDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Initialize ViewModel
    LaunchedEffect(Unit) {
        if (clubToEdit != null) {
            viewModel.initForEdit(clubToEdit)
        } else {
            viewModel.initForAdd(bagId)
        }
    }

    // Close sheet when saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Large)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            Text(
                text = if (clubToEdit != null) "Edit Club" else "Add Club",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(Spacing.Large))

            // Club Name
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onAction(ClubDetailAction.UpdateName(it)) },
                label = { Text("Club Name") },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Club Type Dropdown
            ClubTypeDropdown(
                selectedType = state.clubType,
                onTypeSelected = { viewModel.onAction(ClubDetailAction.UpdateClubType(it)) }
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Loft (optional)
            OutlinedTextField(
                value = state.loft,
                onValueChange = { viewModel.onAction(ClubDetailAction.UpdateLoft(it)) },
                label = { Text("Loft (degrees) - Optional") },
                isError = state.loftError != null,
                supportingText = state.loftError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Carry Distance
            OutlinedTextField(
                value = state.carryDistance,
                onValueChange = { viewModel.onAction(ClubDetailAction.UpdateCarryDistance(it)) },
                label = { Text("Carry Distance (yards)") },
                isError = state.carryDistanceError != null,
                supportingText = state.carryDistanceError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Total Distance
            OutlinedTextField(
                value = state.totalDistance,
                onValueChange = { viewModel.onAction(ClubDetailAction.UpdateTotalDistance(it)) },
                label = { Text("Total Distance (yards)") },
                isError = state.totalDistanceError != null,
                supportingText = state.totalDistanceError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.Large))

            // AC14: Miss Bias Visual Selector
            Text(
                text = "Miss Bias",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            MissBiasSelector(
                selectedBias = state.missBias,
                onBiasSelected = { viewModel.onAction(ClubDetailAction.UpdateMissBias(it)) }
            )

            Spacer(modifier = Modifier.height(Spacing.ExtraLarge))

            // Error message
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = Spacing.Medium)
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = { viewModel.onAction(ClubDetailAction.SaveClub) },
                    enabled = !state.isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (clubToEdit != null) "Update" else "Add")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))
        }
    }
}

/**
 * Club Type Dropdown
 */
@Composable
private fun ClubTypeDropdown(
    selectedType: ClubType,
    onTypeSelected: (ClubType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedTextField(
            value = selectedType.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Club Type") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ClubType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * AC14: Miss Bias Visual Selector with ball flight diagram
 */
@Composable
private fun MissBiasSelector(
    selectedBias: MissBias,
    onBiasSelected: (MissBias) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .padding(Spacing.Medium)
    ) {
        // Visual representation (simplified for now)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MissBias.values().forEach { bias ->
                MissBiasOption(
                    bias = bias,
                    isSelected = selectedBias == bias,
                    onClick = { onBiasSelected(bias) }
                )
            }
        }
    }
}

/**
 * Individual miss bias option
 */
@Composable
private fun MissBiasOption(
    bias: MissBias,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(Spacing.Small)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                // Simple visual indicator for bias type
                Text(
                    text = when (bias) {
                        MissBias.STRAIGHT -> "→"
                        MissBias.SLICE -> "↗"
                        MissBias.HOOK -> "↖"
                        MissBias.PUSH -> "⇗"
                        MissBias.PULL -> "⇖"
                    },
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))

        Text(
            text = bias.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
