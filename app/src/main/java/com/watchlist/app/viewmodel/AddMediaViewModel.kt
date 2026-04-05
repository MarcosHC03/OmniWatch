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
import javax.inject.Inject

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
    val searchResults: List<TmdbMedia> = emptyList(),
    val isSearching: Boolean = false,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val isEditing: Boolean = false,
    val editingItemId: Long = -1L
)

@HiltViewModel
class AddMediaViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMediaUiState())
    val uiState: StateFlow<AddMediaUiState> = _uiState

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
                isEditing = true,
                editingItemId = itemId
            )
        }
    }

    fun searchTmdb(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = repository.searchMulti(query)
            _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
        }
    }

    fun selectTmdbResult(media: TmdbMedia) {
        val year = runCatching {
            media.displayDate.take(4).toInt()
        }.getOrDefault(0)

        val type = when (media.mediaType) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.SERIES
            else -> _uiState.value.mediaType
        }
        _uiState.value = _uiState.value.copy(
            title = media.displayTitle,
            overview = media.overview ?: "",
            posterPath = media.fullPosterPath,
            year = year,
            tmdbId = media.id,
            mediaType = type,
            searchResults = emptyList()
        )
    }

    fun updateTitle(v: String) { _uiState.value = _uiState.value.copy(title = v) }
    fun updateMediaType(v: MediaType) { _uiState.value = _uiState.value.copy(mediaType = v) }
    fun updateWatchStatus(v: WatchStatus) { _uiState.value = _uiState.value.copy(watchStatus = v) }
    fun updateRating(v: Float) { _uiState.value = _uiState.value.copy(rating = v) }
    fun updateTotalEpisodes(v: Int) { _uiState.value = _uiState.value.copy(totalEpisodes = v) }
    fun updateWatchedEpisodes(v: Int) { _uiState.value = _uiState.value.copy(watchedEpisodes = v) }
    fun updateCurrentSeason(v: Int) { _uiState.value = _uiState.value.copy(currentSeason = v) }
    fun updatePlatform(v: String) { _uiState.value = _uiState.value.copy(platform = v) }
    fun clearSearch() { _uiState.value = _uiState.value.copy(searchResults = emptyList()) }

    fun saveItem() {
        val s = _uiState.value
        if (s.title.isBlank()) return
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true)
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
                tmdbId = s.tmdbId
            )
            if (s.isEditing) repository.updateItem(entity)
            else repository.insertItem(entity)
            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }
}
