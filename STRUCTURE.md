# 🏛️ Estructura del Proyecto (AudioStudio)

## 📂 Árbol de Directorios y Módulos

### 🔧 Motor Nativo en C++20 (`app/src/main/cpp/`)
- `CMakeLists.txt`: Configuración de compilación con C++20 (`-std=c++20`), C11 y optimización `-O3` para arquitecturas `armeabi-v7a`, `x86`, `arm64-v8a` y `x86_64`.
- `ffmpeg_pure_core.h`: API del motor de audio en C puro con definición de `ffmpeg_core_transcode_audio` y callbacks de progreso.
- `ffmpeg_pure_core.c`: Implementación del núcleo transcodificador 100% FFmpeg en C puro (decodificación universal, remuestreo con `libswresample`, mezcla de canales y codificadores directos de MP3, AAC/M4A, WAV, FLAC y OGG/Opus).
- `raii_wrappers.hpp`: Envoltorios RAII de C++20 (`std::unique_ptr` con destructores deterministas) para `AVFrame`, `AVPacket`, `SwrContext`, `AVFormatContext` y descriptores de archivo.
- `native-lib.cpp`: Puente JNI que expone `convertAudioFileNative` y `processAudioNative` con despacho de hilos y progreso en tiempo real hacia Kotlin.
- `include/`: Cabeceras C puras de la API de FFmpeg:
  - `libavutil/avutil.h`: Gestión de memoria, utilidades y estructura `AVFrame`.
  - `libavcodec/avcodec.h`: Estructura `AVPacket`, contextos y descriptores de códecs.
  - `libavformat/avformat.h`: Contextos de contenedores y flujos `AVFormatContext`.
  - `libswresample/swresample.h`: Contexto `SwrContext` para remuestreo y remezcla de canales.

### 🧩 Capa de Lógica de Audio y JNI (`app/src/main/java/com/example/audio/`)
- `ConvertXStorageManager.kt`: Gestor del sistema de almacenamiento público `ConvertX`. Ubica y crea la estructura `ConvertX/Converter` en el almacenamiento del teléfono, registra en MediaStore y refresca `MediaScannerConnection`.
- `NativeAudioEngine.kt`: Conector JNI en Kotlin. Carga `libaudiostudio_native.so` y expone `convertAudioFile(...)` con interfaz `NativeProgressListener`.
- `AudioConverterEngine.kt`: Motor de conversión que orquesta la transcodificación 100% mediante FFmpeg nativo, sin utilizar `MediaCodec` ni `MediaMuxer`, y exporta a `ConvertX/Converter`.
- `AudioExportHelper.kt`: Utilidad de exportación a `ConvertX/Converter` y compartición segura vía `FileProvider`.
- `AudioPlayerController.kt`: Controlador de reproducción con control de estado y seekbar interactivo.
- `AudioFormatType.kt`: Definición tipada de códecs (MP3, AAC, WAV, FLAC, OPUS, OGG).
- `AudioMetadataReader.kt`: Extracción de metadatos de archivos de audio locales del usuario.
- `AudioInfo.kt`: Modelo descriptivo del audio cargado (frecuencia, canales, bitrate, tamaño, duración).

### 🚀 Automatización CI/CD y Herramientas
- `.github/workflows/build_debug_apk.yml`: Flujo de GitHub Actions con checkout, instalación de NDK/CMake/FFmpeg, generación de keystore debug y compilación limpia sin caché (`--no-build-cache --no-daemon --rerun-tasks`).
- `generate_debug_keystore.sh`: Script ejecutable bash que fuerza la generación inmediata y sin esperas de `debug.keystore` con `keytool`.
- `gradlew`: Wrapper ejecutable POSIX para compilar con Gradle en entornos de integración continua y terminales móviles.

### 💾 Persistencia de Datos (`app/src/main/java/com/example/data/local/`)
- `AppDatabase.kt`: Base de datos Room para persistencia local.
- `ConversionDao.kt`: Consultas reactivas (Flow) para historial de conversiones.
- `ConversionEntity.kt`: Entidad de persistencia con metadatos de entrada y salida.

### 🎨 Capa de Presentación Jetpack Compose (`app/src/main/java/com/example/ui/`)
- `MainActivity.kt`: Punto de entrada con `enableEdgeToEdge()` y controlador de navegación.
- `ConverterViewModel.kt`: ViewModel centralizado con StateFlow para el flujo de conversión, incluyendo lógica de clamping y techo técnico de fidelidad (`getMaxAllowedBitrate`, `getMaxAllowedChannels`, `getMaxAllowedSampleRate`).
- `screens/`:
  - `home/HomeScreen.kt`: Pantalla principal inspirada en AudioLab, con carrusel de herramientas recientes, cuadrícula de herramientas con candados explicativos para funciones en desarrollo y barra inferior de navegación.
  - `select/SelectAudioScreen.kt`: Selector de audio local conectado al almacenamiento del dispositivo móvil (100% audios reales).
  - `config/ConvertConfigScreen.kt`: Configuración granular de códec, bitrate (64k-320k), canales, frecuencia y ganancia, con candados visuales inteligentes (`🔒`) y restricción dinámica de rango para no superar los parámetros del archivo original.
  - `progress/ConvertProgressScreen.kt`: Barra de progreso en tiempo real con mensajes nativos de FFmpeg y cancelación.
  - `result/ConvertResultScreen.kt`: Pantalla de éxito con reproductor integrado y opciones para compartir.
  - `history/HistoryScreen.kt`: Historial completo con estadísticas y reproductor individual por elemento.
- `theme/`: Paleta de colores Neón Studio (Superficie oscura, verde neón, cian y violeta) y tipografía Material 3.
