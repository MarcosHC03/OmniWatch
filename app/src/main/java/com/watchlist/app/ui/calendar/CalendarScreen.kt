package com.watchlist.app.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.ui.WatchListBottomBar
import com.watchlist.app.viewmodel.CalendarViewModel
import com.watchlist.app.viewmodel.WeekDay

// ---------------------------------------------------------------------------
// Pantalla raíz
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    navController: NavHostController,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Viendo", "Por ver")

    // Snackbar para errores
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Calendario", fontWeight = FontWeight.Bold) },
                    actions = {
                        // El buscador que nos robó Claude
                        IconButton(onClick = { /* TODO: Buscador de estrenos V1.1 */ }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar Estrenos")
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick  = { selectedTabIndex = index },
                            text     = { Text(title) }
                        )
                    }
                }
            }
        },
        bottomBar    = { WatchListBottomBar(navController = navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                when (selectedTabIndex) {
                    0 -> VIendoTabContent(
                        airingSchedule   = state.airingSchedule,
                        finishedWatching = state.finishedWatching
                    )
                    1 -> PorVerTabContent(
                        upcomingReleases = state.upcomingReleases,
                        releasedItems    = state.releasedItems
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Tab "Viendo"
// ---------------------------------------------------------------------------

@Composable
fun VIendoTabContent(
    airingSchedule: List<WeekDay>,
    finishedWatching: List<MediaItemEntity>
) {
    val hasAiring   = airingSchedule.any { it.items.isNotEmpty() }
    val hasFinished = finishedWatching.isNotEmpty()

    if (!hasAiring && !hasFinished) {
        EmptyMessage("No tenés títulos en estado 'Viendo'.")
        return
    }

    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Sección "En Emisión" ────────────────────────────────────────────
        item {
            SectionHeader(
                title    = "En emisión",
                subtitle = if (hasAiring) null else "Ningún título marcado como en emisión"
            )
        }

        if (hasAiring) {
            airingSchedule.forEach { weekDay ->
                if (weekDay.items.isNotEmpty()) {
                    item(key = "day_${weekDay.label}") {
                        DayHeader(weekDay.label)
                    }
                    items(
                        items = weekDay.items,
                        key   = { "airing_${it.id}" }
                    ) { mediaItem ->
                        MediaCalendarCard(item = mediaItem)
                    }
                }
            }
        }

        // ── Separador ───────────────────────────────────────────────────────
        item { Spacer(Modifier.height(8.dp)) }
        item { HorizontalDivider() }
        item { Spacer(Modifier.height(8.dp)) }

        // ── Sección "Finalizados" ───────────────────────────────────────────
        item {
            SectionHeader(
                title    = "Finalizados",
                subtitle = if (hasFinished) null else "Ningún título marcado como finalizado"
            )
        }

        if (hasFinished) {
            items(
                items = finishedWatching,
                key   = { "finished_${it.id}" }
            ) { mediaItem ->
                MediaCalendarCard(item = mediaItem)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Tab "Por ver"
// ---------------------------------------------------------------------------

@Composable
fun PorVerTabContent(
    upcomingReleases: List<MediaItemEntity>,
    releasedItems: List<MediaItemEntity>
) {
    val hasUpcoming  = upcomingReleases.isNotEmpty()
    val hasReleased  = releasedItems.isNotEmpty()

    if (!hasUpcoming && !hasReleased) {
        EmptyMessage("No tenés títulos en 'Por ver'.")
        return
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── Sección "Próximos" ──────────────────────────────────────────────
        item {
            SectionHeader(
                title    = "Próximos",
                subtitle = if (hasUpcoming) null else "No hay estrenos próximos en tu lista"
            )
        }

        if (hasUpcoming) {
            items(
                items = upcomingReleases,
                key   = { "upcoming_${it.id}" }
            ) { mediaItem ->
                MediaCalendarCard(
                    item        = mediaItem,
                    showReleaseDate = true
                )
            }
        }

        // ── Separador ───────────────────────────────────────────────────────
        item { Spacer(Modifier.height(8.dp)) }
        item { HorizontalDivider() }
        item { Spacer(Modifier.height(8.dp)) }

        // ── Sección "Ya estrenados" ─────────────────────────────────────────
        item {
            SectionHeader(
                title    = "Ya estrenados",
                subtitle = if (hasReleased) null else "Ningún título ya estrenado en tu lista"
            )
        }

        if (hasReleased) {
            items(
                items = releasedItems,
                key   = { "released_${it.id}" }
            ) { mediaItem ->
                MediaCalendarCard(
                    item            = mediaItem,
                    showReleaseDate = true
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ---------------------------------------------------------------------------
// Componentes reutilizables
// ---------------------------------------------------------------------------

/** Tarjeta con póster + info, reutilizada en ambas pestañas. */
@Composable
fun MediaCalendarCard(
    item: MediaItemEntity,
    showReleaseDate: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Póster
            if (item.posterPath.isNotBlank()) {
                AsyncImage(
                    model              = item.posterPath,
                    contentDescription = "Poster de ${item.title}",
                    modifier           = Modifier
                        .size(width = 60.dp, height = 90.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback sin imagen
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 90.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (item.mediaType) {
                            MediaType.MOVIE  -> "🎬"
                            MediaType.SERIES -> "📺"
                            MediaType.ANIME  -> "⛩️"
                        },
                        fontSize = 22.sp
                    )
                }
            }

            // Datos
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .weight(1f)
            ) {
                Text(
                    text       = item.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )

                if (item.platform.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = item.platform,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }

                if (showReleaseDate) {
                    Spacer(Modifier.height(4.dp))
                    val dateLabel = if (item.releaseDate.isNotBlank())
                        item.releaseDate
                    else
                        "Sin fecha"
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text     = dateLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color    = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Progreso de episodios para Series/Anime en emisión
                if (item.mediaType != MediaType.MOVIE && item.totalEpisodes > 0) {
                    Spacer(Modifier.height(4.dp))
                    val progress = item.watchedEpisodes.toFloat() / item.totalEpisodes.toFloat()
                    Text(
                        text     = "${item.watchedEpisodes}/${item.totalEpisodes} eps",
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    if (progress > 0f) {
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress        = { progress },
                            modifier        = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color           = MaterialTheme.colorScheme.primary,
                            trackColor      = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Encabezado de sección grande (En emisión, Finalizados, Próximos…). */
@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary
        )
        if (subtitle != null) {
            Text(
                text     = subtitle,
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

/** Encabezado de día de la semana dentro del calendario de emisión. */
@Composable
private fun DayHeader(label: String) {
    Text(
        text       = label,
        fontWeight = FontWeight.Medium,
        fontSize   = 13.sp,
        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier   = Modifier.padding(top = 10.dp, bottom = 2.dp)
    )
}

/** Pantalla vacía genérica. */
@Composable
private fun EmptyMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text  = message,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    }
}
