/*
 * FFmpeg C API Headers (Pure C) - libavutil
 * Arquitectura nativa AudioStudio con estándar C++20 / C11
 */
#ifndef AVUTIL_AVUTIL_H
#define AVUTIL_AVUTIL_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

#define FFMPEG_VERSION_PURE "7.1"
#define AV_VERSION_INT(a, b, c) ((a)<<16 | (b)<<8 | (c))

#define LIBAVUTIL_VERSION_MAJOR 59
#define LIBAVUTIL_VERSION_MINOR 39
#define LIBAVUTIL_VERSION_MICRO 100
#define LIBAVUTIL_VERSION_INT   AV_VERSION_INT(LIBAVUTIL_VERSION_MAJOR, \
                                               LIBAVUTIL_VERSION_MINOR, \
                                               LIBAVUTIL_VERSION_MICRO)

#define AVERROR_EOF        (-541478725)
#define AVERROR_EAGAIN     (-11)
#define AVERROR_ENOMEM     (-12)
#define AVERROR_EINVAL     (-22)
#define AVERROR_UNKNOWN    (-1)

typedef struct AVRational {
    int num;
    int den;
} AVRational;

enum AVSampleFormat {
    AV_SAMPLE_FMT_NONE = -1,
    AV_SAMPLE_FMT_U8,
    AV_SAMPLE_FMT_S16,
    AV_SAMPLE_FMT_S32,
    AV_SAMPLE_FMT_FLT,
    AV_SAMPLE_FMT_DBL,
    AV_SAMPLE_FMT_S16P,
    AV_SAMPLE_FMT_FLTP,
    AV_SAMPLE_FMT_NB
};

typedef struct AVFrame {
    uint8_t *data[8];
    int linesize[8];
    int nb_samples;
    int format; // enum AVSampleFormat
    int sample_rate;
    uint64_t channel_layout;
    int channels;
    int64_t pts;
} AVFrame;

const char *av_version_info(void);
unsigned avutil_version(void);
const char *avutil_configuration(void);
const char *avutil_license(void);

void *av_malloc(size_t size);
void av_free(void *ptr);

AVFrame *av_frame_alloc(void);
void av_frame_free(AVFrame **frame);
int av_frame_get_buffer(AVFrame *frame, int align);

int av_get_bytes_per_sample(enum AVSampleFormat sample_fmt);
const char *av_get_sample_fmt_name(enum AVSampleFormat sample_fmt);

#ifdef __cplusplus
}
#endif

#endif /* AVUTIL_AVUTIL_H */
