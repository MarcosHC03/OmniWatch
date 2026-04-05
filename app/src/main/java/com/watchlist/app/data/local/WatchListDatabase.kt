package com.watchlist.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.watchlist.app.data.local.dao.MediaItemDao
import com.watchlist.app.data.local.entities.MediaItemEntity

@Database(
    entities = [MediaItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WatchListDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
}
