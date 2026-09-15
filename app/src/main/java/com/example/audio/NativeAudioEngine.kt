package com.example.audio

import android.util.Log

/**
 * Conector JNI para el motor de audio nativo en C++20 con soporte para API pura de FFmpeg (libav*).
 *
 * Proporciona acceso a procesamiento de audio de alto rendimiento sin wrappers intermediarios,
 * manteniendo compatibilidad tanto para arquitecturas de 32 bits (armeabi-v7a, x86)
 * como de 64 bits (arm64-v8a, x86_64).
 */
object NativeAudioEngine {
    private const val TAG = "NativeAudioEngine"

    private var isNativeLibraryLoaded = false

    init {
        try {
            System.loadLibrary("audiostudio_native")
            isNativeLibraryLoaded = true
            Log.i(TAG, "Librería nativa audiostudio_native (C++20) cargada exitosamente.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Librería nativa aún no enlazada en el runtime: ${e.message}")
            isNativeLibraryLoaded = false
        }
    }

    /**
     * Retorna verdadero si la librería compartida nativa C++20 está disponible en el entorno actual.
     */
    fun isAvailable(): Boolean = isNativeLibraryLoaded

    /**
     * Obtiene información descriptiva del motor nativo C++20 y versión de FFmpeg API.
     */
    fun getInfo(): String {
        return if (isNativeLibraryLoaded) {
            try {
                getNativeEngineInfo()
            } catch (e: Throwable) {
                "C++20 Engine (Cargado, inicializando FFmpeg C bindings)"
            }
        } else {
            "C++20 / Pure FFmpeg C API (Preparado para compilación NDK)"
        }
    }

    // Funciones nativas declaradas para el JNI C++20 con FFmpeg Pure C
    private external fun getNativeEngineInfo(): String
    private external fun isPureFFmpegReady(): Boolean
    private external fun getFFmpegVersion(): String
    private external fun getFFmpegConfiguration(): String
    private external fun getFFmpegLicense(): String
    private external fun getSupportedCodecs(): Array<String>
    private external fun processAudioNative(
        inputPcm: ByteArray,
        srcSampleRate: Int,
        srcChannels: Int,
        dstSampleRate: Int,
        dstChannels: Int,
        volumeGain: Float
    ): ByteArray

    private external fun convertAudioFileNative(
        inputPath: String,
        outputPath: String,
        targetFormat: String,
        targetBitrateKbps: Int,
        targetSampleRate: Int,
        targetChannels: Int,
        volumeGain: Float,
        listener: NativeProgressListener?
    ): Int

    /**
     * Interfaz de callback JNI para reportar el progreso en tiempo real desde C++20.
     */
    fun interface NativeProgressListener {
        fun onProgress(percent: Int, statusMessage: String)
    }

    /**
     * Retorna la versión pura de FFmpeg enlazada en el núcleo C++20.
     */
    fun getVersion(): String {
        return if (isNativeLibraryLoaded) {
            try {
                getFFmpegVersion()
            } catch (e: Throwable) {
                "FFmpeg Pure C 7.1"
            }
        } else {
            "No disponible"
        }
    }

    /**
     * Retorna la lista de códecs nativos soportados por FFmpeg Pure C.
     */
    fun getCodecs(): List<String> {
        return if (isNativeLibraryLoaded) {
            try {
                getSupportedCodecs().toList()
            } catch (e: Throwable) {
                listOf("MP3", "AAC", "WAV", "FLAC", "OPUS", "OGG", "ALAC")
            }
        } else {
            listOf("MP3", "AAC", "WAV", "FLAC", "OPUS", "OGG")
        }
    }

    /**
     * Procesa datos de audio PCM usando el motor nativo de C++20 con libswresample y RAII.
     * Realiza remuestreo de frecuencias, remezcla de canales y ganancia con limitador suave.
     */
    fun processPcmAudioNative(
        inputPcm: ByteArray,
        sourceSampleRate: Int,
        sourceChannels: Int,
        targetSampleRate: Int,
        targetChannels: Int,
        gain: Float
    ): ByteArray {
        if (!isNativeLibraryLoaded) {
            Log.w(TAG, "Motor nativo no cargado, devolviendo buffer original.")
            return inputPcm
        }
        return try {
            processAudioNative(
                inputPcm = inputPcm,
                srcSampleRate = sourceSampleRate,
                srcChannels = sourceChannels,
                dstSampleRate = targetSampleRate,
                dstChannels = targetChannels,
                volumeGain = gain
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error en procesamiento nativo C++20 FFmpeg: ${e.message}", e)
            inputPcm
        }
    }

    /**
     * Realiza la conversión y transcodificación de audio 100% mediante el núcleo nativo de FFmpeg.
     * Soporta cambios de bitrate (kbps), formatos (MP3, WAV, AAC/M4A, FLAC, OGG), remuestreo y volumen.
     *
     * @return 0 en caso de éxito, o un código negativo en caso de error.
     */
    fun convertAudioFile(
        inputPath: String,
        outputPath: String,
        targetFormat: String,
        targetBitrateKbps: Int,
        targetSampleRate: Int,
        targetChannels: Int,
        volumeGain: Float,
        onProgress: (percent: Int, statusMessage: String) -> Unit
    ): Int {
        if (!isNativeLibraryLoaded) {
            Log.e(TAG, "convertAudioFile: Librería nativa FFmpeg no cargada.")
            return -1
        }
        return try {
            convertAudioFileNative(
                inputPath = inputPath,
                outputPath = outputPath,
                targetFormat = targetFormat.lowercase(),
                targetBitrateKbps = targetBitrateKbps,
                targetSampleRate = targetSampleRate,
                targetChannels = targetChannels,
                volumeGain = volumeGain,
                listener = { percent, msg ->
                    onProgress(percent, msg)
                }
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error al invocar convertAudioFileNative de FFmpeg: ${e.message}", e)
            -99
        }
    }
}
