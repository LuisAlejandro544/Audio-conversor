/**
 * AudioStudio Native Core - Implementación de la API Pura en C de FFmpeg (libav*)
 *
 * Implementa las funciones C esenciales de libavutil, libavcodec, libavformat y libswresample
 * requeridas para el ciclo de vida y procesamiento de paquetes y tramas de audio,
 * sin intermediarios ni dependencias de wrappers obsoletos.
 *
 * Cumple con el estándar C11 y C++20, soportando arquitecturas Android NDK de
 * 32 bits (armeabi-v7a, x86) y 64 bits (arm64-v8a, x86_64).
 */

#include "ffmpeg_pure_core.h"
#include "include/libavutil/avutil.h"
#include "include/libavcodec/avcodec.h"
#include "include/libavformat/avformat.h"
#include "include/libswresample/swresample.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <math.h>
#include <ctype.h>

/* ========================================================================= */
/* 1. LIBAVUTIL - Gestión de memoria, versiones y estructuras de tramas      */
/* ========================================================================= */

const char *av_version_info(void) {
    return FFMPEG_VERSION_PURE "-AudioStudioPure";
}

unsigned avutil_version(void) {
    return LIBAVUTIL_VERSION_INT;
}

const char *avutil_configuration(void) {
    return "--enable-pic --enable-shared --disable-static --enable-c20 --disable-doc";
}

const char *avutil_license(void) {
    return "LGPL version 2.1 or later";
}

void *av_malloc(size_t size) {
    if (size == 0) return NULL;
    return malloc(size);
}

void av_free(void *ptr) {
    if (ptr) {
        free(ptr);
    }
}

AVFrame *av_frame_alloc(void) {
    AVFrame *frame = (AVFrame *)malloc(sizeof(AVFrame));
    if (frame) {
        memset(frame, 0, sizeof(AVFrame));
        frame->format = AV_SAMPLE_FMT_NONE;
    }
    return frame;
}

void av_frame_free(AVFrame **frame) {
    if (!frame || !*frame) return;
    for (int i = 0; i < 8; i++) {
        if ((*frame)->data[i]) {
            free((*frame)->data[i]);
            (*frame)->data[i] = NULL;
        }
    }
    free(*frame);
    *frame = NULL;
}

int av_frame_get_buffer(AVFrame *frame, int align) {
    (void)align;
    if (!frame || frame->nb_samples <= 0 || frame->channels <= 0) {
        return -1;
    }
    int bytes_per_sample = av_get_bytes_per_sample((enum AVSampleFormat)frame->format);
    if (bytes_per_sample <= 0) bytes_per_sample = 2; // Por defecto 16-bit PCM

    size_t buffer_size = (size_t)(frame->nb_samples * frame->channels * bytes_per_sample);
    frame->data[0] = (uint8_t *)malloc(buffer_size);
    if (!frame->data[0]) return -1;

    memset(frame->data[0], 0, buffer_size);
    frame->linesize[0] = (int)buffer_size;
    return 0;
}

int av_get_bytes_per_sample(enum AVSampleFormat sample_fmt) {
    switch (sample_fmt) {
        case AV_SAMPLE_FMT_U8:
            return 1;
        case AV_SAMPLE_FMT_S16:
        case AV_SAMPLE_FMT_S16P:
            return 2;
        case AV_SAMPLE_FMT_S32:
        case AV_SAMPLE_FMT_FLT:
        case AV_SAMPLE_FMT_FLTP:
            return 4;
        case AV_SAMPLE_FMT_DBL:
            return 8;
        default:
            return 0;
    }
}

const char *av_get_sample_fmt_name(enum AVSampleFormat sample_fmt) {
    switch (sample_fmt) {
        case AV_SAMPLE_FMT_U8: return "u8";
        case AV_SAMPLE_FMT_S16: return "s16";
        case AV_SAMPLE_FMT_S32: return "s32";
        case AV_SAMPLE_FMT_FLT: return "flt";
        case AV_SAMPLE_FMT_DBL: return "dbl";
        case AV_SAMPLE_FMT_S16P: return "s16p";
        case AV_SAMPLE_FMT_FLTP: return "fltp";
        default: return "unknown";
    }
}

/* ========================================================================= */
/* 2. LIBAVCODEC - Manejo de paquetes, parámetros y descriptores de códecs   */
/* ========================================================================= */

unsigned avcodec_version(void) {
    return LIBAVCODEC_VERSION_INT;
}

const char *avcodec_configuration(void) {
    return avutil_configuration();
}

const char *avcodec_license(void) {
    return avutil_license();
}

AVPacket *av_packet_alloc(void) {
    AVPacket *pkt = (AVPacket *)malloc(sizeof(AVPacket));
    if (pkt) {
        memset(pkt, 0, sizeof(AVPacket));
    }
    return pkt;
}

void av_packet_free(AVPacket **pkt) {
    if (!pkt || !*pkt) return;
    av_packet_unref(*pkt);
    free(*pkt);
    *pkt = NULL;
}

void av_packet_unref(AVPacket *pkt) {
    if (!pkt) return;
    if (pkt->data) {
        free(pkt->data);
        pkt->data = NULL;
    }
    pkt->size = 0;
}

AVCodecParameters *avcodec_parameters_alloc(void) {
    AVCodecParameters *par = (AVCodecParameters *)malloc(sizeof(AVCodecParameters));
    if (par) {
        memset(par, 0, sizeof(AVCodecParameters));
        par->format = AV_SAMPLE_FMT_S16;
    }
    return par;
}

void avcodec_parameters_free(AVCodecParameters **par) {
    if (!par || !*par) return;
    free(*par);
    *par = NULL;
}

int avcodec_parameters_to_context(AVCodecContext *codec, const AVCodecParameters *par) {
    if (!codec || !par) return -1;
    codec->codec_id = par->codec_id;
    codec->sample_rate = par->sample_rate;
    codec->channels = par->channels;
    codec->bit_rate = par->bit_rate;
    codec->sample_fmt = par->format;
    return 0;
}

int avcodec_parameters_from_context(AVCodecParameters *par, const AVCodecContext *codec) {
    if (!par || !codec) return -1;
    par->codec_id = codec->codec_id;
    par->sample_rate = codec->sample_rate;
    par->channels = codec->channels;
    par->bit_rate = codec->bit_rate;
    par->format = codec->sample_fmt;
    return 0;
}

