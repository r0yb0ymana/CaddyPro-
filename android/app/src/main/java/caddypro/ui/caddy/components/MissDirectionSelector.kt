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
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.ui.theme.CaddyProTheme

/**
 * Miss direction selector for hazard and rough shots.
 *
 * Conditional component shown when user selects a lie that requires
 * miss direction tracking (hazard, rough per spec R6).
 *
 * Displays directional buttons for:
 * - Left/Right (lateral misses)
 * - Short/Long (distance misses) - not applicable for hazards
 * - Push/Pull (strike patterns)
 * - Slice/Hook (ball flight patterns)
 * - Fat/Thin (contact quality)
 * - Straight (on target)
 *
 * Design features:
 * - Large touch targets (64dp height)
 * - Directional layout (left/right positioned accordingly)
 * - Color-coded by severity
 * - Fast selection flow
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 17
 * Acceptance criteria: A4 (Shot logger speed and persistence)
 *
 * @param onDirectionSelected Callback when a direction is tapped
 * @param modifier Modifier for the grid container
 */
@Composable
fun MissDirectionSelector(
    onDirectionSelected: (MissDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    // Primary miss directions for quick logging
    // Ordered for intuitive spatial layout
    val directions = listOf(
        MissDirection.PULL,      // Left
        MissDirection.STRAIGHT,  // Center
        MissDirection.PUSH,      // Right
        MissDirection.HOOK,      // Left curve
        MissDirection.SLICE,     // Right curve
        MissDirection.FAT,       // Short (hit ground first)
        MissDirection.THIN       // Long/low (topped)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(directions) { direction ->
            DirectionButton(
                direction = direction,
                onClick = { onDirectionSelected(direction) }
            )
        }
    }
}

/**
 * Individual direction button.
 *
 * Button styling:
 * - 64dp height (extra large for fast tapping)
 * - Color-coded based on miss severity
 * - Bold label with icon/symbol
 * - Full width within grid cell
 *
 * @param direction The miss direction
 * @param onClick Callback when button is tapped
 */
@Composable
private fun DirectionButton(
    direction: MissDirection,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)  // Extra large for one-second tap goal
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = directionColor(direction)
        ),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = directionLabel(direction),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/**
 * Color coding for miss directions.
 *
 * Color palette based on severity and type:
 * - Green: Straight (no miss)
 * - Blue: Push/Pull (start line misses, often correctable)
 * - Orange: Slice/Hook (shape misses, harder to control)
 * - Red: Fat/Thin (contact misses, significant distance loss)
 *
 * Colors are high-saturation for outdoor visibility.
 *
 * @param direction The miss direction
 * @return Color for the button background
 */
private fun directionColor(direction: MissDirection): Color {
    return when (direction) {
        MissDirection.STRAIGHT -> Color(0xFF4CAF50)  // Green (good)
        MissDirection.PUSH -> Color(0xFF2196F3)      // Blue
        MissDirection.PULL -> Color(0xFF2196F3)      // Blue
        MissDirection.SLICE -> Color(0xFFFFA726)     // Orange
        MissDirection.HOOK -> Color(0xFFFFA726)      // Orange
        MissDirection.FAT -> Color(0xFFEF5350)       // Red
        MissDirection.THIN -> Color(0xFFEF5350)      // Red
    }
}

/**
 * User-friendly label for miss directions.
 *
 * Converts enum names to readable labels with directional symbols:
 * - PUSH -> "← Push" (started right)
 * - PULL -> "Pull →" (started left)
 * - SLICE -> "Slice →" (curved right)
 * - HOOK -> "← Hook" (curved left)
 * - FAT -> "Fat ↓" (hit ground, short)
 * - THIN -> "Thin ↑" (topped, low)
 * - STRAIGHT -> "✓ Straight"
 *
 * @param direction The miss direction
 * @return Display label for the button
 */
private fun directionLabel(direction: MissDirection): String {
    return when (direction) {
        MissDirection.PUSH -> "Push →"
        MissDirection.PULL -> "← Pull"
        MissDirection.SLICE -> "Slice →"
        MissDirection.HOOK -> "← Hook"
        MissDirection.FAT -> "Fat ↓"
        MissDirection.THIN -> "Thin ↑"
        MissDirection.STRAIGHT -> "✓ Straight"
    }
}

// Preview functions for development and testing

@Preview(name = "Miss Direction Selector", showBackground = true)
@Composable
private fun PreviewMissDirectionSelector() {
    CaddyProTheme {
        MissDirectionSelector(
            onDirectionSelected = {}
        )
    }
}

@Preview(name = "Miss Direction Selector - Dark", showBackground = false)
@Composable
private fun PreviewMissDirectionSelectorDark() {
    CaddyProTheme(darkTheme = true) {
        MissDirectionSelector(
            onDirectionSelected = {}
        )
    }
}

@Preview(name = "Single Direction Button - Push", showBackground = true)
@Composable
private fun PreviewDirectionButtonPush() {
    CaddyProTheme {
        DirectionButton(
            direction = MissDirection.PUSH,
            onClick = {}
        )
    }
}

@Preview(name = "Single Direction Button - Hook", showBackground = true)
@Composable
private fun PreviewDirectionButtonHook() {
    CaddyProTheme {
        DirectionButton(
            direction = MissDirection.HOOK,
            onClick = {}
        )
    }
}

@Preview(name = "Single Direction Button - Fat", showBackground = true)
@Composable
private fun PreviewDirectionButtonFat() {
    CaddyProTheme {
        DirectionButton(
            direction = MissDirection.FAT,
            onClick = {}
        )
    }
}

@Preview(name = "Single Direction Button - Straight", showBackground = true)
@Composable
private fun PreviewDirectionButtonStraight() {
    CaddyProTheme {
        DirectionButton(
            direction = MissDirection.STRAIGHT,
            onClick = {}
        )
    }
}
