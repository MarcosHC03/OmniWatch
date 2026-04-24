package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.local.entities.*
import com.watchlist.app.data.repository.MediaRepository
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.PrintVolumeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddPrintUiState(
    val title: String = "",
    val author: String = "",
    val synopsis: String = "",
    val posterPath: String = "",
    val printType: PrintType = PrintType.MANGA,
    val status: ReadStatus = ReadStatus.PLANNED,
    val rating: Float = 0f,
    val totalVolumes: String = "",
    val totalChapters: String = "",
    val currentVolume: String = "",
    //val currentChapter: String = "0",
    //val currentPage: String = "0",
    //val totalPagesInCurrentFile: String = "0",
    
    val isSearching: Boolean = false,
    val searchResults: List<PrintMediaEntity> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val isEditing: Boolean = false,
    val editingItemId: Long = -1L
)

@HiltViewModel
class AddPrintMediaViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPrintUiState())
    val uiState: StateFlow<AddPrintUiState> = _uiState

    fun loadItemForEditing(itemId: Long) {
        if (itemId <= 0L) return
        viewModelScope.launch {
            val item = repository.getPrintItemById(itemId) ?: return@launch
            _uiState.value = AddPrintUiState(
                title = item.title,
                author = item.author,
                synopsis = item.synopsis,
                posterPath = item.posterPath,
                printType = item.printType,
                status = item.status,
                rating = item.rating,
                totalVolumes = item.totalVolumes.toString(),
                totalChapters = item.totalChapters.toString(),
                //currentVolume = item.currentVolume.toString(),
                //currentChapter = item.currentChapter.toString(),
                //currentPage = item.currentPage.toString(),
                //totalPagesInCurrentFile = item.totalPagesInCurrentFile.toString(),
                isEditing = true,
                editingItemId = itemId
            )
        }
    }

    fun searchManga(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            val results = repository.searchJikanManga(query)
            _uiState.value = _uiState.value.copy(searchResults = results, isSearching = false)
        }
    }

    fun selectResult(item: PrintMediaEntity) {
        _uiState.value = _uiState.value.copy(
            title = item.title,
            author = item.author,
            synopsis = item.synopsis,
            posterPath = item.posterPath,
            totalVolumes = item.totalVolumes.toString(),
            totalChapters = item.totalChapters.toString(),
            searchResults = emptyList()
        )
    }

    // --- Updates de campos ---
    fun updateTitle(v: String) { _uiState.value = _uiState.value.copy(title = v) }
    fun updateAuthor(v: String) { _uiState.value = _uiState.value.copy(author = v) }
    fun updateType(v: PrintType) { _uiState.value = _uiState.value.copy(printType = v) }
    fun updateStatus(v: ReadStatus) { _uiState.value = _uiState.value.copy(status = v) }
    fun updateRating(v: Float) { _uiState.value = _uiState.value.copy(rating = v) }
    fun updateCurrentVolume(v: String) { _uiState.value = _uiState.value.copy(currentVolume = v) }
    //fun updateCurrentChapter(v: String) { _uiState.value = _uiState.value.copy(currentChapter = v) }
    //fun updateCurrentPage(v: String) { _uiState.value = _uiState.value.copy(currentPage = v) }
    fun updateTotalVolumes(v: String) { _uiState.value = _uiState.value.copy(totalVolumes = v) }
    fun updateTotalChapters(v: String) { _uiState.value = _uiState.value.copy(totalChapters = v) }
    //fun updateTotalPages(v: String) { _uiState.value = _uiState.value.copy(totalPagesInCurrentFile = v) }

    fun save() {
        // Evitamos que el usuario toque el botón 2 veces y guarde duplicados
        if (_uiState.value.isSaving) return
        _uiState.value = _uiState.value.copy(isSaving = true)

        viewModelScope.launch {
            val s = _uiState.value
            val entity = PrintMediaEntity(
                id = if (s.isEditing) s.editingItemId else 0L,
                title = s.title,
                author = s.author,
                synopsis = s.synopsis,
                posterPath = s.posterPath,
                printType = s.printType,
                status = s.status,
                rating = s.rating,
                totalVolumes = s.totalVolumes.toIntOrNull() ?: 0,
                totalChapters = s.totalChapters.toIntOrNull() ?: 0
            )

            // 1. Guardamos la franquicia y Room nos devuelve el ID nuevo
            val newFranchiseId = repository.insertPrintItem(entity)

            // 2. EL BUCLE MÁGICO (Solo lo hacemos si es una obra NUEVA)
            if (!s.isEditing) {
                val readVolumes = s.currentVolume.toIntOrNull() ?: 0
                
                if (readVolumes > 0) {
                    for (i in 1..readVolumes) {
                        val volume = PrintVolumeEntity(
                            printMediaId = newFranchiseId, // Lo conectamos a la franquicia recién creada
                            volumeNumber = i,
                            totalPages = 0, // Queda en 0 como acordamos, para editarlo a mano si hace falta
                            currentPage = 0
                        )
                        repository.insertPrintVolume(volume) // Insertamos el tomo
                    }
                }
            }

            // Terminamos y avisamos a la pantalla que vuelva atrás
            _uiState.value = _uiState.value.copy(savedSuccessfully = true, isSaving = false)
        }
    }
}