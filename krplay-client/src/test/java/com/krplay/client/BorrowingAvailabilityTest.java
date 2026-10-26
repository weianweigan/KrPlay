package com.krplay.client;

import android.view.View;
import android.view.ViewGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=29)
public class BorrowingAvailabilityTest {
    private boolean hasEntry(View view,String label){
        if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return true;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)
            if(hasEntry(((ViewGroup)view).getChildAt(i),label))return true;
        return false;
    }
    @Test public void borrowingEntryIsAbsentWhilePerformanceEntryRemains(){
        ActivityController<MainActivity> owner=Robolectric.buildActivity(MainActivity.class).create();
        try{View root=owner.get().getWindow().getDecorView();
            assertFalse(hasEntry(root,"网络"));assertTrue(hasEntry(root,"性能"));
        }finally{owner.destroy();}
    }
    @Test public void legacyWorkerStartDoesNotOpenBorrowingConnections()throws Exception{
        MainActivity activity=Robolectric.buildActivity(MainActivity.class).get();
        ReverseSocks5Worker worker=new ReverseSocks5Worker(activity,"127.0.0.1",s->{});
        try{worker.start();java.lang.reflect.Field field=ReverseSocks5Worker.class.getDeclaredField("maintainer");
            field.setAccessible(true);assertNull(field.get(worker));
        }finally{worker.close();}
    }
}
