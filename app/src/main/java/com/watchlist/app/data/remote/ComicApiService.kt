package com.watchlist.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ComicApiService {
    
    // Ruta para buscar un cómic específico
    @GET("search/")
    suspend fun searchComics(
        @Query("query") query: String,
        @Query("resources") resources: String = "volume",
        @Query("limit") limit: Int = 20,
        @Query("format") format: String = "json"
        // ELIMINADO: @Query("api_key")
    ): ComicVineResponse<List<ComicVineVolume>>

    @GET("volumes/")
    suspend fun getLatestVolumes(
        @Query("sort") sort: String = "date_added:desc",
        @Query("limit") limit: Int = 20,
        @Query("format") format: String = "json",
        @Query("filter") filter: String? = null // Le puse nullable por si a veces no querés filtrar
        // ELIMINADO: @Query("api_key")
    ): ComicVineResponse<List<ComicVineVolume>>
}