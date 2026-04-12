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
    val totalEpisodes: Int = 0,
    val watchedEpisodes: Int = 0,
    val currentSeason: Int = 1,
    val platform: String = "",
    val year: Int = 0,
    val tmdbId: Int = 0,

    // Nuevos campos — Misión 2
    val releaseDate: String = "",    // dd/MM/yyyy — editable por el usuario
    val isAiring: Boolean = false,

    // Temporadas TMDB
    val availableSeasons: Int = 0,
    val episodesPerSeason: Map<Int, Int> = emptyMap(),

    // Validación episodios
    val watchedEpisodesError: Boolean = false,

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
            _uiState.value = AddMediaUiState(
                title = item.title,
                overview = item.overview,
                posterPath = item.posterPath,
                mediaType = item.mediaType,
                watchStatus = item.watchStatus,
                rating = item.rating,
                totalEpisodes = item.totalEpisodes,
                watchedEpisodes = item.watchedEpisodes,
                currentSeason = item.currentSeason,
                platform = item.platform,
                year = item.year,
                tmdbId = item.tmdbId,
                releaseDate = item.releaseDate,
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

        // Extraer y formatear fecha de la API al formato de display ─────────
        // TMDB usa `release_date` (películas) o `first_air_date` (series/anime)
        // MAL/Jikan usa `start_date`; ambos llegan en displayDate o releaseDate del modelo
        val rawDate = when {
            !media.releaseDate.isNullOrBlank()   -> media.releaseDate
            !media.firstAirDate.isNullOrBlank()  -> media.firstAirDate
            else                                 -> null
        }
        val formattedDate = rawDate.toDisplayDate()

        _uiState.value = _uiState.value.copy(
            title = media.displayTitle,
            overview = media.overview ?: "",
            posterPath = media.fullPosterPath,
            year = year,
            tmdbId = media.id,
            mediaType = type,
            releaseDate = formattedDate,
            availableSeasons = 0,
            episodesPerSeason = emptyMap(),
            currentSeason = 1,
            totalEpisodes = media.totalEpisodes ?: 0,
            watchedEpisodes = 0,
            watchedEpisodesError = false,
            searchResults = emptyList()
        )

        // Para series: segunda llamada a TMDB para obtener temporadas
        if (type == MediaType.SERIES) {
            viewModelScope.launch {
                val details = repository.getTvDetails(media.id) ?: return@launch
                val validSeasons = details.seasons.filter { it.seasonNumber > 0 }
                val episodesMap = validSeasons.associate { it.seasonNumber to it.episodeCount }

                // Usamos false por defecto para que el usuario lo decida a mano
                val airing = false
                val airDate = formattedDate

                _uiState.value = _uiState.value.copy(
                    availableSeasons = validSeasons.size,
                    episodesPerSeason = episodesMap,
                    totalEpisodes = episodesMap[1] ?: 0,
                    isAiring = airing,
                    releaseDate = airDate
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }

    fun setSeasonData(numberOfSeasons: Int, episodesPerSeason: Map<Int, Int>) {
        _uiState.value = _uiState.value.copy(
            availableSeasons = numberOfSeasons,
            episodesPerSeason = episodesPerSeason
        )
    }

    // ---- Actualizadores de campos ----

    fun updateTitle(v: String) {
        _uiState.value = _uiState.value.copy(title = v)
    }

    fun updateMediaType(v: MediaType) {
        _uiState.value = _uiState.value.copy(
            mediaType = v,
            currentSeason = 1,
            availableSeasons = 0
        )
    }

    fun updateWatchStatus(v: WatchStatus) {
        val s = _uiState.value
        val newWatched = when (v) {
            WatchStatus.COMPLETED -> s.totalEpisodes
            WatchStatus.PLANNED   -> 0
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

    fun updateTotalEpisodes(v: Int) {
        val s = _uiState.value
        _uiState.value = s.copy(
            totalEpisodes = v,
            watchedEpisodesError = s.watchedEpisodes > v && v > 0
        )
    }

    fun updateWatchedEpisodes(v: Int) {
        val s = _uiState.value
        val newStatus = if (v > 0 && s.watchStatus == WatchStatus.PLANNED)
            WatchStatus.WATCHING else s.watchStatus
        _uiState.value = s.copy(
            watchedEpisodes = v,
            watchStatus = newStatus,
            watchedEpisodesError = s.totalEpisodes > 0 && v > s.totalEpisodes
        )
    }

    fun updateCurrentSeason(v: Int) {
        val s = _uiState.value
        _uiState.value = s.copy(
            currentSeason = v,
            totalEpisodes = s.episodesPerSeason[v] ?: s.totalEpisodes
        )
    }

    fun updatePlatform(v: String) {
        _uiState.value = _uiState.value.copy(platform = v)
    }

    fun updateReleaseDate(v: String) {
        _uiState.value = _uiState.value.copy(releaseDate = v)
    }

    fun updateIsAiring(v: Boolean) {
        _uiState.value = _uiState.value.copy(isAiring = v)
    }

    // ---- Guardar ----

    fun saveItem() {
        val s = _uiState.value
        if (s.title.isBlank() || s.watchedEpisodesError) return
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true)

            // Calculamos automáticamente el día de la semana (1=Lunes, 7=Domingo)
            val dayOfWeek = try {
                if (s.releaseDate.isNotBlank()) {
                    LocalDate.parse(s.releaseDate, DateTimeFormatter.ofPattern("dd/MM/yyyy")).dayOfWeek.value
                } else 0
            } catch (e: Exception) {
                0
            }

            val entity = MediaItemEntity(
                id = if (s.isEditing) s.editingItemId else 0L,
                title = s.title,
                overview = s.overview,
                posterPath = s.posterPath,
                mediaType = s.mediaType,
                watchStatus = s.watchStatus,
                rating = s.rating,
                totalEpisodes = s.totalEpisodes,
                watchedEpisodes = s.watchedEpisodes,
                currentSeason = s.currentSeason,
                platform = s.platform,
                year = s.year,
                tmdbId = s.tmdbId,
                releaseDate = s.releaseDate,
                isAiring = s.isAiring,
                airDayOfWeek = dayOfWeek
            )
            if (s.isEditing) repository.updateItem(entity)
            else repository.insertItem(entity)
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }
}
