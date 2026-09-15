package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream

/**
 * Utilidad para exportar y compartir archivos de audio convertidos.
 *
 * Funciones:
 * 1. Guardar el archivo en la colección pública de Música/Audio del dispositivo (MediaStore),
 *    compatible con Android 10+ Scoped Storage y versiones previas.
 * 2. Compartir el archivo con otras aplicaciones mediante FileProvider.
 */
object AudioExportHelper {

    /**
     * Guarda el archivo de audio en la carpeta pública accesible ConvertX/Converter.
     * Retorna el URI o referencia del archivo guardado.
     */
    fun saveToMusicDirectory(context: Context, sourceFile: File, title: String): Uri? {
        val exportedFile = ConvertXStorageManager.exportToConvertX(
            context = context,
            sourceFile = sourceFile,
            targetFileName = sourceFile.name,
            subfolder = ConvertXStorageManager.SUBFOLDER_CONVERTER
        )
        return Uri.fromFile(exportedFile)
    }

    /**
     * Abre el diálogo estándar de Android para compartir el archivo de audio con otras apps.
     */
    fun shareAudioFile(context: Context, file: File) {
        val authority = "${context.packageName}.provider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val mimeType = when (file.extension.lowercase()) {
            "m4a" -> "audio/mp4"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "mp3" -> "audio/mpeg"
            else -> "audio/*"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Compartir audio con...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
