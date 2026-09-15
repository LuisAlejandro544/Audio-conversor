# 🧠 AI Context & Reglas del Proyecto

## 🎯 Directrices Principales
- **Proyecto**: AudioStudio (Conversor y Suite de Audio para Android).
- **Versión Mínima de Android**: Android 8.0 Oreo (API 26) o superior (`minSdk = 26`).
- **Entorno del Usuario**: Teléfono móvil (sin PC disponible).
- **Distribución**: APK para plataformas de terceros y Uptodown (no sujeto a restricciones innecesarias de Google Play).
- **Motor de Audio**: FFmpeg Pure C + C++20 NDK activo y transcodificando al 100% bajo la capucha (sin exponer jerga técnica al usuario final).

## 🚨 Reglas Críticas
1. **Conversión 100% con FFmpeg**: Toda la transcodificación de audio (cambio de formato, tasa de bits/bitrate, frecuencia de muestreo, canales y volumen) se realiza directamente a través de la API pura en C de FFmpeg (`libavcodec`, `libavformat`, `libswresample`, `libavutil`). Queda estrictamente prohibido volver a utilizar `MediaCodec` o `MediaMuxer` para el proceso de conversión de audio.
2. **FFmpeg Puro en C (libav*)**: NUNCA usar `ffmpeg-kit` ni wrappers intermediarios abandonados. Invocar y mantener la API nativa pura en C de FFmpeg.
3. **Estándar C++**: Usar estrictamente C++20 (`set(CMAKE_CXX_STANDARD 20)`) con gestión de memoria RAII (`raii_wrappers.hpp`).
4. **Repositorio Ligero**: NUNCA commitear archivos binarios `.so` a git. Mantenerlos en `.gitignore`.
5. **Soporte Multi-Arquitectura**: Compilación garantizada tanto para arquitecturas de 32 bits (`armeabi-v7a`, `x86`) como de 64 bits (`arm64-v8a`, `x86_64`).
6. **Diseño de Interfaz y Experiencia de Usuario**:
   - No minimalismo extremo. Mantener pantallas divididas y modulares.
   - Herramientas sin lógica completa deben mostrarse bloqueadas con candado y aviso explicativo en diálogo.
   - No exponer telemetría ni jerga de bajo nivel (como C++, CMake, NDK) en la interfaz principal del usuario.
7. **Idioma**: Archivos de commit (`commit_message.txt`), comentarios de código y documentación técnica en español.
8. **Integración en Gradle**: El bloque `externalNativeBuild` con CMake debe permanecer configurado y validado en `app/build.gradle.kts`.
9. **Almacenamiento Público "ConvertX" con Subcarpetas**: Queda estrictamente prohibido guardar los archivos de audio convertidos en rutas privadas o restringidas como `Android/data/...`. Toda pista generada se guarda de forma accesible en la carpeta pública del teléfono `"ConvertX"`, con subcarpetas organizadas según la herramienta (subcarpeta actual para el convertidor: `"ConvertX/Converter"`). Esto garantiza que el usuario desde su teléfono móvil acceda a sus audios con cualquier explorador de archivos o reproductor sin trabas de permisos.
10. **Compilación CI en GitHub Actions y Firma Debug desde 0**: Se mantiene el flujo de GitHub Actions sin caché para generar el APK Debug con compilación limpia desde cero (`--no-build-cache --no-daemon --rerun-tasks`), junto con el script `generate_debug_keystore.sh` que obliga a generar una firma debug desde cero de forma no interactiva.
11. **Techo Técnico de Fidelidad y Protección Anti-Upsampling**: Está terminantemente prohibido permitir en la interfaz o en los modelos de conversión la selección de valores de bitrate, sample rate o canales superiores a los del archivo de audio original (ej. si el origen es de 128 kbps o 44.1 kHz, las opciones de 192/256/320 kbps o 48 kHz deben bloquearse visualmente con candados y en el ViewModel; si la pista es Mono, no se debe permitir duplicar a estéreo falso). Esto previene upsampling destructivo, pérdida de fidelidad y tamaño de archivo inflado innecesariamente.

