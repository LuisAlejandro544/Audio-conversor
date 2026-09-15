/**
 * AudioStudio Native Core - RAII Memory Management (C++20)
 *
 * Encapsula de forma estricta los tipos de datos en C puro de FFmpeg (libav*)
 * utilizando punteros inteligentes de C++20 (std::unique_ptr) con destructores
 * específicos, garantizando liberación determinista de memoria sin fugas.
 */

#ifndef AUDIOSTUDIO_RAII_WRAPPERS_HPP
#define AUDIOSTUDIO_RAII_WRAPPERS_HPP

#include <memory>
#include <span>
#include <concepts>

#include "include/libavutil/avutil.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavformat/avformat.h"
#include "include/libswresample/swresample.h"

namespace audiostudio::raii {

/**
 * Deleter para AVFrame de libavutil.
 */
struct AVFrameDeleter {
    void operator()(AVFrame* frame) const noexcept {
        if (frame != nullptr) {
            av_frame_free(&frame);
        }
    }
};

/**
 * Deleter para AVPacket de libavcodec.
 */
struct AVPacketDeleter {
    void operator()(AVPacket* pkt) const noexcept {
        if (pkt != nullptr) {
            av_packet_free(&pkt);
        }
    }
};

/**
 * Deleter para SwrContext de libswresample.
 */
struct SwrContextDeleter {
    void operator()(SwrContext* swr) const noexcept {
        if (swr != nullptr) {
            swr_free(&swr);
        }
    }
};

/**
 * Deleter para AVFormatContext de libavformat.
 */
struct AVFormatContextDeleter {
    void operator()(AVFormatContext* fmt) const noexcept {
        if (fmt != nullptr) {
            avformat_free_context(fmt);
        }
    }
};

/**
 * Deleter para AVCodecContext de libavcodec.
 */
struct AVCodecContextDeleter {
    void operator()(AVCodecContext* ctx) const noexcept {
        if (ctx != nullptr) {
            avcodec_free_context(&ctx);
        }
    }
};

/**
 * Deleter para AVCodecParameters de libavcodec.
 */
struct AVCodecParametersDeleter {
    void operator()(AVCodecParameters* par) const noexcept {
        if (par != nullptr) {
            avcodec_parameters_free(&par);
        }
    }
};

/**
 * Deleter para AVFormatContext abierto como entrada (avformat_close_input).
 */
struct AVFormatInputCloser {
    void operator()(AVFormatContext* fmt) const noexcept {
        if (fmt != nullptr) {
            avformat_close_input(&fmt);
        }
    }
};

/**
 * Deleter para punteros de archivo de C (FILE*).
 */
struct FileCloser {
    void operator()(FILE* fp) const noexcept {
        if (fp != nullptr) {
            fclose(fp);
        }
    }
};

// Definición de tipos RAII modernos de C++20
using UniqueAVFrame = std::unique_ptr<AVFrame, AVFrameDeleter>;
using UniqueAVPacket = std::unique_ptr<AVPacket, AVPacketDeleter>;
using UniqueSwrContext = std::unique_ptr<SwrContext, SwrContextDeleter>;
using UniqueAVFormatContext = std::unique_ptr<AVFormatContext, AVFormatContextDeleter>;
using UniqueAVFormatInput = std::unique_ptr<AVFormatContext, AVFormatInputCloser>;
using UniqueAVCodecContext = std::unique_ptr<AVCodecContext, AVCodecContextDeleter>;
using UniqueAVCodecParameters = std::unique_ptr<AVCodecParameters, AVCodecParametersDeleter>;
using UniqueFile = std::unique_ptr<FILE, FileCloser>;

/**
 * Fabricador seguro de AVFrame con RAII.
 */
inline UniqueAVFrame make_unique_frame() {
    return UniqueAVFrame(av_frame_alloc());
}

/**
 * Fabricador seguro de AVPacket con RAII.
 */
inline UniqueAVPacket make_unique_packet() {
    return UniqueAVPacket(av_packet_alloc());
}

/**
 * Fabricador seguro de SwrContext con RAII.
 */
inline UniqueSwrContext make_unique_swr() {
    return UniqueSwrContext(swr_alloc());
}

} // namespace audiostudio::raii

#endif // AUDIOSTUDIO_RAII_WRAPPERS_HPP
