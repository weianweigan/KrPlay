package com.krplay.client;

import android.graphics.Insets;
import android.graphics.Color;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** Apply only insets delivered to the content; decor-consumed bars must not be counted twice. */
final class ClientWindowInsets {
    private ClientWindowInsets() {}
    static void configure(Window window) {
        // Layout behind a visible status bar; never hide bars or reapply flags on focus.
        // OEM launchers may interpret immersive requests as leaving their split mode.
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
            |WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setStatusBarContrastEnforced(false);
        View decor=window.getDecorView();
        if(Build.VERSION.SDK_INT>=30){
            window.setDecorFitsSystemWindows(false);
            window.getInsetsController().setSystemBarsAppearance(0,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }else{
            decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
    static void fit(View content,View controls) {
        content.setOnApplyWindowInsetsListener((view,insets)->{
            int left,top,right,bottom,videoTop;
            if(Build.VERSION.SDK_INT>=30){
                Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;
                // Status bar may overlay video; cutouts and window controls may not.
                videoTop=insets.getInsets(WindowInsets.Type.captionBar()|WindowInsets.Type.displayCutout()
                    |WindowInsets.Type.navigationBars()).top;
            }else{
                left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();
                right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();
                videoTop=0;
                DisplayCutout cutout=insets.getDisplayCutout();
                if(cutout!=null){left=Math.max(left,cutout.getSafeInsetLeft());top=Math.max(top,cutout.getSafeInsetTop());
                    right=Math.max(right,cutout.getSafeInsetRight());bottom=Math.max(bottom,cutout.getSafeInsetBottom());
                    videoTop=cutout.getSafeInsetTop();}
            }
            view.setPadding(left,videoTop,right,bottom);
            FrameLayout.LayoutParams params=(FrameLayout.LayoutParams)controls.getLayoutParams();
            int controlsTop=Math.max(0,top-videoTop);
            if(params.topMargin!=controlsTop){params.topMargin=controlsTop;controls.setLayoutParams(params);}
            return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:
                insets.consumeSystemWindowInsets().consumeDisplayCutout().consumeStableInsets();
        });
        content.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            public void onViewAttachedToWindow(View view){view.requestApplyInsets();}
            public void onViewDetachedFromWindow(View view){}
        });
        content.requestApplyInsets();
    }
}
