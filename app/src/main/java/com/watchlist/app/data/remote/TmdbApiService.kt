package com.watchlist.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

interface TmdbApiService {

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponse

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponse

    @GET("trending/movie/week")
    suspend fun getTrendingMovies(
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponse

    @GET("trending/tv/week")
    suspend fun getTrendingTv(
        @Query("language") language: String = "es-MX"
    ): TmdbSearchResponse

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("language") language: String = "es-MX",
        @Query("region") region: String = "AR"
    ): TmdbReleasesResponse

    @GET("discover/tv")
    suspend fun getUpcomingTv(
        // Hoy es 5 de abril de 2026, así que filtramos desde el primero del mes.
        @Query("first_air_date.gte") firstAirDateGte: String = "2026-04-01", 
        @Query("language") language: String = "es-MX",
        @Query("sort_by") sortBy: String = "first_air_date.asc" // Las ordena de la más cercana al futuro
    ): TmdbReleasesResponse

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int
    ): TmdbTvDetails
}
