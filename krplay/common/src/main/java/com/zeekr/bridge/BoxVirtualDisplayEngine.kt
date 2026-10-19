package com.zeekr.bridge

import android.content.Context
import android.util.Log
import com.krplay.protocol.Wire
import java.io.*

/** Root is isolated to an app_process worker. Ordinary APK never claims signature permissions. */
class BoxVirtualDisplayEngine(private val context:Context,private val stream:RawTcpNaluServer) {
    @Volatile private var process:Process?=null
    private var commands:DataOutputStream?=null
    @Synchronized fun start(width:Int,height:Int,fps:Int) {
        stop()
        val apk=context.applicationInfo.sourceDir
        require(!apk.contains("'"))
        val command="CLASSPATH='$apk' app_process /system/bin com.zeekr.bridge.BoxRootMain $width $height $fps ${context.packageName}/com.zeekr.bridge.BoxLauncherActivity"
        val p=ProcessBuilder("su","-c",command).start();process=p;commands=DataOutputStream(p.outputStream)
        Thread({try{p.errorStream.bufferedReader().useLines{lines->lines.forEach{Log.w("KrPlay-Root",it)}}}catch(_:Exception){}},"KrPlay-Root-Log").start()
        Thread({val data=ByteArray(Wire.MAX_FRAME);try{val input=DataInputStream(BufferedInputStream(p.inputStream,65536));while(process===p){val f=Wire.header(input);input.readFully(data,0,f.size);synchronized(this){if(process!==p)return@synchronized;when(f.type){
            4->{val w=((data[0].toInt()and 255)shl 8)or(data[1].toInt()and 255);val h=((data[2].toInt()and 255)shl 8)or(data[3].toInt()and 255);stream.updateResolution(w,h)}
            0,2->stream.pushVideoNalu(false,f.pts,data,0,f.size)
            16->stream.pushAudioPcm(f.pts,data,0,f.size)
        }}}}catch(e:Exception){if(process===p)Log.e("KrPlay-Root","Virtual display worker failed; check root authorization",e)}finally{synchronized(this){if(process===p)stop()}}},"KrPlay-Root-Video").start()
    }
    @Synchronized fun touch(t:Wire.Touch){try{commands?.let{t.write(it);it.flush()}}catch(e:IOException){Log.e("KrPlay-Root","Touch failed",e)}}
    @Synchronized fun key(action:Int,code:Int){try{commands?.let{it.writeByte(3);it.writeByte(action);it.writeInt(code);it.flush()}}catch(e:IOException){Log.e("KrPlay-Root","Key failed",e)}}
    @Synchronized fun requestKeyFrame(){try{commands?.let{it.writeByte(4);it.writeByte(0);it.flush()}}catch(_:IOException){}}
    @Synchronized fun stop(){val p=process;process=null;runCatching{commands?.writeByte(127);commands?.flush()};runCatching{commands?.close()};commands=null;if(p!=null)Thread({try{if(!p.waitFor(2,java.util.concurrent.TimeUnit.SECONDS))p.destroy()}catch(_:Exception){p.destroy()}},"KrPlay-Root-Stop").start()}
}
