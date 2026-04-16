package com.watchlist.app.ui.discovery

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.remote.TmdbRelease
import com.watchlist.app.ui.WatchListBottomBar
import com.watchlist.app.viewmodel.DiscoveryViewModel

// ---------------------------------------------------------------------------
// Pantalla principal
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    navController: NavHostController,
    viewModel: DiscoveryViewModel = hiltViewModel()
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cine", "Series")

    // Snackbar para errores de red
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // ── Barra de título ──────────────────────────────────────────
                TopAppBar(
                    title = { Text("Descubrí", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { viewModel.loadContent() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Recargar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // ── Buscador ─────────────────────────────────────────────────
                OutlinedTextField(
                    value         = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder   = { Text("Buscar estrenos...") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // ── Chips de filtro ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cine", "Series", "Anime").forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick  = { selectedTab = index },
                            label    = { Text(label, fontSize = 13.sp) }
                        )
                    }
                }
            }
        },
        bottomBar    = { WatchListBottomBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> {
                    val (list, mediaType) = when (selectedTab) {
                        0    -> state.filteredMovies to MediaType.MOVIE
                        1    -> state.filteredTv     to MediaType.SERIES
                        else -> state.filteredAnime  to MediaType.ANIME
                    }

                    if (list.isEmpty()) {
                        EmptyDiscovery(
                            query     = state.searchQuery,
                            modifier  = Modifier.align(Alignment.Center)
                        )
                    } else {
                        ReleaseGrid(
                            releases      = list,
                            savedIds      = state.savedReleaseIds,
                            mediaType     = mediaType,
                            onQuickSave   = { release ->
                                viewModel.quickSave(release, mediaType)
                                Toast.makeText(
                                    context,
                                    "Agregado a Por Ver",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Grilla de estrenos
// ---------------------------------------------------------------------------

@Composable
private fun ReleaseGrid(
    releases: List<TmdbRelease>,
    savedIds: Set<Int>,
    mediaType: MediaType,
    onQuickSave: (TmdbRelease) -> Unit
) {
    LazyVerticalGrid(
        columns             = GridCells.Fixed(2),
        contentPadding      = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp),
        modifier            = Modifier.fillMaxSize()
    ) {
        items(
            items = releases,
            key   = { it.id }
        ) { release ->
            ReleaseCard(
                release     = release,
                isSaved     = release.id in savedIds,
                onQuickSave = { onQuickSave(release) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Tarjeta individual
// ---------------------------------------------------------------------------

@Composable
private fun ReleaseCard(
    release: TmdbRelease,
    isSaved: Boolean,
    onQuickSave: () -> Unit
) {
    Card(
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            /// ── Póster ───────────────────────────────────────────────────────
            val posterUrl = if (release.posterPath?.startsWith("http") == true) {
                release.posterPath // Si viene de Jikan, ya trae el link completo
            } else {
                release.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" } // Si viene de TMDB
            }

            AsyncImage(
                model              = posterUrl,
                contentDescription = release.displayTitle,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)   // ratio estándar de póster de cine
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            )

            // ── Gradiente + texto en la parte inferior del póster ────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.0f),
                                Color.Black.copy(alpha = 0.75f)
                            ),
                            startY = 0f,
                            endY   = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // ── Botón + / check superpuesto (esquina superior derecha) ───────
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                QuickSaveButton(isSaved = isSaved, onClick = onQuickSave)
            }
        }

        // ── Título y fecha (debajo del póster) ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text       = release.displayTitle,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 13.sp,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
            if (release.displayDate.isNotBlank()) {
                Text(
                    text     = formatDisplayDate(release.displayDate),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Botón +/check animado
// ---------------------------------------------------------------------------

@Composable
private fun QuickSaveButton(
    isSaved: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSaved)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.primaryContainer

    val contentColor = if (isSaved)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    IconButton(
        onClick  = { if (!isSaved) onClick() },
        enabled  = !isSaved,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(containerColor.copy(alpha = 0.92f))
    ) {
        AnimatedContent(
            targetState      = isSaved,
            transitionSpec   = {
                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
            },
            label = "save_icon_anim"
        ) { saved ->
            Icon(
                imageVector        = if (saved) Icons.Filled.Check else Icons.Filled.Add,
                contentDescription = if (saved) "Guardado" else "Agregar a Por Ver",
                tint               = contentColor,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Estado vacío
// ---------------------------------------------------------------------------

@Composable
private fun EmptyDiscovery(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🎬", fontSize = 40.sp)
        Text(
            text       = if (query.isNotBlank())
                "Sin resultados para \"$query\""
            else
                "No hay estrenos disponibles",
            fontWeight = FontWeight.Medium,
            fontSize   = 15.sp
        )
        Text(
            text     = if (query.isNotBlank())
                "Probá con otro título"
            else
                "Revisá tu conexión y tocá recargar",
            fontSize = 13.sp,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

// ---------------------------------------------------------------------------
// Helper de fecha
// ---------------------------------------------------------------------------

/** "2025-04-18" → "18 abr 2025" para mostrar en la tarjeta. */
private fun formatDisplayDate(raw: String): String =
    runCatching {
        val date = java.time.LocalDate.parse(
            raw.trim(),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        )
        date.format(
            java.time.format.DateTimeFormatter.ofPattern(
                "d MMM yyyy",
                java.util.Locale("es", "AR")
            )
        ).replaceFirstChar { it.uppercase() }
    }.getOrDefault(raw)
