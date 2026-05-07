package com.watchlist.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.local.entities.PrintFranchiseWithVolumes
import com.watchlist.app.data.local.entities.PrintVolumeEntity
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrintDetailsViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle // Esto nos deja leer el ID que viene de la navegación
) : ViewModel() {

    private val franchiseId: Long = savedStateHandle.get<Long>("franchiseId") ?: -1L

    // Acá escuchamos a la base de datos en tiempo real
    val franchiseData: StateFlow<PrintFranchiseWithVolumes?> = repository.getFranchiseWithVolumes(franchiseId)
        .map { data -> 
            // Si hay datos, devolvemos una copia con la lista de tomos ordenada
            data?.copy(volumes = data.volumes.sortedBy { it.volumeNumber }) 
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addVolume(volumeNumberStr: String, currentPageStr: String, totalPagesStr: String) {
        val vNum = volumeNumberStr.toIntOrNull() ?: 1
        val cPage = currentPageStr.toIntOrNull() ?: 0
        val tPages = totalPagesStr.toIntOrNull() ?: 0
        
        viewModelScope.launch {
            val newVolume = PrintVolumeEntity(
                printMediaId = franchiseId,
                volumeNumber = vNum,
                totalPages = tPages,
                currentPage = cPage
            )
            repository.insertPrintVolume(newVolume)
        }
    }

    fun updateVolumePages(volume: PrintVolumeEntity, currentPageStr: String, totalPagesStr: String) {
        val currentP = currentPageStr.toIntOrNull() ?: 0
        val totalP = totalPagesStr.toIntOrNull() ?: 0

        viewModelScope.launch {
            // Hacemos una copia del tomo actual, pero le cambiamos las páginas
            val updatedVolume = volume.copy(
                currentPage = currentP,
                totalPages = totalP
            )
            repository.updatePrintVolume(updatedVolume)
        }
    }

    fun deleteVolume(volume: PrintVolumeEntity) {
        viewModelScope.launch {
            repository.deletePrintVolume(volume)
        }
    }

    // revisamos si el manga tiene datos faltantes y los traemos de Jikan si es necesario
    init {
        viewModelScope.launch {
            // Nos "colgamos" a escuchar la variable que creamos arriba
            franchiseData.collect { data ->
                if (data != null) {
                    val entity = data.franchise
                    
                    // LÓGICA DE CARGA PEREZOSA (LAZY LOADING)
                    if ((entity.author.isBlank() || entity.author == "Autor Desconocido") && entity.externalId > 0) {
                        viewModelScope.launch {
                            repository.fetchMissingMangaDetails(entity.id, entity.externalId)
                        }
                    }
                }
            }
        }
    }
}