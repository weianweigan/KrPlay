package com.krplay.protocol;

import java.util.ArrayDeque;
import java.util.Iterator;

/** Bounded audio mailbox. Evict PCM, never microphone/stream-control messages. */
public final class RealtimeAudioQueue<T> {
    public static final long MAX_AGE_NS=120_000_000L;
    public static final class Entry<T> {
        public final T value; public final long enqueuedNs,durationNs;
        Entry(T value,long now,long duration){this.value=value;enqueuedNs=now;durationNs=duration;}
    }
    private final ArrayDeque<Entry<T>> entries=new ArrayDeque<>();
    private long durationNs,dropped;
    public synchronized long dropped(){return dropped;}
    private boolean evictPcm(){
        Iterator<Entry<T>> i=entries.iterator();while(i.hasNext()){
            Entry<T> e=i.next();if(e.durationNs>0){i.remove();durationNs-=e.durationNs;dropped++;return true;}
        }return false;
    }
    public synchronized boolean offer(T value,long now,long duration){
        while(!entries.isEmpty() && (entries.size()>=32 || durationNs+duration>MAX_AGE_NS)){
            if(!evictPcm())break;
        }
        if(entries.size()>=32)return false;
        entries.addLast(new Entry<>(value,now,duration));durationNs+=duration;notifyAll();return true;
    }
    public synchronized Entry<T> poll(long waitMs) throws InterruptedException {
        if(entries.isEmpty())wait(waitMs);
        Entry<T> e=entries.pollFirst();if(e!=null)durationNs-=e.durationNs;return e;
    }
    public synchronized void clear(){entries.clear();durationNs=0;notifyAll();}
}
