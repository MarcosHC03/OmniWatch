package com.watchlist.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchlist.app.data.remote.TmdbMedia
import com.watchlist.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val news: List<com.watchlist.app.data.local.entities.NewsArticleEntity> = emptyList(),
    val trending: List<TmdbMedia> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadContent()

        // 1. Nos quedamos escuchando la cisterna (Base de datos) para siempre
        viewModelScope.launch {
            repository.localNewsFlow.collect { newsList ->
                // Corregido: _uiState con 'S' mayúscula
                _uiState.value = _uiState.value.copy(news = newsList)
            }
        }

        // 2. Mandamos a buscar agua nueva a la manguera (RSS) en segundo plano
        viewModelScope.launch {
            repository.refreshNewsFromRss()
        }
    }

    fun loadContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Solo traemos las películas en tendencia
                val trending = repository.getTrendingAll()
                
                // Usamos .copy() para actualizar solo el trending y que no se borren las noticias
                _uiState.value = _uiState.value.copy(trending = trending, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "No se pudo cargar el contenido. Revisá tu conexión."
                )
            }
        }
    }
}