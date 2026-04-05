package com.watchlist.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)

val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)

val WatchBlue = Color(0xFF185FA5)
val WatchBlueDark = Color(0xFF378ADD)
val WatchBlueSurface = Color(0xFFE6F1FB)

val StatusWatching = Color(0xFF0F6E56)
val StatusWatchingBg = Color(0xFFE1F5EE)
val StatusCompleted = Color(0xFF3B6D11)
val StatusCompletedBg = Color(0xFFEAF3DE)
val StatusPlanned = Color(0xFF854F0B)
val StatusPlannedBg = Color(0xFFFAEEDA)

private val DarkColorScheme = darkColorScheme(
    primary = WatchBlueDark,
    secondary = PurpleGrey80,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = WatchBlue,
    secondary = PurpleGrey40,
    background = Color(0xFFF8F8F8),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
)

@Composable
fun WatchListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
