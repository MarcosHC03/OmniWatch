package com.watchlist.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey val url: String, // Usamos el link como ID para no guardar noticias repetidas
    val title: String,
    val imageUrl: String,
    val source: String, // "Crunchyroll", "MyAnimeList", "Espinof", etc.
    val publishedAt: Long, // Fecha para ordenarlas de más nuevas a más viejas
    val isPersonalized: Boolean = false // Para saber si es una noticia general o de tus animes
)