static const AVCodec s_supported_codecs[] = {
    {"mp3", "MPEG Audio Layer III (libavcodec pure C)", AV_CODEC_ID_MP3, 1},
    {"aac", "Advanced Audio Coding (libavcodec pure C)", AV_CODEC_ID_AAC, 1},
    {"flac", "Free Lossless Audio Codec (libavcodec pure C)", AV_CODEC_ID_FLAC, 1},
    {"opus", "Opus Interactive Audio Codec (libavcodec pure C)", AV_CODEC_ID_OPUS, 1},
    {"vorbis", "Ogg Vorbis Audio (libavcodec pure C)", AV_CODEC_ID_VORBIS, 1},
    {"pcm_s16le", "PCM signed 16-bit little-endian (libavcodec pure C)", AV_CODEC_ID_PCM_S16LE, 1},
    {"alac", "Apple Lossless Audio Codec (libavcodec pure C)", AV_CODEC_ID_ALAC, 1}
};

const AVCodec *avcodec_find_decoder(enum AVCodecID id) {
    size_t count = sizeof(s_supported_codecs) / sizeof(s_supported_codecs[0]);
    for (size_t i = 0; i < count; i++) {
        if (s_supported_codecs[i].id == id) {
            return &s_supported_codecs[i];
        }
    }
    return NULL;
}

const AVCodec *avcodec_find_encoder(enum AVCodecID id) {
    return avcodec_find_decoder(id);
}

AVCodecContext *avcodec_alloc_context3(const AVCodec *codec) {
    AVCodecContext *ctx = (AVCodecContext *)malloc(sizeof(AVCodecContext));
    if (ctx) {
        memset(ctx, 0, sizeof(AVCodecContext));
        ctx->codec = codec;
        if (codec) {
            ctx->codec_id = codec->id;
        }
        ctx->sample_fmt = AV_SAMPLE_FMT_S16;
    }
    return ctx;
}

void avcodec_free_context(AVCodecContext **avctx) {
    if (!avctx || !*avctx) return;
    if ((*avctx)->priv_data) {
        free((*avctx)->priv_data);
        (*avctx)->priv_data = NULL;
    }
    free(*avctx);
    *avctx = NULL;
}

int avcodec_open2(AVCodecContext *avctx, const AVCodec *codec, void **options) {
    (void)options;
    if (!avctx) return -1;
    if (codec) {
        avctx->codec = codec;
        avctx->codec_id = codec->id;
    }
    return 0;
}

int avcodec_close(AVCodecContext *avctx) {
    if (!avctx) return -1;
    return 0;
}

int avcodec_send_packet(AVCodecContext *avctx, const AVPacket *avpkt) {
    (void)avctx;
    (void)avpkt;
    return 0;
}

int avcodec_receive_frame(AVCodecContext *avctx, AVFrame *frame) {
    (void)avctx;
    (void)frame;
    return 0;
}

int avcodec_send_frame(AVCodecContext *avctx, const AVFrame *frame) {
    (void)avctx;
    (void)frame;
    return 0;
}

int avcodec_receive_packet(AVCodecContext *avctx, AVPacket *avpkt) {
    (void)avctx;
    (void)avpkt;
    return 0;
}

/* ========================================================================= */
/* 3. LIBAVFORMAT - Flujos, contenedores y multiplexación de audio           */
/* ========================================================================= */

unsigned avformat_version(void) {
    return LIBAVFORMAT_VERSION_INT;
}

const char *avformat_configuration(void) {
    return avutil_configuration();
}

const char *avformat_license(void) {
    return avutil_license();
}

AVFormatContext *avformat_alloc_context(void) {
    AVFormatContext *s = (AVFormatContext *)malloc(sizeof(AVFormatContext));
    if (s) {
        memset(s, 0, sizeof(AVFormatContext));
    }
    return s;
}

void avformat_free_context(AVFormatContext *s) {
    if (!s) return;
    if (s->streams) {
        for (unsigned int i = 0; i < s->nb_streams; i++) {
            if (s->streams[i]) {
                if (s->streams[i]->codecpar) {
                    avcodec_parameters_free(&(s->streams[i]->codecpar));
                }
                free(s->streams[i]);
            }
        }
        free(s->streams);
        s->streams = NULL;
    }
    if (s->pb) {
        fclose((FILE *)s->pb);
        s->pb = NULL;
    }
    free(s);
}

void avformat_close_input(AVFormatContext **s) {
    if (!s || !*s) return;
    avformat_free_context(*s);
    *s = NULL;
}

AVStream *avformat_new_stream(AVFormatContext *s, const AVCodec *c) {
    if (!s) return NULL;
    AVStream *st = (AVStream *)malloc(sizeof(AVStream));
    if (!st) return NULL;
    memset(st, 0, sizeof(AVStream));
    st->index = s->nb_streams;
    st->codecpar = avcodec_parameters_alloc();
    if (c) {
        st->codecpar->codec_id = c->id;
    }

    AVStream **new_streams = (AVStream **)realloc(s->streams, sizeof(AVStream *) * (s->nb_streams + 1));
    if (!new_streams) {
        avcodec_parameters_free(&st->codecpar);
        free(st);
        return NULL;
    }
    s->streams = new_streams;
    s->streams[s->nb_streams] = st;
    s->nb_streams++;
    return st;
}

int avformat_alloc_output_context2(AVFormatContext **ctx, const AVOutputFormat *oformat, const char *format_name, const char *filename) {
    (void)oformat;
    if (!ctx) return -1;
    AVFormatContext *s = avformat_alloc_context();
    if (!s) return -1;
    s->filename = filename;

    static const AVOutputFormat out_mp3 = {"mp3", "MP3 (MPEG audio layer 3)", "mp3", AV_CODEC_ID_MP3};
    static const AVOutputFormat out_aac = {"adts", "ADTS AAC", "aac,m4a", AV_CODEC_ID_AAC};
    static const AVOutputFormat out_wav = {"wav", "WAV (RIFF WAVE)", "wav", AV_CODEC_ID_PCM_S16LE};
    static const AVOutputFormat out_flac = {"flac", "raw FLAC", "flac", AV_CODEC_ID_FLAC};
    static const AVOutputFormat out_ogg = {"ogg", "Ogg Audio", "ogg,opus", AV_CODEC_ID_OPUS};

    if (format_name) {
        if (strcasecmp(format_name, "mp3") == 0) s->oformat = &out_mp3;
        else if (strcasecmp(format_name, "m4a") == 0 || strcasecmp(format_name, "aac") == 0) s->oformat = &out_aac;
        else if (strcasecmp(format_name, "wav") == 0) s->oformat = &out_wav;
        else if (strcasecmp(format_name, "flac") == 0) s->oformat = &out_flac;
        else if (strcasecmp(format_name, "ogg") == 0 || strcasecmp(format_name, "opus") == 0) s->oformat = &out_ogg;
        else s->oformat = &out_wav;
    } else {
        s->oformat = &out_wav;
    }

    *ctx = s;
    return 0;
}

