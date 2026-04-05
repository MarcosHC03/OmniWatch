package com.watchlist.app.ui.releases

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.watchlist.app.data.remote.TmdbRelease
import com.watchlist.app.ui.WatchListBottomBar
import com.watchlist.app.viewmodel.ReleaseGroup
import com.watchlist.app.viewmodel.ReleasesViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleasesScreen(
    navController: NavHostController,
    viewModel: ReleasesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Estrenos", fontWeight = FontWeight.Bold)
                        Text(
                            "Próximos lanzamientos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadReleases() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Recargar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = { WatchListBottomBar(navController) }
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.loadReleases() }) { Text("Reintentar") }
            }

            state.groups.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sin estrenos disponibles.\nConfigurá tu TMDB API key.",
                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    fontSize = 13.sp
                )
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp
                )
            ) {
                state.groups.forEach { group ->
                    item(key = group.monthLabel) {
                        MonthHeader(group.monthLabel)
                    }
                    items(group.releases, key = { it.id }) { release ->
                        ReleaseItemRow(release = release)
                    }
                }
            }
        }
    }
}

@Composable
fun MonthHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
fun ReleaseItemRow(release: TmdbRelease) {
    val dateParser = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val parsedDate = runCatching {
        LocalDate.parse(release.displayDate, dateParser)
    }.getOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date box
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = parsedDate?.dayOfMonth?.toString() ?: "?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                parsedDate?.let {
                    Text(
                        text = it.format(DateTimeFormatter.ofPattern("MMM", Locale("es", "AR")))
                            .uppercase(),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f)
                    )
                }
            }
        }

        // Poster thumbnail
        val posterUrl = release.posterPath?.let { "https://image.tmdb.org/t/p/w92$it" }
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = release.displayTitle,
                modifier = Modifier
                    .width(36.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Info
        Column(Modifier.weight(1f)) {
            Text(
                text = release.displayTitle,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Type badge
        val isMovie = release.mediaType == "movie" || release.title != null
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isMovie)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = if (isMovie) "PELI" else "SERIE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isMovie)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}
