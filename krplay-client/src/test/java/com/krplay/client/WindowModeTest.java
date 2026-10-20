package com.krplay.client;

import android.app.Dialog;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=29)
public class WindowModeTest {
    private static void assertNoFullscreenRequest(Window window) {
        assertEquals(0,window.getAttributes().flags&WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int immersive=View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            |View.SYSTEM_UI_FLAG_IMMERSIVE|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        assertEquals(0,window.getDecorView().getSystemUiVisibility()&immersive);
    }
    @Test public void launchLeavesWindowModeToTheHeadUnit() {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        try{assertNoFullscreenRequest(controller.get().getWindow());}
        finally{controller.destroy();}
    }
    @Test public void outsidePerformanceDialogDismissalDoesNotRestoreImmersiveMode()throws Exception {outsideDismissal("showPerformance");}
    @Test public void outsideConnectionSettingsDismissalDoesNotRestoreImmersiveMode()throws Exception {outsideDismissal("settings");}
    private static void outsideDismissal(String method)throws Exception {
        ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity=controller.get();Dialog dialog=null;
        try {
            // Custom vehicle splits may not report Android multi-window mode. The safe
            // policy must hold without relying on isInMultiWindowMode().
            assertFalse(activity.isInMultiWindowMode());
            for(int i=0;i<2;i++) {
                Method show=MainActivity.class.getDeclaredMethod(method);show.setAccessible(true);show.invoke(activity);
                dialog=ShadowDialog.getLatestDialog();assertTrue(dialog.isShowing());
                assertNoFullscreenRequest(dialog.getWindow());
                MotionEvent outside=MotionEvent.obtain(0,0,MotionEvent.ACTION_OUTSIDE,0,0,0);
                try{assertTrue(dialog.onTouchEvent(outside));}finally{outside.recycle();}
                assertFalse(dialog.isShowing());
                activity.onWindowFocusChanged(true);
                assertNoFullscreenRequest(activity.getWindow());
            }
        }finally{if(dialog!=null)dialog.dismiss();controller.destroy();}
    }
}
