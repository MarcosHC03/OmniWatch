package com.watchlist.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// ---- MOLDES PARA LEER EL JSON DE MAL ----
data class MalAnimeListResponse(
    val data: List<MalAnimeListItem>
)

data class MalAnimeListItem(
    val node: MalAnimeNode,
    val list_status: MalListStatus
)

data class MalAnimeNode(
    val id: Int,
    val title: String,
    @SerializedName("main_picture") val mainPicture: MalPicture?,
    @SerializedName("num_episodes") val numEpisodes: Int,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("status") val status: String?
)

data class MalPicture(
    val medium: String,
    val large: String?
)

data class MalListStatus(
    val status: String, 
    val score: Int,
    @SerializedName("num_episodes_watched") val numEpisodesWatched: Int
)

// ---- EL TELÉFONO DE DATOS ----
interface MalDataApiService {
    
    // Endpoint oficial para traer la lista de un usuario
    @GET("v2/users/@me/animelist")
    suspend fun getMyAnimeList(
        @Header("Authorization") token: String,
        @Query("fields") fields: String = "list_status,num_episodes,media_type,start_date,status",
        @Query("limit") limit: Int = 1000 // Pedimos hasta 1000 de un saque para no renegar con la paginación ahora
    ): MalAnimeListResponse
}