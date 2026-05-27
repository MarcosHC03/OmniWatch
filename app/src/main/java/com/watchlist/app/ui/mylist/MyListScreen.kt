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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Tv
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
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.PrintType
import com.watchlist.app.data.local.entities.ReadStatus
import com.watchlist.app.navigation.Screen
import com.watchlist.app.ui.StarRatingBar
import com.watchlist.app.ui.WatchListBottomBar
import com.watchlist.app.ui.WatchStatusBadge
import com.watchlist.app.ui.ReadStatusBadge
import com.watchlist.app.viewmodel.MyListViewModel
import com.watchlist.app.viewmodel.ListMode

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

    val context = androidx.compose.ui.platform.LocalContext.current

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

    LaunchedEffect(state.importErrorMessage) {
        state.importErrorMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
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
            Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (state.listMode == ListMode.AUDIOVISUAL) "Mi lista" else "Mi Biblioteca", 
                            fontWeight = FontWeight.Bold 
                        ) 
                    },
                    actions = {
                        // 1. La Lupa
                        IconButton(onClick = { showSearch = !showSearch }) { 
                            Icon(Icons.Filled.Search, contentDescription = "Buscar") 
                        }
                        
                        // 2. El Toggle (Audiovisual vs Impresos)
                        IconButton(onClick = { viewModel.toggleListMode() }) {
                            // Usamos íconos nativos. Si tenés iconos mejores podés cambiarlos.
                            if (state.listMode == ListMode.AUDIOVISUAL) {
                                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.MenuBook, contentDescription = "Modo Lectura") // Icono de libro
                            } else {
                                Icon(androidx.compose.material.icons.Icons.Outlined.Tv, contentDescription = "Modo Pantalla") // Icono de TV
                            }
                        }

                        // 3. Menú ⋮ (Todo lo extra agrupado acá)
                        Box {
                            IconButton(onClick = { showMoreMenu = true }, enabled = !state.isBackupProcessing && !state.isImporting) {
                                if (state.isBackupProcessing || state.isImporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                                }
                            }
                            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                // Opción MAL (Visible en anime O en manga)
                                val showMalImport = (state.listMode == ListMode.AUDIOVISUAL && state.selectedTab == MediaType.ANIME) ||
                                                    (state.listMode == ListMode.PRINTED && state.selectedPrintTab == PrintType.MANGA)

                                if (showMalImport) {
                                    DropdownMenuItem(
                                        text = { Text("Importar de MAL") },
                                        leadingIcon = { Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        onClick = { showMoreMenu = false; viewModel.startMalLogin() }
                                    )
                                    HorizontalDivider() // Separador
                                }
                                
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
                        value = localSearchText,
                        onValueChange = { nuevoTexto ->
                            localSearchText = nuevoTexto
                            viewModel.setSearch(nuevoTexto)
                        },
                        placeholder = { Text("Buscar en tu lista...") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true, shape = RoundedCornerShape(12.dp)
                    )
                }

                // ── TABS DINÁMICOS SEGÚN EL MODO ──
                if (state.listMode == ListMode.AUDIOVISUAL) {
                    TabRow(
                        selectedTabIndex = when (state.selectedTab) { MediaType.SERIES -> 0; MediaType.MOVIE -> 1; MediaType.ANIME -> 2 },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        listOf(MediaType.SERIES to "Series", MediaType.MOVIE to "Películas", MediaType.ANIME to "Anime").forEach { (type, label) ->
                            Tab(selected = state.selectedTab == type, onClick = { viewModel.selectTab(type) }, text = { Text(label, fontSize = 13.sp) })
                        }
                    }
                } else {
                    TabRow(
                        selectedTabIndex = when (state.selectedPrintTab) { 
                            PrintType.COMIC -> 0 
                            PrintType.MANGA -> 1 
                            PrintType.MANHWA -> 2 
                            PrintType.NOVEL -> 3 
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        listOf(
                            PrintType.COMIC to "Cómic", 
                            PrintType.MANGA to "Manga", 
                            PrintType.MANHWA to "Manhwa", 
                            PrintType.NOVEL to "N. Gráfica"
                        ).forEach { (type, label) ->
                            Tab(selected = state.selectedPrintTab == type, onClick = { viewModel.selectPrintTab(type) }, text = { Text(label, fontSize = 11.sp, maxLines = 1) })
                        }
                    }
                }

                FilterStatusRow(selectedStatus = state.filterStatus, listMode = state.listMode, onFilterChanged = { viewModel.setFilter(it) })
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (state.listMode == ListMode.AUDIOVISUAL) {
                        navController.navigate(Screen.AddMedia.createRoute())
                    } else {
                        navController.navigate(Screen.AddPrintMedia.createRoute())
                    }
                }, 
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        bottomBar = { WatchListBottomBar(navController) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val itemsAreEmpty = if (state.listMode == ListMode.AUDIOVISUAL) state.items.isEmpty() else state.printItems.isEmpty()

        if (itemsAreEmpty) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nada por acá todavía 👀", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    
                    // ── LÓGICA PARA TEXTO DINÁMICO ──
                    val tipoTexto = if (state.listMode == ListMode.AUDIOVISUAL) {
                        when (state.selectedTab) {
                            MediaType.MOVIE -> "película"
                            MediaType.SERIES -> "serie"
                            MediaType.ANIME -> "anime"
                        }
                    } else {
                        when (state.selectedPrintTab) {
                            PrintType.COMIC -> "cómic"
                            PrintType.MANGA -> "manga"
                            PrintType.MANHWA -> "manhwa"
                            PrintType.NOVEL -> "novela gráfica"
                        }
                    }
                    
                    Text("Tocá + para agregar tu primer $tipoTexto", color = MaterialTheme.colorScheme.onSurface.copy(0.5f), fontSize = 13.sp)
                    
                    // ── BOTÓN DE MAL (ANIME O MANGA) ──
                    val showMalImportEmpty = (state.listMode == ListMode.AUDIOVISUAL && state.selectedTab == MediaType.ANIME) ||
                                             (state.listMode == ListMode.PRINTED && state.selectedPrintTab == PrintType.MANGA)

                    if (showMalImportEmpty) {
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
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                    start = 16.dp, end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.listMode == ListMode.AUDIOVISUAL) {
                    items(state.items, key = { it.id }) { item ->
                        MediaItemCard(
                            item = item,
                            onEdit = { navController.navigate(Screen.AddMedia.createRoute(item.id)) },
                            onDelete = { viewModel.deleteItem(item) },
                            onStatusChange = { newStatus -> viewModel.updateStatus(item, newStatus) },
                            onPlusOne = { viewModel.plusOneEpisode(it) }
                        )
                    }
                } else {
                    // ── ESTO ES LO NUEVO: LISTA DE IMPRESOS ──
                    items(state.printItems, key = { it.id }) { item ->
                        PrintMediaItemCard(
                            item = item,
                            onClick = { navController.navigate(Screen.PrintDetails.createRoute(item.id)) },
                            onEdit = { navController.navigate(Screen.AddPrintMedia.createRoute(item.id)) },
                            onDelete = { viewModel.deletePrintItem(item) },
                            onPlusOneClick = { viewModel.plusOnePrintChapter(it) },
                            // --- Por ahora solo manda un Toast de aviso ---
                            onReadClick = { android.widget.Toast.makeText(context, "Próximamente... (v2.5)", android.widget.Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
        }
    }
}

// ---- Chips de filtro ----
@Composable
fun FilterStatusRow(selectedStatus: WatchStatus?, listMode: ListMode, onFilterChanged: (WatchStatus?) -> Unit) {
    val isPrint = listMode == ListMode.PRINTED // Variable para saber si es biblioteca
    
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = selectedStatus == null, onClick = { onFilterChanged(null) }, label = { Text("Todos", fontSize = 12.sp) })
        FilterChip(selected = selectedStatus == WatchStatus.WATCHING, onClick = { onFilterChanged(WatchStatus.WATCHING) }, label = { Text(if (isPrint) "Leyendo" else "Viendo", fontSize = 12.sp) })
        FilterChip(selected = selectedStatus == WatchStatus.COMPLETED, onClick = { onFilterChanged(WatchStatus.COMPLETED) }, label = { Text(if (isPrint) "Leídos" else "Vistos", fontSize = 12.sp) })
        FilterChip(selected = selectedStatus == WatchStatus.PLANNED, onClick = { onFilterChanged(WatchStatus.PLANNED) }, label = { Text(if (isPrint) "Por leer" else "Por ver", fontSize = 12.sp) })
    }
}

// ---- Card Audiovisual ----
@Composable
fun MediaItemCard(
    item: MediaItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (WatchStatus) -> Unit,
    onPlusOne: (MediaItemEntity) -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusMenu  by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar") },
            text  = { Text("¿Eliminás \"${item.title}\" de tu lista?") },
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
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Póster ──────────────────────────────────────────────────────
            if (item.posterPath.isNotBlank()) {
                AsyncImage(
                    model              = item.posterPath,
                    contentDescription = item.title,
                    modifier           = Modifier
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
                ) { Text("🎬", fontSize = 24.sp) }
            }

            // ── Contenido central ────────────────────────────────────────────
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Título
                Text(
                    text       = item.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )

                // Badge de estado (tocable para cambiar)
                Box {
                    Box(Modifier.clickable { showStatusMenu = true }) {
                        WatchStatusBadge(item.watchStatus)
                    }
                    DropdownMenu(
                        expanded          = showStatusMenu,
                        onDismissRequest  = { showStatusMenu = false }
                    ) {
                        WatchStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Text(when (status) {
                                        WatchStatus.WATCHING  -> "Viendo"
                                        WatchStatus.COMPLETED -> "Visto"
                                        WatchStatus.PLANNED   -> "Por ver"
                                    })
                                },
                                onClick = { onStatusChange(status); showStatusMenu = false }
                            )
                        }
                    }
                }

                // Puntaje
                if (item.rating > 0f) {
                    StarRatingBar(rating = item.rating, starSize = 14)
                }

                // Temporada + progreso de episodios (solo Series y Anime)
                if (item.mediaType != MediaType.MOVIE && item.totalEpisodes > 0) {
                    val progress = (item.watchedEpisodes.toFloat() / item.totalEpisodes.toFloat())
                        .coerceIn(0f, 1f)

                    // "T2 · 6/13 eps"
                    val label = buildString {
                        if (item.mediaType == MediaType.SERIES) append("T${item.currentSeason} · ")
                        append("${item.watchedEpisodes}/${item.totalEpisodes} eps")
                    }
                    Text(
                        text     = label,
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    LinearProgressIndicator(
                        progress    = { progress },
                        modifier    = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color       = MaterialTheme.colorScheme.primary,
                        trackColor  = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Plataforma
                if (item.platform.isNotBlank()) {
                    Text(
                        text     = item.platform,
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // ── Acciones verticales ──────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                // Botón +1 episodio
                if (item.mediaType != MediaType.MOVIE && item.watchStatus == WatchStatus.WATCHING) {
                    IconButton(
                        onClick = {
                            if (item.watchedEpisodes < item.totalEpisodes) {
                                onPlusOne(item)
                            } else {
                                android.widget.Toast.makeText(
                                    ctx,
                                    "Modifique la serie a la siguiente temporada editando la tarjeta",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        // Ícono diferente si ya completó todos los eps
                        val tint = if (item.watchedEpisodes >= item.totalEpisodes && item.totalEpisodes > 0)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primary

                        Icon(
                            imageVector        = Icons.Filled.AddCircle,
                            contentDescription = "+1 episodio",
                            tint               = tint,
                            modifier           = Modifier.size(28.dp)
                        )
                    }
                }

                // Editar
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Editar",
                        modifier           = Modifier.size(16.dp)
                    )
                }

                // Eliminar
                IconButton(
                    onClick  = { showDeleteDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Eliminar",
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


// ---- Card Impresos ----
@Composable
fun PrintMediaItemCard(
    item: PrintMediaEntity,
    onClick: (PrintMediaEntity) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPlusOneClick: (PrintMediaEntity) -> Unit,
    onReadClick: () -> Unit // Para la v2.5
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStatusMenu  by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar") },
            text  = { Text("¿Eliminás \"${item.title}\" de tu biblioteca?") },
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
        modifier  = Modifier.fillMaxWidth().clickable { onClick(item) },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Póster (Izquierda) ───────────────────────────────────────────
            if (item.posterPath.isNotBlank()) {
                AsyncImage(
                    model              = item.posterPath,
                    contentDescription = item.title,
                    modifier           = Modifier
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
                ) { Text("📖", fontSize = 24.sp) }
            }

            // ── Contenido central ────────────────────────────────────────────
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Título
                Text(
                    text       = item.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )

                // Estado
                ReadStatusBadge(status = item.status)

                // Rating
                if (item.rating > 0f) {
                    StarRatingBar(rating = item.rating, starSize = 14)
                }

                // Tomo y Capítulo Global (Sin páginas, eso va en Detalles)
                val volTotal = if (item.totalVolumes > 0) item.totalVolumes.toString() else "?"
                val capTotal = if (item.totalChapters > 0) item.totalChapters.toString() else "?"
                
                val progressText = "Vol ${item.currentVolume} / $volTotal • Cap ${item.currentChapter} / $capTotal"
                
                Text(
                    text     = progressText,
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                
                // ── BARRA DE PROGRESO UNIFICADA E INTELIGENTE ──
                val progress = when {
                    // Prioridad 1: Capítulos (Es lo que suma el botón +1 y es más preciso)
                    item.totalChapters > 0 -> (item.currentChapter.toFloat() / item.totalChapters.toFloat()).coerceIn(0f, 1f)
                    
                    // Prioridad 2: Tomos (Fallback si no sabemos el total de capítulos)
                    item.totalVolumes > 0 -> (item.currentVolume.toFloat() / item.totalVolumes.toFloat()).coerceIn(0f, 1f)
                    
                    // Si todo es 0 (ej: manga en publicación sin tope), la barra queda vacía
                    else -> 0f 
                }

                // Ahora le pasamos este 'progress' inteligente al indicador visual
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Autor
                if (item.author.isNotBlank()) {
                    Text(
                        text     = item.author,
                        fontSize = 11.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // ── Acciones verticales (Derecha) ────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                // Botón de +1 Capítulo Rápido
                if (item.status == ReadStatus.READING) {
                    IconButton(
                        onClick = {
                            if (item.currentChapter < item.totalChapters) {
                                onPlusOneClick(item)
                            } else {
                                android.widget.Toast.makeText(
                                    ctx,
                                    "¡Tope alcanzado! Editá la franquicia para seguir leyendo.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        // Ícono diferente si ya completó todos los eps
                        val tint = if (item.currentChapter >= item.totalChapters && item.totalChapters > 0)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.primary

                        Icon(
                            imageVector        = Icons.Filled.AddCircle,
                            contentDescription = "+1 capítulo",
                            tint               = tint,
                            modifier           = Modifier.size(28.dp)
                        )
                    }
                }

                // Editar
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                }

                // Eliminar
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }

                // Lector (Para la v2.5) - Usamos un ícono de libro abierto
                IconButton(onClick = onReadClick, modifier = Modifier.size(32.dp)) {
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.MenuBook, contentDescription = "Leer archivo", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}