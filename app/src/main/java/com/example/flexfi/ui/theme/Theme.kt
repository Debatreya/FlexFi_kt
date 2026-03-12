package com.example.flexfi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val FlexFiColorScheme = lightColorScheme(
    primary = FlexFiBlue,
    onPrimary = FlexFiWhite,
    primaryContainer = FlexFiLightBlue,
    onPrimaryContainer = FlexFiDarkBlue,
    secondary = FlexFiTeal,
    onSecondary = FlexFiWhite,
    secondaryContainer = FlexFiTealLight,
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = FlexFiPurple,
    onTertiary = FlexFiWhite,
    tertiaryContainer = FlexFiPurpleLight,
    error = FlexFiRed,
    onError = FlexFiWhite,
    errorContainer = FlexFiRedLight,
    background = FlexFiWhite,
    onBackground = FlexFiDarkText,
    surface = FlexFiWhite,
    onSurface = FlexFiDarkText,
    surfaceVariant = FlexFiGreySurface,
    onSurfaceVariant = FlexFiBodyText,
    outline = FlexFiGreyBorder,
    outlineVariant = Color(0xFFEEEEEE)
)

/** Reusable gradient brushes used across the app. */
object FlexFiGradients {
    val primary = Brush.horizontalGradient(listOf(FlexFiBlue, FlexFiTeal))
    val primaryVertical = Brush.verticalGradient(listOf(FlexFiBlue, FlexFiTeal))
    val splash = Brush.verticalGradient(listOf(Color(0xFF2979FF), Color(0xFF00C9A7)))
    val card = Brush.horizontalGradient(listOf(Color(0xFF2962FF), Color(0xFF00BFA5)))
    val button = Brush.horizontalGradient(listOf(FlexFiBlue, FlexFiTeal))
    val fab = Brush.linearGradient(listOf(FlexFiBlue, FlexFiTeal))
}

@Composable
fun FlexFiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlexFiColorScheme,
        typography = FlexFiTypography,
        content = content
    )
}