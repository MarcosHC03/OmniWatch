package com.watchlist.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.watchlist.app.data.local.entities.DiscoveryPrintCacheEntity

@Dao
interface DiscoveryPrintCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DiscoveryPrintCacheEntity>)

    @Query("SELECT * FROM discovery_print_cache WHERE externalId = :id")
    suspend fun getById(id: Int): DiscoveryPrintCacheEntity?

    @Query("DELETE FROM discovery_print_cache")
    suspend fun clearCache()
}