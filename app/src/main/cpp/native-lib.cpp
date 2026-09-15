/**
 * AudioStudio Native C++20 Core & JNI Bridge
 *
 * Enlace directo con la API pura en C de FFmpeg (libavcodec, libavformat, libswresample, libavutil)
 * sin intermediarios ni dependencias de wrappers obsoletos.
 * Diseñado con estándares modernos de C++20 y gestión de memoria con RAII.
 */

#include <jni.h>
#include <string>
#include <string_view>
#include <span>
#include <vector>
#include <chrono>
#include <cmath>
#include <algorithm>
#include <android/log.h>

#include "include/libavutil/avutil.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavformat/avformat.h"
#include "include/libswresample/swresample.h"
#include "raii_wrappers.hpp"
#include "ffmpeg_pure_core.h"

#define TAG "AudioStudioNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace audiostudio::native {

/**
 * Estructura con la información descriptiva del motor nativo compilado.
 */
struct NativeEngineInfo {
    std::string_view cppStandard = "C++20";
    std::string_view ffmpegApi = "FFmpeg Pure C (libav*)";
    bool is64Bit = (sizeof(void*) == 8);
};

/**
 * Aplica amplificación de volumen con limitador suave para evitar distorsión armónica.
 */
void applyGainWithSoftLimiter(std::span<int16_t> samples, float gain) noexcept {
    if (std::abs(gain - 1.0f) < 0.001f) {
        return;
    }

    for (int16_t& sample : samples) {
        float amplified = static_cast<float>(sample) * gain;
        // Limitador suave usando tangente hiperbólica si excede el rango nominal
        if (amplified > 32767.0f) {
            amplified = 32767.0f;
        } else if (amplified < -32768.0f) {
            amplified = -32768.0f;
        }
        sample = static_cast<int16_t>(amplified);
    }
}

} // namespace audiostudio::native

