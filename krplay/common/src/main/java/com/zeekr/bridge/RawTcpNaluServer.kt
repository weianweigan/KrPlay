package com.zeekr.bridge

import com.krplay.protocol.Wire
import java.io.DataOutputStream
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** One owner per stream; bounded preallocated packets, no producer waits on socket I/O. */
class RawTcpNaluServer(private val port: Int = 8081, private val requestIdr: () -> Unit = {}) {
    private class Packet { val bytes = ByteArray(Wire.MAX_FRAME); var type=0; var pts=0L; var size=0;var receivedNs=0L }
    private val free = ArrayBlockingQueue<Packet>(8).apply { repeat(8) { offer(Packet()) } }
    val slowDisconnects=java.util.concurrent.atomic.AtomicLong()
    val sourceResets=java.util.concurrent.atomic.AtomicLong()
    val queueRecoveries=java.util.concurrent.atomic.AtomicLong()
    val sendFailures=java.util.concurrent.atomic.AtomicLong()
    val keyframeRequests=java.util.concurrent.atomic.AtomicLong()
    private class Client(val socket: Socket) {
        val queue=ArrayBlockingQueue<Packet>(4)
        @Volatile var writingSince=0L
        @Volatile var waitingKey=true
        val requests=com.krplay.protocol.VideoPolicy.KeyframeRequests()
    }
    @Volatile var timingEnabled=false
    @Volatile private var running=false
    @Volatile private var client: Client?=null
    private var server: ServerSocket?=null
    private var watchdog: java.util.concurrent.ScheduledExecutorService?=null
    private val configs=sortedMapOf<Int,ByteArray>()
    private var codecHevc=false
    @Volatile private var cachedCsd: ByteArray?=null
    private var cachedIdr: ByteArray?=null
    private var width=1920; private var height=1080
    val connected get() = client!=null
    val waitingForKeyFrame get() = client?.waitingKey==true

