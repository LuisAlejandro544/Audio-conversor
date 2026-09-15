package com.example.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Parámetros de configuración solicitados por el usuario para la conversión de audio.
 */
data class ConversionConfig(
    val sourceUri: Uri,
    val sourceInfo: AudioInfo,
    val targetFormat: AudioFormatType,
    val targetBitrateKbps: Int,
    val targetSampleRateHz: Int, // 0 significa conservar la frecuencia original
    val targetChannels: Int, // 0 = original, 1 = Mono, 2 = Estéreo
    val volumeGainFactor: Float, // 1.0f = 100%, 0.5f = 50%, 1.5f = 150%, 2.0f = 200%
    val customOutputFileName: String
)

/**
 * Resultado de una conversión de audio.
 */
data class ConversionResult(
    val outputFile: File,
    val format: AudioFormatType,
    val fileSize: Long,
    val durationMs: Long,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channelCount: Int
)

/**
 * Motor central de conversión de audio 100% basado en FFmpeg nativo (libav*).
 *
 * Arquitectura y características:
 * - Reemplaza al 100% las herramientas de Android (MediaCodec, MediaMuxer) por la API nativa en C de FFmpeg.
 * - Toda la cadena: cambio de bitrate exacto (64k - 320k), cambio de formato (MP3, WAV, AAC/M4A, FLAC, OGG),
 *   remuestreo de frecuencia de muestreo (libswresample), mezcla de canales y amplificación con limitador suave
 *   se realiza en el núcleo nativo en C/C++20 sin wrappers intermediarios.
 * - Compatible con arquitecturas de 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 * - Ideal para distribución en tiendas de APKs de terceros como Uptodown.
 */
class AudioConverterEngine(private val context: Context) {

    companion object {
        private const val TAG = "AudioConverterEngine"
    }

    @Volatile
    private var isCancelled = false

    fun cancel() {
        isCancelled = true
    }

    /**
     * Ejecuta la conversión de audio al 100% con FFmpeg nativo en segundo plano.
     *
     * @param config Parámetros elegidos por el usuario (formato, bitrate, frecuencia, etc.)
     * @param onProgress Callback (progreso de 0 a 100, mensaje de estado)
     */
    suspend fun convertAudio(
        config: ConversionConfig,
        onProgress: (percent: Int, statusMessage: String) -> Unit
    ): ConversionResult = withContext(Dispatchers.IO) {
        isCancelled = false
        onProgress(5, "Inicializando motor de conversión 100% FFmpeg...")

        // 1. Obtener o crear archivo fuente local legible por fopen de FFmpeg
        var tempInputFile: File? = null
        val sourcePath: String = try {
            if (config.sourceUri.scheme == "file") {
                config.sourceUri.path ?: throw IllegalArgumentException("Ruta local vacía")
            } else {
                onProgress(10, "Copiando archivo fuente para análisis nativo...")
                val tempFile = File.createTempFile("ffmpeg_src_", ".audio", context.cacheDir)
                tempInputFile = tempFile
                context.contentResolver.openInputStream(config.sourceUri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("No se pudo abrir el archivo fuente")
                tempFile.absolutePath
            }
        } catch (e: Exception) {
            tempInputFile?.delete()
            throw IllegalStateException("Error preparando archivo fuente para FFmpeg: ${e.message}", e)
        }

        if (isCancelled) {
            tempInputFile?.delete()
            throw InterruptedException("Conversión cancelada por el usuario")
        }

        // 2. Preparar el archivo de destino con la extensión correspondiente
        val cleanName = if (config.customOutputFileName.isNotBlank()) {
            config.customOutputFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        } else {
            "audiostudio_${System.currentTimeMillis()}"
        }

        val targetExt = config.targetFormat.extension
        val finalFileName = if (cleanName.endsWith(".$targetExt", ignoreCase = true)) {
            cleanName
        } else {
            "$cleanName.$targetExt"
        }

        // Búfer de trabajo de transcodificación
        val workDir = File(context.cacheDir, "ffmpeg_work").apply { mkdirs() }
        val workingOutputFile = File(workDir, "temp_${System.currentTimeMillis()}_$finalFileName")
        if (workingOutputFile.exists()) {
            workingOutputFile.delete()
        }

        onProgress(15, "Iniciando pipeline nativo FFmpeg (libavformat + libavcodec)...")

        // 3. Ejecutar conversión completa en C++20 / C puro con FFmpeg
        val resultCode = NativeAudioEngine.convertAudioFile(
            inputPath = sourcePath,
            outputPath = workingOutputFile.absolutePath,
            targetFormat = targetExt,
            targetBitrateKbps = config.targetBitrateKbps,
            targetSampleRate = config.targetSampleRateHz,
            targetChannels = config.targetChannels,
            volumeGain = config.volumeGainFactor,
            onProgress = { percent, msg ->
                if (!isCancelled) {
                    onProgress(percent, msg)
                }
            }
        )

        // Limpiar archivo temporal de entrada
        tempInputFile?.delete()

        if (isCancelled) {
            workingOutputFile.delete()
            throw InterruptedException("Conversión cancelada por el usuario")
        }

        if (resultCode != 0 || !workingOutputFile.exists() || workingOutputFile.length() == 0L) {
            Log.e(TAG, "Fallo en la conversión nativa FFmpeg. Código: $resultCode")
            throw IllegalStateException("Error al transcodificar con FFmpeg (código $resultCode). Verifica los parámetros de entrada.")
        }

        onProgress(95, "Exportando a carpeta pública ConvertX/Converter...")

        // Guardar y colocar en la carpeta pública ConvertX/Converter accesible por el usuario
        val publicFile = ConvertXStorageManager.exportToConvertX(
            context = context,
            sourceFile = workingOutputFile,
            targetFileName = finalFileName,
            subfolder = ConvertXStorageManager.SUBFOLDER_CONVERTER
        )

        // Limpiar búfer de trabajo temporal
        if (workingOutputFile.absolutePath != publicFile.absolutePath) {
            workingOutputFile.delete()
        }

        onProgress(100, "¡Conversión finalizada! Guardado en ConvertX/Converter")

        val finalDurationMs = if (config.sourceInfo.durationMs > 0) {
            config.sourceInfo.durationMs
        } else {
            // Estimar duración basada en el tamaño del archivo generado y bitrate
            val bytes = publicFile.length()
            val bits = bytes * 8
            val durationSec = bits.toDouble() / (config.targetBitrateKbps * 1000)
            (durationSec * 1000).toLong()
        }

        val finalSampleRate = if (config.targetSampleRateHz > 0) config.targetSampleRateHz else config.sourceInfo.sampleRateHz
        val finalChannels = if (config.targetChannels > 0) config.targetChannels else config.sourceInfo.channelCount

        ConversionResult(
            outputFile = publicFile,
            format = config.targetFormat,
            fileSize = publicFile.length(),
            durationMs = finalDurationMs,
            bitrateKbps = config.targetBitrateKbps,
            sampleRateHz = finalSampleRate,
            channelCount = finalChannels
        )
    }
}
