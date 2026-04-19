package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.data.remote.TmdbMedia
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

// Formatos que puede devolver la API → formato que mostramos al usuario
private val API_DATE_FORMATS = listOf(
    DateTimeFormatter.ofPattern("yyyy-MM-dd"),   // TMDB: "2024-03-15"
    DateTimeFormatter.ofPattern("yyyy-MM"),      // MAL a veces: "2024-03"
    DateTimeFormatter.ofPattern("yyyy")          // Solo año: "2024"
)
private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Convierte cualquier fecha cruda de API al formato dd/MM/yyyy.
 *  Devuelve la cadena original si no puede parsearla, y "" si la entrada está en blanco. */
private fun String?.toDisplayDate(): String {
    if (isNullOrBlank()) return ""
    val trimmed = trim()
    for (formatter in API_DATE_FORMATS) {
        try {
            // Los formatos cortos (año solo, año-mes) se parsean ajustando al 1er día
            val date = when (formatter.toString()) {
                DateTimeFormatter.ofPattern("yyyy").toString() ->
                    LocalDate.of(trimmed.toInt(), 1, 1)
                DateTimeFormatter.ofPattern("yyyy-MM").toString() ->
                    LocalDate.parse("$trimmed-01", DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                else ->
                    LocalDate.parse(trimmed, formatter)
            }
            return date.format(DISPLAY_FORMAT)
        } catch (_: DateTimeParseException) {
            // Intentar con el siguiente formato
        } catch (_: NumberFormatException) {
            // El "año solo" no era un número válido
        }
    }
    return trimmed // Si nada funcionó, devolvemos la cadena tal cual
}

// ---------------------------------------------------------------------------
// Estado
// ---------------------------------------------------------------------------

data class AddMediaUiState(
    val title: String = "",
    val overview: String = "",
    val posterPath: String = "",
    val mediaType: MediaType = MediaType.SERIES,
    val watchStatus: WatchStatus = WatchStatus.PLANNED,
    val rating: Float = 0f,
    val totalEpisodes: String = "",
    val watchedEpisodes: String = "",
    val currentSeason: String = "1",
    val platform: String = "",
    val year: Int = 0,
    val tmdbId: Int = 0,

    // Fecha editable separada
    val releaseDay: String = "",
    val releaseMonth: String = "",
    val releaseYear: String = "",
    val isAiring: Boolean = false,

    // Temporadas TMDB
    val availableSeasons: Int = 0,
    val episodesPerSeason: Map<Int, Int> = emptyMap(),

    // Validación episodios
    val watchedEpisodesError: Boolean = false,

    // Errores visuales
    val titleError: Boolean = false,
    val dateError: Boolean = false,

    // Búsqueda
    val searchResults: List<TmdbMedia> = emptyList(),
    val isSearching: Boolean = false,

    // Guardado
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val isEditing: Boolean = false,
    val editingItemId: Long = -1L
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class AddMediaViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMediaUiState())
    val uiState: StateFlow<AddMediaUiState> = _uiState

    // ---- Carga para edición ----

    fun loadItemForEditing(itemId: Long) {
        if (itemId <= 0L) return
        viewModelScope.launch {
            val item = repository.getItemById(itemId) ?: return@launch
            val dateParts = item.releaseDate.split("/")
            _uiState.value = AddMediaUiState(
                title = item.title,
                overview = item.overview,
                posterPath = item.posterPath,
                mediaType = item.mediaType,
                watchStatus = item.watchStatus,
                rating = item.rating,
                totalEpisodes   = item.totalEpisodes.takeIf { it > 0 }?.toString() ?: "",
                watchedEpisodes = item.watchedEpisodes.takeIf { it > 0 }?.toString() ?: "",
                currentSeason   = item.currentSeason.toString(),
                platform = item.platform,
                year = item.year,
                tmdbId = item.tmdbId,
                releaseDay = dateParts.getOrNull(0) ?: "",
                releaseMonth = dateParts.getOrNull(1) ?: "",
                releaseYear = dateParts.getOrNull(2) ?: "",
                isAiring = item.isAiring,
                isEditing = true,
                editingItemId = itemId
            )
        }
    }

    // ---- Búsqueda ----

    fun searchTmdb(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = if (_uiState.value.mediaType == MediaType.ANIME)
                repository.searchJikanAnime(query)
            else
                repository.searchMulti(query)
            _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
        }
    }

    fun selectTmdbResult(media: TmdbMedia) {
        val year = runCatching { media.displayDate.take(4).toInt() }.getOrDefault(0)
        val type = when (media.mediaType) {
            "movie"  -> MediaType.MOVIE
            "tv"     -> MediaType.SERIES
            "anime"  -> MediaType.ANIME
            else     -> _uiState.value.mediaType
        }

        val rawDate = when {
            !media.releaseDate.isNullOrBlank()   -> media.releaseDate
            !media.firstAirDate.isNullOrBlank()  -> media.firstAirDate
            else                                 -> null
        }?.trim()

        var rDay = ""
        var rMonth = ""
        var rYear = ""

        if (!rawDate.isNullOrBlank()) {
            if (rawDate.length == 4 && rawDate.toIntOrNull() != null) {
                // ¡El parche para Jikan! Si solo manda el año, lo dejamos así sin inventar días.
                rYear = rawDate
            } else {
                val formattedDate = rawDate.toDisplayDate()
                val dateParts = formattedDate.split("/")
                rDay = dateParts.getOrNull(0) ?: ""
                rMonth = dateParts.getOrNull(1) ?: ""
                rYear = dateParts.getOrNull(2) ?: ""
            }
        }

        // El parche del póster principal para Jikan
        val finalPoster = if (media.posterPath?.startsWith("http") == true) {
            media.posterPath
        } else {
            media.fullPosterPath
        }

        _uiState.value = _uiState.value.copy(
            title = media.displayTitle,
            overview = media.overview ?: "",
            posterPath = finalPoster ?: "",
            year = year,
            tmdbId = media.id,
            mediaType = type,
            releaseDay = rDay,
            releaseMonth = rMonth,
            releaseYear = rYear,
            availableSeasons = 0,
            episodesPerSeason = emptyMap(),
            currentSeason = "1",
            totalEpisodes = (media.totalEpisodes ?: 0).toString(),
            watchedEpisodes = "0",
            watchedEpisodesError = false,
            searchResults = emptyList()
        )

        // Para series: segunda llamada a TMDB para obtener temporadas
        if (type == MediaType.SERIES) {
            viewModelScope.launch {
                val details = repository.getTvDetails(media.id) ?: return@launch
                val validSeasons = details.seasons.filter { it.seasonNumber > 0 }
                val episodesMap = validSeasons.associate { it.seasonNumber to it.episodeCount }

                _uiState.value = _uiState.value.copy(
                    availableSeasons = validSeasons.size,
                    episodesPerSeason = episodesMap,
                    totalEpisodes = (episodesMap[1] ?: 0).toString(),
                    isAiring = false
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }

    /**
     * Llamada desde la pantalla cuando se abre desde el Home con un título pre-cargado.
     * Busca el query y selecciona automáticamente el primer resultado.
     */
    fun autoSearchAndSelect(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = repository.searchMulti(query)
            if (results.isNotEmpty()) {
                // Seleccionamos el primero directamente — misma lógica que el usuario eligiendo
                selectTmdbResult(results.first())
            }
            // Si no hay resultados, al menos dejamos el título escrito para que el usuario continúe
            _uiState.value = _uiState.value.copy(
                title = if (results.isEmpty()) query else _uiState.value.title,
                searchResults = emptyList(),
                isSearching = false
            )
        }
    }

    fun setSeasonData(numberOfSeasons: Int, episodesPerSeason: Map<Int, Int>) {
        _uiState.value = _uiState.value.copy(
            availableSeasons = numberOfSeasons,
            episodesPerSeason = episodesPerSeason
        )
    }

    // ---- Actualizadores de campos ----

    fun updateTitle(v: String) {
        _uiState.value = _uiState.value.copy(title = v, titleError = false)
    }

    fun updateMediaType(v: MediaType) {
        _uiState.value = _uiState.value.copy(
            mediaType = v,
            currentSeason = "1",
            availableSeasons = 0
        )
    }

    fun updateWatchStatus(v: WatchStatus) {
        val s = _uiState.value
        val newWatched = when (v) {
            WatchStatus.COMPLETED -> s.totalEpisodes
            WatchStatus.PLANNED   -> ""
            WatchStatus.WATCHING  -> s.watchedEpisodes
        }
        _uiState.value = s.copy(
            watchStatus = v,
            watchedEpisodes = newWatched,
            watchedEpisodesError = false
        )
    }

    fun updateRating(v: Float) {
        _uiState.value = _uiState.value.copy(rating = v)
    }

    fun updateTotalEpisodes(v: String) {
        val s = _uiState.value
        val totalInt = v.toIntOrNull() ?: 0
        val watchedInt = s.watchedEpisodes.toIntOrNull() ?: 0
        _uiState.value = s.copy(
            totalEpisodes = v,
            watchedEpisodesError = watchedInt > totalInt && totalInt > 0
        )
    }

    fun updateWatchedEpisodes(v: String) {
        val s = _uiState.value
        val watchedInt = v.toIntOrNull() ?: 0
        val totalInt = s.totalEpisodes.toIntOrNull() ?: 0
        val newStatus = if (watchedInt > 0 && s.watchStatus == WatchStatus.PLANNED)
            WatchStatus.WATCHING else s.watchStatus
        _uiState.value = s.copy(
            watchedEpisodes = v,
            watchStatus = newStatus,
            watchedEpisodesError = totalInt > 0 && watchedInt > totalInt
        )
    }

    fun updateCurrentSeason(v: String) {
        val s = _uiState.value
        val seasonInt = v.toIntOrNull() ?: 1

        // Autocompletado desde el mapa de temporadas local (sin llamada a red)
        val autoTotal = s.episodesPerSeason[seasonInt]
        _uiState.value = s.copy(
            currentSeason = v,
            totalEpisodes = autoTotal?.toString() ?: s.totalEpisodes
        )

        // Misión 2: si no tenemos datos locales del mapa Y es una Serie con tmdbId válido,
        // consultamos TMDB en segundo plano para obtener el total de eps de esa temporada
        if (autoTotal == null && s.mediaType == MediaType.SERIES && s.tmdbId > 0 && seasonInt > 0) {
            fetchEpisodesForSeason(s.tmdbId, seasonInt)
        }
    }

    fun updatePlatform(v: String) {
        _uiState.value = _uiState.value.copy(platform = v)
    }

    fun updateReleaseDay(v: String) {
        _uiState.value = _uiState.value.copy(releaseDay = v, dateError = false)
    }
    
    fun updateReleaseMonth(v: String) {
        _uiState.value = _uiState.value.copy(releaseMonth = v, dateError = false)
    }
    
    fun updateReleaseYear(v: String) {
        _uiState.value = _uiState.value.copy(releaseYear = v, dateError = false)
    }

    fun updateIsAiring(v: Boolean) {
        _uiState.value = _uiState.value.copy(isAiring = v)
    }

    // ---- Guardar ----

    fun saveItem() {
        val s = _uiState.value

        // 1. Validamos la lógica en variables separadas
        val isTitleValid = s.title.isNotBlank()

        val d = s.releaseDay.toIntOrNull() ?: 0
        val m = s.releaseMonth.toIntOrNull() ?: 0
        val y = s.releaseYear.toIntOrNull() ?: 0

        // Si escribieron algo, le pedimos a Kotlin que intente crear la fecha en el calendario real
        val isDateValid = if (s.releaseDay.isNotBlank() || s.releaseMonth.isNotBlank() || s.releaseYear.isNotBlank()) {
            try {
                // Intentamos crear la fecha exacta. Si le pasás (2024, 2, 31), esto tira un error al instante.
                java.time.LocalDate.of(y, m, d)
                
                // Si sobrevivió a la línea de arriba, la fecha existe. 
                // Solo le sumamos nuestra regla de lógica humana:
                y > 1800
            } catch (e: Exception) {
                false // Si tiró error (mes 15, 31 de febrero, etc), la fecha es inválida
            }
        } else true // Si está todo vacío, es válido

        // 2. Si algo está mal, prendemos las alarmas visuales y CORTAMOS acá
        if (!isTitleValid || !isDateValid || s.watchedEpisodesError) {
            _uiState.value = s.copy(
                titleError = !isTitleValid,
                dateError = !isDateValid
            )
            return
        }

        // Si llegamos acá, todo está perfecto. Armamos la fecha final:
        val finalDate = if (s.releaseDay.isNotBlank()) "${s.releaseDay.padStart(2, '0')}/${s.releaseMonth.padStart(2, '0')}/$y" else ""

        val totalEpsInt    = s.totalEpisodes.toIntOrNull() ?: 0
        val watchedEpsInt  = s.watchedEpisodes.toIntOrNull() ?: 0
        val currentSeasonInt = s.currentSeason.toIntOrNull()?.coerceAtLeast(1) ?: 1

        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true)

            // Calculamos el día de la semana usando nuestra finalDate recién armada
            val dayOfWeek = try {
                if (finalDate.isNotBlank())
                    LocalDate.parse(finalDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        .dayOfWeek.value
                else 0
            } catch (_: Exception) { 0 }

            val entity = MediaItemEntity(
                id             = if (s.isEditing) s.editingItemId else 0L,
                title          = s.title,
                overview       = s.overview,
                posterPath     = s.posterPath,
                mediaType      = s.mediaType,
                watchStatus    = s.watchStatus,
                rating         = s.rating,
                totalEpisodes  = totalEpsInt,
                watchedEpisodes = watchedEpsInt,
                currentSeason  = currentSeasonInt,
                platform       = s.platform,
                year           = s.year,
                tmdbId         = s.tmdbId,
                releaseDate    = finalDate,
                isAiring       = s.isAiring,
                airDayOfWeek   = dayOfWeek
            )
            if (s.isEditing) repository.updateItem(entity)
            else repository.insertItem(entity)
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }

    /**
     * Consulta TMDB en segundo plano para obtener el total de episodios de una
     * temporada específica y actualiza el estado silenciosamente.
     * Solo se llama cuando el usuario escribe un número de temporada que no está
     * en el mapa local (episodesPerSeason), evitando llamadas redundantes.
     */
    private fun fetchEpisodesForSeason(tmdbId: Int, season: Int) {
        viewModelScope.launch {
            val details = runCatching { repository.getTvDetails(tmdbId) }.getOrNull() ?: return@launch

            // Actualizamos el mapa local con todos los datos que nos devolvió TMDB
            // para que futuros cambios de temporada no necesiten otra llamada a red
            val updatedMap = details.seasons
                .filter { it.seasonNumber > 0 }
                .associate { it.seasonNumber to it.episodeCount }

            val episodesForThisSeason = updatedMap[season]

            // Solo actualizamos si el usuario no cambió de temporada mientras esperaba
            val currentState = _uiState.value
            if (currentState.currentSeason.toIntOrNull() == season) {
                _uiState.value = currentState.copy(
                    episodesPerSeason = updatedMap,
                    availableSeasons  = updatedMap.size,
                    // Solo pisamos totalEpisodes si TMDB nos dio un dato concreto
                    totalEpisodes = episodesForThisSeason?.toString() ?: currentState.totalEpisodes
                )
            }
        }
    }

    // ---- Carga desde el Caché de Descubrimiento (Offline First) ----

    fun loadFromCache(cacheId: Int) {
        if (cacheId <= 0) return
        
        viewModelScope.launch {
            // Suponiendo que agregaste un método en tu repository para buscar en el caché
            val cachedItem = repository.getDiscoveryCacheItem(cacheId) ?: return@launch
            
            // Reutilizamos tu lógica de fechas
            val formattedDate = cachedItem.releaseDate.toDisplayDate()
            val dateParts = formattedDate.split("/")
            val rDay = dateParts.getOrNull(0) ?: ""
            val rMonth = dateParts.getOrNull(1) ?: ""
            val rYear = dateParts.getOrNull(2) ?: ""

            // Pasamos los datos del caché al estado de la UI
            _uiState.value = _uiState.value.copy(
                title = cachedItem.title,
                overview = cachedItem.overview,
                posterPath = cachedItem.posterPath,
                mediaType = cachedItem.mediaType,
                tmdbId = cachedItem.tmdbId,
                year = rYear.toIntOrNull() ?: 0,
                releaseDay = rDay,
                releaseMonth = rMonth,
                releaseYear = rYear,
                availableSeasons = 0,
                episodesPerSeason = emptyMap(),
                currentSeason = "1",
                totalEpisodes = "", // Lo dejamos vacío para que el usuario o la API (si hay red) lo complete
                watchedEpisodes = "0",
                watchedEpisodesError = false,
                isSearching = false
            )

            // Si es una serie y tenemos internet, podríamos intentar buscar las temporadas,
            // pero ya le dejamos al usuario todo el texto principal cargado offline.
            if (cachedItem.mediaType == MediaType.SERIES) {
                // Tu función existente para buscar temporadas en segundo plano (si hay red)
                fetchEpisodesForSeason(cachedItem.tmdbId, 1)
            }
        }
    }
}