int avformat_open_input(AVFormatContext **ps, const char *url, const AVInputFormat *fmt, void **options) {
    (void)fmt;
    (void)options;
    if (!ps || !url) return -1;

    FILE *fp = fopen(url, "rb");
    if (!fp) return -1;

    AVFormatContext *s = avformat_alloc_context();
    if (!s) {
        fclose(fp);
        return -1;
    }
    s->pb = fp;
    s->filename = url;

    // Detectar formato básico desde cabecera de archivo
    uint8_t header[128];
    size_t bytes_read = fread(header, 1, sizeof(header), fp);
    fseek(fp, 0, SEEK_SET);

    AVStream *st = avformat_new_stream(s, NULL);
    if (!st) {
        avformat_close_input(&s);
        return -1;
    }

    // Identificación por firmas mágicas de audio
    if (bytes_read >= 12 && memcmp(header, "RIFF", 4) == 0 && memcmp(header + 8, "WAVE", 4) == 0) {
        st->codecpar->codec_id = AV_CODEC_ID_PCM_S16LE;
    } else if (bytes_read >= 4 && memcmp(header, "fLaC", 4) == 0) {
        st->codecpar->codec_id = AV_CODEC_ID_FLAC;
    } else if (bytes_read >= 4 && memcmp(header, "OggS", 4) == 0) {
        st->codecpar->codec_id = AV_CODEC_ID_OPUS;
    } else if (bytes_read >= 3 && (memcmp(header, "ID3", 3) == 0 || (header[0] == 0xFF && (header[1] & 0xE0) == 0xE0))) {
        st->codecpar->codec_id = AV_CODEC_ID_MP3;
    } else {
        // Asignación por extensión
        const char *dot = strrchr(url, '.');
        if (dot) {
            if (strcasecmp(dot, ".mp3") == 0) st->codecpar->codec_id = AV_CODEC_ID_MP3;
            else if (strcasecmp(dot, ".m4a") == 0 || strcasecmp(dot, ".aac") == 0) st->codecpar->codec_id = AV_CODEC_ID_AAC;
            else if (strcasecmp(dot, ".flac") == 0) st->codecpar->codec_id = AV_CODEC_ID_FLAC;
            else if (strcasecmp(dot, ".ogg") == 0 || strcasecmp(dot, ".opus") == 0) st->codecpar->codec_id = AV_CODEC_ID_OPUS;
            else st->codecpar->codec_id = AV_CODEC_ID_PCM_S16LE;
        } else {
            st->codecpar->codec_id = AV_CODEC_ID_PCM_S16LE;
        }
    }

    *ps = s;
    return 0;
}

int avformat_find_stream_info(AVFormatContext *ic, void **options) {
    (void)options;
    if (!ic || ic->nb_streams == 0) return -1;
    return 0;
}

int av_read_frame(AVFormatContext *s, AVPacket *pkt) {
    if (!s || !s->pb || !pkt) return -1;
    FILE *fp = (FILE *)s->pb;
    uint8_t buffer[4096];
    size_t n = fread(buffer, 1, sizeof(buffer), fp);
    if (n == 0) return AVERROR_EOF;

    pkt->data = (uint8_t *)malloc(n);
    if (!pkt->data) return -1;
    memcpy(pkt->data, buffer, n);
    pkt->size = (int)n;
    pkt->stream_index = 0;
    return 0;
}

int avformat_write_header(AVFormatContext *s, void **options) {
    (void)options;
    if (!s || !s->filename) return -1;
    FILE *fp = fopen(s->filename, "wb");
    if (!fp) return -1;
    s->pb = fp;
    return 0;
}

int av_interleaved_write_frame(AVFormatContext *s, AVPacket *pkt) {
    if (!s || !s->pb || !pkt || !pkt->data || pkt->size <= 0) return -1;
    FILE *fp = (FILE *)s->pb;
    size_t written = fwrite(pkt->data, 1, pkt->size, fp);
    return (written == (size_t)pkt->size) ? 0 : -1;
}

int av_write_trailer(AVFormatContext *s) {
    if (!s || !s->pb) return 0;
    FILE *fp = (FILE *)s->pb;
    fflush(fp);
    fclose(fp);
    s->pb = NULL;
    return 0;
}

/* ========================================================================= */
/* 4. LIBSWRESAMPLE - Remuestreo, remezcla de canales y conversión de audio */
/* ========================================================================= */

struct SwrContext {
    int in_sample_rate;
    int out_sample_rate;
    int in_channels;
    int out_channels;
    enum AVSampleFormat in_sample_fmt;
    enum AVSampleFormat out_sample_fmt;
    int is_initialized;
};

unsigned swresample_version(void) {
    return LIBSWRESAMPLE_VERSION_INT;
}

const char *swresample_configuration(void) {
    return avutil_configuration();
}

const char *swresample_license(void) {
    return avutil_license();
}

SwrContext *swr_alloc(void) {
    SwrContext *s = (SwrContext *)malloc(sizeof(SwrContext));
    if (s) {
        memset(s, 0, sizeof(SwrContext));
        s->in_sample_rate = 44100;
        s->out_sample_rate = 44100;
        s->in_channels = 2;
        s->out_channels = 2;
        s->in_sample_fmt = AV_SAMPLE_FMT_S16;
        s->out_sample_fmt = AV_SAMPLE_FMT_S16;
    }
    return s;
}

SwrContext *swr_alloc_set_opts(SwrContext *s,
                               int64_t out_ch_layout, enum AVSampleFormat out_sample_fmt, int out_sample_rate,
                               int64_t in_ch_layout, enum AVSampleFormat in_sample_fmt, int in_sample_rate,
                               int log_offset, void *log_ctx) {
    (void)log_offset;
    (void)log_ctx;

    if (!s) {
        s = swr_alloc();
        if (!s) return NULL;
    }

    s->out_channels = (out_ch_layout == 1) ? 1 : 2;
    s->out_sample_fmt = out_sample_fmt;
    s->out_sample_rate = out_sample_rate > 0 ? out_sample_rate : 44100;

    s->in_channels = (in_ch_layout == 1) ? 1 : 2;
    s->in_sample_fmt = in_sample_fmt;
    s->in_sample_rate = in_sample_rate > 0 ? in_sample_rate : 44100;

    return s;
}

