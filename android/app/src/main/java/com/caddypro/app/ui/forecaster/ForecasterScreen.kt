package com.caddypro.app.ui.forecaster

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.caddypro.app.domain.model.AdjustedClubDistance
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.ShotDirection
import com.caddypro.app.domain.model.WeatherData
import com.caddypro.app.ui.theme.CaddyProColors
import com.caddypro.app.ui.theme.DataTextStyles
import com.caddypro.app.ui.theme.JetBrainsMonoFontFamily
import com.caddypro.app.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Forecaster HUD Screen
 *
 * Displays weather conditions and adjusted carry distances for each club.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecasterScreen(
    viewModel: ForecasterViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onAction(ForecasterAction.LocationPermissionGranted)
        } else {
            viewModel.onAction(ForecasterAction.LocationPermissionDenied)
        }
    }

    // Request permission on first load if not granted
    LaunchedEffect(state.hasLocationPermission) {
        if (!state.hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Start/stop auto refresh based on screen lifecycle
    DisposableEffect(state.hasLocationPermission) {
        if (state.hasLocationPermission) {
            viewModel.startAutoRefresh()
        }
        onDispose {
            viewModel.stopAutoRefresh()
        }
    }

    // Adjustment detail sheet
    if (state.showAdjustmentDetail && state.selectedClub != null) {
        AdjustmentDetailSheet(
            club = state.selectedClub!!,
            weather = state.weather,
            useMetric = state.useMetric,
            onDismiss = { viewModel.onAction(ForecasterAction.DismissDetail) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forecaster HUD", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.hasWeatherData) {
                        IconButton(
                            onClick = { viewModel.onAction(ForecasterAction.RefreshWeather) },
                            enabled = !state.isRefreshing
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh weather")
                        }
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
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                !state.hasLocationPermission -> {
                    NoLocationPermission(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    ForecasterContent(
                        state = state,
                        onAction = viewModel::onAction
                    )
                }
            }
        }
    }
}

@Composable
private fun ForecasterContent(
    state: ForecasterState,
    onAction: (ForecasterAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Spacing.XXLarge)
    ) {
        // Weather header
        item {
            if (state.hasWeatherData && state.weather != null) {
                WeatherHeader(
                    weather = state.weather,
                    lastUpdatedText = state.lastUpdatedText,
                    isStale = state.isWeatherStale,
                    isRefreshing = state.isRefreshing,
                    useMetric = state.useMetric
                )
            } else {
                NoWeatherData()
            }
        }

        // Shot direction selector
        item {
            ShotDirectionSelector(
                selectedDirection = state.shotDirection,
                onDirectionSelected = { onAction(ForecasterAction.UpdateShotDirection(it)) }
            )
        }

        // Stale warning
        if (state.isWeatherStale) {
            item {
                StaleWeatherWarning()
            }
        }

        // Club distance cards grouped by type
        if (state.adjustedDistances.isNotEmpty()) {
            val grouped = state.adjustedDistances.groupBy { it.clubType }
            ClubType.values().forEach { type ->
                val clubsOfType = grouped[type] ?: return@forEach
                item {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = Spacing.Medium,
                            top = Spacing.Medium,
                            bottom = Spacing.Small
                        )
                    )
                }
                items(clubsOfType, key = { it.clubId }) { club ->
                    ClubDistanceCard(
                        club = club,
                        useMetric = state.useMetric,
                        onClick = { onAction(ForecasterAction.SelectClub(club)) }
                    )
                }
            }
        } else if (!state.isLoading && state.hasWeatherData) {
            item {
                EmptyClubList()
            }
        }
    }
}

