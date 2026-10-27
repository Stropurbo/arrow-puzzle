package com.sabbir.arrowpuzzle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sabbir.arrowpuzzle.GameConfig

/**
 * Single source of truth for app-wide surfaces and text colors.
 *
 * The board strokes use `onBackground`, so that slot is pure black in light mode
 * and a soft off-white in dark mode. Text uses `onSurface`, which is a hair
 * softer than the arrow color for comfortable long-form reading.
 */
private val LightColors = lightColorScheme(
    primary = GameConfig.Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7EAFD),
    onPrimaryContainer = Color(0xFF232B7A),
    secondary = GameConfig.Success,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCF4EC),
    onSecondaryContainer = Color(0xFF0C4B3A),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF0D3),
    onTertiaryContainer = Color(0xFF5A2E00),
    background = GameConfig.LightBackground,
    onBackground = GameConfig.LightArrow,
    surface = GameConfig.LightSurface,
    onSurface = Color(0xFF121826),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF5A6478),
    outline = Color(0xFFC9D2E0),
    outlineVariant = Color(0xFFE3E9F2),
    error = GameConfig.Danger,
    onError = Color.White,
    scrim = Color.Black
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B99FF),
    onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF2C3780),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF4ED3AA),
    onSecondary = Color(0xFF06251C),
    secondaryContainer = Color(0xFF14483A),
    onSecondaryContainer = Color(0xFFC7F5E6),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF2A1C00),
    tertiaryContainer = Color(0xFF4A3200),
    onTertiaryContainer = Color(0xFFFFE3A3),
    background = GameConfig.DarkBackground,
    onBackground = GameConfig.DarkArrow,
    surface = GameConfig.DarkSurface,
    onSurface = GameConfig.DarkArrow,
    surfaceVariant = Color(0xFF2A3446),
    onSurfaceVariant = Color(0xFFA9B4C8),
    outline = Color(0xFF3A465C),
    outlineVariant = Color(0xFF2C3648),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF2A0A0A),
    scrim = Color.Black
)

@Composable
fun ArrowPuzzleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
