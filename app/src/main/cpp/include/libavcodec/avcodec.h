/*
 * FFmpeg C API Headers (Pure C) - libavcodec
 * Arquitectura nativa AudioStudio con estándar C++20 / C11
 */
#ifndef AVCODEC_AVCODEC_H
#define AVCODEC_AVCODEC_H

#include "../libavutil/avutil.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LIBAVCODEC_VERSION_MAJOR 61
#define LIBAVCODEC_VERSION_MINOR 19
#define LIBAVCODEC_VERSION_MICRO 100
#define LIBAVCODEC_VERSION_INT   AV_VERSION_INT(LIBAVCODEC_VERSION_MAJOR, \
                                                LIBAVCODEC_VERSION_MINOR, \
                                                LIBAVCODEC_VERSION_MICRO)

enum AVCodecID {
    AV_CODEC_ID_NONE = 0,
    AV_CODEC_ID_MP3 = 0x15001,
    AV_CODEC_ID_AAC = 0x15002,
    AV_CODEC_ID_FLAC = 0x1500C,
    AV_CODEC_ID_OPUS = 0x1503C,
    AV_CODEC_ID_VORBIS = 0x15003,
    AV_CODEC_ID_PCM_S16LE = 0x10000,
    AV_CODEC_ID_ALAC = 0x15010
};

typedef struct AVPacket {
    uint8_t *data;
    int size;
    int64_t pts;
    int64_t dts;
    int stream_index;
    int flags;
    int64_t duration;
    int64_t pos;
} AVPacket;

typedef struct AVCodec {
    const char *name;
    const char *long_name;
    enum AVCodecID id;
    int is_encoder;
} AVCodec;

typedef struct AVCodecParameters {
    enum AVCodecID codec_id;
    int sample_rate;
    int channels;
    int bit_rate;
    enum AVSampleFormat format;
} AVCodecParameters;

typedef struct AVCodecContext {
    const AVCodec *codec;
    enum AVCodecID codec_id;
    int bit_rate;
    int sample_rate;
    int channels;
    enum AVSampleFormat sample_fmt;
    void *priv_data;
} AVCodecContext;

unsigned avcodec_version(void);
const char *avcodec_configuration(void);
const char *avcodec_license(void);

AVPacket *av_packet_alloc(void);
void av_packet_free(AVPacket **pkt);
void av_packet_unref(AVPacket *pkt);

const AVCodec *avcodec_find_decoder(enum AVCodecID id);
const AVCodec *avcodec_find_encoder(enum AVCodecID id);
AVCodecContext *avcodec_alloc_context3(const AVCodec *codec);
void avcodec_free_context(AVCodecContext **avctx);

AVCodecParameters *avcodec_parameters_alloc(void);
void avcodec_parameters_free(AVCodecParameters **par);

int avcodec_open2(AVCodecContext *avctx, const AVCodec *codec, void **options);
int avcodec_close(AVCodecContext *avctx);

int avcodec_send_packet(AVCodecContext *avctx, const AVPacket *avpkt);
int avcodec_receive_frame(AVCodecContext *avctx, AVFrame *frame);

int avcodec_send_frame(AVCodecContext *avctx, const AVFrame *frame);
int avcodec_receive_packet(AVCodecContext *avctx, AVPacket *avpkt);

int avcodec_parameters_to_context(AVCodecContext *codec, const AVCodecParameters *par);
int avcodec_parameters_from_context(AVCodecParameters *par, const AVCodecContext *codec);

#ifdef __cplusplus
}
#endif

#endif /* AVCODEC_AVCODEC_H */
