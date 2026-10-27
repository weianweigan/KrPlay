package com.zeekr.bridge

import android.content.Context
import com.krplay.protocol.AudioWire
import com.krplay.protocol.Ports
import com.krplay.protocol.Wire
import com.shilapi.xcertplay.*
import com.shilapi.xcertplay.airplay.AudioFormat
import com.shilapi.xcertplay.airplay.MicrophoneConfig
import com.shilapi.xcertplay.media.PcmInput
import com.shilapi.xcertplay.media.RemoteAudioRoute
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Session-owned remote route. CLIENT never silently activates the host microphone/speaker. */
class ClientAudioRoute(context:Context,port:Int=Ports.AUDIO):RemoteAudioRoute {
    private val outputClient=AirPlayPersistence.loadAudioOutput(context)==CarPlayAudioOutput.CLIENT
    private val inputSource=AirPlayPersistence.loadMicrophoneSource(context)
    @Volatile private var running=true
    @Volatile private var peer:Socket?=null
    @Volatile private var input:Input?=null
    @Volatile private var micAllowed=false
    @Volatile private var failure=""
    private val queue=com.krplay.protocol.RealtimeAudioQueue<ByteArray>()
    private val dropped=AtomicLong();private val sent=AtomicLong();private val captured=AtomicLong()
    private val ids=java.util.concurrent.atomic.AtomicInteger()
    private var routeListener:((Boolean)->Unit)?=null
    private val server=ServerSocket(port)
    val localPort get()=server.localPort
    override val microphoneEnabled get()=inputSource!=CarPlayMicrophoneSource.DISABLED
    override val playbackAvailable get()=outputClient&&peer?.isClosed==false
    init { current=this;Thread(::accept,"KrPlay-Audio-Accept").apply{isDaemon=true;start()} }
    private fun accept(){while(running){val s=try{server.accept()}catch(_:Exception){break}
        try{Wire.socket(s);s.soTimeout=5000
            val data=DataInputStream(s.getInputStream())
            require(data.readInt()==AudioWire.MAGIC&&data.readUnsignedByte()==AudioWire.VERSION)
            micAllowed=data.readBoolean();s.soTimeout=0
            synchronized(this){Wire.close(peer);queue.clear();peer=s;failure="";input?.request()}
            routeListener?.invoke(playbackAvailable)
            Thread({read(s,data)},"KrPlay-Audio-Read").apply{isDaemon=true;start()}
            val out=DataOutputStream(BufferedOutputStream(s.getOutputStream(),8192))
            while(running&&peer===s&&!s.isClosed){val packet=queue.poll(100)?:continue
                if(packet.durationNs>0 && System.nanoTime()-packet.enqueuedNs>com.krplay.protocol.RealtimeAudioQueue.MAX_AGE_NS){dropped.incrementAndGet();continue}
                out.write(packet.value);out.flush()}
        }catch(e:Exception){if(running)failure=e.javaClass.simpleName}
        finally{synchronized(this){if(peer===s){peer=null;micAllowed=false;queue.clear();input?.clear()}};Wire.close(s);routeListener?.invoke(playbackAvailable)}
    }}
    private fun read(s:Socket,data:DataInputStream){try{while(running&&peer===s){when(data.readUnsignedByte()){
        1->{val id=data.readInt();val n=data.readUnsignedShort();require(n in 2..AudioWire.MAX_PCM&&n%2==0);val bytes=ByteArray(n);data.readFully(bytes)
            input?.takeIf{it.id==id}?.let{AudioWire.format(it.rate,it.channels,n);it.offer(bytes);captured.addAndGet(n.toLong())}}
        2->{val id=data.readInt();val code=data.readUnsignedByte();require(code in 1..3);if(input?.id==id)failure=when(code){1->"KrPlay-Client 麦克风权限未允许";2->"KrPlay-Client 不支持协商的录音格式";else->"KrPlay-Client 麦克风读取失败"}}
        else->throw IOException("Unknown audio input")
    }}}catch(_:Exception){}finally{Wire.close(s)}}
    private fun send(durationNs:Long=0,writer:(DataOutputStream)->Unit){if(peer==null)return
        val b=ByteArrayOutputStream();writer(DataOutputStream(b));val bytes=b.toByteArray()
        if(!queue.offer(bytes,System.nanoTime(),durationNs)){dropped.incrementAndGet();Wire.close(peer)} // reconnect/replay mic config, never lose control frames
    }
    override fun output(type:Int,format:AudioFormat,pcm:ByteArray,offset:Int,length:Int):Boolean {
        if(!outputClient)return false
        val step=minOf(AudioWire.MAX_PCM/(format.channels*2),format.sampleRate/50)*(format.channels*2)
        var off=offset;val end=offset+length
        while(off<end){val n=minOf(step,end-off);send(n*1_000_000_000L/(format.sampleRate*format.channels*2)){AudioWire.pcm(it,type,format.sampleRate,format.channels,pcm,off,n)};sent.addAndGet(n.toLong());off+=n}
        return true
    }
    override fun onPlaybackRouteChanged(listener:(Boolean)->Unit){routeListener=listener;listener(playbackAvailable)}
    override fun audioStopped(type:Int){send{it.writeByte(4);it.writeInt(type)}}
    override fun microphone(type:Int,config:MicrophoneConfig):PcmInput? {
        if(inputSource!=CarPlayMicrophoneSource.CLIENT)return null
        return synchronized(this){input?.close();Input(type,ids.incrementAndGet(),config.sampleRate,config.channels).also{input=it;it.request()}}
    }
    override fun microphoneStopped(type:Int){synchronized(this){if(input?.type==type){input?.close();input=null}}}
    override fun onMicrophoneRouteChanged(listener:()->Unit){} // Stable PcmInput survives reconnect; no local fallback.
    private inner class Input(val type:Int,val id:Int,val rate:Int,val channels:Int):PcmInput {
        @Volatile private var open=true
        private val samples=ArrayBlockingQueue<ByteArray>(5)
        private var remainder:ByteArray?=null;private var offset=0
        fun request(){if(open&&micAllowed)send{it.writeByte(2);it.writeInt(id);it.writeInt(rate);it.writeByte(channels)}
            else if(open)failure="等待 KrPlay-Client 连接并允许麦克风权限"}
        fun clear(){samples.clear();remainder=null;offset=0}
        fun offer(bytes:ByteArray){if(!open)return;if(!samples.offer(bytes)){samples.poll();samples.offer(bytes);dropped.incrementAndGet()}}
        override fun read(buffer:ByteArray):Int {if(!open)return -1;val bytes=remainder?:samples.poll(50,TimeUnit.MILLISECONDS)?:return 0
            val n=minOf(buffer.size,bytes.size-offset);bytes.copyInto(buffer,0,offset,offset+n);offset+=n
            if(offset==bytes.size){remainder=null;offset=0}else remainder=bytes
            return n}
        override fun close(){open=false;clear();send{it.writeByte(3);it.writeInt(id)}}
    }
    override fun close(){running=false;input?.close();input=null;Wire.close(server);Wire.close(peer);peer=null;queue.clear();if(current===this)current=null}
    companion object {
        @Volatile private var current:ClientAudioRoute?=null
        fun snapshot():JSONObject {val c=current;return JSONObject().put("connected",c?.peer?.isClosed==false).put("output",if(c?.outputClient!=false)"CLIENT" else "ANDROID")
            .put("microphone",c?.inputSource?.name?:"CLIENT").put("micActive",c?.input!=null).put("micBytes",c?.captured?.get()?:0).put("pcmBytes",c?.sent?.get()?:0).put("dropped",(c?.dropped?.get()?:0)+(c?.queue?.dropped()?:0)).put("error",c?.failure?:"")}
    }
}
