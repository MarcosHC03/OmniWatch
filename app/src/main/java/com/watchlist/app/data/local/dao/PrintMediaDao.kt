package com.watchlist.app.data.local.dao

import androidx.room.*
import com.watchlist.app.data.local.entities.PrintFranchiseWithVolumes
import com.watchlist.app.data.local.entities.PrintMediaEntity
import com.watchlist.app.data.local.entities.PrintType
import com.watchlist.app.data.local.entities.PrintVolumeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrintMediaDao {
    
    // --- 1. OPERACIONES DE LA FRANQUICIA ---
    @Query("SELECT * FROM print_media_items WHERE printType = :type ORDER BY updatedAt DESC")
    fun getFranchisesByType(type: PrintType): Flow<List<PrintMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFranchise(item: PrintMediaEntity): Long

    @Update
    suspend fun updateFranchise(item: PrintMediaEntity)

    @Delete
    suspend fun deleteFranchise(item: PrintMediaEntity)

    @Query("SELECT * FROM print_media_items WHERE id = :id")
    suspend fun getFranchiseById(id: Long): PrintMediaEntity?

    // --- 2. OPERACIONES DE LOS TOMOS (NUEVO) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVolume(item: PrintVolumeEntity): Long

    @Update
    suspend fun updateVolume(item: PrintVolumeEntity)

    @Delete
    suspend fun deleteVolume(item: PrintVolumeEntity)

    // --- 3. LA CONSULTA RELACIONAL MAGISTRAL ---
    // @Transaction es obligatorio porque Room hace 2 consultas por detrás y las une
    @Transaction 
    @Query("SELECT * FROM print_media_items WHERE id = :franchiseId")
    fun getFranchiseWithVolumes(franchiseId: Long): Flow<PrintFranchiseWithVolumes?>
}