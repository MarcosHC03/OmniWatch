package com.watchlist.app.data.remote

import com.google.gson.annotations.SerializedName

// 1. La caja principal que envuelve todo
data class JikanAnimeListResponse(
    @SerializedName("data") val data: List<JikanAnimeListItem>
)

// 2. Cada elemento de tu lista personal
data class JikanAnimeListItem(
    @SerializedName("anime") val anime: JikanAnimeNode,
    // Jikan usa números para el estado: 1 = Viendo, 2 = Completado, 3 = Pausado, 4 = Dropeado, 6 = Por Ver
    @SerializedName("watching_status") val status: Int, 
    @SerializedName("score") val score: Int, // MAL puntúa del 1 al 10
    @SerializedName("episodes_watched") val episodesWatched: Int
)

// 3. Los datos puros de la serie/película
data class JikanAnimeNode(
    @SerializedName("mal_id") val malId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("images") val images: JikanImages,
    @SerializedName("type") val type: String?, // Te dice si es "TV", "Movie", "OVA", etc.
    @SerializedName("episodes") val totalEpisodes: Int?,
    @SerializedName("year") val year: Int?
)

// 4. La estructura anidada para sacar el póster
data class JikanImages(
    @SerializedName("jpg") val jpg: JikanJpg
)

data class JikanJpg(
    @SerializedName("image_url") val imageUrl: String
)
