package com.watchlist.app.data.repository

import com.watchlist.app.data.local.dao.MediaItemDao
import com.watchlist.app.data.local.dao.NewsDao
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
import com.watchlist.app.data.remote.RssApiService
import com.watchlist.app.data.remote.RssParser
import com.watchlist.app.data.local.entities.NewsArticleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val dao: MediaItemDao,
    private val tmdbApi: TmdbApiService,
    private val newsApi: NewsApiService,
    private val jikanApiService: JikanApiService,
    private val malApi: MalApiService,
    private val malDataApi: com.watchlist.app.data.remote.MalDataApiService,
    private val newsDao: NewsDao,
    private val rssApi: RssApiService
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

    // Descarga la lista oficial y la guarda en la base de datos
    suspend fun syncOfficialMalList(accessToken: String) {
        // 1. Llamamos a la API con el Pase VIP
        val response = malDataApi.getMyAnimeList("Bearer $accessToken")

        // 2. Traducimos los objetos de MAL a los nuestros
        val entities = response.data.map { item ->
            MediaItemEntity(
                tmdbId = item.node.id, // Guardamos el ID de MAL acá por ahora
                title = item.node.title,
                posterPath = item.node.mainPicture?.large ?: item.node.mainPicture?.medium ?: "",
                mediaType = MediaType.ANIME,
                watchStatus = when (item.list_status.status) {
                    "watching" -> WatchStatus.WATCHING
                    "completed" -> WatchStatus.COMPLETED
                    else -> WatchStatus.PLANNED // plan_to_watch, on_hold, dropped
                },
                rating = (item.list_status.score / 2.0f), // MAL puntúa del 1-10, nosotros de 1-5 estrellas
                totalEpisodes = item.node.numEpisodes,
                watchedEpisodes = item.list_status.numEpisodesWatched,
                platform = "MyAnimeList"
            )
        }

        // 3. Guardamos todo junto en la base de datos (REPLACE actualiza los que ya existen)
        dao.insertAll(entities)
    }

    // 1. El tubo directo a la base de datos (Esto carga al instante)
    val localNewsFlow = newsDao.getAllNews()

    suspend fun refreshNewsFromRss() {
        try {
            // 1. Armamos nuestra lista de diarios a la carta
            val feeds = listOf(
                Pair("https://somoskudasai.com/feed/", "SomosKudasai"), // Anime
                Pair("https://www.espinof.com/feed", "Espinof") // Películas y Series
            )
            
            val allArticles = mutableListOf<NewsArticleEntity>()

            // 2. Pasamos a recolectar por cada diario
            for ((url, sourceName) in feeds) {
                try {
                    val xmlData = rssApi.getRssFeed(url)
                    val articles = RssParser.parse(xmlData, sourceName)
                    allArticles.addAll(articles)
                } catch (e: Exception) {
                    // Si un diario falla (ej. Espinof está caído), imprimimos el error 
                    // pero dejamos que el ciclo 'for' siga con el próximo.
                    e.printStackTrace()
                }
            }

            // 3. Si recolectamos aunque sea 1 noticia, actualizamos la caché
            if (allArticles.isNotEmpty()) {
                // Mezclamos un poco la lista para que no queden todas las de un diario arriba
                allArticles.shuffle() 
                
                newsDao.clearAllNews()
                newsDao.insertNews(allArticles)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
