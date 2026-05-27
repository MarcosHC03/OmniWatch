package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.PrintType
import com.watchlist.app.data.local.entities.PrintVolumeEntity
import com.watchlist.app.data.local.entities.ReadStatus
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    // Progreso del lector (Usamos String para los TextField)
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
    val editingItemId: Long = -1L,
    
    // Caché y Errores (Agregados para el flujo de Discovery)
    val externalId: Int = -1,
    val isLoading: Boolean = false,
    val error: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class AddPrintMediaViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ÚNICA declaración de estado
    private val _uiState = MutableStateFlow(AddPrintUiState())
    val uiState: StateFlow<AddPrintUiState> = _uiState.asStateFlow()

    init {
        // Atrapamos los parámetros que mandó la DiscoveryScreen
        val itemId = savedStateHandle.get<Long>("itemId") ?: -1L
        val printCacheId = savedStateHandle.get<Int>("printCacheId") ?: -1

        if (itemId != -1L) {
            // Modo Edición: El usuario tocó un manga que ya tenía en su lista
            loadItemFromLocalDb(itemId)
        } else if (printCacheId != -1) {
            // Modo Nuevo: El usuario tocó un manga fresco de Discovery
            loadItemFromPrintCache(printCacheId)
        }
    }

    // ── Carga de datos ────────────────────────────────────────────────────────

    private fun loadItemFromPrintCache(cacheId: Int) {
        viewModelScope.launch {
            val cachedItem = repository.getDiscoveryPrintCacheItem(cacheId)
            
            if (cachedItem != null) {
                _uiState.update { 
                    it.copy(
                        title = cachedItem.title,
                        author = cachedItem.author,
                        posterPath = cachedItem.posterPath,
                        synopsis = cachedItem.synopsis,
                        printType = cachedItem.printType,
                        totalVolumes = cachedItem.totalVolumes.takeIf { v -> v > 0 }?.toString() ?: "",
                        totalChapters = cachedItem.totalChapters.takeIf { c -> c > 0 }?.toString() ?: "",
                        externalId = cachedItem.externalId,
                        status = ReadStatus.PLANNED,
                        currentVolume = "",
                        currentChapter = ""
                    ) 
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "No se pudieron cargar los datos del título. Volvé a intentar."
                    )
                }
            }
        }
    }

    private fun loadItemFromLocalDb(itemId: Long) {
        if (itemId <= 0L) return
        viewModelScope.launch {
            val item = repository.getPrintItemById(itemId) ?: return@launch
            _uiState.update { 
                it.copy(
                    title          = item.title,
                    author         = item.author,
                    synopsis       = item.synopsis,
                    posterPath     = item.posterPath,
                    printType      = item.printType,
                    status         = item.status,
                    rating         = item.rating,
                    totalVolumes   = item.totalVolumes.takeIf { v -> v > 0 }?.toString() ?: "",
                    totalChapters  = item.totalChapters.takeIf { c -> c > 0 }?.toString() ?: "",
                    currentVolume  = item.currentVolume.takeIf { v -> v > 0 }?.toString() ?: "",
                    currentChapter = item.currentChapter.takeIf { c -> c > 0 }?.toString() ?: "",
                    externalId     = item.externalId,
                    isEditing      = true,
                    editingItemId  = itemId
                )
            }
        }
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────────

    fun searchManga(query: String, type: PrintType) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyList()) }
            val results = if (type == PrintType.COMIC || type == PrintType.NOVEL) {
                repository.searchComicVine(query)
            } else {
                repository.searchJikanManga(query)
            }
            _uiState.update { it.copy(isSearching = false, searchResults = results) }
        }
    }

    fun selectResult(item: PrintMediaEntity) {
        _uiState.update {
            it.copy(
                title         = item.title,
                author        = item.author,
                synopsis      = item.synopsis,
                posterPath    = item.posterPath,
                totalVolumes  = item.totalVolumes.takeIf { v -> v > 0 }?.toString() ?: "",
                totalChapters = item.totalChapters.takeIf { c -> c > 0 }?.toString() ?: "",
                searchResults = emptyList()
            )
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchResults = emptyList()) }
    }

    // ── Actualizadores de campos ──────────────────────────────────────────────

    fun updateTitle(v: String) {
        _uiState.update { it.copy(title = v, titleError = false) }
    }

    fun updateAuthor(v: String) {
        _uiState.update { it.copy(author = v) }
    }

    fun updateType(v: PrintType) {
        _uiState.update { it.copy(printType = v) }
    }

    fun updateStatus(v: ReadStatus) {
        _uiState.update { s ->
            val newVolume  = if (v == ReadStatus.PLANNED) "" else s.currentVolume
            val newChapter = if (v == ReadStatus.PLANNED) "" else s.currentChapter
            s.copy(
                status         = v,
                currentVolume  = newVolume,
                currentChapter = newChapter
            )
        }
    }

    fun updateRating(v: Float) {
        _uiState.update { it.copy(rating = v) }
    }

    fun updateCurrentVolume(v: String) {
        _uiState.update { it.copy(currentVolume = v) }
    }

    fun updateTotalVolumes(v: String) {
        _uiState.update { it.copy(totalVolumes = v) }
    }

    fun updateCurrentChapter(v: String) {
        _uiState.update { it.copy(currentChapter = v) }
    }

    fun updateTotalChapters(v: String) {
        _uiState.update { it.copy(totalChapters = v) }
    }

    // ── Guardar ───────────────────────────────────────────────────────────────

    fun save() {
        val s = _uiState.value

        if (s.title.isBlank()) {
            _uiState.update { it.copy(titleError = true) }
            return
        }

        if (s.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

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
                currentChapter = s.currentChapter.toIntOrNull() ?: 0,
                externalId     = s.externalId 
            )

            val newId = repository.insertPrintItem(entity)

            if (!s.isEditing) {
                val readVolumes = s.currentVolume.toIntOrNull() ?: 0
                if (readVolumes > 0) {
                    val newVolumes = (1..readVolumes).map { i ->
                        PrintVolumeEntity(
                            printMediaId = newId,
                            volumeNumber = i,
                            totalPages   = 0,
                            currentPage  = 0
                        )
                    }
                    // Insertamos todos los tomos de una sola vez si tu DAO lo soporta (si no, podés hacer un bucle como tenías)
                    newVolumes.forEach { repository.insertPrintVolume(it) }
                }
            }

            _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
        }
    }
}