package com.watchlist.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {

    @GET("v2/everything")
    suspend fun getEntertainmentNews(
        // Buscamos palabras clave y bloqueamos explícitamente la basura de farándula
        @Query("q") query: String = "(cine OR películas OR series) AND NOT (TMZ OR rapper OR kardashian OR gossip OR escándalo)",
        @Query("language") language: String = "es", // Forzamos español para evitar medios yankees de chimentos
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("pageSize") pageSize: Int = 20
    ): NewsResponse
}
