package com.krplay.protocol;

/** One AudioTrack lifetime; pause preserves its head, flush/rebuild requires a new instance. */
public final class AudioProgress {
    private long lastHead,played,written;
    public void written(long frames){if(frames<0)throw new IllegalArgumentException();written+=frames;}
    public long update(int rawHead){long head=rawHead&0xffffffffL;played+=(head-lastHead)&0xffffffffL;lastHead=head;return Math.min(written,played);}
    public long played(){return Math.min(written,played);}
    public long queued(){return Math.max(0,written-played());}
    public long written(){return written;}
    public static int startFrames(int requested,int actualCapacity){return Math.max(1,Math.min(requested,Math.max(1,actualCapacity-1)));}
    public static boolean ready(long queued,int threshold,boolean ending,long idleNs){return queued>0&&(queued>=threshold||ending||idleNs>=150_000_000L);}
}
