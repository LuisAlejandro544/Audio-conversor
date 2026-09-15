/*
 * FFmpeg C API Headers (Pure C) - libswresample
 * Arquitectura nativa AudioStudio con estándar C++20 / C11
 */
#ifndef SWRESAMPLE_SWRESAMPLE_H
#define SWRESAMPLE_SWRESAMPLE_H

#include "../libavutil/avutil.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LIBSWRESAMPLE_VERSION_MAJOR 5
#define LIBSWRESAMPLE_VERSION_MINOR 3
#define LIBSWRESAMPLE_VERSION_MICRO 100
#define LIBSWRESAMPLE_VERSION_INT   AV_VERSION_INT(LIBSWRESAMPLE_VERSION_MAJOR, \
                                                   LIBSWRESAMPLE_VERSION_MINOR, \
                                                   LIBSWRESAMPLE_VERSION_MICRO)

typedef struct SwrContext SwrContext;

unsigned swresample_version(void);
const char *swresample_configuration(void);
const char *swresample_license(void);

SwrContext *swr_alloc(void);
int swr_init(SwrContext *s);
void swr_free(SwrContext **s);

int swr_convert(SwrContext *s,
                uint8_t **out, int out_count,
                const uint8_t **in, int in_count);

SwrContext *swr_alloc_set_opts(SwrContext *s,
                               int64_t out_ch_layout, enum AVSampleFormat out_sample_fmt, int out_sample_rate,
                               int64_t in_ch_layout, enum AVSampleFormat in_sample_fmt, int in_sample_rate,
                               int log_offset, void *log_ctx);

#ifdef __cplusplus
}
#endif

#endif /* SWRESAMPLE_SWRESAMPLE_H */
