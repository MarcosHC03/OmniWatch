package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyListUiState(
    val items: List<MediaItemEntity> = emptyList(),
    val selectedTab: MediaType = MediaType.SERIES,
    val filterStatus: WatchStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,

    // Estado de importación MAL
    val isImporting: Boolean = false,
    val importSuccessMessage: String? = null,
    val importErrorMessage: String? = null
)

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MediaType.SERIES)
    private val _filterStatus = MutableStateFlow<WatchStatus?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _importState = MutableStateFlow(
        Triple(false, null as String?, null as String?) // isImporting, successMsg, errorMsg
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MyListUiState> = combine(
        _selectedTab,
        _filterStatus,
        _searchQuery,
        _importState
    ) { tab, status, query, importTriple ->
        object {
            val tab = tab
            val status = status
            val query = query
            val isImporting = importTriple.first
            val successMsg = importTriple.second
            val errorMsg = importTriple.third
        }
    }.flatMapLatest { params ->
        val itemsFlow = if (params.query.isBlank()) {
            if (params.status != null)
                repository.getItemsByType(params.tab)
                    .map { list -> list.filter { it.watchStatus == params.status } }
            else
                repository.getItemsByType(params.tab)
        } else {
            repository.searchItems(params.query)
                .map { list -> list.filter { it.mediaType == params.tab } }
        }

        itemsFlow.map { items ->
            MyListUiState(
                items = items,
                selectedTab = params.tab,
                filterStatus = params.status,
                searchQuery = params.query,
                isImporting = params.isImporting,
                importSuccessMessage = params.successMsg,
                importErrorMessage = params.errorMsg
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyListUiState())

    fun selectTab(type: MediaType) { _selectedTab.value = type }
    fun setFilter(status: WatchStatus?) { _filterStatus.value = status }
    fun setSearch(query: String) { _searchQuery.value = query }

    fun deleteItem(item: MediaItemEntity) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun updateStatus(item: MediaItemEntity, status: WatchStatus) {
        viewModelScope.launch { repository.updateItem(item.copy(watchStatus = status)) }
    }

    // ---- Importación desde MyAnimeList ----

    fun importFromMyAnimeList(username: String) {
        if (username.isBlank()) return
        viewModelScope.launch {
            _importState.value = Triple(true, null, null)
            try {
                repository.importFromMyAnimeList(username)
                // Después de importar, cambiar el tab a ANIME para mostrar los resultados
                _selectedTab.value = MediaType.ANIME
                _importState.value = Triple(
                    false,
                    "Lista de $username importada correctamente",
                    null
                )
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("404") == true ->
                        "Usuario \"$username\" no encontrado en MyAnimeList"
                    e.message?.contains("network") == true ||
                    e.message?.contains("connect") == true ->
                        "Sin conexión. Revisá tu internet e intentá de nuevo"
                    else -> "No se pudo importar la lista. Intentá de nuevo"
                }
                _importState.value = Triple(false, null, msg)
            }
        }
    }

    fun clearImportMessages() {
        _importState.value = Triple(false, null, null)
    }
}
