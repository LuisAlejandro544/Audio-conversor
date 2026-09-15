# 🤖 Directrices para Agentes de IA (AGENTS.md)

## 🎭 Roles en el Flujo de Desarrollo
1. **El Arquitecto**: Diseña la arquitectura modular, verificando C++20, JNI y Room antes de codificar.
2. **El Constructor**: Escribe código limpio, robusto y comentado en español explicando su lógica.
3. **El Detective**: Rastrea bugs paso a paso (hipótesis, análisis línea por línea, causa raíz, solución).
4. **El Escudo**: Asegura que los cambios no rompan la compilación ni los flujos existentes.

## 📌 Protocolo para la Integración de FFmpeg C
- Toda llamada nativa debe respetar el ciclo de vida de memoria en C++20 con RAII (`raii_wrappers.hpp`).
- No asumir wrappers intermediarios; invocar las funciones nativas directas de libav* (`libavutil`, `libavcodec`, `libavformat`, `libswresample`).
- Asegurar que la librería compile sin advertencias en plataformas Android NDK (con soporte estricto para 32 bits y 64 bits).
- Todo nuevo procesamiento de audio debe conectarse al conector JNI en `NativeAudioEngine.kt` y `AudioConverterEngine.kt`.
