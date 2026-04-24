package com.watchlist.app.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

// Esta clase NO es una @Entity (no es una tabla), es solo un "paquete" de envío
data class PrintFranchiseWithVolumes(
    @Embedded 
    val franchise: PrintMediaEntity, // Los datos generales (Título, Póster)
    
    @Relation(
        parentColumn = "id", // El ID de la franquicia
        entityColumn = "printMediaId" // El ID que pusimos en el tomo para conectarlos
    )
    val volumes: List<PrintVolumeEntity> // La lista de tomos asociados
)