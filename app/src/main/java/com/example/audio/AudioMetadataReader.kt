package com.example.audio

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Utilidad especializada en inspeccionar y extraer metadatos técnicos de archivos de audio.
 *
 * Utiliza las APIs nativas de Android (MediaMetadataRetriever y MediaExtractor)
 * para leer con precisión la duración, tasa de bits, canales y frecuencia de muestreo.
 */
object AudioMetadataReader {

    fun readAudioInfo(context: Context, uri: Uri): AudioInfo? {
        val resolver = context.contentResolver
        var fileName = "audio_${System.currentTimeMillis()}"
        var fileSize = 0L

        // Obtener nombre del archivo y tamaño desde el ContentResolver si es un Content URI
        if (uri.scheme == "content") {
            try {
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) {
                            cursor.getString(nameIndex)?.let { fileName = it }
                        }
                        if (sizeIndex != -1) {
                            fileSize = cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (_: Exception) {
            }
        } else if (uri.scheme == "file" || uri.path != null) {
            val file = File(uri.path ?: "")
            if (file.exists()) {
                fileName = file.name
                fileSize = file.length()
            }
        }

        if (fileSize == 0L) {
            try {
                resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    fileSize = pfd.statSize
                }
            } catch (_: Exception) {
            }
        }

        var durationMs = 0L
        var bitrateKbps = 0
        var sampleRateHz = 44100
        var channelCount = 2
        var formatExt = fileName.substringAfterLast('.', "mp3").uppercase()

        // 1. Intentar leer con MediaMetadataRetriever
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)

            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?.let {
                durationMs = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.let {
                bitrateKbps = it / 1000
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.let { mime ->
                if (mime.contains("audio/")) {
                    formatExt = mime.substringAfter("audio/").uppercase()
                }
            }
            retriever.release()
        } catch (_: Exception) {
        }

        // 2. Extraer parámetros precisos de pista con MediaExtractor
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRateHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        val br = format.getInteger(MediaFormat.KEY_BIT_RATE)
                        if (br > 0) bitrateKbps = br / 1000
                    }
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        val durUs = format.getLong(MediaFormat.KEY_DURATION)
                        if (durUs > 0 && durationMs == 0L) durationMs = durUs / 1000
                    }
                    break
                }
            }
            extractor.release()
        } catch (_: Exception) {
        }

        // Si el bitrate no vino explícito, calcular una estimación basada en tamaño / duración
        if (bitrateKbps <= 0 && durationMs > 0 && fileSize > 0) {
            val durationSec = durationMs / 1000.0
            bitrateKbps = ((fileSize * 8) / (durationSec * 1000)).toInt()
        }
        if (bitrateKbps <= 0) {
            bitrateKbps = 192 // Valor estándar por defecto
        }

        return AudioInfo(
            uri = uri,
            file = if (uri.scheme == "file") File(uri.path ?: "") else null,
            fileName = fileName,
            format = formatExt,
            sizeBytes = fileSize,
            durationMs = durationMs,
            sampleRateHz = sampleRateHz,
            channelCount = channelCount,
            bitrateKbps = bitrateKbps
        )
    }
}
