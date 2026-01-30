package caddypro.ui.caddy

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.WeatherData
import caddypro.domain.navcaddy.context.RoundState
import caddypro.ui.caddy.components.ForecasterHud
import caddypro.ui.caddy.components.HapticFeedbackManager
import caddypro.ui.caddy.components.PinSeekerMap
import caddypro.ui.caddy.components.ReadinessHud
import caddypro.ui.caddy.components.ShotConfirmationToast
import caddypro.ui.caddy.components.ShotLogger
import caddypro.ui.theme.CaddyProTheme
import com.example.app.ui.components.ErrorView
import com.example.app.ui.components.LoadingView

/**
 * Main Live Caddy Mode screen.
 *
 * Integrates all Live Caddy components into a cohesive HUD:
 * - Top bar with round context and end round button
 * - Forecaster HUD showing real-time weather
 * - Readiness HUD showing biometric readiness score
 * - PinSeeker map with hole strategy and hazard awareness
 * - Floating action button for shot logger
 * - Modal bottom sheet for shot logging
 * - Haptic feedback for shot confirmation (R6, A4)
 * - Shot confirmation toast (R6, A4)
 *
 * Supports:
 * - Loading states and error handling
 * - "No Active Round" placeholder when no round is active
 * - Low distraction mode with auto-lock prevention (R7)
 * - Material 3 design with outdoor visibility optimization
 *
 * Spec reference: live-caddy-mode.md R1-R7
 * Plan reference: live-caddy-mode-plan.md Task 18, Task 22
 * Acceptance criteria: A1-A4 (all)
 *
 * @param viewModel ViewModel managing Live Caddy state
 * @param onNavigateBack Callback to navigate back from the screen
 * @param modifier Optional modifier for the screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCaddyScreen(
    viewModel: LiveCaddyViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Create haptic feedback manager (respects settings)
    val view = LocalView.current
    val hapticFeedback = remember(state.settings.hapticFeedback) {
        HapticFeedbackManager(view, enabled = state.settings.hapticFeedback)
    }

    // Trigger haptic feedback when shot is confirmed (A4)
    LaunchedEffect(state.lastShotConfirmed) {
        if (state.lastShotConfirmed) {
            hapticFeedback.success()
        }
    }

    // Apply window flags for low distraction mode (R7: Safety and Distraction Controls)
    if (state.settings.autoLockPrevention) {
        KeepScreenOn()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            LiveCaddyTopBar(
                roundState = state.roundState,
                onNavigateBack = onNavigateBack,
                onEndRound = { viewModel.onAction(LiveCaddyAction.EndRound) }
            )
        },
        floatingActionButton = {
            // Show FAB when shot logger is not visible
            if (!state.isShotLoggerVisible && state.roundState != null) {
                FloatingActionButton(
                    onClick = {
                        viewModel.onAction(LiveCaddyAction.ShowShotLogger)
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log Shot"
                    )
                }
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
                    LoadingView(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.error != null -> {
                    ErrorView(
                        message = state.error ?: "Unknown error",
                        onRetry = { viewModel.onAction(LiveCaddyAction.LoadContext) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                state.roundState == null -> {
                    NoActiveRoundPlaceholder(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LiveCaddyContent(
                        state = state,
                        onAction = viewModel::onAction
                    )
                }
            }

            // Shot confirmation toast (A4: haptic confirmation)
            // Positioned at bottom of screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ShotConfirmationToast(
                    visible = state.lastShotConfirmed,
                    shotDetails = state.lastShotDetails,
                    onDismiss = { viewModel.onAction(LiveCaddyAction.DismissShotConfirmation) }
                )
            }
        }
    }

    // Modal bottom sheet for shot logger (R6: Real-Time Shot Logger)
    if (state.isShotLoggerVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onAction(LiveCaddyAction.DismissShotLogger) },
            sheetState = bottomSheetState
        ) {
            ShotLogger(
                clubs = state.clubs,
                selectedClub = state.selectedClub,
                onClubSelected = { club ->
                    viewModel.onAction(LiveCaddyAction.SelectClub(club))
                },
                onShotLogged = { result ->
                    viewModel.onAction(LiveCaddyAction.LogShot(result))
                },
                hapticFeedback = hapticFeedback,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

/**
 * Main content layout for Live Caddy HUD.
 *
 * Displays all HUD components in a scrollable column:
 * - Forecaster HUD (weather)
 * - Readiness HUD (biometric readiness)
 * - PinSeeker Map (hole strategy)
 * - Optional distraction warning banner
 *
 * Each component can be expanded/collapsed independently.
 *
 * @param state Current Live Caddy UI state
 * @param onAction Callback for user actions
 * @param modifier Optional modifier for the layout
 */
