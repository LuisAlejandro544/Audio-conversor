# 🗺️ Roadmap de Desarrollo - AudioStudio

## 📍 Fase 1: Base de la Aplicación y UI Modular (Completada ✅)
- Arquitectura de pantallas múltiples (Home, Selector, Configuración, Progreso, Resultados, Historial).
- Persistencia de historial con SQLite / Room Database.
- Motor de conversión inicial y reproductor de audio integrado.

## 📍 Fase 2: Cimentación Nativa C++20 & Cabeceras FFmpeg (Completada ✅)
- Directorio nativo en `app/src/main/cpp` con soporte C++20 estricto (`-std=c++20`).
- Cabeceras C puras de FFmpeg (`libavutil`, `libavcodec`, `libavformat`, `libswresample`).
- Conector JNI modular en `NativeAudioEngine.kt`.
- Reglas en `.gitignore` para proteger el repositorio de binarios pesados.

## 📍 Fase 3: Integración Total de FFmpeg Pure C (Completada ✅)
- Configuración de CMake y NDK en `app/build.gradle.kts` con `externalNativeBuild`.
- Compilación nativa verificada para arquitecturas de 32 bits (`armeabi-v7a`, `x86`) y 64 bits (`arm64-v8a`, `x86_64`).
- Implementación de biblioteca compartida `libaudiostudio_native.so` con núcleo en C (`ffmpeg_pure_core.c`) y puente JNI (`native-lib.cpp`).
- Gestión de ciclo de vida de memoria en C++20 con RAII (`raii_wrappers.hpp`) para `AVFrame`, `AVPacket`, `SwrContext` y `AVFormatContext`.
- **Sustitución 100% de MediaCodec y MediaMuxer**: Implementación del pipeline completo de transcodificación de audio en C puro con FFmpeg (`ffmpeg_core_transcode_audio`).
- Soporte completo para cambio de formato (MP3, WAV, AAC/M4A, FLAC, OGG), cambio de bitrate (64k-320k), remuestreo y ganancia en `AudioConverterEngine.kt` vía JNI.
- Rediseño de interfaz de usuario inspirado en AudioLab (cuadrícula 4x de herramientas, carrusel de herramientas recientes, barra de navegación y FAB de acción rápida).
- Indicadores visuales de candado en herramientas pendientes de lógica completa (Recorte, Fusión, Mezclador, Etiquetas ID3, Grabadora, Audio 8D, Silencios).
- Limpieza de la interfaz de usuario: retirada de telemetría y banners técnicos del "Motor C++" para no confundir al usuario final, manteniendo el motor nativo al 100% bajo la capucha.
- Escala de fuente fija (`fontScale = 1.0f`) mediante `CompositionLocalProvider` y `LocalDensity` para garantizar estabilidad visual frente a configuraciones del sistema móvil.
- Eliminación total de audios de prueba sintéticos, garantizando procesamiento exclusivo con audios reales del usuario.
- **Almacenamiento Público "ConvertX" con Subcarpetas (Completado ✅)**:
  - Salida directa de archivos convertidos en la carpeta pública del teléfono `ConvertX/Converter`, evitando el almacenamiento oculto `Android/data/...`.
  - Integración con `MediaScannerConnection` y `MediaStore` para visibilidad inmediata en reproductores multimedia y exploradores de archivos del dispositivo.
  - Notificaciones y tarjetas de ubicación directa en UI para el usuario móvil.
- **Automatización CI con GitHub Actions & Firma Debug (Completado ✅)**:
  - Workflow `.github/workflows/build_debug_apk.yml` configurado con Gradle y NDK sin caché.
  - Script `generate_debug_keystore.sh` para forzar la creación desde cero de `debug.keystore` de forma no interactiva.
- **Protección Dinámica de Calidad y Techo Técnico (Completado ✅)**:
  - Validación en tiempo real de los límites nativos del archivo fuente (bitrate, frecuencia de muestreo y canales).
  - Bloqueo de opciones superiores a las nativas en la pantalla de configuración (`ConvertConfigScreen`) con candados visuales `🔒` y deslizador acotado.
  - Supresión de falso estéreo en archivos Mono de 1 canal.
  - Clamping estricto en `ConverterViewModel` para asegurar que el estado y la transcodificación nunca intenten upsampling destructivo.

## 📍 Fase 4: Filtros DSP Avanzados y Edición de Audio (Siguiente Fase 🚀)
- Ecualizador paramétrico nativo de 5 bandas en C++20 con FFmpeg.
- Normalización de sonoridad según norma EBU R128 (`libavfilter`).
- Herramienta de recorte de audio con precisión de milisegundos mediante demuxer/muxer nativo de FFmpeg.
- Herramienta de fusión y mezclador de pistas de audio multipista con FFmpeg.
- Conversión por lotes (Batch processing) en segundo plano con notificación de progreso.
