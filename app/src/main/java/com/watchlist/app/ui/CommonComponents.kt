package com.watchlist.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.PrintType
import com.watchlist.app.data.local.entities.ReadStatus
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.navigation.bottomNavItems
import com.watchlist.app.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// Navegación
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WatchListBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WatchBlue,
                    selectedTextColor = WatchBlue,
                    indicatorColor = WatchBlueSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Badges — Audiovisual
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun WatchStatusBadge(status: WatchStatus) {
    val (bg, fg, label) = when (status) {
        WatchStatus.WATCHING  -> Triple(StatusWatchingBg, StatusWatching, "Viendo")
        WatchStatus.COMPLETED -> Triple(StatusCompletedBg, StatusCompleted, "Visto")
        WatchStatus.PLANNED   -> Triple(StatusPlannedBg, StatusPlanned, "Por ver")
    }
    StatusBadge(bg = bg, fg = fg, label = label)
}

@Composable
fun MediaTypeBadge(type: MediaType) {
    val (bg, fg, label) = when (type) {
        MediaType.SERIES -> Triple(WatchBlueSurface, WatchBlue, "SERIE")
        MediaType.MOVIE  -> Triple(Color(0xFFFAECE7), Color(0xFF993C1D), "PELI")
        MediaType.ANIME  -> Triple(Color(0xFFEEEDFE), Color(0xFF534AB7), "ANIME")
    }
    StatusBadge(bg = bg, fg = fg, label = label, textSize = 10)
}

// ─────────────────────────────────────────────────────────────────────────────
// Badges — Impreso (Manga / Cómic)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Badge de estado de lectura. Análogo a [WatchStatusBadge] pero para
 * la sección de Impresos. Reutiliza los mismos tokens de color del tema.
 */
@Composable
fun ReadStatusBadge(status: ReadStatus) {
    val (bg, fg, label) = when (status) {
        ReadStatus.READING   -> Triple(StatusWatchingBg, StatusWatching, "Leyendo")
        ReadStatus.COMPLETED -> Triple(StatusCompletedBg, StatusCompleted, "Leído")
        ReadStatus.PLANNED   -> Triple(StatusPlannedBg, StatusPlanned, "Por leer")
        ReadStatus.ON_HOLD   -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pausado")
    }
    StatusBadge(bg = bg, fg = fg, label = label)
}

/**
 * Badge de tipo de impreso. Análogo a [MediaTypeBadge] pero para
 * los distintos formatos de lectura.
 */
@Composable
fun PrintTypeBadge(type: PrintType) {
    val (bg, fg, label) = when (type) {
        PrintType.MANGA         -> Triple(Color(0xFFEEEDFE), Color(0xFF534AB7), "MANGA")
        PrintType.MANHWA        -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "MANHWA")
        PrintType.COMIC         -> Triple(Color(0xFFFAECE7), Color(0xFF993C1D), "CÓMIC")
        PrintType.NOVEL -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), "NOV. GRÁ.")
    }
    StatusBadge(bg = bg, fg = fg, label = label, textSize = 10)
}

// ─────────────────────────────────────────────────────────────────────────────
// Primitivo interno para badges
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Componente base de badge. Todas las variantes públicas delegan aquí para
 * garantizar consistencia visual en márgenes, forma y tipografía.
 */
@Composable
private fun StatusBadge(
    bg: Color,
    fg: Color,
    label: String,
    textSize: Int = 11
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = textSize.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rating
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StarRatingBar(
    rating: Float,
    maxStars: Int = 5,
    onRatingChanged: ((Float) -> Unit)? = null,
    starSize: Int = 20
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..maxStars) {
            val filled = i <= rating
            if (onRatingChanged != null) {
                IconButton(
                    onClick = { onRatingChanged(i.toFloat()) },
                    modifier = Modifier.size(starSize.dp)
                ) {
                    Icon(
                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = if (filled) Color(0xFFBA7517)
                               else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                        modifier = Modifier.size(starSize.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = if (filled) Color(0xFFBA7517)
                           else MaterialTheme.colorScheme.onSurface.copy(0.3f),
                    modifier = Modifier.size(starSize.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tipografía compartida
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Etiqueta de sección para formularios. Compartida entre AddMediaScreen y
 * AddPrintMediaScreen para garantizar coherencia visual.
 *
 * Era una función `private` en AddMediaScreen; se promueve aquí para
 * evitar duplicación.
 */
@Composable
fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = modifier
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