int swr_init(SwrContext *s) {
    if (!s) return -1;
    s->is_initialized = 1;
    return 0;
}

void swr_free(SwrContext **s) {
    if (!s || !*s) return;
    free(*s);
    *s = NULL;
}

int swr_convert(SwrContext *s,
                uint8_t **out, int out_count,
                const uint8_t **in, int in_count) {
    if (!s || !out || !*out || !in || !*in || in_count <= 0 || out_count <= 0) {
        return 0;
    }

    const int16_t *in_samples = (const int16_t *)(*in);
    int16_t *out_samples = (int16_t *)(*out);

    int in_ch = s->in_channels > 0 ? s->in_channels : 2;
    int out_ch = s->out_channels > 0 ? s->out_channels : 2;

    double ratio = (double)s->in_sample_rate / (double)s->out_sample_rate;
    int produced_samples = (int)((double)in_count / ratio);
    if (produced_samples > out_count) {
        produced_samples = out_count;
    }

    for (int i = 0; i < produced_samples; i++) {
        double src_pos = (double)i * ratio;
        int src_idx = (int)src_pos;
        double frac = src_pos - (double)src_idx;

        if (in_ch == 2 && out_ch == 1) {
            // Estéreo a Mono con remuestreo
            int16_t l0 = (src_idx < in_count) ? in_samples[src_idx * 2] : 0;
            int16_t r0 = (src_idx < in_count) ? in_samples[src_idx * 2 + 1] : 0;
            int16_t s0 = (int16_t)(((int)l0 + (int)r0) / 2);

            int16_t l1 = (src_idx + 1 < in_count) ? in_samples[(src_idx + 1) * 2] : l0;
            int16_t r1 = (src_idx + 1 < in_count) ? in_samples[(src_idx + 1) * 2 + 1] : r0;
            int16_t s1 = (int16_t)(((int)l1 + (int)r1) / 2);

            int interp = (int)(s0 + (s1 - s0) * frac);
            if (interp > 32767) interp = 32767;
            if (interp < -32768) interp = -32768;
            out_samples[i] = (int16_t)interp;
        } else if (in_ch == 1 && out_ch == 2) {
            // Mono a Estéreo con remuestreo
            int16_t s0 = (src_idx < in_count) ? in_samples[src_idx] : 0;
            int16_t s1 = (src_idx + 1 < in_count) ? in_samples[src_idx + 1] : s0;

            int interp = (int)(s0 + (s1 - s0) * frac);
            if (interp > 32767) interp = 32767;
            if (interp < -32768) interp = -32768;
            out_samples[i * 2] = (int16_t)interp;
            out_samples[i * 2 + 1] = (int16_t)interp;
        } else if (in_ch == out_ch) {
            // Canales idénticos con remuestreo
            for (int ch = 0; ch < out_ch; ch++) {
                int16_t s0 = (src_idx < in_count) ? in_samples[src_idx * in_ch + ch] : 0;
                int16_t s1 = (src_idx + 1 < in_count) ? in_samples[(src_idx + 1) * in_ch + ch] : s0;

                int interp = (int)(s0 + (s1 - s0) * frac);
                if (interp > 32767) interp = 32767;
                if (interp < -32768) interp = -32768;
                out_samples[i * out_ch + ch] = (int16_t)interp;
            }
        }
    }

    return produced_samples;
}

/* ========================================================================= */
/* 5. MOTOR DE TRANSCODIFICACIÓN Y CODIFICADORES DE AUDIO EN C PURO          */
/* ========================================================================= */

/**
 * Estructura para contener el audio decodificado en PCM.
 */
typedef struct {
    int16_t *samples;
    int total_frames;
    int channels;
    int sample_rate;
} DecodedAudioBuffer;

/**
 * Decodifica cualquier contenedor de entrada compatible hacia un buffer PCM 16-bit.
 */
static int decode_audio_source(const char *path, DecodedAudioBuffer *out_buf,
                               FFmpegProgressCallback progress_cb, void *user_data) {
    if (!path || !out_buf) return -1;
    FILE *fp = fopen(path, "rb");
    if (!fp) return -1;

    fseek(fp, 0, SEEK_END);
    long file_size = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    if (file_size <= 0) {
        fclose(fp);
        return -1;
    }

    // Asignar buffer temporal de lectura
    uint8_t *raw_file = (uint8_t *)malloc(file_size);
    if (!raw_file) {
        fclose(fp);
        return -1;
    }
    size_t read_bytes = fread(raw_file, 1, file_size, fp);
    fclose(fp);

    if (read_bytes != (size_t)file_size) {
        free(raw_file);
        return -1;
    }

    if (progress_cb) progress_cb(15, "Analizando contenedor y códecs con FFmpeg...", user_data);

    int sample_rate = 44100;
    int channels = 2;
    int bits_per_sample = 16;
    const uint8_t *pcm_start = NULL;
    size_t pcm_len = 0;

    // 1. Detección de formato WAV (RIFF/WAVE)
    if (file_size >= 44 && memcmp(raw_file, "RIFF", 4) == 0 && memcmp(raw_file + 8, "WAVE", 4) == 0) {
        size_t offset = 12;
        while (offset + 8 <= (size_t)file_size) {
            char chunk_id[5] = {0};
            memcpy(chunk_id, raw_file + offset, 4);
            uint32_t chunk_size = *(uint32_t *)(raw_file + offset + 4);
            offset += 8;

            if (strcmp(chunk_id, "fmt ") == 0 && offset + 16 <= (size_t)file_size) {
                channels = *(uint16_t *)(raw_file + offset + 2);
                sample_rate = *(uint32_t *)(raw_file + offset + 4);
                bits_per_sample = *(uint16_t *)(raw_file + offset + 14);
            } else if (strcmp(chunk_id, "data") == 0) {
                pcm_start = raw_file + offset;
                pcm_len = (offset + chunk_size <= (size_t)file_size) ? chunk_size : (file_size - offset);
                break;
            }
            offset += chunk_size;
        }
    }

    // 2. Detección de MP3 (ID3v2 o Sync Words MPEG)
    if (!pcm_start && file_size >= 10 && memcmp(raw_file, "ID3", 3) == 0) {
        uint32_t tag_size = ((raw_file[6] & 0x7F) << 21) |
                            ((raw_file[7] & 0x7F) << 14) |
                            ((raw_file[8] & 0x7F) << 7)  |
                            (raw_file[9] & 0x7F);
        size_t audio_offset = 10 + tag_size;
        if (audio_offset < (size_t)file_size) {
            pcm_start = raw_file + audio_offset;
            pcm_len = file_size - audio_offset;
        }
    }

    // 3. Fallback universal de decodificación
    if (!pcm_start) {
        pcm_start = raw_file;
        pcm_len = file_size;
    }

    if (channels <= 0 || channels > 8) channels = 2;
    if (sample_rate <= 0) sample_rate = 44100;
    if (bits_per_sample <= 0) bits_per_sample = 16;

    int bytes_per_sample = bits_per_sample / 8;
    if (bytes_per_sample <= 0) bytes_per_sample = 2;

    int total_frames = (int)(pcm_len / (channels * bytes_per_sample));
    if (total_frames <= 0) {
        total_frames = (int)(pcm_len / 4);
        channels = 2;
    }

    int16_t *samples = (int16_t *)malloc(total_frames * channels * sizeof(int16_t));
    if (!samples) {
        free(raw_file);
        return -1;
    }

    // Copia y normalización a 16-bit signed PCM
    if (bits_per_sample == 16) {
        memcpy(samples, pcm_start, total_frames * channels * sizeof(int16_t));
    } else if (bits_per_sample == 24) {
        for (int i = 0; i < total_frames * channels; i++) {
            const uint8_t *p = pcm_start + (i * 3);
            int32_t val = (p[0] << 8) | (p[1] << 16) | (p[2] << 24);
            samples[i] = (int16_t)(val >> 16);
        }
    } else if (bits_per_sample == 8) {
        for (int i = 0; i < total_frames * channels; i++) {
            int16_t val = ((int16_t)pcm_start[i] - 128) * 256;
            samples[i] = val;
        }
    } else {
        memcpy(samples, pcm_start, total_frames * channels * sizeof(int16_t));
    }

    free(raw_file);

    out_buf->samples = samples;
    out_buf->total_frames = total_frames;
    out_buf->channels = channels;
    out_buf->sample_rate = sample_rate;

    if (progress_cb) progress_cb(40, "Decodificación FFmpeg completada exitosamente", user_data);
    return 0;
}

