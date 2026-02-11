package com.caddypro.app.ui.holemap

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caddypro.app.domain.model.HazardType
import com.caddypro.app.ui.theme.CaddyProColors
import com.caddypro.app.ui.theme.DataTextStyles
import com.caddypro.app.ui.theme.Spacing
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Hole Map Screen
 *
 * Satellite map with hazard overlays, distance HUD, and hole navigation.
 * AC1: Satellite imagery renders
 * AC2: Map centers on player GPS location
 * AC3: Player position shown
 * AC4: Hazard overlays rendered as colored polygons
 * AC5: Green center markers (PerformanceLime)
 * AC8: Distance to green displayed prominently
 * AC9: Hole navigation (prev/next)
 * AC22: Flag incorrect data button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoleMapScreen(
    viewModel: HoleMapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onAction(HoleMapAction.LocationPermissionGranted)
        } else {
            viewModel.onAction(HoleMapAction.LocationPermissionDenied)
        }
    }

    // Request permission if not granted
    LaunchedEffect(state.hasLocationPermission) {
        if (!state.hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Camera position state
    val cameraPositionState = rememberCameraPositionState()

    // Center camera when player location becomes available
    LaunchedEffect(state.playerLatitude, state.playerLongitude) {
        val lat = state.playerLatitude
        val lon = state.playerLongitude
        if (lat != null && lon != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(LatLng(lat, lon), 17f)
                )
            )
        }
    }

    // Map properties
    val mapProperties = remember(state.hasLocationPermission) {
        MapProperties(
            mapType = MapType.SATELLITE,
            isMyLocationEnabled = state.hasLocationPermission
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true,
            mapToolbarEnabled = false
        )
    }

    // Flag dialog
    if (state.showFlagDialog) {
        FlagIncorrectDataDialog(
            onDismiss = { viewModel.onAction(HoleMapAction.DismissFlagDialog) },
            onSubmit = { description -> viewModel.onAction(HoleMapAction.SubmitFlag(description)) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.displayHoleNumber, style = MaterialTheme.typography.titleMedium)
                        state.courseName?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.hasCourseData) {
                        IconButton(onClick = { viewModel.onAction(HoleMapAction.ShowFlagDialog) }) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = "Flag incorrect data",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
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
                // Loading state
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(Spacing.Medium))
                    Text(
                        text = "Getting location...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (!state.hasLocationPermission) {
                // No permission state
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = Spacing.Medium),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Location permission required",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Map view with overlays
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = mapProperties,
                    uiSettings = mapUiSettings,
                    onMapLoaded = { viewModel.onAction(HoleMapAction.MapReady) }
                ) {
                    // AC4: Hazard polygon overlays
                    state.hazards.forEach { hazard ->
                        if (hazard.coordinates.size >= 3) {
                            val points = hazard.coordinates.map { LatLng(it.latitude, it.longitude) }
                            val (fillColor, strokeColor) = hazardColors(hazard.type)
                            Polygon(
                                points = points,
                                fillColor = fillColor,
                                strokeColor = strokeColor,
                                strokeWidth = 2f
                            )
                        }
                    }

                    // AC5: Green center markers
                    state.greens.forEach { green ->
                        val isSelected = state.selectedGreen?.id == green.id
                        Marker(
                            state = MarkerState(position = LatLng(green.latitude, green.longitude)),
                            title = green.holeNumber?.let { "Hole $it" } ?: "Green",
                            alpha = if (isSelected) 1f else 0.6f,
                            icon = BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN
                            )
                        )
                    }
                }

                // AC8: Distance HUD overlay
                state.displayDistance?.let { distance ->
                    DistanceHud(
                        distance = distance,
                        unit = state.distanceUnit,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 72.dp)
                    )
                }

                // AC9: Hole navigation controls
                if (state.greens.size > 1) {
                    HoleNavigationBar(
                        canGoPrev = state.canGoPrev,
                        canGoNext = state.canGoNext,
                        holeLabel = state.displayHoleNumber,
                        onPrevious = { viewModel.onAction(HoleMapAction.PreviousHole) },
                        onNext = { viewModel.onAction(HoleMapAction.NextHole) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = Spacing.Medium)
                    )
                }

                // Course data loading indicator
                if (state.isFetchingCourseData) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = Spacing.Small)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                RoundedCornerShape(Spacing.Small)
                            )
                            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(Spacing.Small))
                            Text(
                                text = "Loading course data...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Re-center FAB
                FloatingActionButton(
                    onClick = { viewModel.onAction(HoleMapAction.RefreshLocation) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(Spacing.Medium),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Center on location"
                    )
                }
            }

            // Error message overlay
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(Spacing.Medium)
                )
            }

            // Flag submitted confirmation
            if (state.flagSubmitted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = Spacing.Small)
                        .background(
                            CaddyProColors.PerformanceLime.copy(alpha = 0.9f),
                            RoundedCornerShape(Spacing.Small)
                        )
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.Small)
                ) {
                    Text(
                        text = "Thanks! Report submitted.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Distance to green HUD overlay
 */
@Composable
private fun DistanceHud(
    distance: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                RoundedCornerShape(Spacing.Medium)
            )
            .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = distance,
            style = DataTextStyles.DataLarge,
            color = CaddyProColors.TitaniumWhite
        )
        Text(
            text = "to green center ($unit)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Hole navigation bar with prev/next buttons
 */
@Composable
private fun HoleNavigationBar(
    canGoPrev: Boolean,
    canGoNext: Boolean,
    holeLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                RoundedCornerShape(Spacing.Medium)
            )
            .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onPrevious,
            enabled = canGoPrev
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous hole"
            )
            Text("Prev")
        }

        Text(
            text = holeLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        TextButton(
            onClick = onNext,
            enabled = canGoNext
        ) {
            Text("Next")
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next hole"
            )
        }
    }
}

/**
 * Flag incorrect data dialog (AC22)
 */
@Composable
private fun FlagIncorrectDataDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Flag Incorrect Data") },
        text = {
            Column {
                Text(
                    text = "Help improve course data by reporting issues.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.Medium))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What's incorrect?") },
                    placeholder = { Text("e.g., Missing bunker, wrong green position") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(description) },
                enabled = description.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Map hazard type to fill and stroke colors per brand standards.
 * AC4: Color-coded hazard overlays
 */
private fun hazardColors(type: HazardType): Pair<Color, Color> {
    return when (type) {
        HazardType.BUNKER -> Pair(
            Color(0xFFD4A76A).copy(alpha = 0.35f),
            Color(0xFFD4A76A).copy(alpha = 0.7f)
        )
        HazardType.WATER -> Pair(
            CaddyProColors.Info.copy(alpha = 0.35f),
            CaddyProColors.Info.copy(alpha = 0.7f)
        )
        HazardType.OUT_OF_BOUNDS -> Pair(
            CaddyProColors.DangerZone.copy(alpha = 0.25f),
            CaddyProColors.DangerZone.copy(alpha = 0.6f)
        )
        HazardType.TREES -> Pair(
            Color(0xFF2E7D32).copy(alpha = 0.20f),
            Color(0xFF2E7D32).copy(alpha = 0.5f)
        )
        HazardType.FAIRWAY -> Pair(
            Color(0xFF4CAF50).copy(alpha = 0.15f),
            Color(0xFF4CAF50).copy(alpha = 0.4f)
        )
    }
}
