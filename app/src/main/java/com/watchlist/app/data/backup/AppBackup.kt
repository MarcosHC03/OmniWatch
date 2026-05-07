package com.watchlist.app.data.backup

import com.watchlist.app.data.local.entities.MediaItemEntity
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.PrintVolumeEntity

/**
 * Molde raíz del archivo JSON de backup v2.
 * El campo [version] es la clave para la retrocompatibilidad:
 * si no está presente en el JSON (backup viejo), Gson lo deja en null
 * y el ViewModel sabe que debe tratarlo como formato v1 (solo lista).
 */
data class AppBackup(
    val version: Int = 2,
    val audiovisuales: List<MediaItemEntity> = emptyList(),
    val impresos: List<PrintBackupItem> = emptyList()
)

/**
 * Unidad atómica de backup para impresos.
 * Encapsula la franquicia (ej: "Berserk") junto con todos sus
 * tomos individuales, reflejando la relación 1-a-N de la base de datos.
 */
data class PrintBackupItem(
    val franquicia: PrintMediaEntity,
    val tomos: List<PrintVolumeEntity> = emptyList()
)