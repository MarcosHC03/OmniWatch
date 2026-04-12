package com.watchlist.app.data.remote

import com.watchlist.app.data.local.entities.NewsArticleEntity
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

// 1. El teléfono con disfraz de navegador para engañar al patovica
interface RssApiService {
    @GET
    suspend fun getRssFeed(
        @Url url: String,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    ): String
}

// 2. El Traductor "Todo Terreno" (A prueba de errores de la web)
object RssParser {
    fun parse(xml: String, sourceName: String): List<NewsArticleEntity> {
        val articles = mutableListOf<NewsArticleEntity>()
        
        try {
            // Le pedimos al sabueso que aísle cada bloque de noticia <item>...</item>
            val itemRegex = "<item[>\\s](.*?)</item>".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val titleRegex = "<title>(.*?)</title>".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val linkRegex = "<link>(.*?)</link>".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            
            // Cazadores de fotos: buscan la etiqueta oficial o cualquier imagen escondida en el texto
            val mediaUrlRegex = "(?:media:content|media:thumbnail|enclosure)[^>]+url\\s*=\\s*['\"]([^'\"]+)['\"]".toRegex(RegexOption.IGNORE_CASE)
            val imgTagRegex = "<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"]".toRegex(RegexOption.IGNORE_CASE)

            val items = itemRegex.findAll(xml)
            for (match in items) {
                val itemXml = match.groupValues[1]
                
                // Extraemos y limpiamos los textos (a veces vienen con basura de WordPress)
                var title = titleRegex.find(itemXml)?.groupValues?.get(1)?.trim() ?: ""
                title = title.replace("<![CDATA[", "").replace("]]>", "").trim()
                title = android.text.Html.fromHtml(title, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
                
                var link = linkRegex.find(itemXml)?.groupValues?.get(1)?.trim() ?: ""
                link = link.replace("<![CDATA[", "").replace("]]>", "").trim()
                
                // Soltamos al sabueso de fotos
                var imageUrl = mediaUrlRegex.find(itemXml)?.groupValues?.get(1) ?: ""
                if (imageUrl.isBlank()) {
                    imageUrl = imgTagRegex.find(itemXml)?.groupValues?.get(1) ?: ""
                }
                
                // Si la noticia es válida, la agregamos a la lista
                if (title.isNotBlank() && link.isNotBlank()) {
                    articles.add(
                        NewsArticleEntity(
                            url = link,
                            title = title,
                            imageUrl = imageUrl,
                            source = sourceName,
                            publishedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return articles
    }
}