package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para operaciones sobre la tabla de conversiones de audio.
 *
 * Propósito:
 * Proporciona métodos reactivos mediante Kotlin Flow para observar el historial en tiempo real
 * desde la interfaz de usuario, así como métodos para insertar y eliminar registros.
 */
@Dao
interface ConversionDao {

    /**
     * Obtiene todas las conversiones ordenadas de la más reciente a la más antigua.
     */
    @Query("SELECT * FROM audio_conversions ORDER BY timestamp DESC")
    fun getAllConversions(): Flow<List<ConversionEntity>>

    /**
     * Obtiene una conversión específica por su identificador.
     */
    @Query("SELECT * FROM audio_conversions WHERE id = :id LIMIT 1")
    suspend fun getConversionById(id: Long): ConversionEntity?

    /**
     * Inserta un nuevo registro de conversión en la base de datos local.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(conversion: ConversionEntity): Long

    /**
     * Elimina un registro del historial de conversiones.
     */
    @Delete
    suspend fun deleteConversion(conversion: ConversionEntity)

    /**
     * Borra todo el historial de conversiones.
     */
    @Query("DELETE FROM audio_conversions")
    suspend fun clearAll()
}