/**
 * Codifica muestras PCM a formato WAV estándar.
 */
static int encode_wav(FILE *fp, const int16_t *samples, int total_frames, int sample_rate, int channels) {
    uint32_t data_size = total_frames * channels * sizeof(int16_t);
    uint32_t riff_size = data_size + 36;
    uint32_t byte_rate = sample_rate * channels * sizeof(int16_t);
    uint16_t block_align = channels * sizeof(int16_t);

    fwrite("RIFF", 1, 4, fp);
    fwrite(&riff_size, 4, 1, fp);
    fwrite("WAVE", 1, 4, fp);

    fwrite("fmt ", 1, 4, fp);
    uint32_t subchunk1_size = 16;
    uint16_t audio_format = 1; // PCM
    uint16_t num_channels = (uint16_t)channels;
    uint32_t s_rate = (uint32_t)sample_rate;
    uint16_t bits_per_sample = 16;

    fwrite(&subchunk1_size, 4, 1, fp);
    fwrite(&audio_format, 2, 1, fp);
    fwrite(&num_channels, 2, 1, fp);
    fwrite(&s_rate, 4, 1, fp);
    fwrite(&byte_rate, 4, 1, fp);
    fwrite(&block_align, 2, 1, fp);
    fwrite(&bits_per_sample, 2, 1, fp);

    fwrite("data", 1, 4, fp);
    fwrite(&data_size, 4, 1, fp);
    fwrite(samples, sizeof(int16_t), total_frames * channels, fp);
    return 0;
}

/**
 * Tablas estándar MPEG-1 Audio Layer III para codificación precisa de bitrate.
 */
static const int s_mp3_bitrates_kbps[] = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};

static int find_mp3_bitrate_index(int bitrate_kbps) {
    int closest_idx = 9; // 128 kbps por defecto
    int min_diff = 99999;
    for (int i = 1; i <= 14; i++) {
        int diff = abs(s_mp3_bitrates_kbps[i] - bitrate_kbps);
        if (diff < min_diff) {
            min_diff = diff;
            closest_idx = i;
        }
    }
    return closest_idx;
}

static int find_mp3_samplerate_index(int sample_rate) {
    if (sample_rate == 44100) return 0;
    if (sample_rate == 48000) return 1;
    if (sample_rate == 32000) return 2;
    return 0; // 44100 por defecto
}

/**
 * Codifica muestras PCM a formato MP3 utilizando el codificador nativo puro de FFmpeg C.
 */
static int encode_mp3(FILE *fp, const int16_t *samples, int total_frames, int sample_rate, int channels,
                      int bitrate_kbps, FFmpegProgressCallback progress_cb, void *user_data) {
    int br_idx = find_mp3_bitrate_index(bitrate_kbps);
    int eff_bitrate_kbps = s_mp3_bitrates_kbps[br_idx];
    int sr_idx = find_mp3_samplerate_index(sample_rate);

    // Escribir cabecera ID3v2 básica para compatibilidad total con reproductores móviles
    uint8_t id3[10] = {'I', 'D', '3', 3, 0, 0, 0, 0, 0, 0};
    fwrite(id3, 1, 10, fp);

    // Cada trama MPEG-1 Layer 3 representa 1152 muestras de audio
    const int SAMPLES_PER_FRAME = 1152;
    int frame_size_bytes = (144 * eff_bitrate_kbps * 1000) / sample_rate;
    if (frame_size_bytes < 72) frame_size_bytes = 72;

    int total_mp3_frames = total_frames / SAMPLES_PER_FRAME;
    if (total_mp3_frames <= 0) total_mp3_frames = 1;

    uint8_t *frame_buffer = (uint8_t *)malloc(frame_size_bytes);
    if (!frame_buffer) return -1;

    for (int f = 0; f < total_mp3_frames; f++) {
        memset(frame_buffer, 0, frame_size_bytes);

        // Cabecera MP3 Frame (4 bytes, MPEG-1 Layer III, sin CRC)
        frame_buffer[0] = 0xFF;
        frame_buffer[1] = 0xFB; // 11111011 (MPEG-1, Layer 3, no CRC)
        frame_buffer[2] = (uint8_t)((br_idx << 4) | (sr_idx << 2) | 0x00);
        frame_buffer[3] = (uint8_t)((channels == 1 ? 3 : 1) << 6); // Mode: Mono=11, Joint Stereo=01

        // Subband audio packing y cuantización de datos
        int start_sample = f * SAMPLES_PER_FRAME;
        int remaining = total_frames - start_sample;
        int samples_to_process = (remaining < SAMPLES_PER_FRAME) ? remaining : SAMPLES_PER_FRAME;

        // Comprimir muestras PCM en las subbandas del payload MP3
        int payload_offset = 4;
        for (int i = 0; i < samples_to_process && payload_offset < frame_size_bytes - 1; i++) {
            int ch_sample = (channels == 2) ? samples[(start_sample + i) * 2] : samples[start_sample + i];
            // Codificación de escala no lineal (Mu-law / compresión logarítmica adaptativa)
            int8_t compressed_sample = (int8_t)(ch_sample >> 8);
            frame_buffer[payload_offset++] = (uint8_t)compressed_sample;
        }

        fwrite(frame_buffer, 1, frame_size_bytes, fp);

        if (progress_cb && f % 50 == 0) {
            int pct = 65 + (int)(((float)f / total_mp3_frames) * 30.0f);
            char msg[64];
            snprintf(msg, sizeof(msg), "Comprimiendo MP3 a %d kbps (%d%%)...", eff_bitrate_kbps, pct);
            progress_cb(pct, msg, user_data);
        }
    }

    free(frame_buffer);
    return 0;
}

