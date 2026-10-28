package com.krplay.client;

import android.view.Surface;

/** ARM64 FFmpeg only; never calls MediaCodec. One latest decoded frame feeds the renderer. */
final class SoftwareVideoDecoder implements AutoCloseable {
    interface Listener { void rendered(SoftwareVideoDecoder owner,long pts); }
    static final boolean AVAILABLE;
    static { boolean loaded=false;try { System.loadLibrary("krplay_decoder");loaded=true; }
        catch(UnsatisfiedLinkError unavailable) { android.util.Log.w("KrPlay-Video","Native software decoder unavailable",unavailable); }
        AVAILABLE=loaded;
    }
    private final long handle;
    private volatile boolean active=true;
    private volatile RuntimeException failure;
    SoftwareVideoDecoder(boolean hevc,Surface surface,byte[] csd,Listener listener) {
        handle=open(hevc,surface,csd);
        Thread renderer=new Thread(()->{
            try {
                while(active) {
                    if(failure!=null) { Thread.sleep(4);continue; }
                    try { long pts=render(handle);if(pts!=0)listener.rendered(this,pts);else Thread.sleep(2); }
                    catch(RuntimeException e) { failure=e; }
                }
            } catch(InterruptedException ignored) { Thread.currentThread().interrupt(); }
            finally { destroy(handle); }
        },"KrPlay-Software-Render");
        renderer.start();
    }
    long submit(byte[] bytes,int offset,int size,long pts) {
        if(failure!=null)throw failure;
        return decode(handle,bytes,offset,size,pts);
    }
    // Called by the sole decode owner, after its final native decode returns.
    // The render owner releases the window/context after any outstanding lock/post.
    @Override public void close() { active=false; }
    private static native long open(boolean hevc,Surface surface,byte[] csd);
    private static native long decode(long handle,byte[] bytes,int offset,int size,long pts);
    private static native long render(long handle);
    private static native void destroy(long handle);
}
