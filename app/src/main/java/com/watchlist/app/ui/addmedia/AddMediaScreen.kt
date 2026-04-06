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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onNavigateBack: () -> Unit,
    viewModel: AddMediaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(itemId) {
        if (itemId > 0L) viewModel.loadItemForEditing(itemId)
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
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
                    placeholder = { Text("Buscar en TMDB...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (state.isSearching) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
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
                    singleLine = true
                )

                // Tipo de medio
                FormLabel("Tipo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        MediaType.SERIES to "Serie",
                        MediaType.MOVIE to "Película",
                        MediaType.ANIME to "Anime"
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
                        WatchStatus.PLANNED to "Por ver",
                        WatchStatus.WATCHING to "Viendo",
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

                // ---- Campos de episodios (solo para Series y Anime) ----
                if (state.mediaType != MediaType.MOVIE) {
                    EpisodesSection(
                        state = state,
                        onWatchedEpisodesChange = { viewModel.updateWatchedEpisodes(it) },
                        onTotalEpisodesChange = { viewModel.updateTotalEpisodes(it) },
                        onSeasonChange = { viewModel.updateCurrentSeason(it) }
                    )
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
                    enabled = state.title.isNotBlank() && !state.isSaving && !state.watchedEpisodesError
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

// ---- Sección de episodios con toda la lógica condicional ----
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodesSection(
    state: com.watchlist.app.viewmodel.AddMediaUiState,
    onWatchedEpisodesChange: (Int) -> Unit,
    onTotalEpisodesChange: (Int) -> Unit,
    onSeasonChange: (Int) -> Unit
) {
    val isAnime = state.mediaType == MediaType.ANIME
    val watchedDisabled = state.watchStatus == WatchStatus.PLANNED

    // Temporada como Dropdown (solo para SERIES con temporadas conocidas)
    val showSeasonDropdown = !isAnime && state.availableSeasons > 0
    // Temporada como input de texto (solo para SERIES sin datos de temporadas)
    val showSeasonInput = !isAnime && state.availableSeasons == 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // ---- Fila superior: eps. vistos + total eps. ----
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

            // Episodios vistos
            OutlinedTextField(
                value = if (state.watchedEpisodes > 0 || state.watchStatus == WatchStatus.WATCHING)
                    state.watchedEpisodes.toString() else "",
                onValueChange = { onWatchedEpisodesChange(it.toIntOrNull() ?: 0) },
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
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (state.watchedEpisodesError)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.outline
                )
            )

            // Total de episodios
            OutlinedTextField(
                value = if (state.totalEpisodes > 0) state.totalEpisodes.toString() else "",
                onValueChange = { onTotalEpisodesChange(it.toIntOrNull() ?: 0) },
                label = { Text("Total eps.") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // ---- Temporada ----
        when {
            // Dropdown con temporadas conocidas (SERIES con datos TMDB)
            showSeasonDropdown -> {
                SeasonDropdown(
                    currentSeason = state.currentSeason,
                    totalSeasons = state.availableSeasons,
                    onSeasonSelected = onSeasonChange
                )
            }
            // Input manual (SERIES sin datos de temporadas)
            showSeasonInput -> {
                OutlinedTextField(
                    value = if (state.currentSeason > 0) state.currentSeason.toString() else "",
                    onValueChange = { onSeasonChange(it.toIntOrNull() ?: 1) },
                    label = { Text("Temporada") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            // Para ANIME: no se muestra nada de temporada (isAnime == true)
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
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null
                )
            },
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
                    onClick = {
                        onSeasonSelected(season)
                        expanded = false
                    }
                )
            }
        }
    }
}

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
        if (media.fullPosterPath.isNotBlank()) {
            AsyncImage(
                model = media.fullPosterPath,
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
                        append(" · ${if (it == "movie") "Película" else "Serie"}")
                    }
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
            )
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 12.dp))
}