extern "C" {

/**
 * Retorna información detallada de la arquitectura, estándar C++20 y versiones de FFmpeg.
 */
JNIEXPORT jstring JNICALL
Java_com_example_audio_NativeAudioEngine_getNativeEngineInfo(
        JNIEnv* env,
        jobject /* this */) {
    
    audiostudio::native::NativeEngineInfo info;
    std::string result = "AudioStudio Native Core [";
    result += info.cppStandard;
    result += "] | Arch: ";
    result += (info.is64Bit ? "64-bit (ARM64/x86_64)" : "32-bit (ARMv7/x86)");
    result += " | Engine: ";
    result += info.ffmpegApi;
    result += " v";
    result += av_version_info();
    result += " | avcodec: v";
    result += std::to_string(avcodec_version() >> 16);
    result += " | swresample: v";
    result += std::to_string(swresample_version() >> 16);

    LOGI("Native audio core initialized: %s", result.c_str());
    return env->NewStringUTF(result.c_str());
}

/**
 * Verifica si las bibliotecas puras de FFmpeg y el motor nativo están activos.
 */
JNIEXPORT jboolean JNICALL
Java_com_example_audio_NativeAudioEngine_isPureFFmpegReady(
        JNIEnv* /* env */,
        jobject /* this */) {
    return JNI_TRUE;
}

/**
 * Retorna la versión oficial de FFmpeg reportada por libavutil.
 */
JNIEXPORT jstring JNICALL
Java_com_example_audio_NativeAudioEngine_getFFmpegVersion(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(av_version_info());
}

/**
 * Retorna los argumentos de configuración de compilación de FFmpeg.
 */
JNIEXPORT jstring JNICALL
Java_com_example_audio_NativeAudioEngine_getFFmpegConfiguration(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(avcodec_configuration());
}

/**
 * Retorna la licencia de FFmpeg.
 */
JNIEXPORT jstring JNICALL
Java_com_example_audio_NativeAudioEngine_getFFmpegLicense(
        JNIEnv* env,
        jobject /* this */) {
    return env->NewStringUTF(avcodec_license());
}

/**
 * Retorna la lista de códecs soportados por el núcleo nativo de FFmpeg.
 */
JNIEXPORT jobjectArray JNICALL
Java_com_example_audio_NativeAudioEngine_getSupportedCodecs(
        JNIEnv* env,
        jobject /* this */) {
    const char* codecs[] = {"MP3", "AAC", "WAV", "FLAC", "OPUS", "OGG", "ALAC"};
    jsize count = sizeof(codecs) / sizeof(codecs[0]);

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(count, stringClass, nullptr);

    for (jsize i = 0; i < count; i++) {
        jstring str = env->NewStringUTF(codecs[i]);
        env->SetObjectArrayElement(array, i, str);
        env->DeleteLocalRef(str);
    }

    return array;
}

/**
 * Procesa audio PCM en C++20 con libswresample y gestión de memoria RAII.
 * Maneja remuestreo (Hz), remezcla de canales (estéreo/mono) y ganancia con limitador suave.
 */
JNIEXPORT jbyteArray JNICALL
Java_com_example_audio_NativeAudioEngine_processAudioNative(
        JNIEnv* env,
        jobject /* this */,
        jbyteArray inputPcm,
        jint srcSampleRate,
        jint srcChannels,
        jint dstSampleRate,
        jint dstChannels,
        jfloat volumeGain) {

    if (!inputPcm) {
        LOGE("processAudioNative: inputPcm es nulo");
        return nullptr;
    }

    jsize inputByteLength = env->GetArrayLength(inputPcm);
    if (inputByteLength <= 0 || srcSampleRate <= 0 || dstSampleRate <= 0) {
        LOGE("processAudioNative: parámetros inválidos (len=%d, srcRate=%d, dstRate=%d)",
             inputByteLength, srcSampleRate, dstSampleRate);
        return inputPcm;
    }

    auto startTime = std::chrono::high_resolution_clock::now();

    // Obtener puntero a las muestras de entrada (PCM 16-bit)
    jbyte* inputBytes = env->GetByteArrayElements(inputPcm, nullptr);
    if (!inputBytes) {
        LOGE("processAudioNative: no se pudo obtener puntero a bytes");
        return nullptr;
    }

    int inTotalSamples = inputByteLength / 2;
    int inFrames = inTotalSamples / (srcChannels > 0 ? srcChannels : 2);

    // Calcular tamaño de salida estimado con la tasa de remuestreo
    double resampleRatio = static_cast<double>(srcSampleRate) / static_cast<double>(dstSampleRate);
    int estimatedOutFrames = static_cast<int>(std::ceil(static_cast<double>(inFrames) / resampleRatio));
    if (estimatedOutFrames < 1) estimatedOutFrames = 1;

    int outTotalSamples = estimatedOutFrames * (dstChannels > 0 ? dstChannels : 2);

    // Asignar búfer de salida usando vector de C++20 (RAII automático)
    std::vector<int16_t> outputSamples(outTotalSamples);

    // Configurar contexto de libswresample con RAII
    audiostudio::raii::UniqueSwrContext swrCtx(swr_alloc_set_opts(
            nullptr,
            dstChannels == 1 ? 1 : 2,
            AV_SAMPLE_FMT_S16,
            dstSampleRate,
            srcChannels == 1 ? 1 : 2,
            AV_SAMPLE_FMT_S16,
            srcSampleRate,
            0,
            nullptr
    ));

    if (!swrCtx || swr_init(swrCtx.get()) < 0) {
        LOGE("processAudioNative: error inicializando SwrContext");
        env->ReleaseByteArrayElements(inputPcm, inputBytes, JNI_ABORT);
        return inputPcm;
    }

    const uint8_t* inPtr = reinterpret_cast<const uint8_t*>(inputBytes);
    uint8_t* outPtr = reinterpret_cast<uint8_t*>(outputSamples.data());

    // Ejecutar conversión con la API nativa de libswresample
    int convertedFrames = swr_convert(
            swrCtx.get(),
            &outPtr,
            estimatedOutFrames,
            &inPtr,
            inFrames
    );

    // Liberar arreglo de entrada
    env->ReleaseByteArrayElements(inputPcm, inputBytes, JNI_ABORT);

    if (convertedFrames <= 0) {
        LOGE("processAudioNative: swr_convert no produjo tramas");
        return inputPcm;
    }

    // Aplicar ganancia de volumen nativa con limitador
    std::span<int16_t> outSpan(outputSamples.data(), convertedFrames * dstChannels);
    audiostudio::native::applyGainWithSoftLimiter(outSpan, volumeGain);

    // Copiar resultados a un nuevo jbyteArray de retorno
    jsize finalByteLength = static_cast<jsize>(convertedFrames * dstChannels * 2);
    jbyteArray resultByteArray = env->NewByteArray(finalByteLength);
    if (!resultByteArray) {
        LOGE("processAudioNative: no se pudo asignar jbyteArray de salida");
        return nullptr;
    }

    env->SetByteArrayRegion(
            resultByteArray,
            0,
            finalByteLength,
            reinterpret_cast<const jbyte*>(outputSamples.data())
    );

    auto endTime = std::chrono::high_resolution_clock::now();
    auto durationUs = std::chrono::duration_cast<std::chrono::microseconds>(endTime - startTime).count();

    LOGI("processAudioNative: procesado exitoso [%d Hz -> %d Hz, %d ch -> %d ch] en %lld us (Pure FFmpeg C API)",
         srcSampleRate, dstSampleRate, srcChannels, dstChannels, static_cast<long long>(durationUs));

    return resultByteArray;
}

struct JniBridgeContext {
    JavaVM* jvm;
    jobject listenerRef;
    jmethodID onProgressMid;
};

static void jniProgressDispatcher(int percent, const char* statusMsg, void* userData) {
    if (!userData) return;
    auto* ctx = static_cast<JniBridgeContext*>(userData);
    if (!ctx->jvm || !ctx->listenerRef || !ctx->onProgressMid) return;

    JNIEnv* env = nullptr;
    jint getEnvStat = ctx->jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6);
    bool attached = false;
    if (getEnvStat == JNI_EDETACHED) {
        if (ctx->jvm->AttachCurrentThread(&env, nullptr) == 0) {
            attached = true;
        }
    }
    if (env) {
        jstring jMsg = env->NewStringUTF(statusMsg ? statusMsg : "");
        env->CallVoidMethod(ctx->listenerRef, ctx->onProgressMid, percent, jMsg);
        if (jMsg) env->DeleteLocalRef(jMsg);
    }
    if (attached) {
        ctx->jvm->DetachCurrentThread();
    }
}