@Composable
private fun WeatherHeader(
    weather: WeatherData,
    lastUpdatedText: String,
    isStale: Boolean,
    isRefreshing: Boolean,
    useMetric: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium)
        ) {
            // Condition + temp row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Weather condition
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        text = weather.conditionDescription.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Temperature (AC23: JetBrains Mono)
                Text(
                    text = "${weather.temperatureC.roundToInt()}°C",
                    style = DataTextStyles.DataSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // Wind + humidity row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Wind (AC10, AC11)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WindDirectionArrow(degrees = weather.windDirectionDeg)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    val windDisplay = if (useMetric) {
                        "${weather.windSpeedKmh.roundToInt()} km/h"
                    } else {
                        "${(weather.windSpeedKmh * 0.621371).roundToInt()} mph"
                    }
                    Text(
                        text = windDisplay,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        text = compassBearing(weather.windDirectionDeg),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Humidity
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text(
                        text = "${weather.humidity}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Small))

            // Last updated (AC3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Updated $lastUpdatedText",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isStale) CaddyProColors.Warning
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isRefreshing) {
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun WindDirectionArrow(degrees: Int) {
    // Simple text-based wind direction arrow
    val arrow = when {
        degrees in 337..360 || degrees in 0..22 -> "↓" // N wind blows south
        degrees in 23..67 -> "↙"   // NE wind blows SW
        degrees in 68..112 -> "←"  // E wind blows west
        degrees in 113..157 -> "↖" // SE wind blows NW
        degrees in 158..202 -> "↑" // S wind blows north
        degrees in 203..247 -> "↗" // SW wind blows NE
        degrees in 248..292 -> "→" // W wind blows east
        degrees in 293..336 -> "↘" // NW wind blows SE
        else -> "•"
    }
    Text(
        text = arrow,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ShotDirectionSelector(
    selectedDirection: ShotDirection,
    onDirectionSelected: (ShotDirection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium)
    ) {
        Text(
            text = "Shot Direction",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = Spacing.Small)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
        ) {
            items(ShotDirection.values().toList()) { direction ->
                FilterChip(
                    selected = selectedDirection == direction,
                    onClick = { onDirectionSelected(direction) },
                    label = { Text(direction.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun ClubDistanceCard(
    club: AdjustedClubDistance,
    useMetric: Boolean,
    onClick: () -> Unit
) {
    val unitLabel = if (useMetric) "m" else "yds"
    val displayBase = if (useMetric) (club.baseCarry * 0.9144).roundToInt() else club.baseCarry
    val displayAdjusted = if (useMetric) (club.adjustedCarry * 0.9144).roundToInt() else club.adjustedCarry
    val displayDelta = displayAdjusted - displayBase

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Club name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = club.clubName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Base carry (AC24: dimmed)
                Text(
                    text = "$displayBase $unitLabel",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Adjusted carry (AC23: large, prominent, JetBrains Mono)
            Text(
                text = "$displayAdjusted",
                style = DataTextStyles.DataSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(Spacing.Small))

            // Delta (AC6: color coded)
            val deltaColor = when {
                displayDelta > 0 -> CaddyProColors.PerformanceLime // AC26: green for positive
                displayDelta < 0 -> CaddyProColors.DangerZone     // AC26: red for negative
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val deltaText = when {
                displayDelta > 0 -> "+$displayDelta"
                else -> "$displayDelta"
            }
            Text(
                text = deltaText,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = deltaColor,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun NoWeatherData() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = "No weather data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Base distances shown without adjustments",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StaleWeatherWarning() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        color = CaddyProColors.Warning.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "Weather data may be outdated",
            style = MaterialTheme.typography.bodySmall,
            color = CaddyProColors.Warning,
            modifier = Modifier.padding(Spacing.Small),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun NoLocationPermission(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Text(
            text = "Location Required",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = "The Forecaster needs your location to fetch weather data and calculate carry adjustments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyClubList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No clubs in active bag",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.Small))
        Text(
            text = "Add clubs to your bag to see adjusted distances",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun compassBearing(degrees: Int): String {
    return when {
        degrees in 337..360 || degrees in 0..22 -> "N"
        degrees in 23..67 -> "NE"
        degrees in 68..112 -> "E"
        degrees in 113..157 -> "SE"
        degrees in 158..202 -> "S"
        degrees in 203..247 -> "SW"
        degrees in 248..292 -> "W"
        degrees in 293..336 -> "NW"
        else -> ""
    }
}
