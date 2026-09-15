package com.example.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Decodificador universal de audio utilizando las APIs nativas de Android (MediaExtractor y MediaCodec).
 *
 * Propósito y Arquitectura:
 * - Decodifica con absoluta fidelidad cualquier formato de audio compatible (MP3, M4A, AAC, WAV, FLAC, OGG, Opus).
 * - Extrae el 100% de la duración del audio hacia muestras PCM crudas de 16 bits little-endian.
 * - Evita el truncamiento de duración y la corrupción de audio al realizar una decodificación
 *   perceptual real a través de los códecs de hardware y software del sistema Android.
 * - Escribe a un archivo temporal en disco para mantener el consumo de memoria RAM en O(1),
 *   asegurando que dispositivos móviles con arquitecturas de 32 o 64 bits y memoria limitada
 *   procesen archivos largos sin OutOfMemoryError.
 */
object AudioDecoder {

    private const val TAG = "AudioDecoder"
    private const val TIMEOUT_US = 10_000L

    /**
     * Metadatos técnicos obtenidos tras una decodificación completa.
     */
    data class DecodedAudio(
        val pcmFile: File,
        val sampleRate: Int,
        val channelCount: Int,
        val durationMs: Long,
        val totalBytes: Long
    )

    /**
     * Decodifica un archivo de audio a muestras PCM 16-bit lineales.
     *
     * @param context Contexto de la aplicación para acceder al ContentResolver o almacenamiento local.
     * @param sourceUri URI del archivo original a decodificar.
     * @param outputPcmFile Archivo temporal donde se escribirán las muestras PCM sin comprimir.
     * @param isCancelled Función lambda para verificar si el usuario canceló el proceso.
     * @param onProgress Callback de notificación de progreso (porcentaje de 0 a 100 y mensaje).
     * @return [DecodedAudio] con los parámetros técnicos reales del audio decodificado.
     */
    fun decodeToPcm(
        context: Context,
        sourceUri: Uri,
        outputPcmFile: File,
        isCancelled: () -> Boolean = { false },
        onProgress: (percent: Int, status: String) -> Unit = { _, _ -> }
    ): DecodedAudio {
        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var fileOutputStream: FileOutputStream? = null

        try {
            // 1. Configurar la fuente en el extractor
            if (sourceUri.scheme == "content") {
                context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                    extractor.setDataSource(pfd.fileDescriptor)
                } ?: extractor.setDataSource(context, sourceUri, null)
            } else if (sourceUri.scheme == "file" || sourceUri.path != null) {
                extractor.setDataSource(sourceUri.path ?: "")
            } else {
                extractor.setDataSource(context, sourceUri, null)
            }

            // 2. Identificar la pista de audio principal
            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null
            var mimeType = ""

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    mimeType = mime
                    break
                }
            }

            if (audioTrackIndex == -1 || trackFormat == null) {
                throw IllegalArgumentException("El archivo seleccionado no contiene ninguna pista de audio reconocible.")
            }

            extractor.selectTrack(audioTrackIndex)

            val sampleRate = if (trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            val channelCount = if (trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val durationUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                trackFormat.getLong(MediaFormat.KEY_DURATION)
            } else 0L

            val durationMs = if (durationUs > 0) durationUs / 1000L else 0L

            onProgress(15, "Decodificando $mimeType (${sampleRate} Hz, ${channelCount} canales)...")

            // 3. Crear e iniciar el decodificador de MediaCodec
            decoder = MediaCodec.createDecoderByType(mimeType)
            decoder.configure(trackFormat, null, null, 0)
            decoder.start()

            fileOutputStream = FileOutputStream(outputPcmFile)
            val bufferInfo = MediaCodec.BufferInfo()
            var sawInputEOS = false
            var sawOutputEOS = false
            var totalBytesWritten = 0L

            var effectiveSampleRate = sampleRate
            var effectiveChannels = channelCount

            // 4. Ciclo de decodificación continua con lectura de tramas y vaciado a PCM
            while (!sawOutputEOS && !isCancelled()) {
                // Alimentar buffers de entrada si aún no hemos alcanzado el final del flujo
                if (!sawInputEOS) {
                    val inIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inputBuffer: ByteBuffer? = decoder.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                sawInputEOS = true
                                decoder.queueInputBuffer(
                                    inIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            } else {
                                val sampleTime = extractor.sampleTime
                                decoder.queueInputBuffer(
                                    inIndex, 0, sampleSize, sampleTime, 0
                                )
                                extractor.advance()

                                if (durationUs > 0) {
                                    val progressFraction = sampleTime.toFloat() / durationUs.toFloat()
                                    val pct = (15 + (progressFraction * 25).toInt()).coerceIn(15, 40)
                                    onProgress(pct, "Decodificando audio fielmente ($pct%)...")
                                }
                            }
                        }
                    }
                }

                // Recuperar buffers de salida con muestras PCM
                val outIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }

                    if (bufferInfo.size > 0) {
                        val outputBuffer: ByteBuffer? = decoder.getOutputBuffer(outIndex)
                        if (outputBuffer != null) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                            val chunk = ByteArray(bufferInfo.size)
                            outputBuffer.get(chunk)
                            fileOutputStream.write(chunk)
                            totalBytesWritten += bufferInfo.size
                        }
                    }

                    decoder.releaseOutputBuffer(outIndex, false)
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = decoder.outputFormat
                    if (newFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        effectiveSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (newFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        effectiveChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    Log.i(TAG, "Formato PCM decodificado: sr=$effectiveSampleRate, ch=$effectiveChannels")
                }
            }

            fileOutputStream.flush()

            if (isCancelled()) {
                throw InterruptedException("Decodificación cancelada por el usuario.")
            }

            // Calcular duración real a partir de los bytes PCM decodificados si durationMs no estaba disponible
            val calculatedDurationMs = if (durationMs > 0) {
                durationMs
            } else {
                val bytesPerSec = effectiveSampleRate * effectiveChannels * 2L
                if (bytesPerSec > 0) (totalBytesWritten * 1000L) / bytesPerSec else 0L
            }

            Log.i(
                TAG,
                "Decodificación finalizada con éxito. PCM: $totalBytesWritten bytes, Duración: ${calculatedDurationMs}ms"
            )

            return DecodedAudio(
                pcmFile = outputPcmFile,
                sampleRate = effectiveSampleRate,
                channelCount = effectiveChannels,
                durationMs = calculatedDurationMs,
                totalBytes = totalBytesWritten
            )
        } finally {
            try { fileOutputStream?.close() } catch (_: Exception) {}
            try {
                decoder?.stop()
                decoder?.release()
            } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}
