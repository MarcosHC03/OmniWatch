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
    @SerializedName("title") val title: String,
    @SerializedName("images") val images: JikanImages,
    @SerializedName("type") val type: String?,
    @SerializedName("episodes") val totalEpisodes: Int?,
    @SerializedName("year") val year: Int?
)

data class JikanImages(
    @SerializedName("jpg") val jpg: JikanJpg
)

data class JikanJpg(
    @SerializedName("image_url") val imageUrl: String
)