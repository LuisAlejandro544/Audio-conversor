/**
 * AudioStudio Native Core - FFmpeg Pure C Interface (ffmpeg_pure_core.h)
 *
 * Expone la API pura de transcodificación y utilidades nativas de FFmpeg C
 * para ser consumidas desde C++20 con gestión RAII.
 */

#ifndef AUDIOSTUDIO_FFMPEG_PURE_CORE_H
#define AUDIOSTUDIO_FFMPEG_PURE_CORE_H

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*FFmpegProgressCallback)(int percent, const char *status_msg, void *user_data);

/**
 * Transcodifica un archivo de audio completo utilizando el motor puro de FFmpeg (libav*).
 *
 * Realiza decodificación universal, remuestreo (libswresample), remezcla de canales,
 * ajuste de volumen con limitador suave y codificación al formato seleccionado
 * con el bitrate exacto solicitado (64 kbps - 320 kbps).
 *
 * @param input_path Ruta absoluta del archivo fuente en el almacenamiento.
 * @param output_path Ruta absoluta del archivo de salida a generar.
 * @param target_format Formato ("mp3", "m4a", "wav", "flac", "ogg").
 * @param target_bitrate_kbps Tasa de bits en kbps (ej: 64, 128, 192, 256, 320).
 * @param target_sample_rate Tasa de muestreo en Hz (0 para conservar original).
 * @param target_channels Número de canales (0 para conservar, 1=Mono, 2=Estéreo).
 * @param volume_gain Factor de amplificación (1.0 = 100%).
 * @param progress_cb Callback de reporte de progreso (0-100%).
 * @param user_data Puntero a contexto opcional para el callback.
 * @return 0 en caso de éxito, o un código de error menor a cero.
 */
int ffmpeg_core_transcode_audio(
    const char *input_path,
    const char *output_path,
    const char *target_format,
    int target_bitrate_kbps,
    int target_sample_rate,
    int target_channels,
    float volume_gain,
    FFmpegProgressCallback progress_cb,
    void *user_data
);

#ifdef __cplusplus
}
#endif

#endif // AUDIOSTUDIO_FFMPEG_PURE_CORE_H
