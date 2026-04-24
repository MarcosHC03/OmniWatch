package com.watchlist.app.ui.addmedia

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.data.remote.TmdbMedia
import com.watchlist.app.ui.StarRatingBar
import com.watchlist.app.viewmodel.AddMediaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMediaScreen(
    itemId: Long,
    autoSearchQuery: String = "",
    cacheId: Int = -1,
    onNavigateBack: () -> Unit,
    viewModel: AddMediaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(itemId, autoSearchQuery, cacheId) {
        if (itemId > 0) {
            viewModel.loadItemForEditing(itemId)
        } else if (cacheId > 0) {
            viewModel.loadFromCache(cacheId) 
        } else if (autoSearchQuery.isNotBlank()) {
            viewModel.autoSearchAndSelect(autoSearchQuery)
        }
    }
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onNavigateBack()
    }

    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) "Editar título" else "Agregar título",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ---- Barra de búsqueda TMDB ----
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { 
                        Text(
                            if (state.mediaType == MediaType.ANIME) "Buscar en MyAnimeList..." else "Buscar en TMDB..."
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (state.isSearching)
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                )
                IconButton(onClick = { viewModel.searchTmdb(searchText) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Buscar")
                }
            }

            // ---- Resultados de búsqueda ----
            if (state.searchResults.isNotEmpty()) {
                Card(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    LazyColumn {
                        items(state.searchResults.take(8)) { media ->
                            TmdbResultItem(
                                media = media,
                                onClick = {
                                    viewModel.selectTmdbResult(media)
                                    searchText = media.displayTitle
                                }
                            )
                        }
                        item {
                            TextButton(
                                onClick = { viewModel.clearSearch() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Cerrar resultados") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ---- Formulario principal ----
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Poster preview
                if (state.posterPath.isNotBlank()) {
                    AsyncImage(
                        model = state.posterPath,
                        contentDescription = null,
                        modifier = Modifier
                            .height(180.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Título
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = state.titleError,
                    supportingText = if (state.titleError) {
                        { Text("El título es obligatorio") }
                    } else null
                )

                // Tipo de medio
                FormLabel("Tipo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        MediaType.SERIES to "Serie",
                        MediaType.MOVIE  to "Película",
                        MediaType.ANIME  to "Anime"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = state.mediaType == type,
                            onClick = { viewModel.updateMediaType(type) },
                            label = { Text(label) }
                        )
                    }
                }

                // Estado de visualización
                FormLabel("Estado")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        WatchStatus.PLANNED   to "Por ver",
                        WatchStatus.WATCHING  to "Viendo",
                        WatchStatus.COMPLETED to "Visto"
                    ).forEach { (status, label) ->
                        FilterChip(
                            selected = state.watchStatus == status,
                            onClick = { viewModel.updateWatchStatus(status) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                // Puntaje
                FormLabel("Puntaje")
                StarRatingBar(
                    rating = state.rating,
                    onRatingChanged = { viewModel.updateRating(it) },
                    starSize = 32
                )

                // Episodios (solo Series y Anime)
                if (state.mediaType != MediaType.MOVIE) {
                    EpisodesSection(
                        state = state,
                        onWatchedEpisodesChange = { viewModel.updateWatchedEpisodes(it) },
                        onTotalEpisodesChange = { viewModel.updateTotalEpisodes(it) },
                        onSeasonChange = { viewModel.updateCurrentSeason(it) }
                    )
                }

                // Fecha de estreno
                FormLabel("Fecha de estreno")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.releaseDay,
                        onValueChange = { if (it.length <= 2) viewModel.updateReleaseDay(it) },
                        label = { Text("Día", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = state.titleError
                    )
                    OutlinedTextField(
                        value = state.releaseMonth,
                        onValueChange = { if (it.length <= 2) viewModel.updateReleaseMonth(it) },
                        label = { Text("Mes", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = state.titleError
                    )
                    OutlinedTextField(
                        value = state.releaseYear,
                        onValueChange = { if (it.length <= 4) viewModel.updateReleaseYear(it) },
                        label = { Text("Año", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = state.titleError
                    )
                }

                if (state.dateError) {
                    Text(
                        text = "Fecha inválida (ej: Día 1-31, Mes 1-12, Año 2024)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // ── MISIÓN 3B: Toggle En Emisión / Finalizado ─────────────────────────
                // Solo visible para Series y Anime (las películas no "están en emisión")
                if (state.mediaType != MediaType.MOVIE) {
                    FormLabel("Estado de emisión")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.isAiring,
                            onClick = { viewModel.updateIsAiring(true) },
                            label = { Text("En emisión", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = !state.isAiring,
                            onClick = { viewModel.updateIsAiring(false) },
                            label = { Text("Finalizado", fontSize = 12.sp) }
                        )
                    }
                }

                // Plataforma
                OutlinedTextField(
                    value = state.platform,
                    onValueChange = { viewModel.updatePlatform(it) },
                    label = { Text("Plataforma (Netflix, HBO, etc.)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Botón guardar
                Button(
                    onClick = { viewModel.saveItem() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !state.isSaving,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (state.isEditing) "Guardar cambios" else "Agregar a mi lista",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ---- Sección de episodios ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodesSection(
    state: com.watchlist.app.viewmodel.AddMediaUiState,
    onWatchedEpisodesChange: (String) -> Unit,
    onTotalEpisodesChange: (String) -> Unit,
    onSeasonChange: (String) -> Unit
) {
    val isAnime = state.mediaType == MediaType.ANIME
    val watchedDisabled = state.watchStatus == WatchStatus.PLANNED

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Fila: eps. vistos + total eps.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.watchedEpisodes,
                onValueChange = { onWatchedEpisodesChange(it) },
                label = { Text("Eps. vistos") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !watchedDisabled,
                isError = state.watchedEpisodesError,
                supportingText = if (state.watchedEpisodesError) {
                    { Text("Mayor al total", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (state.watchedEpisodesError)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (state.watchedEpisodesError)
                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            )
            OutlinedTextField(
                value = state.totalEpisodes,
                onValueChange = { onTotalEpisodesChange(it) },
                label = { Text("Total eps.") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Temporada: dropdown si hay datos TMDB, input manual si no, oculto para Anime
        when {
            !isAnime && state.availableSeasons > 0 -> SeasonDropdown(
                currentSeason = state.currentSeason.toIntOrNull() ?: 1,
                totalSeasons = state.availableSeasons,
                onSeasonSelected = { onSeasonChange(it.toString()) }
            )
            !isAnime -> OutlinedTextField(
                value = state.currentSeason,
                onValueChange = { onSeasonChange(it) },
                label = { Text("Temporada") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            // isAnime == true → no mostramos nada de temporada
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonDropdown(
    currentSeason: Int,
    totalSeasons: Int,
    onSeasonSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = "Temporada $currentSeason",
            onValueChange = {},
            readOnly = true,
            label = { Text("Temporada") },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            (1..totalSeasons).forEach { season ->
                DropdownMenuItem(
                    text = { Text("Temporada $season") },
                    onClick = { onSeasonSelected(season); expanded = false }
                )
            }
        }
    }
}

// ---- Helpers ────────────────────────────────────────────────────────────────

@Composable
private fun FormLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
    )
}

@Composable
fun TmdbResultItem(media: TmdbMedia, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Lógica blindada para la imagen
        val path = media.posterPath ?: ""
        val imageUrl = when {
            path.isBlank() -> "" // Si no hay ruta, queda vacío
            path.startsWith("http") -> path // Si es Jikan, usamos el link directo
            else -> "https://image.tmdb.org/t/p/w342$path" // Si es TMDB, le pegamos el prefijo
        }

        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .width(34.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(5.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                Modifier
                    .width(34.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) { Text("🎬") }
        }
        Column(Modifier.weight(1f)) {
            Text(
                media.displayTitle,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    media.displayDate.take(4).let { if (it.isNotBlank()) append(it) }
                    media.mediaType?.let {
                        append(" · ${when(it) { "movie" -> "Película"; "anime" -> "Anime"; else -> "Serie" }}")
                    }
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
            )
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 12.dp))
}
