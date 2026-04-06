package com.watchlist.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface JikanApiService {
    
    // Este endpoint trae toda la lista de un usuario específico
    @GET("users/{username}/animelist/all")
    suspend fun getUserAnimeList(
        @Path("username") username: String
    ): JikanAnimeListResponse
}
