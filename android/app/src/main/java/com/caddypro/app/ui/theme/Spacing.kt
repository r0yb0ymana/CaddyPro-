package com.caddypro.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * CaddyPro Spacing Tokens
 *
 * Consistent spacing values used throughout the app.
 * Based on 8dp grid system.
 */
object Spacing {
    val None = 0.dp
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val XXLarge = 48.dp
    val XXXLarge = 64.dp

    // Specific spacing for common use cases
    val CardPadding = Medium
    val ScreenPadding = Medium
    val ButtonPadding = Medium
    val ListItemPadding = Medium
    val IconPadding = Small

    // Touch targets
    val MinTouchTarget = 48.dp // Minimum touch target size per accessibility guidelines
}
