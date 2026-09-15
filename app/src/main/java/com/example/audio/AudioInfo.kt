package com.example.audio

import android.net.Uri
import java.io.File
import java.util.Locale

/**
 * Modelo de datos que describe las propiedades técnicas de un archivo de audio seleccionado.
 *
 * Contiene información sobre formato original, tasa de bits (bitrate),
 * frecuencia de muestreo (sample rate), número de canales, duración y tamaño en disco.
 */
data class AudioInfo(
    val uri: Uri,
    val file: File? = null,
    val fileName: String,
    val format: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val sampleRateHz: Int,
    val channelCount: Int,
    val bitrateKbps: Int
) {
    /**
     * Formatea el tamaño en bytes a una representación legible (KB, MB).
     */
    fun getFormattedSize(): String {
        val kb = sizeBytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.2f MB", mb)
        } else {
            String.format(Locale.US, "%.1f KB", kb)
        }
    }

    /**
     * Formatea la duración en formato mm:ss o hh:mm:ss.
     */
    fun getFormattedDuration(): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Descripción textual de canales (Mono o Estéreo).
     */
    fun getChannelsDescription(): String {
        return when (channelCount) {
            1 -> "Mono (1 canal)"
            2 -> "Estéreo (2 canales)"
            else -> "$channelCount canales"
        }
    }
}
