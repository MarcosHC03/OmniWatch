package com.watchlist.app.ui.addprintmedia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.watchlist.app.data.local.entities.PrintType
import com.watchlist.app.data.local.entities.ReadStatus
import com.watchlist.app.viewmodel.AddPrintMediaViewModel

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
                title = { Text(if (state.isEditing) "Editar Lectura" else "Agregar Lectura") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── 1. Buscador (Solo para nuevos) ──
            if (!state.isEditing) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar en MyAnimeList...") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { viewModel.searchManga(searchQuery) }) {
                                Icon(Icons.Default.Search, contentDescription = "Buscar")
                            }
                        }
                    )
                }

                // ── 2. Resultados de Búsqueda ──
                if (state.isSearching) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.searchResults.isNotEmpty()) {
                    item { Text("Resultados:", fontWeight = FontWeight.Bold) }
                    items(state.searchResults) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.selectResult(result) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = result.posterPath,
                                contentDescription = null,
                                modifier = Modifier.size(50.dp, 75.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("Vols: ${result.totalVolumes} • Caps: ${result.totalChapters}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── 3. El Formulario (Se oculta mientras muestra resultados) ──
            if (state.searchResults.isEmpty() && !state.isSearching) {
                
                // Póster Seleccionado
                if (state.posterPath.isNotBlank()) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = state.posterPath,
                                contentDescription = "Póster",
                                modifier = Modifier.height(200.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(value = state.title, onValueChange = viewModel::updateTitle, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                }

                item {
                    OutlinedTextField(value = state.author, onValueChange = viewModel::updateAuthor, label = { Text("Autor/Dibujante") }, modifier = Modifier.fillMaxWidth())
                }

                item {
                    Text("Tipo de obra", fontWeight = FontWeight.Medium)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        PrintType.values().forEach { type ->
                            FilterChip(
                                selected = state.printType == type,
                                onClick = { viewModel.updateType(type) },
                                label = { Text(type.name) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                item {
                    Text("Estado de lectura", fontWeight = FontWeight.Medium)
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        ReadStatus.values().forEach { status ->
                            val label = when(status) {
                                ReadStatus.READING -> "Leyendo"
                                ReadStatus.COMPLETED -> "Leído"
                                ReadStatus.PLANNED -> "Por leer"
                                ReadStatus.ON_HOLD -> "Pausado"
                            }
                            FilterChip(
                                selected = state.status == status,
                                onClick = { viewModel.updateStatus(status) },
                                label = { Text(label) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                // Tomos / Volúmenes
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.currentVolume, 
                            onValueChange = viewModel::updateCurrentVolume, 
                            label = { Text("Tomos ya leídos") }, 
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.totalVolumes, 
                            onValueChange = viewModel::updateTotalVolumes, 
                            label = { Text("Total Vols") }, 
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Capítulos Totales (Ya no pedimos el actual acá)
                item {
                    OutlinedTextField(
                        value = state.totalChapters, 
                        onValueChange = viewModel::updateTotalChapters, 
                        label = { Text("Total de Capítulos de la obra") }, 
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving) {
                        Text(if (state.isSaving) "Guardando..." else "Guardar en Biblioteca")
                    }
                }
            }
        }
    }
}