@Composable
private fun LiveCaddyContent(
    state: LiveCaddyState,
    onAction: (LiveCaddyAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // R7: Safety and Distraction Controls - Warning banner (shown at top)
        DistractionWarningBanner()

        // R2: Forecaster HUD (always visible)
        ForecasterHud(
            weather = state.weather,
            isExpanded = state.isWeatherHudExpanded,
            onToggleExpanded = { expanded ->
                onAction(LiveCaddyAction.ToggleWeatherHud(expanded))
            }
        )

        // R3: BodyCaddy Readiness HUD
        ReadinessHud(
            readiness = state.readiness,
            showDetails = state.isReadinessDetailsVisible,
            onToggleDetails = { visible ->
                onAction(LiveCaddyAction.ToggleReadinessDetails(visible))
            },
            onManualOverride = { score ->
                // Manual override would require a new action
                // For now, just refresh readiness
                onAction(LiveCaddyAction.RefreshReadiness)
            }
        )

        // R4: PinSeeker AI Map (hole strategy)
        if (state.isStrategyMapVisible) {
            PinSeekerMap(
                strategy = state.holeStrategy
            )

            // Hide button
            TextButton(
                onClick = { onAction(LiveCaddyAction.ToggleStrategyMap(false)) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Hide Hole Strategy")
            }
        } else {
            // Show button
            TextButton(
                onClick = { onAction(LiveCaddyAction.ToggleStrategyMap(true)) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Show Hole Strategy")
            }
        }
    }
}

/**
 * Placeholder shown when no active round is in progress.
 *
 * Encourages user to start a round to use Live Caddy features.
 *
 * Spec reference: R1 (Live Round Context)
 *
 * @param modifier Optional modifier for the placeholder
 */
@Composable
private fun NoActiveRoundPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "No Active Round",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start a round to access Live Caddy features including weather forecasting, readiness tracking, and hole strategy.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Distraction warning banner per safety requirement R7.
 *
 * Explicitly discourages phone use during active swing sequence.
 * Always shown to promote safe usage of the app during rounds.
 *
 * Spec reference: R7 (Safety and Distraction Controls)
 */
@Composable
private fun DistractionWarningBanner(
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Text(
            text = "Put your phone away during your swing. Live Caddy is designed for quick reference between shots.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Keeps screen on when auto-lock prevention is enabled.
 *
 * Uses FLAG_KEEP_SCREEN_ON to prevent screen timeout during active use.
 * Flag is automatically cleared when composable leaves composition.
 *
 * Spec reference: R7 (Safety and Distraction Controls)
 */
@Composable
private fun KeepScreenOn() {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

// ============================================================================
// Preview Composables
// ============================================================================

@Preview(name = "Live Caddy Screen - Loading", showBackground = true)
@Composable
private fun LiveCaddyScreenLoadingPreview() {
    CaddyProTheme {
        LiveCaddyContent(
            state = LiveCaddyState(
                isLoading = true
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Live Caddy Screen - No Round", showBackground = true)
@Composable
private fun LiveCaddyScreenNoRoundPreview() {
    CaddyProTheme {
        NoActiveRoundPlaceholder(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Live Caddy Screen - Active Round", showBackground = true)
@Composable
private fun LiveCaddyScreenActivePreview() {
    CaddyProTheme {
        LiveCaddyContent(
            state = LiveCaddyState(
                roundState = RoundState(
                    roundId = 1L,
                    courseId = 1L,
                    currentHole = 1,
                    currentPar = 4,
                    shotsOnCurrentHole = 0,
                    totalScore = 0
                ),
                weather = WeatherData(
                    location = Location(37.7749, -122.4194),
                    windSpeed = 12.0,
                    windDirection = 180.0,
                    temperature = 72.0,
                    humidity = 55,
                    timestamp = System.currentTimeMillis()
                ),
                isLoading = false,
                error = null
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Distraction Warning Banner", showBackground = true)
@Composable
private fun DistractionWarningBannerPreview() {
    CaddyProTheme {
        DistractionWarningBanner()
    }
}
