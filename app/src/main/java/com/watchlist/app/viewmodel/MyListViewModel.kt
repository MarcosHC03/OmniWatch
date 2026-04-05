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
    val isLoading: Boolean = false
)

@HiltViewModel
class MyListViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MediaType.SERIES)
    private val _filterStatus = MutableStateFlow<WatchStatus?>(null)
    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MyListUiState> = combine(
        _selectedTab,
        _filterStatus,
        _searchQuery
    ) { tab, status, query -> Triple(tab, status, query) }
        .flatMapLatest { (tab, status, query) ->
            val flow = if (query.isBlank()) {
                if (status != null)
                    repository.getItemsByType(tab).map { list -> list.filter { it.watchStatus == status } }
                else
                    repository.getItemsByType(tab)
            } else {
                repository.searchItems(query).map { list -> list.filter { it.mediaType == tab } }
            }
            flow.map { items ->
                MyListUiState(
                    items = items,
                    selectedTab = tab,
                    filterStatus = status,
                    searchQuery = query
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyListUiState())

    fun selectTab(type: MediaType) { _selectedTab.value = type }
    fun setFilter(status: WatchStatus?) { _filterStatus.value = status }
    fun setSearch(query: String) { _searchQuery.value = query }

    fun deleteItem(item: MediaItemEntity) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun updateStatus(item: MediaItemEntity, status: WatchStatus) {
        viewModelScope.launch { repository.updateItem(item.copy(watchStatus = status)) }
    }
}
