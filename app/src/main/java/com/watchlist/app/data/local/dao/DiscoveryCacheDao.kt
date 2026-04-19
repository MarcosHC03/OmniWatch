package com.watchlist.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.watchlist.app.data.local.entities.DiscoveryCacheEntity

@Dao
interface DiscoveryCacheDao {
    
    // Inserta una lista de estrenos. Si ya existe uno con el mismo ID, lo reemplaza.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DiscoveryCacheEntity>)

    // Busca un item específico por su ID de TMDB/Jikan
    @Query("SELECT * FROM discovery_cache WHERE tmdbId = :id")
    suspend fun getById(id: Int): DiscoveryCacheEntity?

    // Limpia la tabla. Ideal para ejecutar antes de guardar los nuevos estrenos
    // así no acumulás basura de semanas anteriores.
    @Query("DELETE FROM discovery_cache")
    suspend fun clearCache()
}