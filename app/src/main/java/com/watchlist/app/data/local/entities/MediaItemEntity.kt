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
    val rating: Float = 0f,           // 0 a 5 estrellas
    val totalEpisodes: Int = 0,       // Para series y anime
    val watchedEpisodes: Int = 0,     // Para series y anime
    val currentSeason: Int = 1,
    val genre: String = "",
    val year: Int = 0,
    val platform: String = "",        // Netflix, HBO, etc.
    val tmdbId: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
