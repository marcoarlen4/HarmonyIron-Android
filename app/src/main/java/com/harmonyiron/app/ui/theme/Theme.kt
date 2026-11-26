package com.harmonyiron.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object IronColors {
    val Background = Color(0xFF0D0E12)
    val Surface = Color(0xFF181A20)
    val SurfaceElevated = Color(0xFF22252E)
    val PrimaryOrange = Color(0xFFFF5722)
    val AccentGreen = Color(0xFF00E676)
    val TextPrimary = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFF9CA3AF)
    val Divider = Color(0xFF2A2D37)
}

private val HarmonyIronDarkScheme = darkColorScheme(
    primary = IronColors.PrimaryOrange,
    onPrimary = Color.White,
    secondary = IronColors.AccentGreen,
    onSecondary = Color.Black,
    background = IronColors.Background,
    onBackground = IronColors.TextPrimary,
    surface = IronColors.Surface,
    onSurface = IronColors.TextPrimary,
    surfaceVariant = IronColors.SurfaceElevated,
    onSurfaceVariant = IronColors.TextSecondary
)

@Composable
fun HarmonyIronTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HarmonyIronDarkScheme,
        content = content
    )
}