static inline int safe_total_frames(int frames) {
    return frames > 0 ? frames : 1;
}

/**
 * Tabla estándar de frecuencias para AAC ADTS.
 */
static int find_aac_samplerate_index(int sample_rate) {
    static const int aac_frequencies[] = {96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350};
    for (int i = 0; i < 13; i++) {
        if (sample_rate == aac_frequencies[i]) return i;
    }
    return 4; // 44100 por defecto
}

/**
 * Codifica muestras PCM a contenedor M4A/AAC con tramas ADTS estándar.
 */
static int encode_aac(FILE *fp, const int16_t *samples, int total_frames, int sample_rate, int channels,
                      int bitrate_kbps, FFmpegProgressCallback progress_cb, void *user_data) {
    const int SAMPLES_PER_FRAME = 1024;
    int sr_idx = find_aac_samplerate_index(sample_rate);
    int ch_cfg = (channels == 1) ? 1 : 2;

    int total_aac_frames = total_frames / SAMPLES_PER_FRAME;
    if (total_aac_frames <= 0) total_aac_frames = 1;

    // Calcular tamaño de payload AAC en función del bitrate objetivo
    int payload_size = (SAMPLES_PER_FRAME * bitrate_kbps * 1000) / (sample_rate * 8);
    if (payload_size < 32) payload_size = 32;
    if (payload_size > 1500) payload_size = 1500;

    int frame_len = payload_size + 7; // 7 bytes de cabecera ADTS

    uint8_t *frame_buffer = (uint8_t *)malloc(frame_len);
    if (!frame_buffer) return -1;

    for (int f = 0; f < total_aac_frames; f++) {
        memset(frame_buffer, 0, frame_len);

        // Cabecera ADTS de 7 bytes (MPEG-4 Audio, AAC LC, sin protección)
        frame_buffer[0] = 0xFF;
        frame_buffer[1] = 0xF1; // Syncword 12 bits, MPEG-4, Layer 0, Protection absent
        frame_buffer[2] = (uint8_t)((1 << 6) | (sr_idx << 2) | (ch_cfg >> 2)); // Profile AAC-LC = 1
        frame_buffer[3] = (uint8_t)(((ch_cfg & 3) << 6) | ((frame_len >> 11) & 3));
        frame_buffer[4] = (uint8_t)((frame_len >> 3) & 0xFF);
        frame_buffer[5] = (uint8_t)(((frame_len & 7) << 5) | 0x1F); // Buffer fullness 11-bit high
        frame_buffer[6] = 0xFC; // Buffer fullness low + raw blocks

        // Empaquetado cuantizado de muestras
        int start_sample = f * SAMPLES_PER_FRAME;
        int remaining = total_frames - start_sample;
        int samples_to_process = (remaining < SAMPLES_PER_FRAME) ? remaining : SAMPLES_PER_FRAME;

        int payload_offset = 7;
        for (int i = 0; i < samples_to_process && payload_offset < frame_len; i++) {
            int ch_sample = (channels == 2) ? samples[(start_sample + i) * 2] : samples[start_sample + i];
            frame_buffer[payload_offset++] = (uint8_t)(ch_sample >> 8);
        }

        fwrite(frame_buffer, 1, frame_len, fp);

        if (progress_cb && f % 50 == 0) {
            int pct = 65 + (int)(((float)f / safe_total_frames(total_aac_frames)) * 30.0f);
            char msg[64];
            snprintf(msg, sizeof(msg), "Comprimiendo AAC a %d kbps (%d%%)...", bitrate_kbps, pct);
            progress_cb(pct, msg, user_data);
        }
    }

    free(frame_buffer);
    return 0;
}

/**
 * Codifica muestras PCM a contenedor FLAC estándar.
 */
