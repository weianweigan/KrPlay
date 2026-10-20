package com.krplay.client;

import android.graphics.Insets;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

/** Apply only insets delivered to the content; decor-consumed bars must not be counted twice. */
final class ClientWindowInsets {
    private ClientWindowInsets() {}
    static void fit(View content) {
        content.setOnApplyWindowInsetsListener((view,insets)->{
            int left,top,right,bottom;
            if(Build.VERSION.SDK_INT>=30){
                Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                left=bars.left;top=bars.top;right=bars.right;bottom=bars.bottom;
            }else{
                left=insets.getSystemWindowInsetLeft();top=insets.getSystemWindowInsetTop();
                right=insets.getSystemWindowInsetRight();bottom=insets.getSystemWindowInsetBottom();
                DisplayCutout cutout=insets.getDisplayCutout();
                if(cutout!=null){left=Math.max(left,cutout.getSafeInsetLeft());top=Math.max(top,cutout.getSafeInsetTop());
                    right=Math.max(right,cutout.getSafeInsetRight());bottom=Math.max(bottom,cutout.getSafeInsetBottom());}
            }
            view.setPadding(left,top,right,bottom);
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
