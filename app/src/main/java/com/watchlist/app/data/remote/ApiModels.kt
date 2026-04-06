package com.watchlist.app.data.remote

import com.google.gson.annotations.SerializedName

// ---- TMDB Models ----

data class TmdbSearchResponse(
    val page: Int,
    val results: List<TmdbMedia>,
    @SerializedName("total_results") val totalResults: Int
)

data class TmdbMedia(
    val id: Int,
    val title: String?,
    val name: String?,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("original_name") val originalName: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("media_type") val mediaType: String?,
    @SerializedName("genre_ids") val genreIds: List<Int>?
) {
    val displayTitle get() = title ?: name ?: ""
    val displayOriginalTitle get() = originalTitle ?: originalName ?: ""
    val displayDate get() = releaseDate ?: firstAirDate ?: ""
    val fullPosterPath: String get() {
        if (posterPath == null) return ""
        if (posterPath.startsWith("http")) return posterPath
        return "https://image.tmdb.org/t/p/w500$posterPath"
    }
    val fullBackdropPath get() = backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" } ?: ""
}

data class TmdbReleasesResponse(
    val results: List<TmdbRelease>
)

data class TmdbRelease(
    val id: Int,
    val title: String?,
    val name: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("media_type") val mediaType: String?
) {
    val displayTitle get() = title ?: name ?: ""
    val displayDate get() = releaseDate ?: firstAirDate ?: ""
}

// ---- NewsAPI Models ----

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<NewsArticle>
)

data class NewsArticle(
    val source: NewsSource,
    val author: String?,
    val title: String,
    val description: String?,
    val url: String,
    @SerializedName("urlToImage") val imageUrl: String?,
    @SerializedName("publishedAt") val publishedAt: String,
    val content: String?
)

data class NewsSource(
    val id: String?,
    val name: String
)
