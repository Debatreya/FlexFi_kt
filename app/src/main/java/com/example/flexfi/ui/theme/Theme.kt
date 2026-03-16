package com.example.flexfi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val FlexFiDarkColorScheme = darkColorScheme(
    primary = FlexFiBlue,
    onPrimary = FlexFiWhite,
    secondary = FlexFiTeal,
    onSecondary = FlexFiWhite,
    tertiary = FlexFiPurple,
    onTertiary = FlexFiWhite,
    background = Color(0xFF0F1216),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF1F2630),
    onSurfaceVariant = Color(0xFFA8B3C1),
    error = FlexFiRed,
    onError = FlexFiWhite,
    outline = Color(0xFF39424E)
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
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) FlexFiDarkColorScheme else FlexFiColorScheme,
        typography = FlexFiTypography,
        content = content
    )
}