package com.watchlist.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovery_print_cache")
data class DiscoveryPrintCacheEntity(
    @PrimaryKey val externalId: Int, // ID de MAL o ComicVine
    val title: String,
    val author: String,
    val posterPath: String,
    val synopsis: String,
    val printType: PrintType,
    val totalVolumes: Int,
    val totalChapters: Int
)