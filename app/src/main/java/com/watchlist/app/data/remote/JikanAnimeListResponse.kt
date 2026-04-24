package com.watchlist.app.data.remote

import com.google.gson.annotations.SerializedName

// Respuesta de la lista del usuario (Ahora con paginación)
data class JikanAnimeListResponse(
    @SerializedName("data") val data: List<JikanAnimeListItem>,
    @SerializedName("pagination") val pagination: JikanPagination?
)

data class JikanPagination(
    @SerializedName("has_next_page") val hasNextPage: Boolean
)

// Cada elemento de tu lista personal
data class JikanAnimeListItem(
    @SerializedName("anime") val anime: JikanAnimeNode,
    @SerializedName("watching_status") val status: Int, 
    @SerializedName("score") val score: Int,
    @SerializedName("episodes_watched") val episodesWatched: Int
)

// Respuesta del buscador de Jikan
data class JikanAnimeSearchResponse(
    @SerializedName("data") val data: List<JikanAnimeNode>
)

// Los datos puros de la serie/película
data class JikanAnimeNode(
    @SerializedName("mal_id") val malId: Int,
    @SerializedName("title") val title: String?,
    @SerializedName("images") val images: JikanImages?,
    @SerializedName("type") val type: String?,
    @SerializedName("episodes") val totalEpisodes: Int?,
    @SerializedName("year") val year: Int?,
    @SerializedName("aired") val aired: JikanAired?
)

data class JikanImages(
    @SerializedName("jpg") val jpg: JikanJpg?
)

data class JikanJpg(
    @SerializedName("image_url") val imageUrl: String?
)

data class JikanAired(
    @SerializedName("from") val from: String?
)

// La respuesta general que envuelve la lista
data class JikanMangaSearchResponse(
    val data: List<JikanManga>
)

// El molde de cada manga individual
data class JikanManga(
    @SerializedName("mal_id") val malId: Int,
    val title: String?,
    @SerializedName("title_japanese") val titleJapanese: String?,
    
    val images: JikanImages?,
    
    val synopsis: String?,
    val authors: List<JikanAuthor>?, // Los autores vienen en una lista
    val volumes: Int?,
    val chapters: Int?
)

// El molde para el Autor
data class JikanAuthor(
    @SerializedName("mal_id") val malId: Int,
    val name: String?
)