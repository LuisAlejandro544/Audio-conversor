/*
 * FFmpeg C API Headers (Pure C) - libavformat
 * Arquitectura nativa AudioStudio con estándar C++20 / C11
 */
#ifndef AVFORMAT_AVFORMAT_H
#define AVFORMAT_AVFORMAT_H

#include "../libavcodec/avcodec.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LIBAVFORMAT_VERSION_MAJOR 61
#define LIBAVFORMAT_VERSION_MINOR 7
#define LIBAVFORMAT_VERSION_MICRO 100
#define LIBAVFORMAT_VERSION_INT   AV_VERSION_INT(LIBAVFORMAT_VERSION_MAJOR, \
                                                 LIBAVFORMAT_VERSION_MINOR, \
                                                 LIBAVFORMAT_VERSION_MICRO)

typedef struct AVInputFormat {
    const char *name;
    const char *long_name;
    int flags;
} AVInputFormat;

typedef struct AVOutputFormat {
    const char *name;
    const char *long_name;
    const char *extensions;
    enum AVCodecID audio_codec;
} AVOutputFormat;

typedef struct AVStream {
    int index;
    int id;
    AVCodecParameters *codecpar;
    int64_t duration;
    int64_t nb_frames;
    AVRational time_base;
} AVStream;

typedef struct AVFormatContext {
    const char *filename;
    unsigned int nb_streams;
    AVStream **streams;
    int64_t duration;
    int64_t bit_rate;
    const AVOutputFormat *oformat;
    const AVInputFormat *iformat;
    void *pb;
} AVFormatContext;

unsigned avformat_version(void);
const char *avformat_configuration(void);
const char *avformat_license(void);

AVFormatContext *avformat_alloc_context(void);
void avformat_free_context(AVFormatContext *s);
int avformat_open_input(AVFormatContext **ps, const char *url, const AVInputFormat *fmt, void **options);
int avformat_find_stream_info(AVFormatContext *ic, void **options);
int av_read_frame(AVFormatContext *s, AVPacket *pkt);
void avformat_close_input(AVFormatContext **s);

int avformat_alloc_output_context2(AVFormatContext **ctx, const AVOutputFormat *oformat, const char *format_name, const char *filename);
AVStream *avformat_new_stream(AVFormatContext *s, const AVCodec *c);
int avformat_write_header(AVFormatContext *s, void **options);
int av_interleaved_write_frame(AVFormatContext *s, AVPacket *pkt);
int av_write_trailer(AVFormatContext *s);

#ifdef __cplusplus
}
#endif

#endif /* AVFORMAT_AVFORMAT_H */
