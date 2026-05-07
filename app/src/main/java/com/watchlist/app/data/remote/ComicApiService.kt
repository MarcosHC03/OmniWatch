package com.watchlist.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ComicApiService {
    
    // Ruta para buscar un cómic específico
    @GET("search/")
    suspend fun searchComics(
        @Query("query") query: String,
        @Query("resources") resources: String = "volume", // Solo buscamos colecciones/volúmenes
        @Query("limit") limit: Int = 20,
        // ComicVine exige estos dos parámetros siempre:
        @Query("format") format: String = "json",
        @Query("api_key") apiKey: String 
    ): ComicVineResponse<List<ComicVineVolume>>

    // Ruta para obtener los últimos números/issues lanzados (Para Discovery v1.5)
    @GET("issues/")
    suspend fun getRecentIssues(
        @Query("sort") sort: String = "cover_date:desc", // Los más recientes primero
        @Query("limit") limit: Int = 20,
        @Query("format") format: String = "json",
        @Query("api_key") apiKey: String
    ): ComicVineResponse<List<ComicVineVolume>> // (Reutilizamos el modelo para simplificar por ahora)
}