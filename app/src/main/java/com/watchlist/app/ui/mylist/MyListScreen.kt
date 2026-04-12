package com.watchlist.app.ui.mylist

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
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
    var showMoreMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // ---- SAF launchers (Backup de Claude) ----
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(it) } }

    // ---- Snackbars ----
    LaunchedEffect(state.backupSuccessMessage) {
        state.backupSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearBackupMessages()
        }
    }
    LaunchedEffect(state.backupErrorMessage) {
        state.backupErrorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearBackupMessages()
        }
    }

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

    // LÓGICA DE AGRUPACIÓN (Nuestra UI)
    val groupedItems = remember(state.items, state.selectedTab) {
        if (state.selectedTab == MediaType.SERIES) {
            state.items.groupBy { if (it.tmdbId != 0) it.tmdbId.toString() else it.title }
                .values.toList()
        } else {
            state.items.map { listOf(it) }
        }
    }

    Scaffold(
        topBar = {
            // Fondo sólido para evitar el "bug del cristal"
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { Text("Mi lista", fontWeight = FontWeight.Bold) },
                    actions = {
                        // Botón importar MAL (Ahora lanza el navegador)
                        IconButton(
                            onClick = { viewModel.startMalLogin() },
                            enabled = !state.isImporting
                        ) {
                            if (state.isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Icon(Icons.Outlined.FileDownload, contentDescription = "Importar de MAL", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        IconButton(onClick = { showSearch = !showSearch }) { Icon(Icons.Filled.Search, contentDescription = "Buscar") }
                        IconButton(onClick = { navController.navigate(Screen.AddMedia.createRoute()) }) { Icon(Icons.Filled.Add, contentDescription = "Agregar") }

                        // Menú ⋮ de Backup
                        Box {
                            IconButton(onClick = { showMoreMenu = true }, enabled = !state.isBackupProcessing) {
                                if (state.isBackupProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Exportar backup") },
                                    leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = { showMoreMenu = false; exportLauncher.launch("OmniWatch_Backup.json") }
                                )
                                DropdownMenuItem(
                                    text = { Text("Importar backup") },
                                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = { showMoreMenu = false; importLauncher.launch(arrayOf("application/json")) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )

                if (state.isImporting || state.isBackupProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
                }

                var localSearchText by remember { mutableStateOf(state.searchQuery) }

                if (showSearch) {
                    OutlinedTextField(
                        value = localSearchText, // 1. Lee de la memoria rápida local
                        onValueChange = { nuevoTexto ->
                            localSearchText = nuevoTexto // 2. Actualiza la pantalla al instante (no rompe el cursor)
                            viewModel.setSearch(nuevoTexto) // 3. Le avisa al cerebro para que filtre la lista por detrás
                        },
                        placeholder = { Text("Buscar en tu lista...") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true, shape = RoundedCornerShape(12.dp)
                    )
                }

                TabRow(
                    selectedTabIndex = when (state.selectedTab) { MediaType.SERIES -> 0; MediaType.MOVIE -> 1; MediaType.ANIME -> 2 },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    listOf(MediaType.SERIES to "Series", MediaType.MOVIE to "Películas", MediaType.ANIME to "Anime").forEach { (type, label) ->
                        Tab(selected = state.selectedTab == type, onClick = { viewModel.selectTab(type) }, text = { Text(label, fontSize = 13.sp) })
                    }
                }

                FilterStatusRow(selectedStatus = state.filterStatus, onFilterChanged = { viewModel.setFilter(it) })
                
                // Línea divisoria sutil
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AddMedia.createRoute()) }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        bottomBar = { WatchListBottomBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nada por acá todavía 👀", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Text("Tocá + para agregar tu primer título", color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 13.sp)
                    if (state.selectedTab == MediaType.ANIME) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { viewModel.startMalLogin() }) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Importar desde MyAnimeList")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 80.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(groupedItems, key = { it.first().id }) { group ->
                    if (state.selectedTab == MediaType.SERIES && group.size > 0) {
                        SeriesGroupCard(
                            group = group,
                            onEditSeason = { item -> navController.navigate(Screen.AddMedia.createRoute(item.id)) },
                            onDeleteSeason = { item -> viewModel.deleteItem(item) },
                            onStatusChange = { item, newStatus -> viewModel.updateStatus(item, newStatus) }
                        )
                    } else {
                        MediaItemCard(
                            item = group.first(),
                            onEdit = { navController.navigate(Screen.AddMedia.createRoute(group.first().id)) },
                            onDelete = { viewModel.deleteItem(group.first()) },
                            onStatusChange = { newStatus -> viewModel.updateStatus(group.first(), newStatus) }
                        )
                    }
                }
            }
        }
    }
}

// ---- TARJETA AGRUPADA DESPLEGABLE PARA SERIES ----
@Composable
fun SeriesGroupCard(group: List<MediaItemEntity>, onEditSeason: (MediaItemEntity) -> Unit, onDeleteSeason: (MediaItemEntity) -> Unit, onStatusChange: (MediaItemEntity, WatchStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sortedGroup = group.sortedBy { it.currentSeason }
    val baseItem = sortedGroup.first()

    val overallStatus = when {
        sortedGroup.any { it.watchStatus == WatchStatus.WATCHING } -> WatchStatus.WATCHING
        sortedGroup.all { it.watchStatus == WatchStatus.COMPLETED } -> WatchStatus.COMPLETED
        else -> WatchStatus.PLANNED
    }

    val vistas = sortedGroup.filter { it.watchStatus == WatchStatus.COMPLETED }.map { "T${it.currentSeason}" }
    val viendo = sortedGroup.filter { it.watchStatus == WatchStatus.WATCHING }
    val porVer = sortedGroup.filter { it.watchStatus == WatchStatus.PLANNED }.map { "T${it.currentSeason}" }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth()) {
                if (baseItem.posterPath.isNotBlank()) {
                    AsyncImage(model = baseItem.posterPath, contentDescription = baseItem.title, modifier = Modifier.width(70.dp).height(if (expanded) 105.dp else 125.dp).clip(RoundedCornerShape(topStart = 14.dp, bottomStart = if(expanded) 0.dp else 14.dp)), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.width(70.dp).height(125.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)), contentAlignment = Alignment.Center) { Text("🎬", fontSize = 24.sp) }
                }

                Column(Modifier.weight(1f).padding(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(baseItem.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = "Expandir", tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                    Spacer(Modifier.height(4.dp))
                    WatchStatusBadge(overallStatus)
                    Spacer(Modifier.height(6.dp))

                    if (vistas.isNotEmpty()) Text("Vistas: ${vistas.joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                    viendo.forEach { Text("Viendo: ${it.watchedEpisodes}/${it.totalEpisodes} eps • T${it.currentSeason}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f)) }
                    if (porVer.isNotEmpty()) Text("Por ver: ${porVer.joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                    
                    if (baseItem.platform.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(baseItem.platform, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                    sortedGroup.forEachIndexed { index, item ->
                        MiniSeasonRow(item = item, onEdit = { onEditSeason(item) }, onDelete = { onDeleteSeason(item) }, onStatusChange = { status -> onStatusChange(item, status) })
                        if (index < sortedGroup.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurface.copy(0.05f))
                    }
                }
            }
        }
    }
}

// ---- FILA EN MINIATURA PARA EL DESPLEGABLE ----
@Composable
fun MiniSeasonRow(item: MediaItemEntity, onEdit: () -> Unit, onDelete: () -> Unit, onStatusChange: (WatchStatus) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false }, title = { Text("Eliminar Temporada") },
            text = { Text("¿Eliminás la Temporada ${item.currentSeason} de tu lista?") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Temporada ${item.currentSeason}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            val progress = if (item.totalEpisodes > 0) item.watchedEpisodes.toFloat() / item.totalEpisodes.toFloat() else 0f
            Text(text = "${item.watchedEpisodes}/${item.totalEpisodes} eps", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            if (item.watchStatus == WatchStatus.WATCHING && progress > 0f) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(0.6f).height(3.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        Box {
            Box(Modifier.clickable { showStatusMenu = true }) { WatchStatusBadge(item.watchStatus) }
            DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                WatchStatus.values().forEach { status -> DropdownMenuItem(text = { Text(when (status) { WatchStatus.WATCHING -> "Viendo"; WatchStatus.COMPLETED -> "Visto"; WatchStatus.PLANNED -> "Por ver" }) }, onClick = { onStatusChange(status); showStatusMenu = false }) }
            }
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp)) }
        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
    }
}

// ---- Chips de filtro ----
@Composable
fun FilterStatusRow(selectedStatus: WatchStatus?, onFilterChanged: (WatchStatus?) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selectedStatus == null, onClick = { onFilterChanged(null) }, label = { Text("Todos", fontSize = 12.sp) })
        FilterChip(selected = selectedStatus == WatchStatus.WATCHING, onClick = { onFilterChanged(WatchStatus.WATCHING) }, label = { Text("Viendo", fontSize = 12.sp) })
        FilterChip(selected = selectedStatus == WatchStatus.COMPLETED, onClick = { onFilterChanged(WatchStatus.COMPLETED) }, label = { Text("Vistos", fontSize = 12.sp) })
        FilterChip(selected = selectedStatus == WatchStatus.PLANNED, onClick = { onFilterChanged(WatchStatus.PLANNED) }, label = { Text("Por ver", fontSize = 12.sp) })
    }
}

