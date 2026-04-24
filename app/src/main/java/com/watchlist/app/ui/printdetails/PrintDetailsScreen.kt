package com.watchlist.app.ui.printdetails

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.watchlist.app.viewmodel.PrintDetailsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintDetailsScreen(
    onNavigateBack: () -> Unit,
    viewModel: PrintDetailsViewModel = hiltViewModel()
) {
    val data by viewModel.franchiseData.collectAsState()
    
    // Estados para el Modal de Agregar Tomo
    var showAddVolumeDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var newVolNumber by remember { androidx.compose.runtime.mutableStateOf("") }
    var newVolPages by remember { androidx.compose.runtime.mutableStateOf("") }

    // ── MODAL PARA AGREGAR TOMO ──
    if (showAddVolumeDialog) {
        AlertDialog(
            onDismissRequest = { showAddVolumeDialog = false },
            title = { Text("Agregar Nuevo Tomo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newVolNumber,
                        onValueChange = { newVolNumber = it },
                        label = { Text("Número de Tomo (Ej: 1)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newVolPages,
                        onValueChange = { newVolPages = it },
                        label = { Text("Páginas Totales (Ej: 200)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addVolume(newVolNumber, newVolPages)
                        // Limpiamos los campos y cerramos el modal
                        newVolNumber = ""
                        newVolPages = ""
                        showAddVolumeDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVolumeDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddVolumeDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar Tomo", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        if (data == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val franchise = data!!.franchise
        val volumes = data!!.volumes

        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ── ENCABEZADO (±20%) ──
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título en grande al centro
                Text(
                    text = franchise.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                // Nombre original japonés abajo
                if (franchise.originalTitle.isNotBlank()) {
                    Text(
                        text = franchise.originalTitle,
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                    // Póster a la izquierda
                    AsyncImage(
                        model = franchise.posterPath,
                        contentDescription = "Póster",
                        modifier = Modifier
                            .width(110.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    // Sinopsis y Autor a la derecha
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Autor: ${franchise.author}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = franchise.synopsis,
                            fontSize = 12.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── TOTALES DE LA FRANQUICIA ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tomos: ${franchise.totalVolumes} • Capítulos: ${franchise.totalChapters}",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── LISTA DE TOMOS / CAPÍTULOS ──
            if (volumes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay tomos agregados todavía.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp), // Margen abajo para el FAB
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(volumes, key = { it.volumeId }) { volume ->
                        // Tarjeta del tomo mejorada
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Tomo ${volume.volumeNumber}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Páginas: ${volume.currentPage} / ${if (volume.totalPages > 0) volume.totalPages else "?"}", 
                                        fontSize = 12.sp, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Botones de Acción (Editar y Borrar)
                                Row {
                                    IconButton(onClick = { /* Pronto: Abrir modal en modo edición */ }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Editar Tomo", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { /* Pronto: viewModel.deleteVolume(volume) */ }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Borrar Tomo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}