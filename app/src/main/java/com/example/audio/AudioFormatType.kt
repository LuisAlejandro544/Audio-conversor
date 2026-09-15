package com.example.audio

/**
 * Formatos de audio soportados por el conversor.
 *
 * Cada formato define su extensión de archivo, tipo MIME oficial,
 * nombre descriptivo para el usuario y si permite ajuste libre de bitrate con pérdida.
 */
enum class AudioFormatType(
    val extension: String,
    val displayName: String,
    val mimeType: String,
    val isLossless: Boolean,
    val defaultBitrateKbps: Int,
    val description: String
) {
    M4A(
        extension = "m4a",
        displayName = "M4A (AAC)",
        mimeType = "audio/mp4a-latm",
        isLossless = false,
        defaultBitrateKbps = 192,
        description = "Estándar moderno móvil. Excelente compresión y fidelidad sonora."
    ),
    WAV(
        extension = "wav",
        displayName = "WAV (PCM)",
        mimeType = "audio/wav",
        isLossless = true,
        defaultBitrateKbps = 1411,
        description = "Formato sin compresión (Lossless). Máxima calidad de estudio."
    ),
    FLAC(
        extension = "flac",
        displayName = "FLAC",
        mimeType = "audio/flac",
        isLossless = true,
        defaultBitrateKbps = 850,
        description = "Compresión sin pérdida. Calidad de CD con menor peso que WAV."
    ),
    OGG(
        extension = "ogg",
        displayName = "OGG (Opus)",
        mimeType = "audio/opus",
        isLossless = false,
        defaultBitrateKbps = 128,
        description = "Gran eficiencia para voz, podcasts y streaming con bajo bitrate."
    ),
    MP3(
        extension = "mp3",
        displayName = "MP3 (Compatible)",
        mimeType = "audio/mpeg",
        isLossless = false,
        defaultBitrateKbps = 192,
        description = "Compatibilidad universal con cualquier reproductor tradicional."
    );

    companion object {
        fun fromExtension(ext: String): AudioFormatType {
            val clean = ext.lowercase().removePrefix(".")
            return values().firstOrNull { it.extension.equals(clean, ignoreCase = true) } ?: M4A
        }
    }
}
