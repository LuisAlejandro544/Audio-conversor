package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Gestor del sistema de almacenamiento público "ConvertX".
 *
 * Misión y arquitectura:
 * - Evita guardar audios en rutas internas ocultas o restringidas como `Android/data/com.app/...`,
 *   en las que los usuarios en Android 11+ no pueden ver sus archivos con exploradores estándar.
 * - Crea de manera limpia y modular la carpeta principal "ConvertX" en el almacenamiento del teléfono,
 *   con subcarpetas organizadas según la función.
 * - Subcarpeta actual para la herramienta de conversión: "ConvertX/Converter".
 * - Preparado para futuras herramientas modulares (ej: "ConvertX/Recorder", "ConvertX/Cutter").
 * - Notifica al sistema mediante MediaScannerConnection y MediaStore para que los reproductores
 *   locales de música (Samsung Music, Xiaomi, VLC, etc.) reconozcan las canciones de inmediato.
 */
object ConvertXStorageManager {

    private const val TAG = "ConvertXStorageManager"

    // Nombre de la carpeta principal accesible por el usuario
    const val ROOT_FOLDER_NAME = "ConvertX"

    // Subcarpetas modulares según herramienta
    const val SUBFOLDER_CONVERTER = "Converter"
    const val SUBFOLDER_RECORDER = "Recorder"
    const val SUBFOLDER_CUTTER = "Cutter"

    /**
     * Obtiene el directorio público destino en el almacenamiento visible del teléfono.
     * Prioridad 1: Carpeta pública en /Music/ConvertX/<subfolder>
     * Prioridad 2: Raíz del almacenamiento externo /ConvertX/<subfolder>
     * Prioridad 3: Descargas públicas /Download/ConvertX/<subfolder>
     */
    fun getPublicConvertXDir(context: Context, subfolder: String = SUBFOLDER_CONVERTER): File {
        // Opción 1: Dentro de la carpeta pública de Música: /Music/ConvertX/<subfolder>
        try {
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            val convertXMusic = File(musicDir, "$ROOT_FOLDER_NAME/$subfolder")
            if (!convertXMusic.exists()) {
                convertXMusic.mkdirs()
            }
            if (convertXMusic.exists() && convertXMusic.canWrite()) {
                Log.d(TAG, "Utilizando directorio público de Música: ${convertXMusic.absolutePath}")
                return convertXMusic
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo acceder a DIRECTORY_MUSIC: ${e.message}")
        }

        // Opción 2: En la raíz del almacenamiento compartido: /ConvertX/<subfolder>
        try {
            val rootDir = Environment.getExternalStorageDirectory()
            val convertXRoot = File(rootDir, "$ROOT_FOLDER_NAME/$subfolder")
            if (!convertXRoot.exists()) {
                convertXRoot.mkdirs()
            }
            if (convertXRoot.exists() && convertXRoot.canWrite()) {
                Log.d(TAG, "Utilizando directorio raíz compartido: ${convertXRoot.absolutePath}")
                return convertXRoot
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo acceder a almacenamiento raíz: ${e.message}")
        }

        // Opción 3: Fallback en directorio externo de archivos
        val fallbackDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "$ROOT_FOLDER_NAME/$subfolder")
        if (!fallbackDir.exists()) {
            fallbackDir.mkdirs()
        }
        return fallbackDir
    }

    /**
     * Retorna una representación amigable y legible para el usuario de dónde se guardó su archivo.
     * Ejemplo: "Música > ConvertX > Converter > mi_audio.mp3"
     */
    fun getDisplayPathForUser(fileName: String, subfolder: String = SUBFOLDER_CONVERTER): String {
        return "$ROOT_FOLDER_NAME / $subfolder / $fileName"
    }

    /**
     * Exporta y replica el archivo de audio procesado dentro de la carpeta pública ConvertX.
     * Además, registra el archivo en MediaStore para que aparezca al instante en todas las
     * aplicaciones de música y en la galería de archivos del dispositivo.
     */
    fun exportToConvertX(
        context: Context,
        sourceFile: File,
        targetFileName: String,
        subfolder: String = SUBFOLDER_CONVERTER
    ): File {
        val targetDir = getPublicConvertXDir(context, subfolder)
        val finalPublicFile = File(targetDir, targetFileName)

        // Si el archivo origen ya está en el destino, evitamos copiarlo sobre sí mismo
        if (sourceFile.absolutePath != finalPublicFile.absolutePath) {
            try {
                if (finalPublicFile.exists()) {
                    finalPublicFile.delete()
                }
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(finalPublicFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Archivo copiado exitosamente a ConvertX: ${finalPublicFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error al copiar archivo a ConvertX: ${e.message}", e)
            }
        }

        val resolvedFile = if (finalPublicFile.exists() && finalPublicFile.length() > 0) {
            finalPublicFile
        } else {
            sourceFile
        }

        // Indexación inmediata en MediaStore para reproductores de música (Android 10+)
        registerInMediaStore(context, resolvedFile, targetFileName, subfolder)

        // Notificación activa al escáner de medios del sistema Android
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(resolvedFile.absolutePath),
                null
            ) { path, uri ->
                Log.i(TAG, "MediaScanner indexó: $path -> $uri")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo no crítico al invocar MediaScanner: ${e.message}")
        }

        return resolvedFile
    }

    /**
     * Inserta una entrada en MediaStore para garantizar la visibilidad en reproductores
     * de audio y exploradores de archivos que usen Scoped Storage.
     */
    fun registerInMediaStore(
        context: Context,
        audioFile: File,
        displayName: String,
        subfolder: String = SUBFOLDER_CONVERTER
    ): Uri? {
        val mimeType = when (audioFile.extension.lowercase()) {
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "mp3" -> "audio/mpeg"
            else -> "audio/*"
        }

        val relativePath = "${Environment.DIRECTORY_MUSIC}/$ROOT_FOLDER_NAME/$subfolder"

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.TITLE, displayName.substringBeforeLast('.'))
            put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        return try {
            val uri = resolver.insert(collection, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outStream ->
                    FileInputStream(audioFile).use { inStream ->
                        inStream.copyTo(outStream)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                Log.i(TAG, "Registrado en MediaStore con URI: $uri y ruta: $relativePath")
            }
            uri
        } catch (e: Exception) {
            Log.w(TAG, "Aviso registrando en MediaStore: ${e.message}")
            null
        }
    }
}
