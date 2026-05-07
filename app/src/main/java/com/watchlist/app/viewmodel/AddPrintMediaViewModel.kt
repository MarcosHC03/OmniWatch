package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.PrintType
import com.watchlist.app.data.local.entities.PrintVolumeEntity
import com.watchlist.app.data.local.entities.ReadStatus
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Estado de UI
// ─────────────────────────────────────────────────────────────────────────────

data class AddPrintUiState(
    // Metadatos de la obra
    val title: String = "",
    val author: String = "",
    val synopsis: String = "",
    val posterPath: String = "",
    val printType: PrintType = PrintType.MANGA,

    // Progreso del lector
    val status: ReadStatus = ReadStatus.PLANNED,
    val rating: Float = 0f,
    val currentVolume: String = "",
    val totalVolumes: String = "",
    val currentChapter: String = "",
    val totalChapters: String = "",

    // Búsqueda
    val isSearching: Boolean = false,
    val searchResults: List<PrintMediaEntity> = emptyList(),

    // Validación
    val titleError: Boolean = false,

    // Guardado / edición
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val isEditing: Boolean = false,
    val editingItemId: Long = -1L
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class AddPrintMediaViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPrintUiState())
    val uiState: StateFlow<AddPrintUiState> = _uiState

    // ── Carga para edición ────────────────────────────────────────────────────

    fun loadItemForEditing(itemId: Long) {
        if (itemId <= 0L) return
        viewModelScope.launch {
            val item = repository.getPrintItemById(itemId) ?: return@launch
            _uiState.value = AddPrintUiState(
                title          = item.title,
                author         = item.author,
                synopsis       = item.synopsis,
                posterPath     = item.posterPath,
                printType      = item.printType,
                status         = item.status,
                rating         = item.rating,
                totalVolumes   = item.totalVolumes.takeIf { it > 0 }?.toString() ?: "",
                totalChapters  = item.totalChapters.takeIf { it > 0 }?.toString() ?: "",
                currentVolume  = item.currentVolume.takeIf { it > 0 }?.toString() ?: "",
                currentChapter = item.currentChapter.takeIf { it > 0 }?.toString() ?: "",
                isEditing      = true,
                editingItemId  = itemId
            )
        }
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────────

    fun searchManga(query: String, type: PrintType) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, searchResults = emptyList())
            val results = if (type == PrintType.COMIC || type == PrintType.GRAPHIC_NOVEL) {
                repository.searchComicVine(query)
            } else {
                repository.searchJikanManga(query)
            }
            _uiState.value = _uiState.value.copy(isSearching = false, searchResults = results)
        }
    }

    fun selectResult(item: PrintMediaEntity) {
        _uiState.value = _uiState.value.copy(
            title         = item.title,
            author        = item.author,
            synopsis      = item.synopsis,
            posterPath    = item.posterPath,
            totalVolumes  = item.totalVolumes.takeIf { it > 0 }?.toString() ?: "",
            totalChapters = item.totalChapters.takeIf { it > 0 }?.toString() ?: "",
            searchResults = emptyList()
        )
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchResults = emptyList())
    }

    // ── Actualizadores de campos ──────────────────────────────────────────────

    fun updateTitle(v: String) {
        _uiState.value = _uiState.value.copy(title = v, titleError = false)
    }

    fun updateAuthor(v: String) {
        _uiState.value = _uiState.value.copy(author = v)
    }

    fun updateType(v: PrintType) {
        _uiState.value = _uiState.value.copy(printType = v)
    }

    fun updateStatus(v: ReadStatus) {
        val s = _uiState.value
        // Si el usuario marca "Por leer", limpiamos el progreso para evitar inconsistencias
        val newVolume  = if (v == ReadStatus.PLANNED) "" else s.currentVolume
        val newChapter = if (v == ReadStatus.PLANNED) "" else s.currentChapter
        _uiState.value = s.copy(
            status        = v,
            currentVolume  = newVolume,
            currentChapter = newChapter
        )
    }

    fun updateRating(v: Float) {
        _uiState.value = _uiState.value.copy(rating = v)
    }

    fun updateCurrentVolume(v: String) {
        _uiState.value = _uiState.value.copy(currentVolume = v)
    }

    fun updateTotalVolumes(v: String) {
        _uiState.value = _uiState.value.copy(totalVolumes = v)
    }

    fun updateCurrentChapter(v: String) {
        _uiState.value = _uiState.value.copy(currentChapter = v)
    }

    fun updateTotalChapters(v: String) {
        _uiState.value = _uiState.value.copy(totalChapters = v)
    }

    // ── Guardar ───────────────────────────────────────────────────────────────

    fun save() {
        val s = _uiState.value

        // 1. Validación previa
        val isTitleValid = s.title.isNotBlank()
        if (!isTitleValid) {
            _uiState.value = s.copy(titleError = true)
            return
        }

        // 2. Evitar doble pulsación
        if (s.isSaving) return
        _uiState.value = s.copy(isSaving = true)

        viewModelScope.launch {
            val entity = PrintMediaEntity(
                id             = if (s.isEditing) s.editingItemId else 0L,
                title          = s.title,
                author         = s.author,
                synopsis       = s.synopsis,
                posterPath     = s.posterPath,
                printType      = s.printType,
                status         = s.status,
                rating         = s.rating,
                totalVolumes   = s.totalVolumes.toIntOrNull() ?: 0,
                totalChapters  = s.totalChapters.toIntOrNull() ?: 0,
                currentVolume  = s.currentVolume.toIntOrNull() ?: 0,
                currentChapter = s.currentChapter.toIntOrNull() ?: 0
            )

            // 3. Persistimos la franquicia y obtenemos su ID real
            val newId = repository.insertPrintItem(entity)

            // 4. Solo para obras NUEVAS: creamos los registros de tomo individuales
            //    que permiten trackear páginas por volumen más adelante.
            if (!s.isEditing) {
                val readVolumes = s.currentVolume.toIntOrNull() ?: 0
                if (readVolumes > 0) {
                    for (i in 1..readVolumes) {
                        repository.insertPrintVolume(
                            PrintVolumeEntity(
                                printMediaId = newId,
                                volumeNumber = i,
                                totalPages   = 0,
                                currentPage  = 0
                            )
                        )
                    }
                }
            }

            _uiState.value = _uiState.value.copy(isSaving = false, savedSuccessfully = true)
        }
    }
}