// ---- Card Normal ----
@Composable
fun MediaItemCard(item: MediaItemEntity, onEdit: () -> Unit, onDelete: () -> Unit, onStatusChange: (WatchStatus) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false }, title = { Text("Eliminar") },
            text = { Text("¿Eliminás \"${item.title}\" de tu lista?") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.fillMaxWidth()) {
            if (item.posterPath.isNotBlank()) {
                AsyncImage(model = item.posterPath, contentDescription = item.title, modifier = Modifier.width(70.dp).height(105.dp).clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.width(70.dp).height(105.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)), contentAlignment = Alignment.Center) { Text("🎬", fontSize = 24.sp) }
            }
            Column(Modifier.weight(1f).padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(text = item.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp)) }
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Box {
                    Box(Modifier.clickable { showStatusMenu = true }) { WatchStatusBadge(item.watchStatus) }
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        WatchStatus.values().forEach { status -> DropdownMenuItem(text = { Text(when (status) { WatchStatus.WATCHING -> "Viendo"; WatchStatus.COMPLETED -> "Visto"; WatchStatus.PLANNED -> "Por ver" }) }, onClick = { onStatusChange(status); showStatusMenu = false }) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (item.rating > 0) StarRatingBar(rating = item.rating, starSize = 16)
                if (item.mediaType != MediaType.MOVIE && item.totalEpisodes > 0) {
                    Spacer(Modifier.height(4.dp))
                    val progress = if (item.totalEpisodes > 0) item.watchedEpisodes.toFloat() / item.totalEpisodes.toFloat() else 0f
                    Text(text = buildString { append("${item.watchedEpisodes}/${item.totalEpisodes} eps"); if (item.mediaType == MediaType.SERIES) append(" · T${item.currentSeason}") }, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.55f))
                    if (item.watchStatus == WatchStatus.WATCHING && progress > 0f) {
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
                if (item.platform.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(text = item.platform, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.45f))
                }
            }
        }
    }
}