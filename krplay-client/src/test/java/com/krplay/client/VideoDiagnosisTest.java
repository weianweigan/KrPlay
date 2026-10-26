package com.krplay.client;

import com.krplay.protocol.PerformanceStats;
import java.lang.reflect.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=29)
public class VideoDiagnosisTest {
    private static Object field(Object owner,String name)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private static void set(Object owner,String name,Object value)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);
    }
    private static String diagnosis(MainActivity activity)throws Exception {
        Method m=MainActivity.class.getDeclaredMethod("diagnosis");m.setAccessible(true);return (String)m.invoke(activity);
    }
    @Test public void largeTransferDelayIsNotMisdiagnosedAsDecoderOverload()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        try {
            MainActivity activity=controller.get();PerformanceStats clock=(PerformanceStats)field(activity,"clock");
            for(int i=0;i<3;i++)clock.probe(1_000_000_000L,1_000_000_000L,1_000_000_000L,1_010_000_000L);
            NaluStreamPlayer player=new NaluStreamPlayer("127.0.0.1",()->{},s->{},(w,h)->{},clock);
            set(activity,"player",player);set(player,"waitKey",false);set(player,"lastVideoReceiveNs",System.nanoTime());
            player.overflows.set(5);player.transferMs=664.7;player.decodeReadyMs=16.4;
            assertTrue(diagnosis(activity).contains("传输/接收耗时偏高"));
        }finally{controller.destroy();}
    }
    @Test public void waitingForKeyframeIsReportedWithoutRequiringClockSync()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        try {
            MainActivity activity=controller.get();set(activity,"player",new NaluStreamPlayer("127.0.0.1",()->{},s->{},(w,h)->{},new PerformanceStats()));
            assertTrue(diagnosis(activity).contains("等待关键帧"));
        }finally{controller.destroy();}
    }
    @Test public void stoppedStreamDoesNotPresentOldLatencyAsALiveMeasurement()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        try {
            MainActivity activity=controller.get();NaluStreamPlayer player=new NaluStreamPlayer("127.0.0.1",()->{},s->{},(w,h)->{},new PerformanceStats());
            set(activity,"player",player);set(player,"waitKey",false);set(player,"lastVideoReceiveNs",System.nanoTime()-2_000_000_000L);
            player.transferMs=664.7;assertTrue(diagnosis(activity).contains("历史帧"));
        }finally{controller.destroy();}
    }
}
