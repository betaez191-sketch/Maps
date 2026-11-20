package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val WorldlyDarkColorScheme = darkColorScheme(
    primary = WorldlyCyan,
    onPrimary = WorldlyOnAccent,
    primaryContainer = WorldlyCyanDim,
    onPrimaryContainer = WorldlyTextPrimary,
    secondary = WorldlyLavender,
    onSecondary = WorldlyMidnight,
    secondaryContainer = WorldlyGlass,
    onSecondaryContainer = WorldlyTextPrimary,
    tertiary = WorldlyAmber,
    onTertiary = WorldlyMidnight,
    background = WorldlyMidnight,
    onBackground = WorldlyTextPrimary,
    surface = WorldlyObsidian,
    onSurface = WorldlyTextPrimary,
    surfaceVariant = WorldlySlate,
    onSurfaceVariant = WorldlyTextSecondary,
    surfaceContainerLowest = WorldlyMidnight,
    surfaceContainerLow = WorldlyObsidian,
    surfaceContainer = WorldlySlate,
    surfaceContainerHigh = WorldlyGlass,
    surfaceContainerHighest = WorldlyGlass,
    outline = WorldlyBorder,
    outlineVariant = WorldlyBorder.copy(alpha = 0.55f),
    error = WorldlyRose,
    onError = WorldlyMidnight
)

val WorldlyShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun WorldlyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WorldlyDarkColorScheme,
        typography = WorldlyTypography,
        shapes = WorldlyShapes,
        content = content
    )
}
