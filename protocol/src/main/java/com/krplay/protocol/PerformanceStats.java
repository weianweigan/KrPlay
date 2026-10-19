package com.krplay.protocol;
import java.util.*;
/** NTP four-timestamp clock estimate. Values are monotonic, never wall-clock/iPhone PTS. */
public final class PerformanceStats {
    private final long[] rtts=new long[64], offsets=new long[64];private int count,next;
    private final double[] latencies=new double[2048];private int latencyCount,latencyNext;
    public synchronized boolean probe(long t0,long t1,long t2,long t3){
        long r=(t3-t0)-(t2-t1);if(t3<t0||t2<t1||r<0||r>2_000_000_000L)return false;
        rtts[next]=r;offsets[next]=(t1-t0)/2+(t2-t3)/2;next=(next+1)%64;count=Math.min(64,count+1);return true;
    }
    public synchronized void reset(){count=next=latencyCount=latencyNext=0;}
    private int best(){int b=0;for(int i=1;i<count;i++)if(rtts[i]<rtts[b])b=i;return b;}
    public synchronized boolean synced(){return count>=3;}
    public synchronized long offsetNs(){return count==0?0:offsets[best()];}
    public synchronized double uncertaintyMs(){return count==0?Double.NaN:rtts[best()]/2e6;}
    public synchronized double rttMs(){return count==0?Double.NaN:rtts[(next+63)%64]/1e6;}
    public synchronized double rttP95Ms(){if(count==0)return Double.NaN;long[] a=Arrays.copyOf(rtts,count);Arrays.sort(a);return a[(int)Math.ceil(count*.95)-1]/1e6;}
    public synchronized double bridgeMs(long serverReceive,long rendered){
        if(!synced()||serverReceive<=0)return Double.NaN;double v=(rendered-(serverReceive-offsetNs()))/1e6;
        if(v < -uncertaintyMs() || v>10000)return Double.NaN;return Math.max(0,v);
    }
    public synchronized void latency(double ms){if(Double.isFinite(ms)){latencies[latencyNext]=ms;latencyNext=(latencyNext+1)%2048;latencyCount=Math.min(2048,latencyCount+1);}}
    public synchronized double latencyP95Ms(){if(latencyCount==0)return Double.NaN;double[] a=Arrays.copyOf(latencies,latencyCount);Arrays.sort(a);return a[(int)Math.ceil(latencyCount*.95)-1];}
}
