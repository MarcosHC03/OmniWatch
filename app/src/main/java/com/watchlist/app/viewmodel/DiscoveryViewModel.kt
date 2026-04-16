package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.data.remote.TmdbRelease
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Estado
// ---------------------------------------------------------------------------

data class DiscoveryUiState(
    // Listas originales descargadas de la API (sin filtrar)
    val allMovies: List<TmdbRelease> = emptyList(),
    val allTv: List<TmdbRelease> = emptyList(),
    val allAnime: List<TmdbRelease> = emptyList(),

    // Listas filtradas por searchQuery — estas son las que muestra la UI
    val filteredMovies: List<TmdbRelease> = emptyList(),
    val filteredTv: List<TmdbRelease> = emptyList(),
    val filteredAnime: List<TmdbRelease> = emptyList(),

    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,

    // IDs de releases ya guardados en "Por Ver" — para mostrar el check
    val savedReleaseIds: Set<Int> = emptySet()
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoveryUiState(isLoading = true))
    val state: StateFlow<DiscoveryUiState> = _state.asStateFlow()

    private val displayFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val apiFormat     = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        loadContent()
    }

    // -------------------------------------------------------------------------
    // Carga inicial
    // -------------------------------------------------------------------------

    fun loadContent() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // 1. Buscamos TODOS los tmdbId que ya tenés en tu lista
                val localItems = repository.getAllItemsOnce()
                val alreadySavedIds = localItems.map { it.tmdbId }.toSet()

                // 2. Traemos las tendencias
                val movies = repository.getPopularMoviesAsReleases()
                val tv     = repository.getPopularTvAsReleases()
                val anime  = repository.getPopularAnimeAsReleases()
                
                _state.update { s ->
                    s.copy(
                        allMovies      = movies,
                        allTv          = tv,
                        allAnime       = anime,
                        filteredMovies = movies, 
                        filteredTv     = tv,
                        filteredAnime  = anime,
                        savedReleaseIds = alreadySavedIds,
                        isLoading      = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error     = "No se pudieron cargar los estrenos. Revisá tu conexión."
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Búsqueda Global en TMDB / Jikan (Con Debounce)
    // -------------------------------------------------------------------------

    private var searchJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        
        // Cancelamos la búsqueda anterior si el usuario tipeó muy rápido
        searchJob?.cancel()

        // Si la barra queda vacía, restauramos los "Populares" al instante
        if (query.isBlank()) {
            _state.update { s -> 
                s.copy(
                    filteredMovies = s.allMovies, 
                    filteredTv     = s.allTv,
                    filteredAnime  = s.allAnime,
                    isLoading      = false
                ) 
            }
            return
        }

        // Si escribió algo, esperamos medio segundo y disparamos a TMDB
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _state.update { it.copy(isLoading = true) }
            
            try {
                // Buscamos en las 3 bases de datos a la vez
                val tmdbMovies = repository.searchMovies(query)
                val tmdbTv     = repository.searchTv(query)
                val jikanAnime = repository.searchJikanAnime(query)

                // Filtramos la basura y empaquetamos con nombres correctos
                val mappedMovies = tmdbMovies.filter { !it.releaseDate.isNullOrBlank() && !it.posterPath.isNullOrBlank() }
                    .map { TmdbRelease(id = it.id, title = it.title, name = it.name, releaseDate = it.releaseDate, firstAirDate = it.firstAirDate, posterPath = it.posterPath, mediaType = "movie") }
                    
                val mappedTv = tmdbTv.filter { !it.firstAirDate.isNullOrBlank() && !it.posterPath.isNullOrBlank() }
                    .map { TmdbRelease(id = it.id, title = it.title, name = it.name, releaseDate = it.releaseDate, firstAirDate = it.firstAirDate, posterPath = it.posterPath, mediaType = "tv") }

                val mappedAnime = jikanAnime.filter { !it.posterPath.isNullOrBlank() }
                    .map { TmdbRelease(id = it.id, title = it.title, name = it.name, releaseDate = it.releaseDate, firstAirDate = it.firstAirDate, posterPath = it.posterPath, mediaType = "anime") }
                    .distinctBy { it.id }

                // Actualizamos la UI con los resultados reales
                _state.update { 
                    it.copy(
                        filteredMovies = mappedMovies, 
                        filteredTv     = mappedTv, 
                        filteredAnime  = mappedAnime,
                        isLoading      = false
                    ) 
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Guardar rápido en "Por Ver"
    // -------------------------------------------------------------------------

    fun quickSave(release: TmdbRelease, mediaType: MediaType) {
        viewModelScope.launch {
            val formattedDate = formatApiDate(release.displayDate)

            // El mismo parche del póster que tiene la UI, pero para guardar en la DB
            val finalPoster = if (release.posterPath?.startsWith("http") == true) {
                release.posterPath
            } else {
                release.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
            }

            val entity = MediaItemEntity(
                title           = release.displayTitle,
                posterPath      = finalPoster,
                mediaType       = mediaType,
                watchStatus     = WatchStatus.PLANNED,
                watchedEpisodes = 0,
                totalEpisodes   = release.totalEpisodes ?: 0,
                releaseDate     = formattedDate,
                year            = parseYear(release.displayDate),
                tmdbId          = release.id
            )
            repository.insertItem(entity)

            // Marcamos el ID como guardado en vivo
            _state.update { it.copy(savedReleaseIds = it.savedReleaseIds + release.id) }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers de fecha
    // -------------------------------------------------------------------------

    /** "2025-04-18" → "18/04/2025". Si solo manda el año "2026", simula "01/01/2026" */
    private fun formatApiDate(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        
        // Parche para Jikan: Si solo manda el año (4 dígitos)
        if (trimmed.length == 4) return "01/01/$trimmed"

        return runCatching {
            LocalDate.parse(trimmed, apiFormat).format(displayFormat)
        }.getOrDefault(trimmed) // Si algo falla, guardamos el texto crudo en vez de perderlo
    }

    private fun parseYear(raw: String?): Int {
        if (raw.isNullOrBlank()) return 0
        val trimmed = raw.trim()
        
        if (trimmed.length == 4) return trimmed.toIntOrNull() ?: 0

        return runCatching {
            LocalDate.parse(trimmed, apiFormat).year
        }.getOrDefault(0)
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
