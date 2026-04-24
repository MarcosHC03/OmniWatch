package com.watchlist.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.watchlist.app.data.local.dao.MediaItemDao
import com.watchlist.app.data.local.dao.NewsDao
import com.watchlist.app.data.local.dao.DiscoveryCacheDao
import com.watchlist.app.data.local.dao.PrintMediaDao
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.NewsArticleEntity
import com.watchlist.app.data.local.entities.DiscoveryCacheEntity
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.PrintVolumeEntity

@Database(
    entities = [
        MediaItemEntity::class,
        NewsArticleEntity::class,
        DiscoveryCacheEntity::class,
        PrintMediaEntity::class,
        PrintVolumeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class WatchListDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun newsDao(): NewsDao
    abstract fun discoveryCacheDao(): DiscoveryCacheDao
    abstract fun printMediaDao(): PrintMediaDao
}
