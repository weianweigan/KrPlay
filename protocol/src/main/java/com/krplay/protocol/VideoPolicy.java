package com.krplay.protocol;
/** Pure policy shared by the client and offline regression tests. */
public final class VideoPolicy {
    private VideoPolicy() {}
    public static int[] viewport(int w,int h,int profile){
        if(w<320||h<240||profile<0||profile>2)throw new IllegalArgumentException("Invalid viewport/profile");
        int maxW=profile==0?4096:profile==2?1280:1920,maxH=profile==0?4096:profile==2?720:1080;
        double scale=Math.min(1,Math.min((double)maxW/w,(double)maxH/h));
        return new int[]{Math.max(320,(int)(w*scale)/2*2),Math.max(240,(int)(h*scale)/2*2)};
    }
    /** Some codecs echo submitted PTS as render time. Use the callback upper estimate instead. */
    public static long renderTime(long submit,long reported,long callback){return reported-submit>=100_000L&&reported<=callback?reported:callback;}
}
