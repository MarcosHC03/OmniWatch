package com.watchlist.app.data.repository

import com.watchlist.app.data.local.dao.MediaItemDao
import com.watchlist.app.data.local.dao.NewsDao
import com.watchlist.app.data.local.dao.DiscoveryCacheDao
import com.watchlist.app.data.local.dao.PrintMediaDao
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.WatchStatus
import com.watchlist.app.data.local.entities.DiscoveryCacheEntity
import com.watchlist.app.data.local.entities.PrintType
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.ReadStatus
import com.watchlist.app.data.local.entities.NewsArticleEntity
import com.watchlist.app.data.local.entities.PrintVolumeEntity
import com.watchlist.app.data.local.entities.PrintFranchiseWithVolumes
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
import com.watchlist.app.data.remote.MalDataApiService
import com.watchlist.app.data.remote.ComicApiService
import com.watchlist.app.data.backup.AppBackup
import com.watchlist.app.data.backup.PrintBackupItem
import com.watchlist.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val dao: MediaItemDao,
    private val printDao: PrintMediaDao,
    private val discoveryCacheDao: DiscoveryCacheDao,
    private val tmdbApi: TmdbApiService,
    private val newsApi: NewsApiService,
    private val jikanApiService: JikanApiService,
    private val malApi: MalApiService,
    private val malDataApi: MalDataApiService,
    private val newsDao: NewsDao,
    private val rssApi: RssApiService,
    private val comicApi: ComicApiService
) {

    // ---- Local DB (Audiovisual) ----
    fun getItemsByType(type: MediaType): Flow<List<MediaItemEntity>> =
        dao.getItemsByType(type)

    // ---- Local DB (Impresos) ----
    fun getAllItems(): Flow<List<MediaItemEntity>> =
        dao.getAllItems()

    /** Una sola lectura sin Flow — usado para exportar backup */
    suspend fun getAllItemsOnce(): List<MediaItemEntity> =
        dao.getAllItemsOnce()

    // ---- Backup v2: Construcción y Restauración ----

    /**
    * Construye el objeto [AppBackup] completo leyendo la base de datos una sola vez.
    * Para cada franquicia impresa, hace una segunda consulta para obtener sus tomos.
    * Llamada desde el ViewModel durante la exportación.
    */
    suspend fun buildBackup(): AppBackup {
        val audiovisuales = dao.getAllItemsOnce()

        val franchises = printDao.getAllFranchisesOnce()
        val impresos = franchises.map { franchise ->
            val tomos = printDao.getVolumesByFranchiseId(franchise.id)
            PrintBackupItem(franquicia = franchise, tomos = tomos)
        }

        return AppBackup(
            version = 2,
            audiovisuales = audiovisuales,
            impresos = impresos
        )
    }

    /**
    * Restaura un [AppBackup] v2 en la base de datos.
    *
    * Estrategia para audiovisuales: REPLACE directo por ID (comportamiento idéntico a v1).
    *
    * Estrategia para impresos (la regla crítica):
    * 1. Se inserta la franquicia con ID = 0 para forzar que Room genere un ID limpio
    *    y evitar colisiones con franquicias ya existentes.
    * 2. Se recupera el [newFranchiseId] que Room devolvió.
    * 3. Se insertan los tomos reasignándoles ese nuevo ID como clave foránea.
    */
    suspend fun restoreBackup(backup: AppBackup) {
        // — Audiovisuales —
        if (backup.audiovisuales.isNotEmpty()) {
            dao.insertAll(backup.audiovisuales)
        }

        // — Impresos —
        for (backupItem in backup.impresos) {
            // Forzamos ID = 0 para que Room autogenere uno nuevo y no haya conflictos
            val franchiseToInsert = backupItem.franquicia.copy(id = 0L)
            val newFranchiseId = printDao.insertFranchise(franchiseToInsert)

            if (backupItem.tomos.isNotEmpty()) {
                // Reasignamos la clave foránea al ID recién generado y limpiamos el ID del tomo
                val volumesWithNewId = backupItem.tomos.map { tomo ->
                    tomo.copy(
                        volumeId = 0L,                       // ID limpio: que Room asigne uno nuevo
                        printMediaId = newFranchiseId  // ← El vínculo correcto
                    )
                }
                printDao.insertAllVolumes(volumesWithNewId)
            }
        }
    }

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
                id = databaseId,
                tmdbId = item.node.id,
                title = item.node.title,
                posterPath = item.node.mainPicture?.large ?: item.node.mainPicture?.medium ?: "",
                mediaType = MediaType.ANIME,
                watchStatus = when (item.list_status.status) {
                    "watching"       -> WatchStatus.WATCHING
                    "completed"      -> WatchStatus.COMPLETED
                    else             -> WatchStatus.PLANNED
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

    // Descarga la lista oficial de MANGAS y la guarda en la base de datos de Impresos
    suspend fun syncOfficialMalMangaList(accessToken: String) {
        val response = malDataApi.getMyMangaList("Bearer $accessToken")
        val localMangas = printDao.getAllFranchisesOnce()

        val entities = response.data.map { item ->
            // MAGIA ANTI-DUPLICADOS (Mejorada): Buscamos por ID de MAL, o si lo creaste a mano, buscamos por título exacto.
            val existingItem = localMangas.find { 
                it.externalId == item.node.id || it.title.equals(item.node.title, ignoreCase = true) 
            }
            val databaseId = existingItem?.id ?: 0L

            // Mejoramos el autor: probamos varias combinaciones por si MAL se pone mañoso
            val authorName = item.node.authors?.firstOrNull()?.node?.let { author ->
                val first = author.firstName?.trim() ?: ""
                val last = author.lastName?.trim() ?: ""
                
                when {
                    first.isNotBlank() && last.isNotBlank() -> "$last, $first"
                    first.isNotBlank() -> first
                    last.isNotBlank() -> last
                    else -> "Autor Desconocido"
                }
            } ?: existingItem?.author ?: ""

            PrintMediaEntity(
                id = databaseId,
                externalId = item.node.id, 
                title = item.node.title,
                originalTitle = existingItem?.originalTitle ?: "",
                posterPath = item.node.mainPicture?.large ?: item.node.mainPicture?.medium ?: "",
                printType = PrintType.MANGA,
                status = when (item.list_status.status) {
                    "reading"   -> ReadStatus.READING
                    "completed" -> ReadStatus.COMPLETED
                    "on_hold"   -> ReadStatus.ON_HOLD
                    else        -> ReadStatus.PLANNED 
                },
                rating = (item.list_status.score / 2.0f),
                totalVolumes = item.node.numVolumes,
                totalChapters = item.node.numChapters,
                currentVolume = item.list_status.numVolumesRead,
                currentChapter = item.list_status.numChaptersRead,
                author = authorName,
                synopsis = item.node.synopsis ?: existingItem?.synopsis ?: ""
            )
        }

        // Guardamos todo y obtenemos la lista de los IDs que Room les asignó
        val insertedIds = printDao.insertAllFranchises(entities)

       // EL BUCLE MÁGICO PARA LA IMPORTACIÓN (Versión Blindada)
        entities.forEachIndexed { index, entity ->
            // Usamos el ID real: si era nuevo, usamos el insertado. Si ya existía, usamos el suyo.
            val realFranchiseId = if (entity.id == 0L) insertedIds[index] else entity.id

            if (entity.currentVolume > 0) {
                // Reutilizamos la función plana del backup para obtener la foto actual
                val tomosExistentes = printDao.getVolumesByFranchiseId(realFranchiseId)
                val cantidadExistente = tomosExistentes.size

                // Si en MAL dice que leíste más tomos de los que hay en el celular...
                if (entity.currentVolume > cantidadExistente) {
                    val tomosNuevos = mutableListOf<PrintVolumeEntity>()
                    // Generamos solo los tomos que faltan
                    for (i in (cantidadExistente + 1)..entity.currentVolume) {
                        tomosNuevos.add(
                            PrintVolumeEntity(
                                printMediaId = realFranchiseId,
                                volumeNumber = i,
                                totalPages = 0,
                                currentPage = 0
                            )
                        )
                    }
                    printDao.insertAllVolumes(tomosNuevos)
                }
            }
        }
    }

    // 1. El tubo directo a la base de datos (Esto carga al instante)
    val localNewsFlow = newsDao.getAllNews()

    suspend fun refreshNewsFromRss() {
        try {
            // 1. Armamos nuestra lista de diarios a la carta
            val feeds = listOf(
                Pair("https://somoskudasai.com/feed/", "SomosKudasai"),
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

    // ---- Memoria / Caché de Descubrimiento (Offline First) ----

    // Esta es la función que AddMediaViewModel llama para leer los datos rápido sin internet
    suspend fun getDiscoveryCacheItem(id: Int): DiscoveryCacheEntity? {
        return discoveryCacheDao.getById(id)
    }

    // Esta función la va a usar DiscoveryViewModel para guardar las tarjetas que baja de internet
    suspend fun saveToDiscoveryCache(items: List<DiscoveryCacheEntity>) {
        // Por si acaso, limpiamos la basura vieja primero para no llenar el celu de datos inútiles
        discoveryCacheDao.clearCache() 
        // Y guardamos los estrenos fresquitos
        discoveryCacheDao.insertAll(items)
    }

    // ---- Local DB (Impresos) ----
    fun getPrintItemsByType(type: PrintType): Flow<List<PrintMediaEntity>> =
        printDao.getFranchisesByType(type) // <-- Antes era getItemsByType

    suspend fun getPrintItemById(id: Long): PrintMediaEntity? = 
        printDao.getFranchiseById(id) // <-- Antes era getById

    suspend fun insertPrintItem(item: PrintMediaEntity): Long {
        // Si es un ítem nuevo (ID 0), chequeamos si ya existe uno con ese título
        if (item.id == 0L) {
            val existing = printDao.getFranchiseByTitle(item.title)
            if (existing != null) {
                // Si existe, lo pisamos manteniendo el ID original para que no se duplique
                return printDao.insertFranchise(item.copy(id = existing.id))
            }
        }
        return printDao.insertFranchise(item)
    }

    suspend fun updatePrintItem(item: PrintMediaEntity) = 
        printDao.updateFranchise(item.copy(updatedAt = System.currentTimeMillis())) // <-- Antes era updateItem

    suspend fun deletePrintItem(item: PrintMediaEntity) {
        printDao.deleteFranchise(item) // <-- Antes era deleteById(item.id)
    }

    // Búsqueda de Manga en Jikan (MyAnimeList)
    suspend fun searchJikanManga(query: String): List<PrintMediaEntity> {
        return try {
            val response = jikanApiService.searchManga(query)
            response.data.map { manga ->
                PrintMediaEntity(
                    title = manga.title ?: "Sin título",
                    originalTitle = manga.titleJapanese ?: "",
                    posterPath = manga.images?.jpg?.imageUrl ?: "",
                    synopsis = manga.synopsis ?: "",
                    author = manga.authors?.firstOrNull()?.name ?: "",
                    printType = PrintType.MANGA,
                    status = ReadStatus.PLANNED,
                    totalVolumes = manga.volumes ?: 0,
                    totalChapters = manga.chapters ?: 0,
                    externalId = manga.malId
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun getFranchiseWithVolumes(id: Long): Flow<PrintFranchiseWithVolumes?> =
        printDao.getFranchiseWithVolumes(id)

    // ---- Operaciones de Tomos Individuales ----
    suspend fun insertPrintVolume(volume: PrintVolumeEntity): Long =
        printDao.insertVolume(volume)

    suspend fun updatePrintVolume(volume: PrintVolumeEntity) =
        printDao.updateVolume(volume)

    suspend fun deletePrintVolume(volume: PrintVolumeEntity) =
        printDao.deleteVolume(volume)


    // RESOLUCIÓN DE PROBLEMA POST-IMPORTACIÓN: Si el manga importado de MAL no tenía autor o sinopsis, vamos a buscar esos datos faltantes usando la API de Jikan y actualizar la franquicia en la base de datos.
    suspend fun fetchMissingMangaDetails(franchiseId: Long, malId: Int) {
        try {
            // Buscamos el manga en Jikan usando su ID de MyAnimeList
            val response = jikanApiService.getMangaById(malId)
            
            // Armamos el nombre del autor (Jikan sí lo manda bien)
            val authorName = response.data.authors?.firstOrNull()?.name ?: "Autor Desconocido"
            
            // Agarramos la franquicia actual de nuestra base de datos
            val currentFranchise = printDao.getFranchiseById(franchiseId)
            
            // Si la encontramos, la actualizamos y la volvemos a guardar
            if (currentFranchise != null) {
                printDao.updateFranchise(currentFranchise.copy(
                    author = authorName,
                    // Si querés, podés aprovechar y pisar la sinopsis acá si MAL te la trajo cortada
                    // synopsis = response.data.synopsis ?: currentFranchise.synopsis 
                ))
            }
        } catch (e: Exception) {
            // Si falla (ej: sin internet), no pasa nada, intentará de nuevo la próxima vez que entres a los detalles
            e.printStackTrace()
        }
    }

    // Agregá tu servicio en el constructor si usás inyección de dependencias (Hilt)
    // private val comicApi: ComicApiService

    suspend fun searchComicVine(query: String): List<PrintMediaEntity> {
        return try {
            val apiKey = BuildConfig.CV_API_KEY
            
            val response = comicApi.searchComics(query = query, apiKey = apiKey)
            
            response.results.map { volume ->
                // ComicVine suele devolver la descripción en HTML feo (ej: <p>Spider-Man...</p>)
                // Limpiamos un poco el texto si no es nulo
                val cleanSynopsis = volume.description?.replace(Regex("<.*?>"), "")?.trim() ?: "Sin descripción disponible."
                val authorOrPublisher = volume.publisher?.name ?: "Editorial Desconocida"

                PrintMediaEntity(
                    title = volume.name ?: "Sin título",
                    originalTitle = "", // En cómics occidentales suele ser el mismo
                    posterPath = volume.image?.medium_url ?: volume.image?.original_url ?: "",
                    synopsis = cleanSynopsis,
                    author = authorOrPublisher, // Usamos la editorial (DC, Marvel, Image) como autor
                    printType = PrintType.COMIC, // Todo lo que viene de acá lo tratamos como cómic
                    status = ReadStatus.PLANNED,
                    totalVolumes = 1, // Para ComicVine, la serie entera es un volumen
                    totalChapters = volume.count_of_issues ?: 0, // La cantidad de números (#1, #2, #3...)
                    externalId = volume.id // Guardamos el ID por si en el futuro queremos hacer lazy loading
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
