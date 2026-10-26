package com.krplay.client;

import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import java.lang.reflect.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk={29,32})
public class ClientWindowInsetsTest {
    private static Object field(Object owner,String name)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private static void set(Object owner,String name,Object value)throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);
    }
    private static void layout(View root) {
        root.measure(View.MeasureSpec.makeMeasureSpec(1280,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));
        root.layout(0,0,1280,800);
    }
    private static void insets(View root,int left,int top,int right,int bottom) {
        root.dispatchApplyWindowInsets(new WindowInsets.Builder().setSystemWindowInsets(Insets.of(left,top,right,bottom)).build());
        layout(root);
    }
    private static void sendViewport(MainActivity activity)throws Exception {
        Method send=MainActivity.class.getDeclaredMethod("sendViewport");send.setAccessible(true);send.invoke(activity);
    }
    @Test public void videoAndReportedViewportExcludeVehicleToolbar()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        try {
            MainActivity activity=controller.get();FrameLayout stage=(FrameLayout)field(activity,"stage");View root=(View)stage.getParent();
            TouchControlClient control=new TouchControlClient("127.0.0.1",s->{},s->{});set(activity,"control",control);set(activity,"videoProfile",0);
            insets(root,0,0,0,96);
            assertEquals(704,stage.getHeight());assertEquals(704,stage.getBottom());
            sendViewport(activity);assertEquals(1280,field(control,"width"));assertEquals(704,field(control,"height"));
            // Re-dispatch after dialog dismissal must not accumulate toolbar padding.
            insets(root,0,0,0,96);assertEquals(704,stage.getHeight());
            insets(root,0,0,0,64);assertEquals(736,stage.getHeight());
            insets(root,0,0,0,0);assertEquals(800,stage.getHeight());
        }finally{controller.destroy();}
    }
    @Test public void alreadyConsumedInsetsDoNotReserveTheToolbarTwice()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        try {
            FrameLayout stage=(FrameLayout)field(controller.get(),"stage");View root=(View)stage.getParent();
            insets(root,0,0,0,0);assertEquals(0,root.getPaddingBottom());assertEquals(800,stage.getHeight());
            insets(root,48,24,0,96);assertEquals(1232,stage.getWidth());assertEquals(704,stage.getHeight());
            assertEquals(0,stage.getTop());
            View controls=((android.view.ViewGroup)root).getChildAt(1);
            assertEquals(24,controls.getTop());assertEquals(680,controls.getHeight());
        }finally{controller.destroy();}
    }
    @Test public void cutoutStillProtectsVideoAndControls()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        try {
            FrameLayout stage=(FrameLayout)field(controller.get(),"stage");View root=(View)stage.getParent();
            android.graphics.Rect notch=new android.graphics.Rect(550,0,650,40);
            android.view.DisplayCutout cutout=new android.view.DisplayCutout(
                new android.graphics.Rect(0,40,0,0),java.util.Collections.singletonList(notch));
            WindowInsets insets=new WindowInsets.Builder().setSystemWindowInsets(Insets.of(0,24,0,96))
                .setDisplayCutout(cutout).build();
            root.dispatchApplyWindowInsets(insets);layout(root);
            assertEquals(40,stage.getTop());assertEquals(664,stage.getHeight());
            assertEquals(40,((android.view.ViewGroup)root).getChildAt(1).getTop());
        }finally{controller.destroy();}
    }
    @Test @Config(sdk=32) public void captionAndSideToolbarRemainProtected()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        try {
            FrameLayout stage=(FrameLayout)field(controller.get(),"stage");View root=(View)stage.getParent();
            WindowInsets insets=new WindowInsets.Builder()
                .setInsets(WindowInsets.Type.statusBars(),Insets.of(0,24,0,0))
                .setInsets(WindowInsets.Type.captionBar(),Insets.of(0,48,0,0))
                .setInsets(WindowInsets.Type.navigationBars(),Insets.of(0,0,80,96)).build();
            root.dispatchApplyWindowInsets(insets);layout(root);
            assertEquals(48,stage.getTop());assertEquals(1200,stage.getWidth());assertEquals(656,stage.getHeight());
            // Re-dispatch and split-window resize must retain the same safe area.
            root.dispatchApplyWindowInsets(insets);
            root.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));
            root.layout(0,0,900,600);
            assertEquals(820,stage.getWidth());assertEquals(456,stage.getHeight());
        }finally{controller.destroy();}
    }
    @Test public void unmeasuredViewportNeverFallsBackToPhysicalScreenSize()throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        try {
            MainActivity activity=controller.get();TouchControlClient control=new TouchControlClient("127.0.0.1",s->{},s->{});
            control.size(1000,600);set(activity,"control",control);sendViewport(activity);
            assertEquals(1000,field(control,"width"));assertEquals(600,field(control,"height"));
        }finally{controller.destroy();}
    }
}
