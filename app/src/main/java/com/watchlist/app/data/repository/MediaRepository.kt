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
                title = item.anime.title ?: "Sin título",
                posterPath = item.anime.images?.jpg?.imageUrl ?: "",
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

    suspend fun searchJikanAnime(query: String): List<TmdbMedia> {
        return try {
            val response = jikanApiService.searchAnime(query)
            response.data.map { anime ->
                val exactDate = anime.aired?.from?.take(10) ?: anime.year?.toString() ?: ""
                TmdbMedia(
                    id = anime.malId,
                    title = anime.title ?: "Sin título",
                    name = anime.title ?: "Sin título",
                    originalTitle = anime.title ?: "",
                    originalName = anime.title ?: "",
                    posterPath = anime.images?.jpg?.imageUrl ?: "", 
                    backdropPath = "",
                    mediaType = "anime",
                    firstAirDate = exactDate,
                    releaseDate = exactDate,
                    overview = "",
                    voteAverage = 0.0,
                    genreIds = emptyList(),
                    totalEpisodes = anime.totalEpisodes
                )
            }
        } catch (e: Exception) {
            // EL DETECTIVE ENCUBIERTO: Si la API explota, mandamos el error a la pantalla
            listOf(
                TmdbMedia(
                    id = 0,
                    title = "❌ ERROR: ${e.javaClass.simpleName}", // Acá veremos qué tipo de error es
                    name = e.message ?: "Error desconocido",       // Acá veremos el detalle
                    originalTitle = "",
                    originalName = "",
                    posterPath = "",
                    backdropPath = "",
                    mediaType = "anime",
                    firstAirDate = "",
                    releaseDate = "",
                    overview = "Ocurrió un error al buscar en Jikan.",
                    voteAverage = 0.0,
                    genreIds = emptyList(),
                    totalEpisodes = 0
                )
            )
        }
    }

    suspend fun exchangeMalCodeForToken(clientId: String, code: String, verifier: String): MalTokenResponse? {
        return try {
            malApi.getAccessToken(clientId, code, verifier)
        } catch (e: Exception) {
            null
        }
    }


    /**
     * Convierte fechas de MAL ("yyyy-MM-dd" o "yyyy-MM") al formato de la app "dd/MM/yyyy".
     * Devuelve "" si la entrada es nula, vacía o no parseable.
     */
    private fun parseMalDate(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val formats = listOf("yyyy-MM-dd", "yyyy-MM")
        for (pattern in formats) {
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
                // Para "yyyy-MM" completamos con día 01 para poder parsear
                val normalized = if (pattern == "yyyy-MM") "$raw-01" else raw
                val date = java.time.LocalDate.parse(
                    normalized,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                )
                return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            } catch (_: Exception) { }
        }
        return "" // Formato desconocido — no perdemos el dato pero tampoco rompemos
    }

    // Descarga la lista oficial y la guarda en la base de datos
    suspend fun syncOfficialMalList(accessToken: String) {
        // 1. Llamamos a la API con el Pase VIP
        val response = malDataApi.getMyAnimeList("Bearer $accessToken")

        // ¡NUEVO! Obtenemos todos los animes que ya están guardados en tu base de datos
        val localAnimes = dao.getAllItemsOnce().filter { it.mediaType == MediaType.ANIME }

        // 2. Traducimos los objetos de MAL a los nuestros
        val entities = response.data.map { item ->
            val formattedDate = parseMalDate(item.node.startDate)

            // airDayOfWeek: lo derivamos de la fecha de inicio si existe
            val airDayOfWeek = if (formattedDate.isNotBlank()) {
                try {
                    java.time.LocalDate.parse(
                        formattedDate,
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    ).dayOfWeek.value  // 1 = Lunes … 7 = Domingo
                } catch (_: Exception) { 0 }
            } else 0

            // ¡LA MAGIA ANTI-DUPLICADOS!
            // Buscamos si este anime de MAL (tmdbId) ya existe en tu celular.
            val existingItem = localAnimes.find { it.tmdbId == item.node.id }
            
            // Si existe, usamos su ID real de Room para pisarlo (actualizarlo). 
            // Si no existe, usamos 0 para que Room lo cree como nuevo.
            val databaseId = existingItem?.id ?: 0L

            MediaItemEntity(
                id = databaseId, // <-- Acá está la solución al problema
                tmdbId = item.node.id,
                title = item.node.title,
                posterPath = item.node.mainPicture?.large ?: item.node.mainPicture?.medium ?: "",
                mediaType = MediaType.ANIME,
                watchStatus = when (item.list_status.status) {
                    "watching"       -> WatchStatus.WATCHING
                    "completed"      -> WatchStatus.COMPLETED
                    else             -> WatchStatus.PLANNED  // plan_to_watch, on_hold, dropped
                },
                rating = (item.list_status.score / 2.0f),
                totalEpisodes = item.node.numEpisodes,
                watchedEpisodes = item.list_status.numEpisodesWatched,
                platform = "MyAnimeList",
                releaseDate = formattedDate,
                isAiring = item.node.status == "currently_airing",
                airDayOfWeek = airDayOfWeek
            )
        }

        // 3. Guardamos todo junto en la base de datos (REPLACE actualiza los IDs existentes, inserta los 0)
        dao.insertAll(entities)
    }

    // 1. El tubo directo a la base de datos (Esto carga al instante)
    val localNewsFlow = newsDao.getAllNews()

    suspend fun refreshNewsFromRss() {
        try {
            // 1. Armamos nuestra lista de diarios a la carta
            val feeds = listOf(
                Pair("https://somoskudasai.com/feed/", "SomosKudasai"), // Anime
                Pair("https://www.sensacine.com/rss/noticias.xml", "SensaCine"),
                Pair("https://www.cinepremiere.com.mx/feed/", "Cine PREMIERE")
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
                // Mezclamos la lista y le asignamos una hora secuencial falsa para que la base de datos respete nuestra mezcla al ordenarlas por fecha
                val timeNow = System.currentTimeMillis()
                val mixedArticles = allArticles.shuffled().mapIndexed { index, article ->
                    article.copy(publishedAt = timeNow - index) 
                }
                
                newsDao.clearAllNews()
                newsDao.insertNews(mixedArticles)
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---- Misión v1.5: Populares para Descubrimiento ----

    suspend fun getPopularMoviesAsReleases(): List<TmdbRelease> =
        runCatching { tmdbApi.getTrendingMovies().results }
            .getOrDefault(emptyList())
            .filter { !it.posterPath.isNullOrBlank() }
            .map { 
                TmdbRelease(
                    id           = it.id,
                    title        = it.title,
                    name         = it.name,
                    releaseDate  = it.releaseDate,
                    firstAirDate = it.firstAirDate,
                    posterPath   = it.posterPath,
                    mediaType    = "movie"
                ) 
            }

    suspend fun getPopularTvAsReleases(): List<TmdbRelease> =
        runCatching { tmdbApi.getTrendingTv().results }
            .getOrDefault(emptyList())
            .filter { !it.posterPath.isNullOrBlank() }
            .map { 
                TmdbRelease(
                    id           = it.id,
                    title        = it.title,
                    name         = it.name,
                    releaseDate  = it.releaseDate,
                    firstAirDate = it.firstAirDate,
                    posterPath   = it.posterPath,
                    mediaType    = "tv"
                ) 
            }

    suspend fun getPopularAnimeAsReleases(): List<TmdbRelease> =
        runCatching {
            val response = jikanApiService.getCurrentSeasonAnime()
            
            // El filtro ahora usa ?. para no crashear
            response.data.filter { !it.images?.jpg?.imageUrl.isNullOrBlank() }
                .map { anime ->
                    val exactDate = anime.aired?.from?.take(10) ?: anime.year?.toString() ?: ""
                    
                    TmdbRelease(
                        id           = anime.malId,
                        title        = anime.title ?: "Sin título",
                        name         = anime.title ?: "Sin título",
                        releaseDate  = exactDate,
                        firstAirDate = exactDate,
                        posterPath   = anime.images?.jpg?.imageUrl,
                        mediaType    = "anime",
                        totalEpisodes = anime.totalEpisodes
                    )
                }
                .distinctBy { it.id }
        }.getOrDefault(emptyList())
}
