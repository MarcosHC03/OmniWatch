package com.watchlist.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JikanApiService {
    
    // Lista del usuario (Corregido sin el /all y con parámetro de página)
    @GET("users/{username}/animelist")
    suspend fun getUserAnimeList(
        @Path("username") username: String,
        @Query("page") page: Int = 1
    ): JikanAnimeListResponse

    // Buscador general de Anime
    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String
    ): JikanAnimeSearchResponse

    @GET("seasons/now")
    suspend fun getCurrentSeasonAnime()
    : JikanAnimeSearchResponse

    // Buscador general de Manga
    @GET("manga")
    suspend fun searchManga(
        @Query("q") query: String
    ): JikanMangaSearchResponse
}