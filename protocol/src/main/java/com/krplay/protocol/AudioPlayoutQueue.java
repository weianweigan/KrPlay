package com.krplay.protocol;

import java.io.IOException;
import java.util.ArrayDeque;

/** Single PCM stream/epoch. Total outstanding frames include software AND AudioTrack. */
public final class AudioPlayoutQueue {
    public final int type,epoch,purpose,rate,channels,frameBytes,capacityFrames;
    public static final class Chunk {public final byte[] data;public int offset;Chunk(byte[] bytes){data=bytes;}}
    private final ArrayDeque<Chunk> chunks=new ArrayDeque<>();
    private long received,played,softwareFrames;
    public volatile boolean cancelled,ending;
    public volatile long lastArrivalNs;
    public AudioPlayoutQueue(int t,int e,int p,int r,int c,int buffer)throws IOException {
        AudioWire.format(r,c,0);if(e<=0||p<0||p>3)throw new IOException("Invalid audio stream");
        type=t;epoch=e;purpose=p;rate=r;channels=c;frameBytes=c*2;capacityFrames=AudioPolicy.frames(r,AudioPolicy.capacityMs(p,buffer));
    }
    public synchronized void offer(long firstFrame,byte[] bytes,long now)throws IOException {
        if(cancelled||ending)throw new IOException("PCM after stream end");
        AudioWire.format(rate,channels,bytes.length);
        if(bytes.length==0||firstFrame!=received)throw new IOException("PCM sample gap/duplicate");
        int frames=bytes.length/frameBytes;
        if(received-played+frames>capacityFrames)throw new IOException("PCM playback window exceeded");
        chunks.addLast(new Chunk(bytes));received+=frames;softwareFrames+=frames;lastArrivalNs=now;
    }
    public synchronized Chunk peek(){return chunks.peekFirst();}
    public synchronized void consume(Chunk chunk,int bytes){
        if(cancelled)return;
        if(chunks.peekFirst()!=chunk||bytes<0||bytes%frameBytes!=0||chunk.offset+bytes>chunk.data.length)throw new IllegalArgumentException("PCM write boundary");
        chunk.offset+=bytes;softwareFrames-=bytes/frameBytes;if(chunk.offset==chunk.data.length)chunks.removeFirst();
    }
    public synchronized void played(long frames){if(frames<played||frames>received)throw new IllegalArgumentException("Invalid playback head");played=frames;}
    public synchronized long playedFrames(){return played;}
    public synchronized long received(){return received;}
    public synchronized long softwareFrames(){return softwareFrames;}
    public synchronized long outstanding(){return received-played;}
    public synchronized void cancel(){cancelled=true;chunks.clear();softwareFrames=0;}
}