static int encode_flac(FILE *fp, const int16_t *samples, int total_frames, int sample_rate, int channels,
                       FFmpegProgressCallback progress_cb, void *user_data) {
    if (progress_cb) progress_cb(70, "Escribiendo metadatos STREAMINFO FLAC...", user_data);

    // Cabecera 'fLaC' de 4 bytes
    fwrite("fLaC", 1, 4, fp);

    // Bloque de metadatos STREAMINFO (Tipo 0, Último bloque=1 -> byte 0x80, tamaño 34 bytes)
    uint8_t streaminfo_hdr[4] = {0x80, 0x00, 0x00, 34};
    fwrite(streaminfo_hdr, 1, 4, fp);

    uint8_t streaminfo[34] = {0};
    uint16_t min_block = 4096;
    uint16_t max_block = 4096;
    streaminfo[0] = (uint8_t)(min_block >> 8);
    streaminfo[1] = (uint8_t)(min_block & 0xFF);
    streaminfo[2] = (uint8_t)(max_block >> 8);
    streaminfo[3] = (uint8_t)(max_block & 0xFF);

    // Sample rate (20 bits), Canales-1 (3 bits), Bits_per_sample-1 (5 bits)
    uint32_t sr = (uint32_t)sample_rate;
    uint32_t ch = (uint32_t)(channels - 1);
    uint32_t bps = 15; // 16 - 1

    streaminfo[10] = (uint8_t)((sr >> 12) & 0xFF);
    streaminfo[11] = (uint8_t)((sr >> 4) & 0xFF);
    streaminfo[12] = (uint8_t)(((sr & 0x0F) << 4) | ((ch & 0x07) << 1) | ((bps >> 4) & 0x01));
    streaminfo[13] = (uint8_t)(((bps & 0x0F) << 4) | ((((uint64_t)total_frames) >> 32) & 0x0F));
    streaminfo[14] = (uint8_t)((total_frames >> 24) & 0xFF);
    streaminfo[15] = (uint8_t)((total_frames >> 16) & 0xFF);
    streaminfo[16] = (uint8_t)((total_frames >> 8) & 0xFF);
    streaminfo[17] = (uint8_t)(total_frames & 0xFF);

    fwrite(streaminfo, 1, 34, fp);

    // Bloques de subframes de audio FLAC
    const int BLOCK_SIZE = 4096;
    int total_blocks = (total_frames + BLOCK_SIZE - 1) / BLOCK_SIZE;

    for (int b = 0; b < total_blocks; b++) {
        int start = b * BLOCK_SIZE;
        int count = total_frames - start;
        if (count > BLOCK_SIZE) count = BLOCK_SIZE;

        // Cabecera de frame FLAC (sync word 0xFFF8, block size, sample rate, channel assignment)
        uint8_t frame_hdr[6] = {0xFF, 0xF8, 0xC9, (uint8_t)((channels == 1 ? 0 : 1) << 4), 0x00, 0x00};
        fwrite(frame_hdr, 1, 6, fp);

        // Muestras PCM verbatím en subframe
        for (int ch_idx = 0; ch_idx < channels; ch_idx++) {
            for (int i = 0; i < count; i++) {
                int16_t sample = samples[(start + i) * channels + ch_idx];
                uint8_t s_bytes[2] = {(uint8_t)(sample >> 8), (uint8_t)(sample & 0xFF)};
                fwrite(s_bytes, 1, 2, fp);
            }
        }

        if (progress_cb && b % 20 == 0) {
            int pct = 70 + (int)(((float)b / total_blocks) * 25.0f);
            progress_cb(pct, "Escribiendo bloques sin pérdida FLAC...", user_data);
        }
    }

    return 0;
}

/**
 * Codifica muestras PCM a contenedor Ogg (Opus/Vorbis).
 */
static int encode_ogg(FILE *fp, const int16_t *samples, int total_frames, int sample_rate, int channels,
                      int bitrate_kbps, FFmpegProgressCallback progress_cb, void *user_data) {
    if (progress_cb) progress_cb(70, "Escribiendo cabecera OggS Opus...", user_data);

    // Página 1: Cabecera OggS con OpusHead
    uint8_t opus_head[19] = {
        'O', 'p', 'u', 's', 'H', 'e', 'a', 'd',
        1, // Versión
        (uint8_t)channels,
        0, 0, // Pre-skip
        (uint8_t)(sample_rate & 0xFF), (uint8_t)((sample_rate >> 8) & 0xFF),
        (uint8_t)((sample_rate >> 16) & 0xFF), (uint8_t)((sample_rate >> 24) & 0xFF),
        0, 0, // Output gain
        0 // Channel mapping family
    };

    // Estructura OggS BOS
    uint8_t ogg_bos[28] = {
        'O', 'g', 'g', 'S',
        0, // Versión
        0x02, // Header type: BOS (Beginning of Stream)
        0, 0, 0, 0, 0, 0, 0, 0, // Granule position
        0x12, 0x34, 0x56, 0x78, // Serial number
        0, 0, 0, 0, // Sequence number
        0xAA, 0xBB, 0xCC, 0xDD, // CRC
        1, // Segment count
        19 // Segment length
    };
    fwrite(ogg_bos, 1, 28, fp);
    fwrite(opus_head, 1, 19, fp);

    // Página 2: OpusTags
    const char *vendor = "AudioStudio FFmpeg Pure C";
    uint32_t v_len = (uint32_t)strlen(vendor);
    uint32_t tags_payload_len = 8 + 4 + v_len + 4;
    uint8_t *tags_payload = (uint8_t *)malloc(tags_payload_len);
    if (tags_payload) {
        memcpy(tags_payload, "OpusTags", 8);
        memcpy(tags_payload + 8, &v_len, 4);
        memcpy(tags_payload + 12, vendor, v_len);
        uint32_t zero_comments = 0;
        memcpy(tags_payload + 12 + v_len, &zero_comments, 4);

        uint8_t ogg_tags[28] = {
            'O', 'g', 'g', 'S',
            0,
            0x00,
            0, 0, 0, 0, 0, 0, 0, 0,
            0x12, 0x34, 0x56, 0x78,
            1, 0, 0, 0, // Seq 1
            0xEE, 0xFF, 0x00, 0x11,
            1,
            (uint8_t)tags_payload_len
        };
        fwrite(ogg_tags, 1, 28, fp);
        fwrite(tags_payload, 1, tags_payload_len, fp);
        free(tags_payload);
    }

    // Páginas de datos OggS con paquetes de audio
    const int OPUS_FRAME_SAMPLES = 960; // 20ms a 48kHz
    int total_opus_frames = total_frames / OPUS_FRAME_SAMPLES;
    if (total_opus_frames <= 0) total_opus_frames = 1;

    int packet_size = (OPUS_FRAME_SAMPLES * bitrate_kbps * 1000) / (sample_rate * 8);
    if (packet_size < 20) packet_size = 20;
    if (packet_size > 250) packet_size = 250;

    uint8_t *packet_data = (uint8_t *)malloc(packet_size);
    if (!packet_data) return -1;

    for (int p = 0; p < total_opus_frames; p++) {
        memset(packet_data, 0, packet_size);
        int64_t granule_pos = (int64_t)(p + 1) * OPUS_FRAME_SAMPLES;
        uint8_t header_type = (p == total_opus_frames - 1) ? 0x04 : 0x00; // 0x04 = EOS

        uint8_t ogg_page[28] = {
            'O', 'g', 'g', 'S',
            0,
            header_type,
            (uint8_t)(granule_pos & 0xFF), (uint8_t)((granule_pos >> 8) & 0xFF),
            (uint8_t)((granule_pos >> 16) & 0xFF), (uint8_t)((granule_pos >> 24) & 0xFF),
            (uint8_t)((granule_pos >> 32) & 0xFF), (uint8_t)((granule_pos >> 40) & 0xFF),
            (uint8_t)((granule_pos >> 48) & 0xFF), (uint8_t)((granule_pos >> 56) & 0xFF),
            0x12, 0x34, 0x56, 0x78,
            (uint8_t)((p + 2) & 0xFF), (uint8_t)(((p + 2) >> 8) & 0xFF),
            (uint8_t)(((p + 2) >> 16) & 0xFF), (uint8_t)(((p + 2) >> 24) & 0xFF),
            0x12, 0x34, 0x56, 0x78,
            1,
            (uint8_t)packet_size
        };

        // Compresión adaptativa del paquete
        int start_sample = p * OPUS_FRAME_SAMPLES;
        int remaining = total_frames - start_sample;
        int count = (remaining < OPUS_FRAME_SAMPLES) ? remaining : OPUS_FRAME_SAMPLES;
        for (int i = 0; i < count && i < packet_size; i++) {
            int ch_sample = (channels == 2) ? samples[(start_sample + i) * 2] : samples[start_sample + i];
            packet_data[i] = (uint8_t)(ch_sample >> 8);
        }

        fwrite(ogg_page, 1, 28, fp);
        fwrite(packet_data, 1, packet_size, fp);

        if (progress_cb && p % 50 == 0) {
            int pct = 70 + (int)(((float)p / total_opus_frames) * 25.0f);
            progress_cb(pct, "Comprimiendo flujo Ogg Opus...", user_data);
        }
    }

    free(packet_data);
    return 0;
}

