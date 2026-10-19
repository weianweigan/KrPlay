package com.krplay.client;
import android.app.*;
import android.content.*;
import android.net.*;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.net.Socket;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=29)
public class ConnectionUxTest {
    private Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private List<CheckBox> checks(View v){List<CheckBox> result=new ArrayList<>();if(v instanceof CheckBox)result.add((CheckBox)v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)result.addAll(checks(((ViewGroup)v).getChildAt(i)));return result;}
    @After public void clearBinding(){LocalTransport.network=null;}
    @Test public void firstLaunchUsesAutomaticConnectionAndHidesBothOverlays()throws Exception {
        android.content.SharedPreferences p=RuntimeEnvironment.getApplication().getSharedPreferences("MainActivity",0);
        p.edit().putBoolean("showPerformance",true).commit(); // Previous diagnostics must not pin the new clean default.
        org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).create();
        try{MainActivity a=c.get();assertTrue((Boolean)field(a,"automaticConnection"));assertEquals(View.GONE,((View)field(a,"status")).getVisibility());assertEquals(View.GONE,((View)field(a,"performanceHud")).getVisibility());}
        finally{c.destroy();}
    }
    @Test public void overlayControlsAreIndependentAndPersistAcrossActivityRecreation()throws Exception {
        org.robolectric.android.controller.ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).create();
        try{MainActivity a=c.get();Method show=MainActivity.class.getDeclaredMethod("showPerformance");show.setAccessible(true);show.invoke(a);
            Dialog d=ShadowDialog.getLatestDialog();List<CheckBox> boxes=checks(d.getWindow().getDecorView());assertEquals(2,boxes.size());
            boxes.get(0).setChecked(true);assertEquals(View.VISIBLE,((View)field(a,"status")).getVisibility());assertEquals(View.GONE,((View)field(a,"performanceHud")).getVisibility());
            boxes.get(1).setChecked(true);assertEquals(View.VISIBLE,((View)field(a,"performanceHud")).getVisibility());
            boxes.get(0).setChecked(false);assertEquals(View.GONE,((View)field(a,"status")).getVisibility());d.dismiss();
        }finally{c.destroy();}
        c=Robolectric.buildActivity(MainActivity.class).create();try{assertEquals(View.GONE,((View)field(c.get(),"status")).getVisibility());assertEquals(View.VISIBLE,((View)field(c.get(),"performanceHud")).getVisibility());}finally{c.destroy();}
    }
    @Test public void discoveryAndLocalSocketsUseWifiEvenWhenCellularIsDefault()throws Exception {
        Context app=RuntimeEnvironment.getApplication();ConnectivityManager cm=(ConnectivityManager)app.getSystemService(Context.CONNECTIVITY_SERVICE);ShadowConnectivityManager shadow=Shadows.shadowOf(cm);shadow.clearAllNetworks();
        Network cell=ShadowNetwork.newInstance(41),wifi=ShadowNetwork.newInstance(42);
        NetworkInfo cellInfo=ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED,ConnectivityManager.TYPE_MOBILE,0,true,true);
        shadow.addNetwork(cell,cellInfo);shadow.addNetwork(wifi,ShadowNetworkInfo.newInstance(NetworkInfo.DetailedState.CONNECTED,ConnectivityManager.TYPE_WIFI,0,true,true));shadow.setActiveNetworkInfo(cellInfo);
        NetworkCapabilities cellCaps=ShadowNetworkCapabilities.newInstance(),wifiCaps=ShadowNetworkCapabilities.newInstance();Shadows.shadowOf(cellCaps).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);Shadows.shadowOf(wifiCaps).addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        shadow.setNetworkCapabilities(cell,cellCaps);shadow.setNetworkCapabilities(wifi,wifiCaps);shadow.setLinkProperties(wifi,new LinkProperties());
        assertEquals(wifi,LocalTransport.preferred(app));LocalTransport.network=wifi;
        try(Socket socket=new Socket()){LocalTransport.bind(socket);assertTrue(Shadows.shadowOf(wifi).isSocketBound(socket));assertFalse(Shadows.shadowOf(cell).isSocketBound(socket));}
        AutoDiscovery discovery=new AutoDiscovery(app,"192.168.43.55",(h,n)->{});
        try{Method m=AutoDiscovery.class.getDeclaredMethod("candidates");m.setAccessible(true);List<?> candidates=(List<?>)m.invoke(discovery);boolean fixed=false;
            for(Object candidate:candidates){assertEquals(wifi,field(candidate,"network"));if("6.6.6.6".equals(field(candidate,"host")))fixed=true;}assertTrue(fixed);
        }finally{discovery.close();}
    }
}
