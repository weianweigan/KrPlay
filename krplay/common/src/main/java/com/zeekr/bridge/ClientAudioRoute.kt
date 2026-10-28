package com.zeekr.bridge

import android.content.Context
import com.krplay.protocol.*
import com.shilapi.xcertplay.*
import com.shilapi.xcertplay.airplay.AudioFormat
import com.shilapi.xcertplay.airplay.MicrophoneConfig
import com.shilapi.xcertplay.media.PcmInput
import com.shilapi.xcertplay.media.RemoteAudioRoute
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.ArrayDeque
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Separate audio transport. Music is flow controlled by played frames, never aged out at 120 ms. */
class ClientAudioRoute(context:Context,port:Int=Ports.AUDIO):RemoteAudioRoute {
    private val outputClient=AirPlayPersistence.loadAudioOutput(context)==CarPlayAudioOutput.CLIENT
    private val inputSource=AirPlayPersistence.loadMicrophoneSource(context)
    @Volatile private var running=true
    @Volatile private var peer:Socket?=null
    @Volatile private var input:Input?=null
    @Volatile private var micAllowed=false
    @Volatile private var failure=""
    @Volatile private var version=AudioWire.VERSION
    @Volatile private var bufferMs=AudioPolicy.DEFAULT_BUFFER_MS
    @Volatile private var writingSince=0L
    private val controls=ArrayDeque<ByteArray>()
    private data class Packet(val bytes:ByteArray,val endFrame:Long,val frames:Int)
    private class Stream(val epoch:Int,val rate:Int,val channels:Int,val purpose:Int) {
        val packets=ArrayDeque<Packet>()
        var nextFrame=0L;var sentFrame=0L;var playedFrame=0L;var queuedFrames=0L
    }
    private val streams=linkedMapOf<Int,Stream>()
    private val epochs=java.util.concurrent.atomic.AtomicInteger()
    private val dropped=AtomicLong();private val sent=AtomicLong();private val captured=AtomicLong()
    private val overflows=AtomicLong();private val reconnects=AtomicLong()
    private val ids=java.util.concurrent.atomic.AtomicInteger()
    private var routeListener:((Boolean)->Unit)?=null
    private val server=ServerSocket(port)
    val localPort get()=server.localPort
    override val microphoneEnabled get()=inputSource!=CarPlayMicrophoneSource.DISABLED
    override val playbackAvailable get()=outputClient&&peer?.isClosed==false
    init {
        current=this
        Thread(::accept,"KrPlay-Audio-Accept").apply{isDaemon=true;start()}
        Thread({while(running){Thread.sleep(100);val since=writingSince
            if(since!=0L&&System.nanoTime()-since>2_000_000_000L){failure="音频发送持续阻塞";Wire.close(peer)}
        }},"KrPlay-Audio-Watchdog").apply{isDaemon=true;start()}
    }
    @Synchronized private fun clearQueues(){controls.clear();streams.clear()}
    private fun accept(){while(running){val s=try{server.accept()}catch(_:Exception){break}
        try{Wire.socket(s);s.soTimeout=5000
            val data=DataInputStream(s.getInputStream())
            require(data.readInt()==AudioWire.MAGIC)
            val negotiated=data.readUnsignedByte();require(negotiated==AudioWire.VERSION||negotiated==AudioWire.BUFFERED_VERSION)
            val allowed=data.readBoolean()
            val requested=if(negotiated==AudioWire.BUFFERED_VERSION)data.readInt() else AudioPolicy.DEFAULT_BUFFER_MS
            require(requested==AudioPolicy.bufferMs(requested));s.soTimeout=0
            val out=DataOutputStream(BufferedOutputStream(s.getOutputStream(),8192))
            if(negotiated==AudioWire.BUFFERED_VERSION){out.writeByte(AudioWire.HELLO_V2);out.writeByte(negotiated);out.flush()}
            synchronized(this){Wire.close(peer);clearQueues();version=negotiated;bufferMs=requested;micAllowed=allowed;peer=s;failure="";input?.request()}
            reconnects.incrementAndGet();routeListener?.invoke(playbackAvailable)
            Thread({read(s,data)},"KrPlay-Audio-Read").apply{isDaemon=true;start()}
            while(running&&peer===s&&!s.isClosed){val bytes=nextPacket()
                if(bytes==null){Thread.sleep(2);continue}
                writingSince=System.nanoTime()
                try{out.write(bytes);out.flush()}finally{writingSince=0}
            }
        }catch(e:Exception){if(running&&failure.isEmpty())failure=e.javaClass.simpleName}
        finally{synchronized(this){if(peer===s){peer=null;micAllowed=false;clearQueues();input?.clear()}};Wire.close(s);routeListener?.invoke(playbackAvailable)}
    }}
    @Synchronized private fun nextPacket():ByteArray? {
        if(controls.isNotEmpty())return controls.removeFirst()
        // Independent per-stream windows: interactive audio need not wait for music credit.
        for(stream in streams.values.sortedBy{if(it.purpose==AudioPolicy.MUSIC)1 else 0}){
            val packet=stream.packets.peekFirst()?:continue
            val capacity=AudioPolicy.frames(stream.rate,AudioPolicy.capacityMs(stream.purpose,bufferMs))
            if(version==AudioWire.BUFFERED_VERSION&&packet.endFrame-stream.playedFrame>capacity)continue
            stream.packets.removeFirst();stream.queuedFrames-=packet.frames;stream.sentFrame=packet.endFrame
            sent.addAndGet(packet.frames.toLong()*stream.channels*2)
            return packet.bytes
        }
        return null
    }
    private fun read(s:Socket,data:DataInputStream){try{while(running&&peer===s){when(data.readUnsignedByte()){
        1->{val id=data.readInt();val n=data.readUnsignedShort();require(n in 2..AudioWire.MAX_PCM&&n%2==0);val bytes=ByteArray(n);data.readFully(bytes)
            input?.takeIf{it.id==id}?.let{AudioWire.format(it.rate,it.channels,n);it.offer(bytes);captured.addAndGet(n.toLong())}}
        2->{val id=data.readInt();val code=data.readUnsignedByte();require(code in 1..3);if(input?.id==id)failure=when(code){1->"KrPlay-Client 麦克风权限未允许";2->"KrPlay-Client 不支持协商的录音格式";else->"KrPlay-Client 麦克风读取失败"}}
        AudioWire.PROGRESS->{require(version==AudioWire.BUFFERED_VERSION);val type=data.readInt();val epoch=data.readInt();val played=data.readLong()
            synchronized(this){streams[type]?.takeIf{it.epoch==epoch}?.let{require(played>=it.playedFrame&&played<=it.sentFrame);it.playedFrame=played}}}
        else->throw IOException("Unknown audio input")
    }}}catch(e:Exception){if(running&&peer===s)failure=e.javaClass.simpleName}finally{Wire.close(s)}}
    @Synchronized private fun send(writer:(DataOutputStream)->Unit){if(peer==null)return
        if(controls.size>=32){failure="音频控制队列已满";Wire.close(peer);return}
        val b=ByteArrayOutputStream();writer(DataOutputStream(b));controls.addLast(b.toByteArray())
    }
    @Synchronized override fun output(type:Int,format:AudioFormat,pcm:ByteArray,offset:Int,length:Int):Boolean {
        if(!outputClient)return false
        if(peer==null||peer?.isClosed==true)return true
        val purpose=AudioPolicy.purpose(format.audioType,type)
        var stream=streams[type]
        if(stream==null||stream.rate!=format.sampleRate||stream.channels!=format.channels||stream.purpose!=purpose){
            if(stream!=null)audioStopped(type)
            if(streams.size>=4){failure="Too many audio streams";Wire.close(peer);return true}
            stream=Stream(epochs.incrementAndGet(),format.sampleRate,format.channels,purpose);streams[type]=stream
        }
        val frameBytes=format.channels*2
        require(offset>=0&&length>=0&&offset<=pcm.size-length&&length%frameBytes==0)
        // The producer never waits on network IO. Exhaustion is an explicit audio-only recovery.
        if(stream.queuedFrames+length/frameBytes>AudioPolicy.frames(format.sampleRate,2000)){
            overflows.incrementAndGet();failure="音频发送队列超过 2000ms，重新缓冲";Wire.close(peer);return true
        }
        val step=minOf(AudioWire.MAX_PCM/frameBytes,format.sampleRate/50)*frameBytes
        var off=offset;val end=offset+length
        while(off<end){val n=minOf(step,end-off);val b=ByteArrayOutputStream();val out=DataOutputStream(b)
            if(version==AudioWire.BUFFERED_VERSION)AudioWire.pcmV2(out,type,stream.epoch,purpose,format.sampleRate,format.channels,stream.nextFrame,pcm,off,n)
            else AudioWire.pcm(out,type,format.sampleRate,format.channels,pcm,off,n)
            val frames=n/frameBytes;stream.nextFrame+=frames;stream.queuedFrames+=frames
            stream.packets.addLast(Packet(b.toByteArray(),stream.nextFrame,frames));off+=n
        }
        return true
    }
    override fun onPlaybackRouteChanged(listener:(Boolean)->Unit){routeListener=listener;listener(playbackAvailable)}
    @Synchronized override fun audioStopped(type:Int){val stream=streams.remove(type)
        // Upstream teardown is cancellation (pause/seek/route switch), not a promise of natural EOS.
        if(version==AudioWire.BUFFERED_VERSION){if(stream!=null)send{it.writeByte(AudioWire.END_V2);it.writeInt(type);it.writeInt(stream.epoch);it.writeBoolean(false)}}
        else send{it.writeByte(4);it.writeInt(type)}
    }
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
    override fun close(){running=false;input?.close();input=null;Wire.close(server);Wire.close(peer);peer=null;clearQueues();if(current===this)current=null}
    companion object {
        @Volatile private var current:ClientAudioRoute?=null
        fun snapshot():JSONObject {val c=current;return JSONObject().put("connected",c?.peer?.isClosed==false).put("output",if(c?.outputClient!=false)"CLIENT" else "ANDROID")
            .put("microphone",c?.inputSource?.name?:"CLIENT").put("micActive",c?.input!=null).put("micBytes",c?.captured?.get()?:0).put("pcmBytes",c?.sent?.get()?:0).put("dropped",c?.dropped?.get()?:0)
            .put("queueOverflows",c?.overflows?.get()?:0).put("connections",c?.reconnects?.get()?:0).put("protocol",c?.version?:0).put("clientBufferMs",c?.bufferMs?:0).put("error",c?.failure?:"")}
    }
}
