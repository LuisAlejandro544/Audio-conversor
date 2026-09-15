# AudioStudio 🎙️ (Conversor y Suite de Audio Nativa)

Aplicación Android de alto rendimiento desarrollada en Kotlin + Jetpack Compose con arquitectura nativa en C++20 y motor de conversión **100% basado en la API pura en C de FFmpeg** (`libavcodec`, `libavformat`, `libswresample`, `libavutil`).

Diseñada para distribución abierta en tiendas de APKs de terceros (como Uptodown) y procesamiento de audio 100% local, offline y sin restricciones. Compatible con **Android 8.0 (Oreo, API 26) y versiones superiores**, optimizada para arquitecturas de **32 bits** (`armeabi-v7a`, `x86`) y **64 bits** (`arm64-v8a`, `x86_64`).

## 🚀 Estado Actual
- **Motor de Conversión 100% FFmpeg**: Sustitución total de las herramientas nativas de Android (`MediaCodec`, `MediaMuxer`) por transcodificación directa en C11/C++20 con FFmpeg. Cada cambio de bitrate (64k - 320k), cambio de formato (MP3, WAV, AAC/M4A, FLAC, OGG), remuestreo y ganancia se procesa en el núcleo nativo.
- **Compatibilidad Multi-Arquitectura**: Compilación nativa validada para `armeabi-v7a`, `x86`, `arm64-v8a` y `x86_64` con CMake 3.22 y Android NDK (r26).
- **Gestión de Memoria Segura**: Ciclo de vida gestionado en C++20 con idiomática RAII (`raii_wrappers.hpp`) mediante `std::unique_ptr` y destructores de liberación determinista.
- **Pipeline de Audio en C Puro**: Implementado en `ffmpeg_pure_core.c` y expuesto vía JNI con despacho asíncrono de progreso en tiempo real (`NativeAudioEngine.kt`).
- **UI Modular e Intuitiva**: Inspirada en suites profesionales de edición como AudioLab, con cuadrícula de herramientas, carrusel de accesos recientes y barra de navegación de 3 pestañas (Inicio, Historial, Ajustes).
- **Herramientas Activas y Bloqueadas**:
  - **Activas (100% funcionales con FFmpeg)**: Convertir Formato, Comprimir Audio (bitrate variable), Ajuste de Volumen/Ganancia (limitador suave), Remuestreo de Frecuencia (8 kHz a 96 kHz) y Balance de Canales Mono/Estéreo.
  - **Bloqueadas con Candado (Próximas fases)**: Recorte, Fusión, Mezclador, Editor de Etiquetas ID3, Grabadora de Voz, Audio 8D y Detección de Silencios.
- **Protección Dinámica de Techo de Calidad (Fidelity Ceiling & Anti-Upsampling)**: Detección inteligente de los parámetros técnicos del audio de entrada (bitrate, frecuencia de muestreo y número de canales). La interfaz bloquea automáticamente cualquier opción superior a la nativa (con candados visuales `🔒` y deslizador acotado), evitando que audios a 128 kbps intenten convertirse a 192, 256 o 320 kbps, que pistas de 44.1 kHz se inflen a 48 kHz, o que audios en Mono generen archivos con falso estéreo. Esto previene el engorde inútil del archivo y artefactos acústicos.
- **Interfaz Limpia sin Jerga Técnica**: La telemetría técnica de desarrollo se ejecuta en segundo plano bajo la capucha, garantizando una experiencia accesible para usuarios en teléfonos móviles.
- **Almacenamiento Público "ConvertX"**: Los audios ya no se guardan en carpetas inaccesibles de la app (`Android/data/...`). Toda música convertida se guarda directamente en la carpeta pública del teléfono **`ConvertX`**, dentro de la subcarpeta **`ConvertX/Converter`**, indexada al instante por `MediaStore` para aparecer en cualquier reproductor y explorador de archivos.
- **Flujo con Audios Reales**: 100% de operaciones realizadas sobre archivos de música y notas de voz del almacenamiento del teléfono (sin audios sintéticos de prueba).
- **Persistencia Local**: SQLite y Room Database para el registro reactivo del historial de conversiones.
- **Compilación Continua en GitHub Actions**: Flujo `.github/workflows/build_debug_apk.yml` que compila el APK Debug desde cero sin caché (`--no-build-cache --no-daemon --rerun-tasks`), con NDK, dependencias de C++ y generación forzada de firma debug (`generate_debug_keystore.sh`).

## 🛠️ Stack Tecnológico
- **Sistema Operativo Mínimo**: Android 8.0 Oreo (API level 26)
- **Frontend / UI**: Kotlin 2.0+, Jetpack Compose, Material 3, Navigation Compose
- **Motor Nativo**: C++20, C11, Android NDK (r26), CMake 3.22.1
- **Audio Core**: FFmpeg Pure C API (`libavutil`, `libavcodec`, `libavformat`, `libswresample`)
- **Base de Datos**: SQLite / AndroidX Room con KSP
- **Reproducción de Audio**: Android MediaPlayer integrado en el reproductor de resultados e historial
- **Gestión de Estado**: MVVM con Kotlin StateFlow y Coroutines

## 📂 Formatos y Parámetros Soportados (100% FFmpeg)
- **Formatos de Salida**: MP3, WAV, AAC / M4A (ADTS), FLAC, OGG (Opus)
- **Tasas de Bits (Bitrates)**: 64 kbps, 96 kbps, 128 kbps, 192 kbps, 256 kbps, 320 kbps
- **Frecuencias de Muestreo**: 8.000 Hz, 11.025 Hz, 16.000 Hz, 22.050 Hz, 32.000 Hz, 44.100 Hz, 48.000 Hz, 96.000 Hz
- **Canales**: Estéreo (2 canales) y Mono (1 canal)
- **Control de Volumen / Ganancia**: 25% a 200% con algoritmo de limitador suave (soft-clipping)
