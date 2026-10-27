package com.krplay.protocol;
/** Pure policy shared by the client and offline regression tests. */
public final class VideoPolicy {
    private VideoPolicy() {}
    /** Coalesce reconnect, decoder and bridge requests onto one upstream request per second. */
    public static final class KeyframeRequests {
        private boolean requested;
        private long lastNs;
        public synchronized boolean acquire(long nowNs) { return acquire(nowNs, false); }
        public synchronized boolean acquire(long nowNs, boolean congested) {
            if(requested && nowNs-lastNs<(congested?2_000_000_000L:1_000_000_000L))return false;
            requested=true;lastNs=nowNs;return true;
        }
    }
    // A scheduling bubble is not a codec failure. Require repeated unsuccessful polls
    // AND a sustained lack of progress, so one VM deschedule cannot trigger reset.
    public static final long INPUT_WARN_NS=60_000_000L, INPUT_STALL_NS=750_000_000L;
    public static boolean inputStalled(long now,long progress,int attempts) {
        return attempts>=8 && now-progress>=INPUT_STALL_NS;
    }
    public static boolean videoPacket(int type) { return type==0||type==1||type==20||type==21; }
    public static int[] viewport(int w,int h,int profile){
        if(w<320||h<240||profile<0||profile>2)throw new IllegalArgumentException("Invalid viewport/profile");
        int maxW=profile==0?4096:profile==2?1280:1920,maxH=profile==0?4096:profile==2?720:1080;
        double scale=Math.min(1,Math.min((double)maxW/w,(double)maxH/h));
        return new int[]{Math.max(320,(int)(w*scale)/2*2),Math.max(240,(int)(h*scale)/2*2)};
    }
    /** Some codecs echo submitted PTS as render time. Use the callback upper estimate instead. */
    public static long renderTime(long submit,long reported,long callback){return reported-submit>=100_000L&&reported<=callback?reported:callback;}
}
