package com.zeekr.bridge

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.*
import android.os.*
import android.view.*
import com.krplay.protocol.Wire
import java.io.*
import java.nio.ByteBuffer

/** Runs only as UID 0 on the Android 9 box, using the installed APK's own DEX. */
object BoxRootMain {
    @JvmStatic fun main(args:Array<String>) {
        check(android.os.Process.myUid()==0){"Virtual display requires root"}
        Looper.prepareMainLooper()
        // Needed by the Android 9 hidden InputManager / ActivityThread APIs in this root worker.
        val runtime=Class.forName("dalvik.system.VMRuntime");val vm=runtime.getDeclaredMethod("getRuntime").invoke(null)
        runtime.getDeclaredMethod("setHiddenApiExemptions",Array<String>::class.java).invoke(vm,arrayOf("L"))
        val activityThread=Class.forName("android.app.ActivityThread")
        val at=activityThread.getMethod("systemMain").invoke(null)
        val context=activityThread.getMethod("getSystemContext").invoke(at) as Context
        val requestedW=args[0].toInt();val requestedH=args[1].toInt();val requestedFps=args[2].toInt();val launcher=args[3]
        val candidates=MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { it.isEncoder && "video/avc" in it.supportedTypes && !it.name.startsWith("OMX.google.") && !it.name.startsWith("c2.android.") }
        val selected=candidates.firstOrNull { it.name.equals("OMX.amlogic.video.encoder.avc",true) } ?: candidates.firstOrNull() ?: error("No hardware AVC encoder")
        val caps=selected.getCapabilitiesForType("video/avc").videoCapabilities ?: error("No video encoder capabilities")
        var scale=minOf(1.0,caps.supportedWidths.upper.toDouble()/requestedW,caps.supportedHeights.upper.toDouble()/requestedH)
        var w=0;var h=0
        for(attempt in 0 until 20){
            w=(requestedW*scale/caps.widthAlignment).toInt()*caps.widthAlignment
            h=(requestedH*scale/caps.heightAlignment).toInt()*caps.heightAlignment
            if(w>0 && h>0 && caps.isSizeSupported(w,h))break
            scale*=0.9
        }
        check(w>=320 && h>=240 && caps.isSizeSupported(w,h)){"Unsupported virtual display resolution"}
        val fps=minOf(requestedFps,caps.getSupportedFrameRatesFor(w,h).upper.toInt()).coerceAtLeast(1)
        System.err.println("KrPlay encoder ${selected.name}: ${w}x$h @$fps")
        val codec=MediaCodec.createByCodecName(selected.name)
        val format=MediaFormat.createVideoFormat("video/avc",w,h).apply{
            setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE,10_000_000);setInteger(MediaFormat.KEY_FRAME_RATE,fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER,1_000_000L/fps)
        }
        codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface=codec.createInputSurface();codec.start()
        val display=(context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager).createVirtualDisplay("KrPlay-Box",w,h,240,surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION)
        val displayId=display.display.displayId
        fun home(){val p=ProcessBuilder("am","start","--display",displayId.toString(),"-n",launcher).redirectErrorStream(true).start();p.inputStream.use{it.copyTo(System.err)};check(p.waitFor()==0){"Cannot launch KrPlay box app launcher"}}
        val output=DataOutputStream(BufferedOutputStream(System.out,65536))
        val resolution=ByteBuffer.allocate(4).putShort(w.toShort()).putShort(h.toShort()).array()
        synchronized(output){Wire.frame(output,4,0,resolution,0,4);output.flush()}
        val active=java.util.concurrent.atomic.AtomicBoolean(true)
        val drainer=Thread({val info=MediaCodec.BufferInfo();val bytes=ByteArray(Wire.MAX_FRAME)
            try{while(active.get()){val idx=codec.dequeueOutputBuffer(info,10000)
                if(idx==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED){val f=codec.outputFormat;for(key in listOf("csd-0","csd-1")){val b=f.getByteBuffer(key)?:continue;val n=b.remaining();check(n<=bytes.size);b.get(bytes,0,n);synchronized(output){Wire.frame(output,2,0,bytes,0,n);output.flush()}}}
                else if(idx>=0){try{val b=codec.getOutputBuffer(idx);if(b!=null&&info.size>0){check(info.size<=bytes.size);b.position(info.offset);b.limit(info.offset+info.size);b.get(bytes,0,info.size);synchronized(output){Wire.frame(output,if(info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG!=0)2 else 0,info.presentationTimeUs,bytes,0,info.size);output.flush()}}}finally{codec.releaseOutputBuffer(idx,false)}}
            }}catch(e:Exception){e.printStackTrace(System.err);active.set(false);runCatching{System.`in`.close()}}
        },"KrPlay-Root-Encoder");drainer.start()
        val manager=context.getSystemService(Context.INPUT_SERVICE)
        val setDisplay=runCatching{InputEvent::class.java.getMethod("setDisplayId",Int::class.javaPrimitiveType)}.getOrNull()
        val forwarder=if(setDisplay==null)manager.javaClass.getMethod("createInputForwarder",Int::class.javaPrimitiveType).invoke(manager,displayId) else null
        val forward=forwarder?.let{Class.forName("android.app.IInputForwarder").getMethod("forwardEvent",InputEvent::class.java)}
        val inject=if(setDisplay!=null)manager.javaClass.getMethod("injectInputEvent",InputEvent::class.java,Int::class.javaPrimitiveType) else null
        fun injectEvent(event:InputEvent){
            val success=if(setDisplay!=null){setDisplay.invoke(event,displayId);inject!!.invoke(manager,event,0)}else forward!!.invoke(forwarder,event)
            check(success==true){"Input injection denied"}
        }
        var downTime=0L;var contacts=emptyList<Int>();var lastTouch:Wire.Touch?=null
        fun touch(received:Wire.Touch){
            if(received.action==3 && contacts.isEmpty())return
            val t=if(received.action==3 && lastTouch!=null)lastTouch!!.let{Wire.Touch(3,it.ids,it.xs,it.ys)} else received
            val now=SystemClock.uptimeMillis();val changed=t.ids[0]
            val eventAction=when(t.action){0->if(contacts.isEmpty()){downTime=now;MotionEvent.ACTION_DOWN}else MotionEvent.ACTION_POINTER_DOWN
                1->if(contacts.size<=1)MotionEvent.ACTION_UP else MotionEvent.ACTION_POINTER_UP
                2->MotionEvent.ACTION_MOVE;else->MotionEvent.ACTION_CANCEL}
            val props=Array(t.ids.size){i->MotionEvent.PointerProperties().apply{id=t.ids[i];toolType=MotionEvent.TOOL_TYPE_FINGER}}
            val coords=Array(t.ids.size){i->MotionEvent.PointerCoords().apply{x=t.xs[i]*(w-1);y=t.ys[i]*(h-1);pressure=if(t.action==3)0f else 1f;size=1f}}
            val e=MotionEvent.obtain(downTime,now,eventAction,t.ids.size,props,coords,0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
            try{injectEvent(e)}finally{e.recycle()}
            lastTouch=t
            contacts=when(t.action){1->t.ids.filter{it!=changed};3->emptyList();else->t.ids.toList()}
        }
        try{home();val input=DataInputStream(System.`in`);while(active.get()){when(input.readUnsignedByte()){
            2->touch(Wire.Touch.read(input))
            3->{val action=input.readUnsignedByte();val key=input.readInt();if(key==KeyEvent.KEYCODE_HOME){if(action==0)home()}else{val now=SystemClock.uptimeMillis();val e=KeyEvent(now,now,action,key,0,0,-1,0,0,InputDevice.SOURCE_KEYBOARD);injectEvent(e)}}
            4->{input.readUnsignedByte();codec.setParameters(Bundle().apply{putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME,0)})}
            127->break;else->error("Unknown root command")
        }}}finally{active.set(false);display.release();drainer.join(1000);runCatching{codec.stop()};codec.release();surface.release();output.close()}
        kotlin.system.exitProcess(0)
    }
}
