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
import com.watchlist.app.data.remote.TmdbTvDetails
import com.watchlist.app.data.remote.JikanApiService
import com.watchlist.app.data.remote.JikanAnimeListItem
import com.watchlist.app.data.remote.MalApiService
import com.watchlist.app.data.remote.MalTokenResponse
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val dao: MediaItemDao,
    private val tmdbApi: TmdbApiService,
    private val newsApi: NewsApiService,
    private val jikanApiService: JikanApiService,
    private val malApi: MalApiService
) {
    // ---- Local DB ----

    fun getItemsByType(type: MediaType): Flow<List<MediaItemEntity>> =
        dao.getItemsByType(type)

    fun getAllItems(): Flow<List<MediaItemEntity>> =
        dao.getAllItems()

    /** Una sola lectura sin Flow — usado para exportar backup */
    suspend fun getAllItemsOnce(): List<MediaItemEntity> =
        dao.getAllItemsOnce()

    fun searchItems(query: String): Flow<List<MediaItemEntity>> =
        dao.searchItems(query)

    suspend fun insertItem(item: MediaItemEntity): Long =
        dao.insertItem(item)

    /** Inserta en lote con REPLACE — usado para importar backup/MAL */
    suspend fun insertAll(items: List<MediaItemEntity>) =
        dao.insertAll(items)

    suspend fun updateItem(item: MediaItemEntity) =
        dao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteItem(item: MediaItemEntity) =
        dao.deleteItem(item)

    suspend fun getItemById(id: Long): MediaItemEntity? =
        dao.getItemById(id)

    // ---- TMDB Remote ----

    suspend fun searchMulti(query: String): List<TmdbMedia> =
        runCatching { tmdbApi.searchMulti(query).results }.getOrDefault(emptyList())

    suspend fun searchMovies(query: String): List<TmdbMedia> =
        runCatching { tmdbApi.searchMovies(query).results }.getOrDefault(emptyList())

    suspend fun searchTv(query: String): List<TmdbMedia> =
        runCatching { tmdbApi.searchTv(query).results }.getOrDefault(emptyList())

    suspend fun getTrendingAll(): List<TmdbMedia> {
        val movies = runCatching { tmdbApi.getTrendingMovies().results }.getOrDefault(emptyList())
        val tv = runCatching { tmdbApi.getTrendingTv().results }.getOrDefault(emptyList())
        return (movies + tv).sortedBy { it.displayTitle }
    }

    suspend fun getUpcomingMovies(): List<TmdbRelease> =
        runCatching { tmdbApi.getUpcomingMovies().results }.getOrDefault(emptyList())

    suspend fun getUpcomingTv(): List<TmdbRelease> =
        runCatching { tmdbApi.getUpcomingTv().results }.getOrDefault(emptyList())

    suspend fun getTvDetails(tvId: Int): TmdbTvDetails? =
        runCatching { tmdbApi.getTvDetails(tvId) }.getOrNull()

    // ---- News Remote ----

    suspend fun getEntertainmentNews(): List<NewsArticle> =
        runCatching { newsApi.getEntertainmentNews().articles }.getOrDefault(emptyList())

    // ---- MyAnimeList (Jikan) ----

    suspend fun importFromMyAnimeList(username: String) {
        var hasNextPage = true
        var currentPage = 1
        val allItems = mutableListOf<JikanAnimeListItem>()

        while (hasNextPage) {
            val response = jikanApiService.getUserAnimeList(username, currentPage)
            allItems.addAll(response.data)
            hasNextPage = response.pagination?.hasNextPage == true
            if (hasNextPage) {
                currentPage++
                kotlinx.coroutines.delay(400)
            }
        }

        val entities = allItems.map { item ->
            MediaItemEntity(
                title = item.anime.title,
                posterPath = item.anime.images.jpg.imageUrl,
                mediaType = MediaType.ANIME,
                watchStatus = when (item.status) {
                    1 -> WatchStatus.WATCHING
                    2 -> WatchStatus.COMPLETED
                    else -> WatchStatus.PLANNED
                },
                rating = (item.score / 2.0f),
                totalEpisodes = item.anime.totalEpisodes ?: 0,
                watchedEpisodes = item.episodesWatched,
                year = item.anime.year ?: 0,
                tmdbId = item.anime.malId
            )
        }
        dao.insertAll(entities)
    }

    suspend fun searchJikanAnime(query: String): List<TmdbMedia> =
        runCatching {
            val response = jikanApiService.searchAnime(query)
            response.data.map { anime ->
                TmdbMedia(
                    id = anime.malId,
                    title = anime.title,
                    name = anime.title,
                    originalTitle = anime.title,
                    originalName = anime.title,
                    posterPath = anime.images.jpg.imageUrl,
                    backdropPath = null,
                    mediaType = "anime",
                    firstAirDate = anime.year?.toString() ?: "",
                    releaseDate = anime.year?.toString() ?: "",
                    overview = "",
                    voteAverage = 0.0,
                    genreIds = emptyList(),
                    totalEpisodes = anime.totalEpisodes
                )
            }
        }.getOrDefault(emptyList())

    suspend fun exchangeMalCodeForToken(clientId: String, code: String, verifier: String): MalTokenResponse? {
        return try {
            malApi.getAccessToken(clientId, code, verifier)
        } catch (e: Exception) {
            null
        }
    }
}
