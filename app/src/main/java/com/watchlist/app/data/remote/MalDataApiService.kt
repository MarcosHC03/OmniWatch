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

// ---- MOLDES PARA LEER EL JSON DE MAL (MANGAS) ----
data class MalMangaListResponse(
    val data: List<MalMangaListItem>
)

data class MalMangaListItem(
    val node: MalMangaNode,
    val list_status: MalMangaListStatus
)

data class MalMangaNode(
    val id: Int,
    val title: String,
    @SerializedName("main_picture") val mainPicture: MalPicture?,
    @SerializedName("num_volumes") val numVolumes: Int,
    @SerializedName("num_chapters") val numChapters: Int,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("status") val status: String?,
    val synopsis: String?, // <-- ¡NUEVO!
    val authors: List<MalMangaAuthorWrapper>? // <-- ¡NUEVO!
)

// Clases para desarmar el formato raro de autores de MAL
data class MalMangaAuthorWrapper(
    val node: MalMangaAuthorNode
)

data class MalMangaAuthorNode(
    val id: Int,
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?
)

data class MalMangaListStatus(
    val status: String, 
    val score: Int,
    @SerializedName("num_volumes_read") val numVolumesRead: Int,
    @SerializedName("num_chapters_read") val numChaptersRead: Int
)

// ---- EL TELÉFONO DE DATOS ----
interface MalDataApiService {
    
    @GET("v2/users/@me/animelist")
    suspend fun getMyAnimeList(
        @Header("Authorization") token: String,
        @Query("fields") fields: String = "list_status,num_episodes,media_type,start_date,status",
        @Query("limit") limit: Int = 1000
    ): MalAnimeListResponse

    @GET("v2/users/@me/mangalist")
    suspend fun getMyMangaList(
        @Header("Authorization") token: String,
        @Query("fields") fields: String = "list_status,num_volumes,num_chapters,media_type,start_date,status,synopsis,authors",
        @Query("limit") limit: Int = 1000
    ): MalMangaListResponse
}