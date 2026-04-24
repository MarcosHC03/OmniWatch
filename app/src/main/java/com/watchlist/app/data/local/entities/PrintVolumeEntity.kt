package com.watchlist.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "print_volumes",
    foreignKeys = [
        ForeignKey(
            entity = PrintMediaEntity::class,
            parentColumns = ["id"], // El ID de la franquicia
            childColumns = ["printMediaId"], // Nuestro nexo
            onDelete = ForeignKey.CASCADE // Magia: Si borrás Kagurabachi, se borran todos sus tomos solos
        )
    ],
    indices = [Index("printMediaId")] // Esto hace que las búsquedas sean ultra rápidas
)
data class PrintVolumeEntity(
    @PrimaryKey(autoGenerate = true)
    val volumeId: Long = 0,
    val printMediaId: Long, // <-- ACÁ se conecta con la franquicia
    
    val volumeNumber: Int = 1, // Ej: 1, 2, 3...
    val title: String = "",    // Opcional: "Kagurabachi Vol. 1 - El inicio"
    
    // --- Lógica pura de Lectura ---
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    
    // --- Para la v2.5 ---
    val localFilePath: String = "",
    val isDownloaded: Boolean = false,
    
    val addedAt: Long = System.currentTimeMillis()
)