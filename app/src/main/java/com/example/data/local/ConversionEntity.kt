package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia local (Room) para registrar el historial de conversiones de audio.
 *
 * Propósito:
 * Permite al usuario consultar conversiones pasadas, comparar la reducción de tamaño conseguida,
 * reproducir el archivo resultante directamente y compartirlo rápidamente con otras aplicaciones.
 */
@Entity(tableName = "audio_conversions")
data class ConversionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Datos del archivo original antes de la conversión
    val originalFileName: String,
    val originalFormat: String,
    val originalSizeBytes: Long,
    val originalBitrateKbps: Int,

    // Datos del archivo resultante convertido
    val convertedFileName: String,
    val convertedFilePath: String,
    val convertedFormat: String,
    val convertedSizeBytes: Long,
    val targetBitrateKbps: Int,
    val targetSampleRateHz: Int,
    val targetChannels: Int, // 1 = Mono, 2 = Estéreo

    // Duración total en milisegundos y fecha de conversión
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)
