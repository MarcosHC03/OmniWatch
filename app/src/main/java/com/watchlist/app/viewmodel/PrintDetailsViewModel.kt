package com.watchlist.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.local.entities.PrintFranchiseWithVolumes
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun addVolume(volumeNumberStr: String, totalPagesStr: String) {
        // Validación básica: si está vacío, ponemos 1 y 0 por defecto
        val vNum = volumeNumberStr.toIntOrNull() ?: 1
        val tPages = totalPagesStr.toIntOrNull() ?: 0
        
        viewModelScope.launch {
            val newVolume = com.watchlist.app.data.local.entities.PrintVolumeEntity(
                printMediaId = franchiseId, // ¡El nexo vital!
                volumeNumber = vNum,
                totalPages = tPages,
                currentPage = 0
            )
            repository.insertPrintVolume(newVolume)
        }
    }
}