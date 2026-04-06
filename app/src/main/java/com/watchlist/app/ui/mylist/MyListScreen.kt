package com.watchlist.app.ui.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FileDownload
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
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.navigation.Screen
import com.watchlist.app.ui.StarRatingBar
import com.watchlist.app.ui.WatchListBottomBar
import com.watchlist.app.ui.WatchStatusBadge
import com.watchlist.app.viewmodel.MyListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    navController: NavHostController,
    viewModel: MyListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var showMalDialog by remember { mutableStateOf(false) }

    // Snackbar para feedback de importación
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.importSuccessMessage) {
        state.importSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessages()
        }
    }
    LaunchedEffect(state.importErrorMessage) {
        state.importErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearImportMessages()
        }
    }

    // ---- Dialogo de importación MAL ----
    if (showMalDialog) {
        MalImportDialog(
            onConfirm = { username ->
                showMalDialog = false
                viewModel.importFromMyAnimeList(username)
            },
            onDismiss = { showMalDialog = false }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Mi lista", fontWeight = FontWeight.Bold) },
                    actions = {
                        // Botón importar desde MAL
                        IconButton(
                            onClick = { showMalDialog = true },
                            enabled = !state.isImporting
                        ) {
                            if (state.isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = "Importar de MyAnimeList",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // Botón buscar
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Filled.Search, contentDescription = "Buscar")
                        }
                        // Botón agregar
                        IconButton(onClick = {
                            navController.navigate(Screen.AddMedia.createRoute())
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Agregar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Banner de carga durante importación
                if (state.isImporting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (showSearch) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearch(it) },
                        placeholder = { Text("Buscar en tu lista...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Tabs: Series / Películas / Anime
                TabRow(
                    selectedTabIndex = when (state.selectedTab) {
                        MediaType.SERIES -> 0
                        MediaType.MOVIE -> 1
                        MediaType.ANIME -> 2
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    listOf(
                        MediaType.SERIES to "Series",
                        MediaType.MOVIE to "Películas",
                        MediaType.ANIME to "Anime"
                    ).forEach { (type, label) ->
                        Tab(
                            selected = state.selectedTab == type,
                            onClick = { viewModel.selectTab(type) },
                            text = { Text(label, fontSize = 13.sp) }
                        )
                    }
                }

                // Chips de filtro por estado
                FilterStatusRow(
                    selectedStatus = state.filterStatus,
                    onFilterChanged = { viewModel.setFilter(it) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddMedia.createRoute()) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Agregar",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        bottomBar = { WatchListBottomBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.items.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Nada por acá todavía 👀",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tocá + para agregar tu primer título",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                        fontSize = 13.sp
                    )
                    if (state.selectedTab == MediaType.ANIME) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { showMalDialog = true }) {
                            Icon(
                                Icons.Outlined.FileDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Importar desde MyAnimeList")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    MediaItemCard(
                        item = item,
                        onEdit = { navController.navigate(Screen.AddMedia.createRoute(item.id)) },
                        onDelete = { viewModel.deleteItem(item) },
                        onStatusChange = { newStatus -> viewModel.updateStatus(item, newStatus) }
                    )
                }
            }
        }
    }
}

// ---- Dialog de importación MAL ----
@Composable
private fun MalImportDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Importar desde MyAnimeList", fontWeight = FontWeight.SemiBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ingresá tu nombre de usuario de MyAnimeList para importar tu lista de anime.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        showError = false
                    },
                    label = { Text("Usuario de MAL") },
                    placeholder = { Text("ej: TuUsuario123") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Ingresá un nombre de usuario") }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isBlank()) {
                        showError = true
                    } else {
                        onConfirm(username.trim())
                    }
                }
            ) {
                Text("Importar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ---- Chips de filtro ----
@Composable
fun FilterStatusRow(
    selectedStatus: WatchStatus?,
    onFilterChanged: (WatchStatus?) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedStatus == null,
            onClick = { onFilterChanged(null) },
            label = { Text("Todos", fontSize = 12.sp) }
        )
        FilterChip(
            selected = selectedStatus == WatchStatus.WATCHING,
            onClick = { onFilterChanged(WatchStatus.WATCHING) },
            label = { Text("Viendo", fontSize = 12.sp) }
        )
        FilterChip(
            selected = selectedStatus == WatchStatus.COMPLETED,
            onClick = { onFilterChanged(WatchStatus.COMPLETED) },
            label = { Text("Vistos", fontSize = 12.sp) }
        )
        FilterChip(
            selected = selectedStatus == WatchStatus.PLANNED,
            onClick = { onFilterChanged(WatchStatus.PLANNED) },
            label = { Text("Por ver", fontSize = 12.sp) }
        )
    }
}

// ---- Card de cada item ----
@Composable
fun MediaItemCard(
    item: MediaItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (WatchStatus) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar") },
            text = { Text("¿Eliminás \"${item.title}\" de tu lista?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            // Poster
            if (item.posterPath.isNotBlank()) {
                AsyncImage(
                    model = item.posterPath,
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(70.dp)
                        .height(105.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier
                        .width(70.dp)
                        .height(105.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎬", fontSize = 24.sp)
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Editar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Badge de estado — tocable para cambiar
                Box {
                    Box(Modifier.clickable { showStatusMenu = true }) {
                        WatchStatusBadge(item.watchStatus)
                    }
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        WatchStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (status) {
                                            WatchStatus.WATCHING -> "Viendo"
                                            WatchStatus.COMPLETED -> "Visto"
                                            WatchStatus.PLANNED -> "Por ver"
                                        }
                                    )
                                },
                                onClick = { onStatusChange(status); showStatusMenu = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                if (item.rating > 0) {
                    StarRatingBar(rating = item.rating, starSize = 16)
                }

                if (item.mediaType != MediaType.MOVIE && item.totalEpisodes > 0) {
                    Spacer(Modifier.height(4.dp))
                    // Barra de progreso de episodios
                    val progress = if (item.totalEpisodes > 0)
                        item.watchedEpisodes.toFloat() / item.totalEpisodes.toFloat()
                    else 0f
                    Text(
                        text = buildString {
                            append("${item.watchedEpisodes}/${item.totalEpisodes} eps")
                            if (item.mediaType == MediaType.SERIES)
                                append(" · T${item.currentSeason}")
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.55f)
                    )
                    if (item.watchStatus == WatchStatus.WATCHING && progress > 0f) {
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                if (item.platform.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.platform,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.45f)
                    )
                }
            }
        }
    }
}
