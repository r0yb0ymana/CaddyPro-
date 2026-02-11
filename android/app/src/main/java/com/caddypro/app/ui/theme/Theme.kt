package com.caddypro.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * CaddyPro Dark Theme
 *
 * CaddyPro uses a dark theme by default for optimal outdoor readability.
 * Based on Material3 dark color scheme with custom brand colors.
 */
private val CaddyProDarkColorScheme = darkColorScheme(
    // Primary - Performance Lime (used sparingly for primary actions)
    primary = CaddyProColors.PerformanceLime,
    onPrimary = CaddyProColors.MatteCharcoal,
    primaryContainer = CaddyProColors.PerformanceLimeDark,
    onPrimaryContainer = CaddyProColors.PerformanceLimeLight,

    // Secondary - Carbon Grey
    secondary = CaddyProColors.CarbonGrey,
    onSecondary = CaddyProColors.TitaniumWhite,
    secondaryContainer = CaddyProColors.SurfaceElevated,
    onSecondaryContainer = CaddyProColors.TitaniumWhite,

    // Tertiary - Info Blue
    tertiary = CaddyProColors.Info,
    onTertiary = CaddyProColors.MatteCharcoal,

    // Background - Matte Charcoal
    background = CaddyProColors.MatteCharcoal,
    onBackground = CaddyProColors.TitaniumWhite,

    // Surface - Elevated and Dim variants
    surface = CaddyProColors.MatteCharcoal,
    onSurface = CaddyProColors.TitaniumWhite,
    surfaceVariant = CaddyProColors.SurfaceElevated,
    onSurfaceVariant = CaddyProColors.TitaniumWhite,

    // Surface containers
    surfaceContainer = CaddyProColors.SurfaceElevated,
    surfaceContainerHigh = CaddyProColors.SurfaceElevated,
    surfaceContainerHighest = CaddyProColors.SurfaceElevated,
    surfaceContainerLow = CaddyProColors.SurfaceDim,
    surfaceContainerLowest = CaddyProColors.SurfaceDim,

    // Error - Danger Zone
    error = CaddyProColors.DangerZone,
    onError = CaddyProColors.TitaniumWhite,
    errorContainer = CaddyProColors.DangerZone,
    onErrorContainer = CaddyProColors.TitaniumWhite,

    // Outline - Subtle borders
    outline = CaddyProColors.BorderSubtle,
    outlineVariant = CaddyProColors.CarbonGrey,

    // Scrim
    scrim = CaddyProColors.MatteCharcoal.copy(alpha = 0.5f),

    // Inverse colors (for light elements on dark background)
    inverseSurface = CaddyProColors.TitaniumWhite,
    inverseOnSurface = CaddyProColors.MatteCharcoal,
    inversePrimary = CaddyProColors.PerformanceLimeDark,

    // Surface tint
    surfaceTint = CaddyProColors.PerformanceLime
)

/**
 * CaddyPro Theme Composable
 *
 * @param darkTheme Always true - CaddyPro only uses dark theme
 * @param content The composable content to be wrapped with the theme
 */
@Composable
fun CaddyProTheme(
    darkTheme: Boolean = true, // Always dark theme for CaddyPro
    content: @Composable () -> Unit
) {
    val colorScheme = CaddyProDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CaddyProTypography,
        content = content
    )
}
