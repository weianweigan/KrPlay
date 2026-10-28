package com.krplay.client;

import com.krplay.protocol.PerformanceStats;
import com.krplay.protocol.Wire;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=29)
public class NaluStreamPlayerTest {
    private static Object field(Object owner,String name)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private static void set(Object owner,String name,Object value)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);
    }
    private static void await(BooleanSupplier condition)throws Exception {
        long end=System.nanoTime()+3_000_000_000L;
        while(!condition.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(5);
        assertTrue("Timed out waiting for video reader",condition.getAsBoolean());
    }
    private static Thread loop(NaluStreamPlayer player,String method)throws Exception {
        Method m=NaluStreamPlayer.class.getDeclaredMethod(method);m.setAccessible(true);
        Thread thread=new Thread(()->{try{m.invoke(player);}catch(Exception e){throw new AssertionError(e);}});
        thread.start();return thread;
    }

    @Test public void decoderBacklogKeepsTcpAndResumesAtKeyframeWithMetadata()throws Exception {backlog(false);}
    @Test public void timedHevcBacklogKeepsTcpAndResumesAtKeyframe()throws Exception {backlog(true);}
    private void backlog(boolean timedHevc)throws Exception {
        AtomicInteger requests=new AtomicInteger();
        NaluStreamPlayer player=new NaluStreamPlayer("127.0.0.1",requests::incrementAndGet,s->{},(w,h)->{},new PerformanceStats());
        LocalTransport.network=null;
        byte[] csd={0,0,0,1,0x67,1,0,0,0,1,0x68,2},key={0,0,1,0x65,3},delta={0,0,1,0x41,3};
        int configType=2,frameType=0;
        if(timedHevc){
            csd=new byte[]{0,0,0,1,0x40,1,0,0,0,1,0x42,1,0,0,0,1,0x44,1};
            key=new byte[22];key[18]=1;key[19]=0x26;key[20]=1;
            delta=new byte[22];delta[18]=1;delta[19]=2;delta[20]=1;
            configType=3;frameType=21;
        }
        BlockingQueue<Object> queue=(BlockingQueue<Object>)field(player,"queue"),free=(BlockingQueue<Object>)field(player,"free");
        try(ServerSocket server=new ServerSocket(8081,1,InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(3000);set(player,"running",true);
            Thread reader=loop(player,"loop");set(player,"reader",reader);
            try(Socket peer=server.accept()) {
                DataOutputStream out=new DataOutputStream(peer.getOutputStream());
                Wire.frame(out,4,0,new byte[]{5,0,2,(byte)208},0,4);
                Wire.frame(out,configType,0,csd,0,csd.length);
                Wire.frame(out,frameType,1,key,0,key.length);
                Wire.frame(out,frameType,2,delta,0,delta.length);
                for(int i=3;i<=11;i++)Wire.frame(out,frameType,i,delta,0,delta.length);out.flush();
                // Hold the decoder until immediate bounded-queue recovery.
                await(()->player.overflows.get()>0||player.reconnects.get()>0);
                assertEquals(0,player.reconnects.get());assertEquals(2,queue.size());
                Object p;while((p=queue.poll())!=null)free.add(p);
                Wire.frame(out,frameType,4,delta,0,delta.length);
                Wire.frame(out,configType,0,csd,0,csd.length);
                Wire.frame(out,frameType,5,key,0,key.length);
                Wire.frame(out,frameType,6,delta,0,delta.length);out.flush();
                await(()->queue.size()==3);
                Object config=queue.remove(),idr=queue.remove(),following=queue.remove();
                assertEquals(configType,field(config,"type"));assertEquals(5L,field(idr,"pts"));assertEquals(6L,field(following,"pts"));
                assertEquals(1,field(idr,"recovery"));assertEquals(field(idr,"recovery"),field(following,"recovery"));
                free.add(config);free.add(idr);free.add(following);
                assertEquals(0,player.reconnects.get());assertTrue(requests.get()>=1&&requests.get()<=3);
            }finally{player.close();reader.join(3000);assertFalse(reader.isAlive());}
        }
    }

    @Test public void silentConnectionRetriesMissingKeyframeWithoutReconnecting()throws Exception {
        AtomicInteger requests=new AtomicInteger();
        NaluStreamPlayer player=new NaluStreamPlayer("127.0.0.1",requests::incrementAndGet,s->{},(w,h)->{},new PerformanceStats());
        LocalTransport.network=null;
        try(ServerSocket server=new ServerSocket(8081,1,InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(3000);set(player,"running",true);
            Thread reader=loop(player,"loop");set(player,"reader",reader);
            try(Socket peer=server.accept()) {
                await(()->requests.get()>=2);
                assertEquals(0,player.reconnects.get());assertTrue(requests.get()<=3);
                assertTrue(player.waitingForKeyFrame());assertEquals(-1,player.videoIdleMs());
            }finally{player.close();reader.join(3000);assertFalse(reader.isAlive());}
        }
    }

    @Test public void codecFailureReportsReasonWithoutClosingVideoSocket()throws Exception {
        AtomicReference<String> status=new AtomicReference<>("");AtomicBoolean closed=new AtomicBoolean();
        NaluStreamPlayer player=new NaluStreamPlayer("127.0.0.1",()->{},status::set,(w,h)->{},new PerformanceStats());
        Socket socket=new Socket(){@Override public synchronized void close(){closed.set(true);}};
        BlockingQueue<Object> free=(BlockingQueue<Object>)field(player,"free"),queue=(BlockingQueue<Object>)field(player,"queue");
        Object packet=free.remove();set(packet,"type",4);set(packet,"n",4); // Zero dimensions trigger a codec-loop IOException.
        queue.add(packet);set(player,"socket",socket);set(player,"running",true);
        Thread decoder=loop(player,"decodeLoop");set(player,"codecThread",decoder);
        try{await(()->status.get().contains("Invalid resolution"));assertFalse(closed.get());assertEquals(0,player.reconnects.get());}
        finally{player.close();decoder.join(3000);assertFalse(decoder.isAlive());}
    }
    @Test public void serverCanInvalidateAHalfOpenVideoConnectionWithoutTreatingStaticVideoAsFailure()throws Exception {
        NaluStreamPlayer player=new NaluStreamPlayer("127.0.0.1",()->{},s->{},(w,h)->{},new PerformanceStats());
        AtomicBoolean closed=new AtomicBoolean();Socket socket=new Socket(){@Override public synchronized void close(){closed.set(true);}};
        set(player,"socket",socket);set(player,"connectedAtNs",System.nanoTime());
        player.serverHasVideoClient(false);assertFalse("Allow new video/control connections to settle",closed.get());
        set(player,"connectedAtNs",System.nanoTime()-3_000_000_000L);
        player.serverHasVideoClient(true);assertFalse("A static screen with a valid server socket stays connected",closed.get());
        player.serverHasVideoClient(false);assertTrue("A server-confirmed stale socket must reconnect",closed.get());
    }
}
