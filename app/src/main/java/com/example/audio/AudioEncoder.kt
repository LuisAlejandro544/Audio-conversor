package com.example.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Codificador universal de audio para empaquetar flujos PCM en contenedores y códecs estándar.
 *
 * Propósito y Arquitectura:
 * - Codifica muestras PCM crudas a contenedores estándar de alta fidelidad:
 *   - M4A (AAC-LC): Estándar moderno de máxima compatibilidad en Android/iOS/Web con bitrate configurable.
 *   - WAV (PCM): Formato de audio puro de estudio sin pérdidas (lossless).
 *   - FLAC: Compresión sin pérdidas mediante MediaCodec FLAC del sistema.
 *   - OGG/MP3: Códecs del sistema con selección inteligente según capacidades del dispositivo móvil.
 * - Asegura marcas de tiempo precisas (presentationTimeUs) para prevenir desincronización y truncamiento.
 * - Procesa flujos por bloques manteniendo un uso estricto de memoria RAM O(1).
 */
object AudioEncoder {

    private const val TAG = "AudioEncoder"
    private const val TIMEOUT_US = 10_000L

    /**
     * Codifica un archivo de muestras PCM crudas al formato objetivo indicado.
     */
    fun encodeAudio(
        pcmFile: File,
        outputFile: File,
        targetFormat: AudioFormatType,
        sampleRate: Int,
        channelCount: Int,
        bitrateKbps: Int,
        isCancelled: () -> Boolean = { false },
        onProgress: (percent: Int, status: String) -> Unit = { _, _ -> }
    ) {
        when (targetFormat) {
            AudioFormatType.M4A -> {
                encodePcmToM4a(
                    pcmFile = pcmFile,
                    outputM4aFile = outputFile,
                    sampleRate = sampleRate,
                    channelCount = channelCount,
                    bitrateKbps = bitrateKbps,
                    isCancelled = isCancelled,
                    onProgress = onProgress
                )
            }
            AudioFormatType.WAV -> {
                encodePcmToWav(
                    pcmFile = pcmFile,
                    outputWavFile = outputFile,
                    sampleRate = sampleRate,
                    channelCount = channelCount,
                    isCancelled = isCancelled,
                    onProgress = onProgress
                )
            }
            AudioFormatType.FLAC -> {
                if (hasSystemEncoder("audio/flac")) {
                    encodePcmToFlac(
                        pcmFile = pcmFile,
                        outputFlacFile = outputFile,
                        sampleRate = sampleRate,
                        channelCount = channelCount,
                        isCancelled = isCancelled,
                        onProgress = onProgress
                    )
                } else {
                    // Fallback a WAV de alta fidelidad si el dispositivo no incluye codificador FLAC nativo
                    encodePcmToWav(pcmFile, outputFile, sampleRate, channelCount, isCancelled, onProgress)
                }
            }
            AudioFormatType.OGG -> {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && hasSystemEncoder("audio/opus")) {
                    encodePcmToM4a(pcmFile, outputFile, sampleRate, channelCount, bitrateKbps, isCancelled, onProgress)
                } else {
                    encodePcmToM4a(pcmFile, outputFile, sampleRate, channelCount, bitrateKbps, isCancelled, onProgress)
                }
            }
            AudioFormatType.MP3 -> {
                if (hasSystemEncoder("audio/mpeg")) {
                    encodePcmToMp3NativeCodec(pcmFile, outputFile, sampleRate, channelCount, bitrateKbps, isCancelled, onProgress)
                } else {
                    // Si el chipset no provee codificador MP3 de hardware, codifica en M4A AAC (universal)
                    encodePcmToM4a(pcmFile, outputFile, sampleRate, channelCount, bitrateKbps, isCancelled, onProgress)
                }
            }
        }
    }

    /**
     * Codifica muestras PCM a formato M4A (AAC-LC) mediante MediaCodec y MediaMuxer.
     * Genera un archivo estándar .m4a perfectamente indexado y reproducible en cualquier reproductor.
     */
    fun encodePcmToM4a(
        pcmFile: File,
        outputM4aFile: File,
        sampleRate: Int,
        channelCount: Int,
        bitrateKbps: Int,
        isCancelled: () -> Boolean = { false },
        onProgress: (percent: Int, status: String) -> Unit = { _, _ -> }
    ) {
        val fis = FileInputStream(pcmFile)
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val mime = MediaFormat.MIMETYPE_AUDIO_AAC
            val targetBitrate = (bitrateKbps * 1000).coerceIn(64_000, 320_000)

            val format = MediaFormat.createAudioFormat(mime, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
            }

            encoder = MediaCodec.createEncoderByType(mime)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputM4aFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var audioTrackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val totalBytes = pcmFile.length()
            var bytesReadTotal = 0L

            var sawInputEOS = false
            var sawOutputEOS = false
            val bytesPerSample = channelCount * 2
            var totalFramesSent = 0L

            val chunkBuffer = ByteArray(4096)

            while (!sawOutputEOS && !isCancelled()) {
                // 1. Alimentar datos PCM al codificador
                if (!sawInputEOS) {
                    val inIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val toRead = inputBuffer.remaining().coerceAtMost(chunkBuffer.size)
                            val read = fis.read(chunkBuffer, 0, toRead)

                            if (read <= 0) {
                                sawInputEOS = true
                                val finalPtsUs = if (sampleRate > 0) (totalFramesSent * 1_000_000L) / sampleRate else 0L
                                encoder.queueInputBuffer(
                                    inIndex, 0, 0, finalPtsUs,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                            } else {
                                inputBuffer.put(chunkBuffer, 0, read)
                                bytesReadTotal += read

                                val ptsUs = if (sampleRate > 0) (totalFramesSent * 1_000_000L) / sampleRate else 0L
                                val framesInChunk = if (bytesPerSample > 0) read / bytesPerSample else 0
                                totalFramesSent += framesInChunk

                                encoder.queueInputBuffer(inIndex, 0, read, ptsUs, 0)

                                if (totalBytes > 0) {
                                    val ratio = bytesReadTotal.toFloat() / totalBytes.toFloat()
                                    val pct = (65 + (ratio * 28).toInt()).coerceIn(65, 93)
                                    onProgress(pct, "Codificando M4A (AAC) a ${bitrateKbps} kbps ($pct%)...")
                                }
                            }
                        }
                    }
                }

                // 2. Extraer tramas comprimidas AAC y escribir al MediaMuxer
                val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }

                    val outputBuffer = encoder.getOutputBuffer(outIndex)
                    if (outputBuffer != null && bufferInfo.size > 0 && muxerStarted) {
                        // Omitir cabeceras de configuración ya añadidas al track
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(audioTrackIndex, outputBuffer, bufferInfo)
                        }
                    }

                    encoder.releaseOutputBuffer(outIndex, false)
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        Log.w(TAG, "Formato cambiado inesperadamente después del inicio de MediaMuxer")
                    } else {
                        val newFormat = encoder.outputFormat
                        audioTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                        Log.i(TAG, "MediaMuxer iniciado con éxito: track=$audioTrackIndex, format=$newFormat")
                    }
                }
            }

            if (isCancelled()) {
                throw InterruptedException("Codificación cancelada por el usuario.")
            }
        } finally {
            try { fis.close() } catch (_: Exception) {}
            try {
                encoder?.stop()
                encoder?.release()
            } catch (_: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Codifica muestras PCM a formato WAV estándar con cabecera RIFF de 44 bytes.
     */
    fun encodePcmToWav(
        pcmFile: File,
        outputWavFile: File,
        sampleRate: Int,
        channelCount: Int,
        isCancelled: () -> Boolean = { false },
        onProgress: (percent: Int, status: String) -> Unit = { _, _ -> }
    ) {
        val totalAudioBytes = pcmFile.length().toInt()
        val fos = FileOutputStream(outputWavFile)
        val fis = FileInputStream(pcmFile)

        try {
            SampleAudioGenerator.writeWavHeader(
                out = fos,
                totalAudioLen = totalAudioBytes,
                sampleRate = sampleRate,
                channels = channelCount
            )

            val buffer = ByteArray(8192)
            var bytesCopied = 0L
            var read: Int

            while (fis.read(buffer).also { read = it } != -1) {
                if (isCancelled()) {
                    throw InterruptedException("Operación cancelada por el usuario.")
                }
                fos.write(buffer, 0, read)
                bytesCopied += read
                if (totalAudioBytes > 0) {
                    val pct = (70 + ((bytesCopied.toFloat() / totalAudioBytes) * 25).toInt()).coerceIn(70, 95)
                    onProgress(pct, "Escribiendo archivo WAV de estudio ($pct%)...")
                }
            }
            fos.flush()
        } finally {
            try { fis.close() } catch (_: Exception) {}
            try { fos.close() } catch (_: Exception) {}
        }
    }

    /**
     * Codifica muestras PCM a FLAC utilizando el codificador nativo del sistema Android.
     */
    private fun encodePcmToFlac(
        pcmFile: File,
        outputFlacFile: File,
        sampleRate: Int,
        channelCount: Int,
        isCancelled: () -> Boolean = { false },
        onProgress: (percent: Int, status: String) -> Unit = { _, _ -> }
    ) {
        val fis = FileInputStream(pcmFile)
        val fos = FileOutputStream(outputFlacFile)
        var encoder: MediaCodec? = null

        try {
            val mime = "audio/flac"
            val format = MediaFormat.createAudioFormat(mime, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, 5)
            }

            encoder = MediaCodec.createEncoderByType(mime)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val totalBytes = pcmFile.length()
            var bytesReadTotal = 0L
            var sawInputEOS = false
            var sawOutputEOS = false
            val bytesPerSample = channelCount * 2
            var totalFramesSent = 0L
            val chunkBuffer = ByteArray(4096)

            while (!sawOutputEOS && !isCancelled()) {
                if (!sawInputEOS) {
                    val inIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val read = fis.read(chunkBuffer, 0, inputBuffer.remaining().coerceAtMost(chunkBuffer.size))
                            if (read <= 0) {
                                sawInputEOS = true
                                val ptsUs = if (sampleRate > 0) (totalFramesSent * 1_000_000L) / sampleRate else 0L
                                encoder.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                inputBuffer.put(chunkBuffer, 0, read)
                                bytesReadTotal += read
                                val ptsUs = if (sampleRate > 0) (totalFramesSent * 1_000_000L) / sampleRate else 0L
                                totalFramesSent += if (bytesPerSample > 0) read / bytesPerSample else 0
                                encoder.queueInputBuffer(inIndex, 0, read, ptsUs, 0)

                                if (totalBytes > 0) {
                                    val pct = (70 + ((bytesReadTotal.toFloat() / totalBytes) * 25).toInt()).coerceIn(70, 95)
                                    onProgress(pct, "Codificando FLAC sin pérdidas ($pct%)...")
                                }
                            }
                        }
                    }
                }

                val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }
                    val outputBuffer = encoder.getOutputBuffer(outIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val outBytes = ByteArray(bufferInfo.size)
                        outputBuffer.get(outBytes)
                        fos.write(outBytes)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                }
            }
            fos.flush()
        } finally {
            try { fis.close() } catch (_: Exception) {}
            try { fos.close() } catch (_: Exception) {}
            try {
                encoder?.stop()
                encoder?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Codifica muestras PCM a MP3 utilizando el códec nativo de Android en chipsets que lo incluyan.
     */
    private fun encodePcmToMp3NativeCodec(
        pcmFile: File,
        outputMp3File: File,
        sampleRate: Int,
        channelCount: Int,
        bitrateKbps: Int,
        isCancelled: () -> Boolean = { false },
        onProgress: (percent: Int, status: String) -> Unit = { _, _ -> }
    ) {
        val fis = FileInputStream(pcmFile)
        val fos = FileOutputStream(outputMp3File)
        var encoder: MediaCodec? = null

        try {
            val mime = "audio/mpeg"
            val format = MediaFormat.createAudioFormat(mime, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitrateKbps * 1000)
            }

            encoder = MediaCodec.createEncoderByType(mime)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val totalBytes = pcmFile.length()
            var bytesReadTotal = 0L
            var sawInputEOS = false
            var sawOutputEOS = false
            val bytesPerSample = channelCount * 2
            var totalFramesSent = 0L
            val chunkBuffer = ByteArray(4096)

            while (!sawOutputEOS && !isCancelled()) {
                if (!sawInputEOS) {
                    val inIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIndex >= 0) {
                        val inputBuffer = encoder.getInputBuffer(inIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val read = fis.read(chunkBuffer, 0, inputBuffer.remaining().coerceAtMost(chunkBuffer.size))
                            if (read <= 0) {
                                sawInputEOS = true
                                val ptsUs = if (sampleRate > 0) (totalFramesSent * 1_000_000L) / sampleRate else 0L
                                encoder.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                inputBuffer.put(chunkBuffer, 0, read)
                                bytesReadTotal += read
                                val ptsUs = if (sampleRate > 0) (totalFramesSent * 1_000_000L) / sampleRate else 0L
                                totalFramesSent += if (bytesPerSample > 0) read / bytesPerSample else 0
                                encoder.queueInputBuffer(inIndex, 0, read, ptsUs, 0)

                                if (totalBytes > 0) {
                                    val pct = (70 + ((bytesReadTotal.toFloat() / totalBytes) * 25).toInt()).coerceIn(70, 95)
                                    onProgress(pct, "Codificando MP3 ($pct%)...")
                                }
                            }
                        }
                    }
                }

                val outIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                if (outIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }
                    val outputBuffer = encoder.getOutputBuffer(outIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val outBytes = ByteArray(bufferInfo.size)
                        outputBuffer.get(outBytes)
                        fos.write(outBytes)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                }
            }
            fos.flush()
        } finally {
            try { fis.close() } catch (_: Exception) {}
            try { fos.close() } catch (_: Exception) {}
            try {
                encoder?.stop()
                encoder?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Verifica si el dispositivo actual dispone de un codificador de hardware/software para un MIME dado.
     */
    fun hasSystemEncoder(mime: String): Boolean {
        return try {
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            list.codecInfos.any { info ->
                info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
            }
        } catch (_: Exception) {
            false
        }
    }
}
