#include <jni.h>
#include <android/native_window_jni.h>
#include <mutex>
#include <memory>
extern "C" {
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
}

// Decode and window posting have different owners. The single pending frame is
// replaced atomically: a blocked Surface never stalls the network/decoder worker.
struct Decoder {
    AVCodecContext* codec = nullptr;
    AVFrame* decoded = av_frame_alloc();
    AVFrame* latest = av_frame_alloc();
    AVFrame* rendering = av_frame_alloc();
    ANativeWindow* window = nullptr;
    SwsContext* scale = nullptr;
    std::mutex mutex;
    ~Decoder() {
        avcodec_free_context(&codec); av_frame_free(&decoded);
        av_frame_free(&latest); av_frame_free(&rendering);
        sws_freeContext(scale); if (window) ANativeWindow_release(window);
    }
};
static void fail(JNIEnv* env, const char* message) {
    env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), message);
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_krplay_client_SoftwareVideoDecoder_open(JNIEnv* env,jclass,jboolean hevc,jobject surface,jbyteArray csd) {
    auto d=std::make_unique<Decoder>();
    const AVCodec* codec=avcodec_find_decoder(hevc?AV_CODEC_ID_HEVC:AV_CODEC_ID_H264);
    d->codec=avcodec_alloc_context3(codec);
    if (!d->codec || !d->decoded || !d->latest || !d->rendering) { fail(env,"FFmpeg allocation failed"); return 0; }
    d->codec->thread_count=4;
    d->codec->thread_type=FF_THREAD_SLICE;
    d->codec->flags|=AV_CODEC_FLAG_LOW_DELAY;
    const int n=env->GetArrayLength(csd);
    d->codec->extradata=(uint8_t*)av_mallocz(n+AV_INPUT_BUFFER_PADDING_SIZE);
    if (!d->codec->extradata) { fail(env,"FFmpeg CSD allocation failed"); return 0; }
    d->codec->extradata_size=n;
    env->GetByteArrayRegion(csd,0,n,(jbyte*)d->codec->extradata);
    if (avcodec_open2(d->codec,codec,nullptr)<0) { fail(env,"FFmpeg decoder open failed"); return 0; }
    d->window=ANativeWindow_fromSurface(env,surface);
    if (!d->window) { fail(env,"Invalid software decoder Surface"); return 0; }
    return reinterpret_cast<jlong>(d.release());
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_krplay_client_SoftwareVideoDecoder_decode(JNIEnv* env,jclass,jlong handle,jbyteArray data,jint offset,jint length,jlong pts) {
    auto* d=reinterpret_cast<Decoder*>(handle);
    AVPacket* p=av_packet_alloc();
    if (!p || av_new_packet(p,length)<0) { av_packet_free(&p);fail(env,"FFmpeg packet allocation failed"); return 0; }
    env->GetByteArrayRegion(data,offset,length,(jbyte*)p->data);p->pts=pts;
    int result=avcodec_send_packet(d->codec,p);av_packet_free(&p);
    if (result<0) { fail(env,"FFmpeg packet rejected"); return 0; }
    jlong ready=0;
    while ((result=avcodec_receive_frame(d->codec,d->decoded))>=0) {
        ready=d->decoded->pts;
        std::lock_guard<std::mutex> lock(d->mutex);
        av_frame_unref(d->latest);av_frame_move_ref(d->latest,d->decoded);
    }
    if(result!=AVERROR(EAGAIN) && result!=AVERROR_EOF)fail(env,"FFmpeg decode failed");
    return ready;
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_krplay_client_SoftwareVideoDecoder_render(JNIEnv* env,jclass,jlong handle) {
    auto* d=reinterpret_cast<Decoder*>(handle);
    { std::lock_guard<std::mutex> lock(d->mutex);
      if (!d->latest->data[0])return 0;
      av_frame_unref(d->rendering);av_frame_move_ref(d->rendering,d->latest); }
    AVFrame* f=d->rendering;
    if(f->width<=0 || f->height<=0 || f->width>4096 || f->height>4096) { fail(env,"Invalid decoded dimensions");return 0; }
    d->scale=sws_getCachedContext(d->scale,f->width,f->height,(AVPixelFormat)f->format,
        f->width,f->height,AV_PIX_FMT_RGBA,SWS_FAST_BILINEAR,nullptr,nullptr,nullptr);
    if(!d->scale) { fail(env,"FFmpeg color conversion unavailable");return 0; }
    if(ANativeWindow_setBuffersGeometry(d->window,f->width,f->height,WINDOW_FORMAT_RGBA_8888)<0) { fail(env,"Surface geometry failed");return 0; }
    ANativeWindow_Buffer buffer;
    if(ANativeWindow_lock(d->window,&buffer,nullptr)<0) { fail(env,"Surface lock failed");return 0; }
    uint8_t* dst[]={static_cast<uint8_t*>(buffer.bits),nullptr,nullptr,nullptr};
    int strides[]={buffer.stride*4,0,0,0};
    int rows=sws_scale(d->scale,f->data,f->linesize,0,f->height,dst,strides);
    int posted=ANativeWindow_unlockAndPost(d->window);
    if(rows!=f->height || posted<0) { fail(env,"Surface post failed");return 0; }
    return f->pts;
}
extern "C" JNIEXPORT void JNICALL
Java_com_krplay_client_SoftwareVideoDecoder_destroy(JNIEnv*,jclass,jlong handle) { delete reinterpret_cast<Decoder*>(handle); }
