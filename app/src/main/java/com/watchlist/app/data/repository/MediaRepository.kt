package com.watchlist.app.data.repository

import com.watchlist.app.data.local.dao.MediaItemDao
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.data.remote.NewsApiService
import com.watchlist.app.data.remote.NewsArticle
import com.watchlist.app.data.remote.TmdbApiService
import com.watchlist.app.data.remote.TmdbMedia
import com.watchlist.app.data.remote.TmdbRelease
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val dao: MediaItemDao,
    private val tmdbApi: TmdbApiService,
    private val newsApi: NewsApiService
) {
    // ---- Local DB ----

    fun getItemsByType(type: MediaType): Flow<List<MediaItemEntity>> =
        dao.getItemsByType(type)

    fun getAllItems(): Flow<List<MediaItemEntity>> =
        dao.getAllItems()

    fun searchItems(query: String): Flow<List<MediaItemEntity>> =
        dao.searchItems(query)

    suspend fun insertItem(item: MediaItemEntity): Long = dao.insertItem(item)

    suspend fun updateItem(item: MediaItemEntity) =
        dao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteItem(item: MediaItemEntity) = dao.deleteItem(item)

    suspend fun getItemById(id: Long): MediaItemEntity? = dao.getItemById(id)

    // ---- TMDB Remote ----

    suspend fun searchMulti(query: String): List<TmdbMedia> =
        runCatching { tmdbApi.searchMulti(query).results }
            .getOrDefault(emptyList())

    suspend fun searchMovies(query: String): List<TmdbMedia> =
        runCatching { tmdbApi.searchMovies(query).results }
            .getOrDefault(emptyList())

    suspend fun searchTv(query: String): List<TmdbMedia> =
        runCatching { tmdbApi.searchTv(query).results }
            .getOrDefault(emptyList())

    suspend fun getTrendingAll(): List<TmdbMedia> {
        val movies = runCatching { tmdbApi.getTrendingMovies().results }.getOrDefault(emptyList())
        val tv = runCatching { tmdbApi.getTrendingTv().results }.getOrDefault(emptyList())
        return (movies + tv).sortedBy { it.displayTitle }
    }

    suspend fun getUpcomingMovies(): List<TmdbRelease> =
        runCatching { tmdbApi.getUpcomingMovies().results }.getOrDefault(emptyList())

    suspend fun getOnAirTv(): List<TmdbRelease> =
        runCatching { tmdbApi.getOnAirTv().results }.getOrDefault(emptyList())

    // ---- News Remote ----

    suspend fun getEntertainmentNews(): List<NewsArticle> =
        runCatching { newsApi.getEntertainmentNews().articles }
            .getOrDefault(emptyList())
}
