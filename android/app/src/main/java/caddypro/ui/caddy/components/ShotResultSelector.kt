package caddypro.ui.caddy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import caddypro.domain.navcaddy.models.Lie
import caddypro.ui.theme.CaddyProTheme

/**
 * Shot result selector grid for logging shot outcomes.
 *
 * Displays all possible lie results in a grid with:
 * - Extra large touch targets (64dp height for one-second taps)
 * - Color-coded buttons (green=good, yellow=ok, red=bad)
 * - Clear labels with high contrast
 * - Intuitive result ordering (best to worst)
 *
 * Results per spec R6:
 * - Fairway (best)
 * - Green
 * - Fringe
 * - Rough (requires miss direction)
 * - Bunker
 * - Water/Hazard (requires miss direction)
 * - Tee (for re-tee or starting hole)
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 17
 * Acceptance criteria: A4 (Shot logger speed and persistence)
 *
 * @param onResultSelected Callback when a result is tapped
 * @param modifier Modifier for the grid container
 */
@Composable
fun ShotResultSelector(
    onResultSelected: (Lie) -> Unit,
    modifier: Modifier = Modifier
) {
    // Ordered from best to worst for intuitive layout
    val results = listOf(
        Lie.FAIRWAY,
        Lie.GREEN,
        Lie.FRINGE,
        Lie.ROUGH,
        Lie.BUNKER,
        Lie.HAZARD
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results) { lie ->
            ResultButton(
                lie = lie,
                onClick = { onResultSelected(lie) }
            )
        }
    }
}

/**
 * Individual result button with color coding.
 *
 * Button styling:
 * - 64dp height (extra large for fast tapping)
 * - Color-coded based on result quality
 * - Bold label with high contrast
 * - Full width within grid cell
 *
 * @param lie The shot result lie
 * @param onClick Callback when button is tapped
 */
@Composable
private fun ResultButton(
    lie: Lie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)  // Extra large for one-second tap goal
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = lieColor(lie)
        ),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = lieLabel(lie),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

/**
 * Color coding for lie results.
 *
 * Color palette based on result quality:
 * - Green: Fairway, Green (excellent position)
 * - Blue-green: Fringe (good position)
 * - Orange: Rough (playable but suboptimal)
 * - Brown: Bunker (penalty but recoverable)
 * - Red: Hazard (penalty stroke)
 *
 * Colors are high-saturation for outdoor visibility.
 *
 * @param lie The shot result lie
 * @return Color for the button background
 */
private fun lieColor(lie: Lie): Color {
    return when (lie) {
        Lie.FAIRWAY -> Color(0xFF4CAF50)      // Green
        Lie.GREEN -> Color(0xFF2196F3)        // Blue
        Lie.FRINGE -> Color(0xFF00BCD4)       // Cyan
        Lie.ROUGH -> Color(0xFFFFA726)        // Orange
        Lie.BUNKER -> Color(0xFF8D6E63)       // Brown
        Lie.HAZARD -> Color(0xFFEF5350)       // Red
        Lie.TEE -> Color(0xFF9E9E9E)          // Gray
    }
}

/**
 * User-friendly label for lie results.
 *
 * Converts enum names to readable labels:
 * - FAIRWAY -> "Fairway"
 * - HAZARD -> "Water" (more intuitive for golfers)
 *
 * @param lie The shot result lie
 * @return Display label for the button
 */
private fun lieLabel(lie: Lie): String {
    return when (lie) {
        Lie.TEE -> "Tee"
        Lie.FAIRWAY -> "Fairway"
        Lie.ROUGH -> "Rough"
        Lie.BUNKER -> "Bunker"
        Lie.GREEN -> "Green"
        Lie.FRINGE -> "Fringe"
        Lie.HAZARD -> "Water"
    }
}

// Preview functions for development and testing

@Preview(name = "Shot Result Selector", showBackground = true)
@Composable
private fun PreviewShotResultSelector() {
    CaddyProTheme {
        ShotResultSelector(
            onResultSelected = {}
        )
    }
}

@Preview(name = "Shot Result Selector - Dark", showBackground = false)
@Composable
private fun PreviewShotResultSelectorDark() {
    CaddyProTheme(darkTheme = true) {
        ShotResultSelector(
            onResultSelected = {}
        )
    }
}

@Preview(name = "Single Result Button - Fairway", showBackground = true)
@Composable
private fun PreviewResultButtonFairway() {
    CaddyProTheme {
        ResultButton(
            lie = Lie.FAIRWAY,
            onClick = {}
        )
    }
}

@Preview(name = "Single Result Button - Hazard", showBackground = true)
@Composable
private fun PreviewResultButtonHazard() {
    CaddyProTheme {
        ResultButton(
            lie = Lie.HAZARD,
            onClick = {}
        )
    }
}

@Preview(name = "Single Result Button - Bunker", showBackground = true)
@Composable
private fun PreviewResultButtonBunker() {
    CaddyProTheme {
        ResultButton(
            lie = Lie.BUNKER,
            onClick = {}
        )
    }
}