    @Synchronized fun start() {
        if(running)return
        val listener=ServerSocket().apply { reuseAddress=true; bind(InetSocketAddress(port)) }
        server=listener;running=true
        watchdog=Executors.newSingleThreadScheduledExecutor().also { executor ->
            // Large IDRs and brief Wi-Fi stalls can take more than 120ms. Queue recovery
            // already bounds buffered video; only abandon a persistently blocked writer.
            executor.scheduleAtFixedRate({client?.let {
                if(it.writingSince!=0L && System.nanoTime()-it.writingSince>2_000_000_000L){slowDisconnects.incrementAndGet();it.writingSince=0;Wire.close(it.socket)}
                // An upstream request may be coalesced or lost. Retry even when a static
                // source sends no more slices: otherwise waitingKey can persist forever.
                if(it.waitingKey && cachedCsd!=null)requestKeyFrame(it)
            }},100,100,TimeUnit.MILLISECONDS)
        }
        Thread({while(running) { val socket=try{listener.accept()}catch(_:Exception){break}
            try { Wire.socket(socket); val c=Client(socket)
                synchronized(this) {
                    dropClient(); client=c
                    enqueue(c,4,0,byteArrayOf((width shr 8).toByte(),width.toByte(),(height shr 8).toByte(),height.toByte()))
                    cachedCsd?.let { enqueue(c,if(codecHevc)3 else 2,0,it) }
                    cachedIdr?.let { enqueue(c,if(codecHevc)1 else 0,0,it) }
                }
                Thread({send(c)},"KrPlay-NALU-Send").start();requestKeyFrame(c)
            } catch(_:Exception){Wire.close(socket)}
        }},"KrPlay-NALU-Accept").start()
    }
    @Synchronized fun stop(){running=false;timingEnabled=false;Wire.close(server);server=null;dropClient();watchdog?.shutdownNow();watchdog=null;configs.clear();cachedCsd=null;cachedIdr=null}
    @Synchronized fun resetSource(w:Int=width,h:Int=height) {sourceResets.incrementAndGet();width=w;height=h;configs.clear();cachedCsd=null;cachedIdr=null;dropClient()}
    @Synchronized fun updateResolution(w:Int,h:Int){require(w in 320..4096 && h in 240..4096);if(width!=w||height!=h)resetSource(w,h)}
    @Synchronized private fun dropClient(){client?.let { Wire.close(it.socket);while(true){val p=it.queue.poll()?:break;free.offer(p)} };client=null}
    @Synchronized fun pushVideoNalu(hevc:Boolean,pts:Long,src:ByteArray,offset:Int,length:Int,receivedNs:Long=System.nanoTime()) {
        if(length<=0||length>Wire.MAX_FRAME||offset<0||offset>src.size-length)return
        if(codecHevc!=hevc){configs.clear();cachedCsd=null;cachedIdr=null;codecHevc=hevc;dropClient()}
        // Scan all start codes. Only configuration and keyframe cache updates allocate.
        var i=offset;val end=offset+length;var key=false;var hasSlice=false;var changed=false
        while(i+2<end){val prefix=startCode(src,i,end)
            if(prefix==0 || i+prefix>=end){i++;continue}
            val nalStart=i+prefix
            val nalType=if(hevc)(src[nalStart].toInt() shr 1)and 63 else src[nalStart].toInt()and 31
            val config=if(hevc)nalType in 32..34 else nalType==7||nalType==8
            if(config){var stop=nalStart+1;while(stop+2<end && startCode(src,stop,end)==0)stop++
                if(stop+2>=end)stop=end
                val old=configs[nalType];val size=stop-nalStart;var same=old?.size==size
                if(same)for(j in 0 until size)if(old!![j]!=src[nalStart+j]){same=false;break}
                if(!same){configs[nalType]=src.copyOfRange(nalStart,stop);changed=true}
            }else if(if(hevc)nalType in 0..31 else nalType in 1..5)hasSlice=true
            if(Wire.key(hevc,nalType))key=true
            i=nalStart+1
        }
        if(changed){cachedIdr=null;cachedCsd=Wire.annex(configs.values.toList());client?.let { c ->
            while(true){val p=c.queue.poll()?:break;free.offer(p)};c.waitingKey=true;cachedCsd?.let{enqueue(c,if(hevc)3 else 2,0,it)}
        }}
        if(!hasSlice)return
        if(key)cachedIdr=src.copyOfRange(offset,end)
        val c=client?:return
        if(c.waitingKey&&!key){requestKeyFrame(c);return}
        if(c.queue.remainingCapacity()<(if(key)2 else 1) || free.size<(if(key)2 else 1)){
            queueRecoveries.incrementAndGet()
            while(true){val p=c.queue.poll()?:break;free.offer(p)};c.waitingKey=true
            if(!key){requestKeyFrame(c);return}
        }
        if(key){cachedCsd?.let { enqueue(c,if(hevc)3 else 2,0,it) };c.waitingKey=false}
        enqueue(c,if(hevc)1 else 0,pts,src,offset,length,receivedNs)
    }
    private fun requestKeyFrame(c:Client){
        if(running && client===c && !c.socket.isClosed && c.requests.acquire(System.nanoTime())){
            keyframeRequests.incrementAndGet();runCatching{requestIdr()}
        }
    }
    private fun startCode(b:ByteArray,i:Int,end:Int):Int = if(i+2<end && b[i]==0.toByte()&&b[i+1]==0.toByte()) when {
        b[i+2]==1.toByte()->3
        i+3<end&&b[i+2]==0.toByte()&&b[i+3]==1.toByte()->4
        else->0
    } else 0
    @Synchronized fun pushAudioPcm(pts:Long,src:ByteArray,offset:Int,length:Int){if(length<0||length%4!=0||length>Wire.MAX_FRAME||offset<0||offset>src.size-length)return;client?.let{enqueue(it,16,pts,src,offset,length)}}
    private fun enqueue(c:Client,t:Int,pts:Long,b:ByteArray,o:Int=0,n:Int=b.size,receivedNs:Long=0){val p=free.poll()?:return;p.type=t;p.pts=pts;p.size=n;p.receivedNs=receivedNs;b.copyInto(p.bytes,0,o,o+n);if(!c.queue.offer(p))free.offer(p)}
    private fun send(c:Client){try{val out=DataOutputStream(BufferedOutputStream(c.socket.getOutputStream(),65536));while(running&&!c.socket.isClosed){val p=c.queue.poll(250,TimeUnit.MILLISECONDS)?:continue
        try{c.writingSince=System.nanoTime();if(timingEnabled&&(p.type==0||p.type==1)&&p.size<=Wire.MAX_FRAME-16){
            out.writeByte(if(p.type==0)20 else 21);out.writeLong(p.pts);out.writeInt(p.size+16);out.writeLong(p.receivedNs);out.writeLong(System.nanoTime());out.write(p.bytes,0,p.size)
        }else Wire.frame(out,p.type,p.pts,p.bytes,0,p.size);out.flush()}finally{c.writingSince=0;free.offer(p)}
    }}catch(_:Exception){if(running)sendFailures.incrementAndGet()}finally{synchronized(this){if(client===c)dropClient()};Wire.close(c.socket)}}
}
