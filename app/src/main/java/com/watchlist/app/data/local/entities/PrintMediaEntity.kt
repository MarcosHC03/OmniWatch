package com.watchlist.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PrintType { COMIC, MANGA, MANHWA, GRAPHIC_NOVEL }
enum class ReadStatus { READING, COMPLETED, PLANNED, ON_HOLD }

@Entity(tableName = "print_media_items")
data class PrintMediaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val originalTitle: String = "",
    val posterPath: String = "",
    val author: String = "",
    val synopsis: String = "",
    val printType: PrintType,
    val status: ReadStatus,
    val rating: Float = 0f,
    
    // Datos generales (lo que nos va a decir MAL)
    val totalVolumes: Int = 0,
    val totalChapters: Int = 0,
    
    val externalId: Int = 0, // ID de MAL
    val addedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)