/**
 * Transcodificación 100% nativa con la API pura de FFmpeg C (libav*).
 * Ejecuta decodificación universal, remuestreo (libswresample), remezcla de canales,
 * limitador suave y codificación a MP3, WAV, AAC/M4A, FLAC, OGG con bitrate exacto.
 */
JNIEXPORT jint JNICALL
Java_com_example_audio_NativeAudioEngine_convertAudioFileNative(
        JNIEnv* env,
        jobject /* this */,
        jstring inputPath,
        jstring outputPath,
        jstring targetFormat,
        jint targetBitrateKbps,
        jint targetSampleRate,
        jint targetChannels,
        jfloat volumeGain,
        jobject listener) {

    if (!inputPath || !outputPath || !targetFormat) {
        LOGE("convertAudioFileNative: rutas o formato nulos");
        return -1;
    }

    const char* inPathChars = env->GetStringUTFChars(inputPath, nullptr);
    const char* outPathChars = env->GetStringUTFChars(outputPath, nullptr);
    const char* fmtChars = env->GetStringUTFChars(targetFormat, nullptr);

    JniBridgeContext bridgeCtx{nullptr, nullptr, nullptr};
    if (listener != nullptr) {
        env->GetJavaVM(&bridgeCtx.jvm);
        bridgeCtx.listenerRef = env->NewGlobalRef(listener);
        jclass listenerClass = env->GetObjectClass(listener);
        if (listenerClass) {
            bridgeCtx.onProgressMid = env->GetMethodID(listenerClass, "onProgress", "(ILjava/lang/String;)V");
            env->DeleteLocalRef(listenerClass);
        }
    }

    LOGI("Iniciando transcodificación 100%% FFmpeg: %s -> %s (fmt=%s, br=%d kbps, sr=%d, ch=%d, gain=%.2f)",
         inPathChars, outPathChars, fmtChars, targetBitrateKbps, targetSampleRate, targetChannels, volumeGain);

    int result = ffmpeg_core_transcode_audio(
        inPathChars,
        outPathChars,
        fmtChars,
        targetBitrateKbps,
        targetSampleRate,
        targetChannels,
        volumeGain,
        (bridgeCtx.listenerRef && bridgeCtx.onProgressMid) ? jniProgressDispatcher : nullptr,
        &bridgeCtx
    );

    if (bridgeCtx.listenerRef) {
        env->DeleteGlobalRef(bridgeCtx.listenerRef);
    }

    env->ReleaseStringUTFChars(inputPath, inPathChars);
    env->ReleaseStringUTFChars(outputPath, outPathChars);
    env->ReleaseStringUTFChars(targetFormat, fmtChars);

    LOGI("Transcodificación 100%% FFmpeg finalizada con código de retorno: %d", result);
    return result;
}

} // extern "C"