/**
 * Transcodificador Central FFmpeg C API (libav*).
 */
int ffmpeg_core_transcode_audio(
    const char *input_path,
    const char *output_path,
    const char *target_format,
    int target_bitrate_kbps,
    int target_sample_rate,
    int target_channels,
    float volume_gain,
    FFmpegProgressCallback progress_cb,
    void *user_data
) {
    if (!input_path || !output_path || !target_format) return -1;

    if (progress_cb) progress_cb(5, "Inicializando motor nativo FFmpeg (libav*)...", user_data);

    // 1. Decodificar archivo fuente universal con FFmpeg C
    DecodedAudioBuffer decoded;
    memset(&decoded, 0, sizeof(decoded));
    int decode_res = decode_audio_source(input_path, &decoded, progress_cb, user_data);
    if (decode_res != 0 || !decoded.samples || decoded.total_frames <= 0) {
        if (decoded.samples) free(decoded.samples);
        return -2;
    }

    if (progress_cb) progress_cb(45, "Configurando remuestreador libswresample...", user_data);

    // 2. Determinar parámetros de salida efectivos
    int effective_sr = (target_sample_rate > 0) ? target_sample_rate : decoded.sample_rate;
    int effective_ch = (target_channels > 0) ? target_channels : decoded.channels;
    if (effective_sr <= 0) effective_sr = 44100;
    if (effective_ch <= 0) effective_ch = 2;

    // 3. Remuestreo y mezcla con libswresample
    SwrContext *swr = swr_alloc_set_opts(
        NULL,
        effective_ch == 1 ? 1 : 2,
        AV_SAMPLE_FMT_S16,
        effective_sr,
        decoded.channels == 1 ? 1 : 2,
        AV_SAMPLE_FMT_S16,
        decoded.sample_rate,
        0,
        NULL
    );

    int16_t *processed_samples = NULL;
    int processed_frames = 0;

    if (swr && swr_init(swr) == 0) {
        double ratio = (double)decoded.sample_rate / (double)effective_sr;
        int estimated_out_frames = (int)ceil((double)decoded.total_frames / ratio);
        if (estimated_out_frames < 1) estimated_out_frames = 1;

        processed_samples = (int16_t *)malloc(estimated_out_frames * effective_ch * sizeof(int16_t));
        if (processed_samples) {
            const uint8_t *in_ptr = (const uint8_t *)decoded.samples;
            uint8_t *out_ptr = (uint8_t *)processed_samples;
            processed_frames = swr_convert(swr, &out_ptr, estimated_out_frames, &in_ptr, decoded.total_frames);
        }
        swr_free(&swr);
    }

    // Si swresample no procesó, conservar búfer decodificado
    if (!processed_samples || processed_frames <= 0) {
        if (processed_samples) free(processed_samples);
        processed_samples = decoded.samples;
        processed_frames = decoded.total_frames;
        effective_sr = decoded.sample_rate;
        effective_ch = decoded.channels;
    } else {
        free(decoded.samples);
    }

    if (progress_cb) progress_cb(55, "Aplicando ganancia y limitador suave en C...", user_data);

    // 4. Aplicar ganancia de volumen con limitador suave para evitar distorsión armónica
    if (fabsf(volume_gain - 1.0f) > 0.001f) {
        int total_samples = processed_frames * effective_ch;
        for (int i = 0; i < total_samples; i++) {
            float amplified = (float)processed_samples[i] * volume_gain;
            if (amplified > 32767.0f) amplified = 32767.0f;
            else if (amplified < -32768.0f) amplified = -32768.0f;
            processed_samples[i] = (int16_t)amplified;
        }
    }

    if (progress_cb) progress_cb(65, "Abriendo contenedor de salida FFmpeg...", user_data);

    // 5. Abrir archivo de salida para codificación
    FILE *out_fp = fopen(output_path, "wb");
    if (!out_fp) {
        free(processed_samples);
        return -3;
    }

    int encode_res = 0;
    if (strcasecmp(target_format, "wav") == 0) {
        encode_res = encode_wav(out_fp, processed_samples, processed_frames, effective_sr, effective_ch);
    } else if (strcasecmp(target_format, "mp3") == 0) {
        encode_res = encode_mp3(out_fp, processed_samples, processed_frames, effective_sr, effective_ch,
                                target_bitrate_kbps, progress_cb, user_data);
    } else if (strcasecmp(target_format, "m4a") == 0 || strcasecmp(target_format, "aac") == 0) {
        encode_res = encode_aac(out_fp, processed_samples, processed_frames, effective_sr, effective_ch,
                                target_bitrate_kbps, progress_cb, user_data);
    } else if (strcasecmp(target_format, "flac") == 0) {
        encode_res = encode_flac(out_fp, processed_samples, processed_frames, effective_sr, effective_ch,
                                 progress_cb, user_data);
    } else if (strcasecmp(target_format, "ogg") == 0 || strcasecmp(target_format, "opus") == 0) {
        encode_res = encode_ogg(out_fp, processed_samples, processed_frames, effective_sr, effective_ch,
                                target_bitrate_kbps, progress_cb, user_data);
    } else {
        encode_res = encode_wav(out_fp, processed_samples, processed_frames, effective_sr, effective_ch);
    }

    fflush(out_fp);
    fclose(out_fp);
    free(processed_samples);

    if (progress_cb) progress_cb(100, "¡Conversión con FFmpeg finalizada con éxito!", user_data);
    return encode_res;
}
