package com.watchlist.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovery_cache")
data class DiscoveryCacheEntity(
    @PrimaryKey 
    val tmdbId: Int, // Usamos el ID de TMDB/Jikan como clave primaria
    val title: String,
    val overview: String,
    val posterPath: String,
    val mediaType: MediaType, // Reutilizamos tu Enum que ya tenés creado
    val releaseDate: String,
    val timestamp: Long = System.currentTimeMillis() // Para saber cuándo guardamos esto
)