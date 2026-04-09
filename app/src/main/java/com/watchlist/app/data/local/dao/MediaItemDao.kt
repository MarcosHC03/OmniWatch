package com.watchlist.app.data.local.dao

import androidx.room.*
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.MediaType
import com.watchlist.app.data.local.entities.WatchStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {

    @Query("SELECT * FROM media_items ORDER BY updatedAt DESC")
    fun getAllItems(): Flow<List<MediaItemEntity>>

    // Versión suspend para leer una sola vez (backup, exportación)
    @Query("SELECT * FROM media_items ORDER BY updatedAt DESC")
    suspend fun getAllItemsOnce(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY updatedAt DESC")
    fun getItemsByType(type: MediaType): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mediaType = :type AND watchStatus = :status ORDER BY updatedAt DESC")
    fun getItemsByTypeAndStatus(type: MediaType, status: WatchStatus): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getItemById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchItems(query: String): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MediaItemEntity): Long

    // Inserción en lote para importar backup o MAL — REPLACE actualiza si ya existe el ID
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Update
    suspend fun updateItem(item: MediaItemEntity)

    @Delete
    suspend fun deleteItem(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM media_items WHERE mediaType = :type")
    fun countByType(type: MediaType): Flow<Int>

    @Query("SELECT COUNT(*) FROM media_items WHERE watchStatus = :status")
    fun countByStatus(status: WatchStatus): Flow<Int>
}
