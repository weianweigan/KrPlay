package com.krplay.client;

import android.media.MediaCodec;
import com.krplay.protocol.PerformanceStats;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import org.robolectric.shadow.api.Shadow;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=29,shadows=VideoCatchupTest.Codec.class)
public class VideoCatchupTest {
    @Implements(MediaCodec.class)
    public static class Codec {
        final ArrayDeque<Integer> outputs=new ArrayDeque<>();
        final List<String> releases=new ArrayList<>();
        @Implementation protected int dequeueOutputBuffer(MediaCodec.BufferInfo info,long timeout){
            Integer i=outputs.poll();if(i==null)return MediaCodec.INFO_TRY_AGAIN_LATER;
            info.presentationTimeUs=i*1000L;return i;
        }
        @Implementation protected void releaseOutputBuffer(int index,boolean render){releases.add(index+":"+render);}
    }
    private static Field field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
    private static void drain(NaluStreamPlayer p)throws Exception{Method m=NaluStreamPlayer.class.getDeclaredMethod("drain");m.setAccessible(true);m.invoke(p);}
    @Test public void decodedBurstOnlyPresentsLatestAndFinalStaticFrameIsNotLost()throws Exception{
        NaluStreamPlayer p=new NaluStreamPlayer("localhost",()->{},s->{},(w,h)->{},new PerformanceStats());
        MediaCodec codec=Shadow.newInstanceOf(MediaCodec.class);Codec fake=Shadow.extract(codec);field(p,"decoder").set(p,codec);
        fake.outputs.addAll(Arrays.asList(MediaCodec.INFO_OUTPUT_FORMAT_CHANGED,1,2,3));drain(p);
        assertEquals(Arrays.asList("1:false","2:false","3:true"),fake.releases);assertEquals(2,p.catchupFrames.get());
        fake.outputs.add(4);drain(p);assertEquals("4:true",fake.releases.get(3));
    }
    @Test public void queuedReferenceFramesSuppressOldOutputUntilBacklogIsConsumed()throws Exception{
        NaluStreamPlayer p=new NaluStreamPlayer("localhost",()->{},s->{},(w,h)->{},new PerformanceStats());
        MediaCodec codec=Shadow.newInstanceOf(MediaCodec.class);Codec fake=Shadow.extract(codec);field(p,"decoder").set(p,codec);
        BlockingQueue<Object> free=(BlockingQueue<Object>)field(p,"free").get(p),queue=(BlockingQueue<Object>)field(p,"queue").get(p);
        Object frame=free.remove();field(frame,"type").set(frame,21);queue.add(frame);
        fake.outputs.add(1);drain(p);assertEquals(Collections.singletonList("1:false"),fake.releases);
        free.add(queue.remove());fake.outputs.add(2);drain(p);assertEquals("2:true",fake.releases.get(1));
    }
}
