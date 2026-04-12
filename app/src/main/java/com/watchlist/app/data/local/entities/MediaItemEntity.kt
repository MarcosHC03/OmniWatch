package com.watchlist.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType { MOVIE, SERIES, ANIME }

enum class WatchStatus { WATCHING, COMPLETED, PLANNED }

@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalTitle: String = "",
    val posterPath: String = "",
    val backdropPath: String = "",
    val overview: String = "",
    val mediaType: MediaType,
    val watchStatus: WatchStatus,
    val rating: Float = 0f,
    val totalEpisodes: Int = 0,
    val watchedEpisodes: Int = 0,
    val currentSeason: Int = 1,
    val genre: String = "",
    val year: Int = 0,
    val platform: String = "",
    val tmdbId: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val airDayOfWeek: Int = 0,       // 0 = sin asignar, 1 = Lunes … 7 = Domingo (ISO-8601)
    val releaseDate: String = "",    // Formato dd/MM/yyyy — vacío si se desconoce
    val isAiring: Boolean = false    // true = en emisión activa, false = finalizado/sin dato
)
