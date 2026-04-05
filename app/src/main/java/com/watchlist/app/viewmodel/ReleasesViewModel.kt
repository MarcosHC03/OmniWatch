package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.remote.TmdbRelease
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ReleaseGroup(
    val monthLabel: String,   // "Abril 2025"
    val releases: List<TmdbRelease>
)

data class ReleasesUiState(
    val groups: List<ReleaseGroup> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReleasesViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReleasesUiState())
    val uiState: StateFlow<ReleasesUiState> = _uiState

    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale("es", "AR"))
    private val dateParser = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init { loadReleases() }

    fun loadReleases() {
        viewModelScope.launch {
            _uiState.value = ReleasesUiState(isLoading = true)
            try {
                val movies = repository.getUpcomingMovies()
                val tv = repository.getUpcomingTv()
                val all = (movies + tv)
                    .filter { it.displayDate.isNotBlank() }
                    .sortedBy { it.displayDate }

                val groups = all
                    .groupBy { release ->
                        runCatching {
                            val date = LocalDate.parse(release.displayDate, dateParser)
                            date.format(monthFormatter).replaceFirstChar { it.uppercase() }
                        }.getOrDefault("Sin fecha")
                    }
                    .map { (month, releases) -> ReleaseGroup(month, releases) }

                _uiState.value = ReleasesUiState(groups = groups, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = ReleasesUiState(
                    isLoading = false,
                    error = "No se pudieron cargar los estrenos."
                )
            }
        }
    }
}
