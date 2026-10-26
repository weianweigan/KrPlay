package com.krplay.client;

import android.app.Dialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Process;
import android.view.View;
import android.view.ViewGroup;
import com.krplay.protocol.PerformanceStats;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNetwork;
import org.robolectric.shadows.ShadowProcess;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class) @Config(sdk=29)
public class ExitAppTest {
    private Object get(Object owner,String name)throws Exception {Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private void set(Object owner,String name,Object value)throws Exception {Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);}
    private View exitButton(View v){if("退出".contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){View found=exitButton(group.getChildAt(i));if(found!=null)return found;}}return null;}

    @Test public void exitWorksWithoutAControlConnectionAndKillsProcessAfterDestruction()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity=controller.get();
        try {
            assertNull(get(activity,"control"));
            View exit=exitButton(activity.getWindow().getDecorView());assertNotNull(exit);exit.performClick();
            assertTrue(activity.isFinishing());assertTrue((Boolean)get(activity,"exiting"));assertFalse((Boolean)get(activity,"active"));
            assertNull(get(activity,"lanChanges"));assertFalse(ShadowProcess.wasKilled(Process.myPid()));
            // A queued lifecycle or connection request must not restart services during exit.
            activity.onStart();
            Method start=MainActivity.class.getDeclaredMethod("startConnections");start.setAccessible(true);start.invoke(activity);
            assertFalse((Boolean)get(activity,"active"));assertNull(get(activity,"control"));
        } finally {controller.destroy();}
        assertTrue(ShadowProcess.wasKilled(Process.myPid()));
    }

    // Robolectric 4.17's Wi-Fi shadow references BlockingOption, which requires API 36.
    @Test @Config(sdk=36) @SuppressWarnings("unchecked") public void exitClosesEveryConnectionAndReleasesLocalResources()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity=controller.get();
        TouchControlClient control=new TouchControlClient("localhost",s->{},j->{});
        NaluStreamPlayer player=new NaluStreamPlayer("localhost",()->{},s->{},(w,h)->{},new PerformanceStats());
        ClientAudio audio=new ClientAudio(activity,"localhost");
        ReverseSocks5Worker socks=new ReverseSocks5Worker(activity,"localhost",s->{});
        AutoDiscovery discovery=new AutoDiscovery(activity,"",(h,n)->{});
        LinkDiagnostics diagnostics=new LinkDiagnostics();
        Socket[] sockets={new Socket(),new Socket(),new Socket(),new Socket(),new Socket(),new Socket()};
        try {
            Object[] workers={control,player,audio,socks};String[] names={"control","player","audio","socks"};
            for(int i=0;i<workers.length;i++){set(workers[i],"running",true);set(activity,names[i],workers[i]);}
            set(control,"socket",sockets[0]);set(player,"socket",sockets[1]);set(audio,"socket",sockets[2]);
            ((Set<Socket>)get(socks,"sockets")).add(sockets[3]);((Set<Socket>)get(discovery,"sockets")).add(sockets[4]);set(diagnostics,"socket",sockets[5]);
            set(activity,"discovery",discovery);set(activity,"diagnostics",diagnostics);set(activity,"active",true);
            WifiManager.WifiLock lock=((WifiManager)activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY,"exit-test");lock.acquire();set(activity,"wifiLock",lock);
            LocalTransport.network=ShadowNetwork.newInstance(42);
            Dialog dialog=new Dialog(activity);dialog.show();set(activity,"networkDialog",dialog);
            Handler ticker=(Handler)get(activity,"ticker");Runnable reconnect=(Runnable)get(activity,"reconnectNetwork");ticker.postDelayed(reconnect,500);
            exitButton(activity.getWindow().getDecorView()).performClick();
            for(Object worker:workers)assertFalse((Boolean)get(worker,"running"));
            for(Socket socket:sockets)assertTrue(socket.isClosed());
            assertTrue((Boolean)get(diagnostics,"closed"));assertNull(get(activity,"discovery"));assertFalse(lock.isHeld());assertNull(LocalTransport.network);assertNull(get(activity,"lanChanges"));assertFalse(ticker.hasCallbacks(reconnect));assertFalse(dialog.isShowing());
            // Android subsequently calls onStop and onDestroy: cleanup must tolerate repetition.
            activity.onStop();
        } finally {controller.destroy();discovery.close();for(Socket socket:sockets)socket.close();LocalTransport.network=null;}
    }

    @Test public void ordinaryDestructionDoesNotKillTheProcess(){
        Robolectric.buildActivity(MainActivity.class).create().destroy();
        assertFalse(ShadowProcess.wasKilled(Process.myPid()));
    }
}
