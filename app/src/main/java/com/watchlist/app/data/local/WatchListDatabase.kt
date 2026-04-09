package com.watchlist.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.watchlist.app.data.local.dao.MediaItemDao
import com.watchlist.app.data.local.dao.NewsDao
import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.NewsArticleEntity

@Database(
    entities = [MediaItemEntity::class, NewsArticleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class WatchListDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun newsDao(): NewsDao
}
