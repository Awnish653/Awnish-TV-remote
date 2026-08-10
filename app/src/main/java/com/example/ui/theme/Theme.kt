package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TvPrimaryCyan,
    onPrimary = Color.Black,
    primaryContainer = TvSurfaceVariantDark,
    onPrimaryContainer = TvTextPrimary,
    secondary = TvSecondaryBlue,
    onSecondary = Color.White,
    tertiary = TvAccentGold,
    onTertiary = Color.Black,
    background = TvBackgroundDark,
    onBackground = TvTextPrimary,
    surface = TvSurfaceDark,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceVariantDark,
    onSurfaceVariant = TvTextSecondary,
    outline = TvOutline,
    error = TvErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = TvSecondaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = TvPrimaryCyan,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = TvErrorRed
)

@Composable
fun AwnishTvRemoteTheme(
    themeMode: String = "DARK", // DARK, LIGHT, SYSTEM
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

