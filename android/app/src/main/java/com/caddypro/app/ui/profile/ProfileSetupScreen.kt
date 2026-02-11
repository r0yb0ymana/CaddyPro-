package com.caddypro.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caddypro.app.data.local.entities.PreferredUnits
import com.caddypro.app.ui.theme.Spacing

/**
 * ProfileSetupScreen
 *
 * First-launch screen for creating player profile.
 * Required fields: Display Name
 * Optional fields: Handicap Index (0.0-54.0), Home Course
 * Toggle: Preferred Units (Metric/Imperial, defaults to Metric)
 */
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = hiltViewModel(),
    onProfileCreated: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate away when profile is saved
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onProfileCreated()
        }
    }

    // Show error snackbar if present
    if (state.errorMessage != null) {
        LaunchedEffect(state.errorMessage) {
            // Error is shown inline on fields
            // Could add Snackbar here if needed
        }
    }

    Scaffold(
        topBar = {
            ProfileSetupTopBar()
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
                ProfileSetupContent(
                    state = state,
                    onAction = viewModel::onAction
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSetupTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "Welcome to CaddyPro",
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
private fun ProfileSetupContent(
    state: ProfileSetupState,
    onAction: (ProfileSetupAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.ScreenPadding)
    ) {
        // Intro text
        Text(
            text = "Let's set up your profile to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = Spacing.Large)
        )

        // Display Name Field (Required)
        OutlinedTextField(
            value = state.displayName,
            onValueChange = { onAction(ProfileSetupAction.UpdateDisplayName(it)) },
            label = { Text("Display Name *") },
            placeholder = { Text("Enter your name") },
            isError = state.displayNameError != null,
            supportingText = {
                state.displayNameError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Handicap Index Field (Optional)
        OutlinedTextField(
            value = state.handicapIndex,
            onValueChange = { onAction(ProfileSetupAction.UpdateHandicapIndex(it)) },
            label = { Text("Handicap Index") },
            placeholder = { Text("0.0 - 54.0") },
            isError = state.handicapError != null,
            supportingText = {
                if (state.handicapError != null) {
                    Text(
                        text = state.handicapError,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Optional: One decimal place (e.g., 12.5)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Preferred Units Toggle
        PreferredUnitsToggle(
            selectedUnits = state.preferredUnits,
            onUnitsSelected = { onAction(ProfileSetupAction.UpdatePreferredUnits(it)) },
            modifier = Modifier.padding(bottom = Spacing.Large)
        )

        // Home Course Field (Optional)
        OutlinedTextField(
            value = state.homeCourse,
            onValueChange = { onAction(ProfileSetupAction.UpdateHomeCourse(it)) },
            label = { Text("Home Course") },
            placeholder = { Text("Your usual golf course") },
            supportingText = {
                Text(
                    text = "Optional: Where do you usually play?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.ExtraLarge),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Save Button
        Button(
            onClick = { onAction(ProfileSetupAction.SaveProfile) },
            enabled = state.isValid() && !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.MinTouchTarget),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.padding(end = Spacing.Small)
            )
            Text(
                text = "Complete Setup",
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Required fields note
        Text(
            text = "* Required field",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.Medium)
        )
    }
}

@Composable
private fun PreferredUnitsToggle(
    selectedUnits: PreferredUnits,
    onUnitsSelected: (PreferredUnits) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Preferred Distance Units",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = Spacing.Small)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            // Metric Button
            FilterChip(
                selected = selectedUnits == PreferredUnits.METRIC,
                onClick = { onUnitsSelected(PreferredUnits.METRIC) },
                label = {
                    Text(
                        text = "Metric (metres)",
                        modifier = Modifier.padding(horizontal = Spacing.Small)
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            // Imperial Button
            FilterChip(
                selected = selectedUnits == PreferredUnits.IMPERIAL,
                onClick = { onUnitsSelected(PreferredUnits.IMPERIAL) },
                label = {
                    Text(
                        text = "Imperial (yards)",
                        modifier = Modifier.padding(horizontal = Spacing.Small)
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        Text(
            text = "Default: Metric (Australia)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.Small)
        )
    }
}
