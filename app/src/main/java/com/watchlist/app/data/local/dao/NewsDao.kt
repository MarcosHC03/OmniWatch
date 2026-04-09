package com.watchlist.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.watchlist.app.data.local.entities.NewsArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {

    // Trae todas las noticias ordenadas por fecha (las más nuevas primero)
    // Usamos Flow para que la pantalla se actualice sola si entran noticias nuevas de fondo
    @Query("SELECT * FROM news_articles ORDER BY publishedAt DESC")
    fun getAllNews(): Flow<List<NewsArticleEntity>>

    // Inserta una lista de noticias. Si alguna ya existe (mismo URL), la ignora para no duplicar.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNews(articles: List<NewsArticleEntity>)

    // Borra las noticias viejas para no llenarte la memoria del celular
    // (Por ejemplo, le pasaremos la fecha de hace 3 días)
    @Query("DELETE FROM news_articles WHERE publishedAt < :timestampLimit")
    suspend fun deleteOldNews(timestampLimit: Long)
    
    // Vacía toda la tabla por si alguna vez queremos forzar una recarga manual
    @Query("DELETE FROM news_articles")
    suspend fun clearAllNews()
}