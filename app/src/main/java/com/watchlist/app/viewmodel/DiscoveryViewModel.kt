package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.data.local.entities.DiscoveryCacheEntity
import com.watchlist.app.data.local.entities.DiscoveryPrintCacheEntity
import com.watchlist.app.data.remote.TmdbRelease
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Estado - Ahora con soporte para Impresos
// ---------------------------------------------------------------------------

data class DiscoveryUiState(
    val isPrintMode: Boolean = false, // El toggle principal (Audiovisual vs Impresos)
    
    // Audiovisual (TMDB / Jikan Anime)
    val allMovies: List<TmdbRelease> = emptyList(),
    val allTv: List<TmdbRelease> = emptyList(),
    val allAnime: List<TmdbRelease> = emptyList(),
    val filteredMovies: List<TmdbRelease> = emptyList(),
    val filteredTv: List<TmdbRelease> = emptyList(),
    val filteredAnime: List<TmdbRelease> = emptyList(),

    // Impresos (¡NUEVO: Listas separadas en caché!)
    val allMangas: List<PrintMediaEntity> = emptyList(),
    val allManhwas: List<PrintMediaEntity> = emptyList(),
    val allNovels: List<PrintMediaEntity> = emptyList(),
    val allComics: List<PrintMediaEntity> = emptyList(),
    
    val filteredPrintItems: List<PrintMediaEntity> = emptyList(),
    val selectedPrintTab: Int = 0, // 0: Manga, 1: Manhwa, 2: Novelas, 3: Cómics

    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    
    val savedReleaseMap: Map<Int, Long> = emptyMap(),
    val savedPrintMap: Map<Int, Long> = emptyMap() // Mapa para IDs de MAL/CV -> Local DB
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
        observeLocalDatabase()
        loadContent()
    }

    // --- LOGICA DE TOGGLE ---

    fun toggleMode(isPrint: Boolean) {
        _state.update { it.copy(isPrintMode = isPrint, searchQuery = "") }
        loadContent()
    }

    fun onPrintTabChange(tabIndex: Int) {
        _state.update { it.copy(selectedPrintTab = tabIndex) }
        
        if (_state.value.searchQuery.isBlank()) {
            _state.update { s -> 
                val newFiltered = when (tabIndex) {
                    0 -> s.allMangas
                    1 -> s.allManhwas
                    2 -> s.allNovels
                    else -> s.allComics
                }
                s.copy(filteredPrintItems = newFiltered) 
            }
        } else {
            viewModelScope.launch { performSearch(_state.value.searchQuery) }
        }
    }

    // -------------------------------------------------------------------------
    // Carga inicial
    // -------------------------------------------------------------------------

    fun loadContent() {
        viewModelScope.launch {
            val s = _state.value
            
            try {
                if (!s.isPrintMode) {
                    // --- AUDIOVISUAL ---
                    if (s.allMovies.isNotEmpty()) {
                        _state.update { it.copy(
                            filteredMovies = it.allMovies, 
                            filteredTv = it.allTv, 
                            filteredAnime = it.allAnime,
                            isLoading = false
                        )}
                        return@launch
                    }

                    _state.update { it.copy(isLoading = true, error = null) }
                    
                    val movies = repository.getPopularMoviesAsReleases()
                    val tv     = repository.getPopularTvAsReleases()
                    val anime  = repository.getPopularAnimeAsReleases()
                    
                    val cacheList = mutableListOf<DiscoveryCacheEntity>()
                    cacheList.addAll(mapToCache(movies, MediaType.MOVIE))
                    cacheList.addAll(mapToCache(tv, MediaType.SERIES))
                    cacheList.addAll(mapToCache(anime, MediaType.ANIME))
                    repository.saveToDiscoveryCache(cacheList)

                    _state.update { 
                        it.copy(
                            allMovies = movies, allTv = tv, allAnime = anime,
                            filteredMovies = movies, filteredTv = tv, filteredAnime  = anime,
                            isLoading = false
                        )
                    }
                } else {
                    // --- IMPRESOS ---
                    if (s.allMangas.isNotEmpty()) {
                        _state.update { state -> 
                            val newFiltered = when (state.selectedPrintTab) {
                                0 -> state.allMangas; 1 -> state.allManhwas
                                2 -> state.allNovels; else -> state.allComics
                            }
                            state.copy(filteredPrintItems = newFiltered, isLoading = false)
                        }
                        return@launch
                    }

                    _state.update { it.copy(isLoading = true, error = null) }
                    
                    // Llamamos a todo secuencialmente con pausas para evitar ban de Jikan
                    val mangas = repository.getPopularPrintAsReleases("manga")
                    kotlinx.coroutines.delay(350) 
                    val manhwas = repository.getPopularPrintAsReleases("manhwa")
                    kotlinx.coroutines.delay(350)
                    val novels = repository.getPopularPrintAsReleases("novel")
                    val comics = repository.getLatestComicReleases()
                    
                    val fullPrintCache = mutableListOf<DiscoveryPrintCacheEntity>()
                    fullPrintCache.addAll(mapToPrintCache(mangas))
                    fullPrintCache.addAll(mapToPrintCache(manhwas))
                    fullPrintCache.addAll(mapToPrintCache(novels))
                    fullPrintCache.addAll(mapToPrintCache(comics))

                    repository.saveToDiscoveryPrintCache(fullPrintCache)

                    _state.update { state -> 
                        val newFiltered = when (state.selectedPrintTab) {
                            0 -> mangas; 1 -> manhwas
                            2 -> novels; else -> comics
                        }
                        state.copy(
                            allMangas = mangas, allManhwas = manhwas,
                            allNovels = novels, allComics = comics,
                            filteredPrintItems = newFiltered, 
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error al cargar contenido.") }
            }
        }
    }

    
    // --- Recarga de datos ---
    
    fun forceRefresh() {
        // Vaciamos el caché en memoria para obligar a loadContent a llamar a las APIs
        _state.update { it.copy(
            allMovies = emptyList(),
            allMangas = emptyList(),
            searchQuery = "" // Limpiamos la barra por si había algo escrito
        )}
        loadContent()
    }

    // -------------------------------------------------------------------------
    // Búsqueda Global (Con Debounce)
    // -------------------------------------------------------------------------

    private var searchJob: kotlinx.coroutines.Job? = null

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        // Si la barra queda vacía, restauramos los "Populares" al instante sin gastar internet
        if (query.isBlank()) {
            _state.update { s -> 
                // Evaluamos qué lista de impresos restaurar según la pestaña activa
                val restoredPrintItems = when (s.selectedPrintTab) {
                    0 -> s.allMangas
                    1 -> s.allManhwas
                    2 -> s.allNovels
                    else -> s.allComics
                }

                s.copy(
                    filteredMovies     = s.allMovies, 
                    filteredTv         = s.allTv,
                    filteredAnime      = s.allAnime,
                    filteredPrintItems = restoredPrintItems,
                    isLoading          = false
                )
            }
            return
        }

        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            performSearch(query)
        }
    }
    private suspend fun performSearch(query: String) {
        _state.update { it.copy(isLoading = true) }
        try {
            if (!_state.value.isPrintMode) {
                // --- BÚSQUEDA DE AUDIOVISUALES SEGÚN EL CHIP ---
                val tmdbMovies = repository.searchMovies(query)
                val tmdbTv     = repository.searchTv(query)
                val jikanAnime = repository.searchJikanAnime(query)

                val mappedMovies = tmdbMovies.filter { !it.releaseDate.isNullOrBlank() && !it.posterPath.isNullOrBlank() }
                    .map {TmdbRelease(id = it.id, title = it.title, name = it.name, releaseDate = it.releaseDate, firstAirDate = it.firstAirDate, posterPath = it.posterPath, mediaType = "movie") }
                    
                val mappedTv = tmdbTv.filter { !it.firstAirDate.isNullOrBlank() && !it.posterPath.isNullOrBlank() }
                    .map { TmdbRelease(id = it.id, title = it.title, name = it.name, releaseDate = it.releaseDate, firstAirDate = it.firstAirDate, posterPath = it.posterPath, mediaType = "tv") }

                val mappedAnime = jikanAnime.filter { !it.posterPath.isNullOrBlank() }
                    .map { TmdbRelease(id = it.id, title = it.title, name = it.name, releaseDate = it.releaseDate, firstAirDate = it.firstAirDate, posterPath = it.posterPath, mediaType = "anime") }
                    .distinctBy { it.id }

                val searchCache = mutableListOf<DiscoveryCacheEntity>()
                searchCache.addAll(mapToCache(mappedMovies, MediaType.MOVIE))
                searchCache.addAll(mapToCache(mappedTv, MediaType.SERIES))
                searchCache.addAll(mapToCache(mappedAnime, MediaType.ANIME))
                repository.saveToDiscoveryCache(searchCache)

                _state.update { 
                    it.copy(
                        filteredMovies = mappedMovies, 
                        filteredTv     = mappedTv, 
                        filteredAnime  = mappedAnime,
                        isLoading      = false
                    ) 
                }
            } else {
                // --- BÚSQUEDA DE IMPRESOS SEGÚN EL CHIP ---
                
                // Determinamos el "type" para Jikan según el Chip
                val jikanType = when (_state.value.selectedPrintTab) {
                    0 -> "manga"
                    1 -> "manhwa"
                    2 -> "novel"
                    else -> null // Para Cómics (ComicVine) no usamos jikanType
                }

                // Disparamos la búsqueda correcta
                val results = if (_state.value.selectedPrintTab == 3) {
                    repository.searchComicVine(query)
                } else {
                    repository.searchJikanManga(query, jikanType) // Pasamos el tipo ajustado
                }

                // GUARDADO EN CACHÉ
                val printCache = results.map { item ->
                    DiscoveryPrintCacheEntity(
                        externalId = item.externalId,
                        title = item.title,
                        author = item.author,
                        posterPath = item.posterPath,
                        synopsis = item.synopsis,
                        printType = item.printType,
                        totalVolumes = item.totalVolumes,
                        totalChapters = item.totalChapters
                    )
                }
                repository.saveToDiscoveryPrintCache(mapToPrintCache(results))

                _state.update { it.copy(filteredPrintItems = results, isLoading = false) }
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false, error = "Error en la búsqueda") }
        }
    }

    // -------------------------------------------------------------------------
    // Guardar rápido en "Por Ver" (Audiovisual)
    // -------------------------------------------------------------------------

    fun quickSaveMedia(release: TmdbRelease, mediaType: MediaType) {
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
            val newId = repository.insertItem(entity)

            // Agregamos el nuevo ID al mapa en vivo para que la UI se actualice
            _state.update { it.copy(savedReleaseMap = it.savedReleaseMap + (release.id to newId)) }
        }
    }

    // -------------------------------------------------------------------------
    // Guardar rápido en "Por Ver" (Impresos)
    // -------------------------------------------------------------------------

    fun quickSavePrint(item: PrintMediaEntity) {
        viewModelScope.launch {
            // La entidad de impresos ya viene armada desde el Repositorio
            val newId = repository.insertPrintItem(item)
            
            // Actualizamos el mapa correspondiente a impresos
            _state.update { it.copy(savedPrintMap = it.savedPrintMap + (item.externalId to newId)) }
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

    // -------------------------------------------------------------------------
    // Helper para Mapear a Caché
    // -------------------------------------------------------------------------

    private fun mapToCache(releases: List<TmdbRelease>, mediaType: MediaType): List<DiscoveryCacheEntity> {
        return releases.map { release ->
            
            // EL FIX: Armamos la URL completa acá mismo, igual que en el quickSave
            val finalPoster = if (release.posterPath?.startsWith("http") == true) {
                release.posterPath
            } else {
                release.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
            }

            DiscoveryCacheEntity(
                tmdbId = release.id,
                title = release.displayTitle,
                overview = "", 
                posterPath = finalPoster, // <-- Usamos la URL arreglada
                mediaType = mediaType,
                releaseDate = formatApiDate(release.displayDate)
            )
        }
    }

    private fun mapToPrintCache(items: List<PrintMediaEntity>): List<DiscoveryPrintCacheEntity> {
        return items.map { item ->
            DiscoveryPrintCacheEntity(
                externalId    = item.externalId,
                title         = item.title,
                author        = item.author,
                posterPath    = item.posterPath,
                synopsis      = item.synopsis,
                printType     = item.printType,
                totalVolumes  = item.totalVolumes,
                totalChapters = item.totalChapters
            )
        }
    }

    // -------------------------------------------------------------------------
    // Observador de la Base de Datos Local (Tiempo Real)
    // -------------------------------------------------------------------------

    private fun observeLocalDatabase() {
        // Observador Audiovisual
        viewModelScope.launch {
            repository.getAllItems().collect { localItems ->
                val savedMap = localItems.filter { it.tmdbId != 0 }.associate { it.tmdbId to it.id }
                _state.update { it.copy(savedReleaseMap = savedMap) }
            }
        }

        // Observador de Impresos
        viewModelScope.launch {
            repository.getAllPrintItems().collect { localPrints ->
                val savedMap = localPrints.filter { it.externalId != 0 }
                    .associate { it.externalId to it.id }
                _state.update { it.copy(savedPrintMap = savedMap) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
