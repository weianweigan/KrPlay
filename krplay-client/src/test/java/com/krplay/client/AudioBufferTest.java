package com.krplay.client;

import com.krplay.protocol.*;
import java.io.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AudioBufferTest {
    @Test public void musicDefaultsAndInteractiveBudgetAreIndependent(){
        assertEquals(300,AudioPolicy.bufferMs(0));assertEquals(300,AudioPolicy.DEFAULT_BUFFER_MS);
        assertEquals(1000,AudioPolicy.bufferMs(1000));assertEquals(40,AudioPolicy.startMs(AudioPolicy.PHONE,1000));
        assertEquals(AudioPolicy.MUSIC,AudioPolicy.purpose("media",101));
        assertEquals(AudioPolicy.NAVIGATION,AudioPolicy.purpose("default",102));
        assertEquals(AudioPolicy.ASSISTANT,AudioPolicy.purpose("speechrecognition",100));
    }
    @Test public void bufferedMusicPreservesBurstSamplesBeyondOldAgeLimit()throws Exception{
        AudioPlayoutQueue q=new AudioPlayoutQueue(102,1,AudioPolicy.MUSIC,48000,2,1000);
        byte[] expected=new byte[48000*4];new Random(19).nextBytes(expected);
        for(int off=0;off<expected.length;off+=3840)q.offer(off/4,Arrays.copyOfRange(expected,off,Math.min(expected.length,off+3840)),0);
        // 750 ms with no playback is not expiry. PCM is still identical, even for partial writes.
        ByteArrayOutputStream result=new ByteArrayOutputStream();long frames=0;
        while(q.peek()!=null){AudioPlayoutQueue.Chunk c=q.peek();int n=Math.min(388,c.data.length-c.offset);result.write(c.data,c.offset,n);q.consume(c,n);frames+=n/4;q.played(frames);}
        assertArrayEquals(expected,result.toByteArray());assertEquals(0,q.outstanding());
    }
    @Test public void totalWindowIncludesAudioTrackAndRejectsGaps()throws Exception{
        AudioPlayoutQueue q=new AudioPlayoutQueue(102,1,0,8000,1,300);
        byte[] chunk=new byte[8000];q.offer(0,chunk,0);q.offer(4000,chunk,0);
        AudioPlayoutQueue.Chunk c=q.peek();q.consume(c,8000); // submitted to hardware but not played
        try{q.offer(8000,new byte[2],0);fail("hardware frames must consume credit");}catch(IOException expected){}
        q.played(1);q.offer(8000,new byte[2],0);
        try{q.offer(8000,new byte[2],0);fail("duplicate sample");}catch(IOException expected){}
        q.cancel();assertEquals(0,q.softwareFrames());
    }
    @Test public void startupTailAndHardwareCapacityDoNotDeadlock(){
        assertEquals(99,AudioProgress.startFrames(14400,100));
        assertFalse(AudioProgress.ready(100,14400,false,0));
        assertTrue(AudioProgress.ready(100,14400,true,0));
        assertTrue(AudioProgress.ready(100,14400,false,150_000_000));
        assertFalse(AudioProgress.ready(0,14400,true,1_000_000_000));
    }
    @Test public void hardwareHeadWrapPreservesMonotonicPlayedFrames(){
        AudioProgress p=new AudioProgress();p.written(0x1_0000_0100L);
        assertEquals(0xfffffff0L,p.update(0xfffffff0));assertEquals(0x1_0000_0020L,p.update(0x20));
        assertEquals(224,p.queued());
    }
    @Test public void v2PcmFrameRoundtripKeepsFormatAndTimeline()throws Exception{
        ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] pcm={1,2,3,4};
        AudioWire.pcmV2(new DataOutputStream(b),102,7,0,44100,2,9876,pcm,0,4);
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(b.toByteArray()));
        assertEquals(5,in.readUnsignedByte());assertEquals(102,in.readInt());assertEquals(7,in.readInt());assertEquals(0,in.readUnsignedByte());
        assertEquals(44100,in.readInt());assertEquals(2,in.readUnsignedByte());assertEquals(9876,in.readLong());assertEquals(4,in.readUnsignedShort());
        byte[] got=new byte[4];in.readFully(got);assertArrayEquals(pcm,got);
    }
}
