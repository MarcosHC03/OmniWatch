package com.watchlist.app.ui.addprintmedia

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.PrintType
import com.watchlist.app.data.local.entities.ReadStatus
import com.watchlist.app.ui.FormLabel
import com.watchlist.app.ui.StarRatingBar
import com.watchlist.app.viewmodel.AddPrintMediaViewModel
import com.watchlist.app.viewmodel.AddPrintUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPrintMediaScreen(
    itemId: Long = -1L,
    onNavigateBack: () -> Unit,
    viewModel: AddPrintMediaViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(itemId) {
        if (itemId > 0) viewModel.loadItemForEditing(itemId)
    }
    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isEditing) "Editar Lectura" else "Agregar Lectura",
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
            // ── Barra de búsqueda (solo para nuevos) ─────────────────────────
            if (!state.isEditing) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                if (state.printType == PrintType.COMIC || state.printType == PrintType.GRAPHIC_NOVEL)
                                    "Buscar en ComicVine..."
                                else
                                    "Buscar en Jikan..."
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
                    IconButton(onClick = { viewModel.searchManga(searchQuery, state.printType) }) {
                        Icon(Icons.Filled.Search, contentDescription = "Buscar")
                    }
                }

                // ── Resultados de búsqueda ────────────────────────────────────
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
                            items(state.searchResults.take(8)) { result ->
                                PrintSearchResultItem(
                                    item = result,
                                    onClick = {
                                        viewModel.selectResult(result)
                                        searchQuery = result.title
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
            }

            // ── Formulario principal ──────────────────────────────────────────
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Póster preview
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
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = state.titleError,
                    supportingText = if (state.titleError) {
                        { Text("El título es obligatorio") }
                    } else null
                )

                // Autor
                OutlinedTextField(
                    value = state.author,
                    onValueChange = viewModel::updateAuthor,
                    label = { Text("Autor / Dibujante") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Tipo de obra
                FormLabel("Tipo de obra")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    PrintType.values().forEach { type ->
                        FilterChip(
                            selected = state.printType == type,
                            onClick = { viewModel.updateType(type) },
                            label = { Text(type.name, fontSize = 12.sp) }
                        )
                    }
                }

                // Estado de lectura
                FormLabel("Estado de lectura")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    listOf(
                        ReadStatus.PLANNED   to "Por leer",
                        ReadStatus.READING   to "Leyendo",
                        ReadStatus.COMPLETED to "Leído",
                        ReadStatus.ON_HOLD   to "Pausado"
                    ).forEach { (status, label) ->
                        FilterChip(
                            selected = state.status == status,
                            onClick = { viewModel.updateStatus(status) },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                // Puntaje
                FormLabel("Puntaje")
                StarRatingBar(
                    rating = state.rating,
                    onRatingChanged = viewModel::updateRating,
                    starSize = 32
                )

                // Tomos y Capítulos
                VolumesSection(
                    state = state,
                    onCurrentVolumeChange = viewModel::updateCurrentVolume,
                    onTotalVolumesChange = viewModel::updateTotalVolumes,
                    onCurrentChapterChange = viewModel::updateCurrentChapter,
                    onTotalChaptersChange = viewModel::updateTotalChapters
                )

                // Botón guardar
                Button(
                    onClick = viewModel::save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !state.isSaving
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (state.isEditing) "Guardar cambios" else "Agregar a mi biblioteca",
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

// ── Sección de tomos y capítulos ──────────────────────────────────────────────

/**
 * Análoga a EpisodesSection en AddMediaScreen.
 * Adapta las etiquetas según si la obra es un cómic occidental o asiática.
 * Los campos de "leídos" se deshabilitan automáticamente si el estado es PLANNED.
 */
@Composable
private fun VolumesSection(
    state: AddPrintUiState,
    onCurrentVolumeChange: (String) -> Unit,
    onTotalVolumesChange: (String) -> Unit,
    onCurrentChapterChange: (String) -> Unit,
    onTotalChaptersChange: (String) -> Unit
) {
    val isComic = state.printType == PrintType.COMIC || state.printType == PrintType.GRAPHIC_NOVEL
    val progressDisabled = state.status == ReadStatus.PLANNED

    val currentVolLabel  = if (isComic) "TPBs leídos"       else "Tomos ya leídos"
    val totalVolLabel    = if (isComic) "Total TPBs"         else "Total Vols."
    val currentChapLabel = if (isComic) "Nros. (#) leídos"   else "Cap. actual"
    val totalChapLabel   = if (isComic) "Total Nros."        else "Total Caps."

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Fila superior: progreso en tomos + total de tomos
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.currentVolume,
                onValueChange = onCurrentVolumeChange,
                label = { Text(currentVolLabel) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !progressDisabled
            )
            OutlinedTextField(
                value = state.totalVolumes,
                onValueChange = onTotalVolumesChange,
                label = { Text(totalVolLabel) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        // Fila inferior: progreso en capítulos + total de capítulos
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.currentChapter,
                onValueChange = onCurrentChapterChange,
                label = { Text(currentChapLabel) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !progressDisabled
            )
            OutlinedTextField(
                value = state.totalChapters,
                onValueChange = onTotalChaptersChange,
                label = { Text(totalChapLabel) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }
    }
}

// ── Item de resultado de búsqueda ─────────────────────────────────────────────

/**
 * Análogo a TmdbResultItem en AddMediaScreen.
 * Muestra miniatura + título + metadatos del resultado de Jikan / ComicVine.
 */
@Composable
private fun PrintSearchResultItem(item: PrintMediaEntity, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (item.posterPath.isNotBlank()) {
            AsyncImage(
                model = item.posterPath,
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
            ) { Text("📚") }
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Vols: ${item.totalVolumes} · Caps: ${item.totalChapters}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
    HorizontalDivider(Modifier.padding(horizontal = 12.dp))
}
