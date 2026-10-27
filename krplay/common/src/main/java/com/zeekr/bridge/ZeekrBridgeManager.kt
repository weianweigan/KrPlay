package com.zeekr.bridge

import android.content.Context
import android.content.Intent
import android.net.VpnService
import org.json.JSONObject
import android.util.Log
import android.view.KeyEvent
import com.krplay.protocol.Wire
import com.shilapi.xcertplay.CarPlayBackgroundSession
import com.shilapi.xcertplay.airplay.AirPlayContact
import com.shilapi.xcertplay.airplay.AirPlayKnobState
import com.shilapi.xcertplay.airplay.VideoCodec
import com.shilapi.xcertplay.media.*
import java.net.*
import java.io.*
import java.util.concurrent.ConcurrentHashMap

object ZeekrBridgeManager {
    @Volatile private var running=false
    @Volatile private var mode=0
    private var listener:ServerSocket?=null
    @Volatile private var control:Socket?=null
    private val stream=RawTcpNaluServer(configureSocket={com.krplay.transport.VideoSocketOptions.configure(it)}) { emitIdr() }
    private val hub=ReverseSocks5Hub()
    private val probe=LinkProbeServer()
    private var box:BoxVirtualDisplayEngine?=null
    private var appContext:Context?=null
    private var sourceDistribution=JSONObject()
    @Volatile private var error=""
    @Volatile private var pendingVpn=false
    private val statusLock=Any()
    private val statusExecutor=java.util.concurrent.Executors.newSingleThreadExecutor()
    private val statusPending=java.util.concurrent.atomic.AtomicBoolean()
    private var width=1920;private var height=1080;private var fps=60
    private val recovery=ConcurrentHashMap<Int,()->Unit>()
    private val keyframeRequests=com.krplay.protocol.VideoPolicy.KeyframeRequests()
    @Volatile private var carConfig:ByteArray?=null
    @Volatile private var carHevc=false
    @Volatile private var carWidth=1280
    @Volatile private var carHeight=720
    @Volatile private var carType=-1
    @Volatile private var session:com.shilapi.xcertplay.airplay.AirPlaySession?=null
    fun session(value:com.shilapi.xcertplay.airplay.AirPlaySession?){session=value}
    private var viewport:((Int,Int)->Unit)?=null
    fun setViewportListener(listener:((Int,Int)->Unit)?){viewport=listener;if(running)listener?.invoke(width,height)}
    @Synchronized fun start(context:Context){if(running)return
        try{appContext=context.applicationContext;sourceDistribution=com.krplay.distribution.SourceDistribution.info(context);listener=ServerSocket(8082);stream.start();if(com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED)hub.start();probe.start();box=BoxVirtualDisplayEngine(context.applicationContext,stream);running=true
            val server=listener!!
            Thread({while(running){val s=try{server.accept()}catch(_:Exception){break};if(!running){Wire.close(s);break};try{Wire.socket(s);s.soTimeout=15000;synchronized(this){Wire.close(control);stream.timingEnabled=false;control=s};sendStatus(s);Thread({controlLoop(s)},"KrPlay-Control").start()}catch(_:Exception){Wire.close(s)}}},"KrPlay-Control-Accept").start()
        }catch(e:Exception){stop();Log.e("KrPlay","Bridge startup failed",e)}
    }
    @Synchronized fun stop(){running=false;Wire.close(listener);listener=null;Wire.close(control);control=null;box?.stop();box=null;stream.stop();hub.stop();probe.close();mode=0;session=null;carConfig=null;recovery.clear()}
    private fun controlLoop(s:Socket){try{val input=DataInputStream(BufferedInputStream(s.getInputStream()));while(running&&!s.isClosed){when(input.readUnsignedByte()){
        1->{val m=input.readUnsignedByte();require(m in 0..1);switchMode(m,s)}
        2->{val t=Wire.Touch.read(input);synchronized(this){if(control===s){if(mode==1)box?.touch(t) else touchCar(t)}}}
        3->{val action=input.readUnsignedByte();val code=input.readInt();require(action in 0..1 && code in 0..288);synchronized(this){if(control===s){if(mode==1)box?.key(action,code) else if(action==0&&code==KeyEvent.KEYCODE_ASSIST){session?.invokeSiri()}else if(action==0){val state=when(code){KeyEvent.KEYCODE_HOME->AirPlayKnobState(home=true);KeyEvent.KEYCODE_BACK->AirPlayKnobState(back=true);else->AirPlayKnobState(select=true)};session?.sendKnob(state)}}}}
        4->{input.readUnsignedByte();if(control===s)requestIdr()}
        5->{val w=input.readUnsignedShort();val h=input.readUnsignedShort();val f=input.readUnsignedByte();require(w in 320..4096 && h in 240..4096 && f in 1..60);synchronized(this){if(control!==s)throw IOException("Stale controller");val changed=width!=w||height!=h||fps!=f;width=w/2*2;height=h/2*2;fps=f;if(changed&&mode==1){stream.resetSource(width,height);box?.start(width,height,fps)};viewport?.invoke(width,height)}}
        9->{val t0=input.readLong();val t1=System.nanoTime();if(control===s){stream.timingEnabled=true;sendStatus(s,t0,t1)}}
        6->{if(control===s)sendStatus(s)}
        7->{val speed=input.readUnsignedByte();require(speed in 0..1);if(control===s)TunnelDiagnostics.start(speed==1)}
        8->{val enabled=input.readUnsignedByte();require(enabled in 0..1);if(control===s){val context=appContext!!
            if(enabled==0){if(NetworkAccessSettings.automatic(context))context.startForegroundService(Intent(context,BorrowVpnService::class.java).setAction("access"))else context.stopService(Intent(context,BorrowVpnService::class.java))}
            else if(!com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED){error="借网功能暂未开放"}
            else if(VpnService.prepare(context)==null)context.startForegroundService(Intent(context,BorrowVpnService::class.java))
            else{pendingVpn=true;error="请在安卓设备上打开 KrPlay，允许 VPN 连接"};publishStatus()
        }}
        else->throw IOException("Unknown control command")
    }}}catch(e:Exception){if(running)Log.d("KrPlay","Control disconnected: ${e.message}")}finally{synchronized(this){if(control===s){cancelTouch();control=null}};Wire.close(s)}}
    @Synchronized private fun switchMode(next:Int,owner:Socket){if(control!==owner)return;if(next==1&&!rootScreenAvailable()){error="设备画面与远程触控需要 root；CarPlay 和 VPN 可正常使用";publishStatus();return};error="";if(next==mode){requestIdr();return};cancelTouch();box?.stop();mode=next
        if(next==1){stream.resetSource(width,height);try{box?.start(width,height,fps)}catch(e:Exception){mode=0;error="未获得 root 授权：${e.message}";publishStatus()}}else{stream.resetSource(carWidth,carHeight);carConfig?.let{stream.pushVideoNalu(carHevc,0,it,0,it.size)};requestIdr()}
    }
    fun clientConnected()=control?.isClosed==false
    fun idleWorkers()=hub.idleWorkers()
    fun rootScreenAvailable():Boolean {
        val paths=(System.getenv("PATH")?:"").split(':')+listOf("/system/bin","/system/xbin","/sbin","/debug_ramdisk")
        return paths.any { java.io.File(it,"su").canExecute() }
    }
    fun vpnAuthorized(){pendingVpn=false;error=""}
    fun consumeVpnRequest():Boolean {val pending=pendingVpn;pendingVpn=false;return pending}
    fun publishStatus(){if(!statusPending.compareAndSet(false,true))return;statusExecutor.execute {try{control?.let{sendStatus(it)}}finally{statusPending.set(false)}}}
    private fun sendStatus(socket:Socket,probeId:Long?=null,receivedNs:Long=0){try{
        val stats=BorrowVpnService.stats()
        val state=JSONObject().put("sourceDistribution",sourceDistribution).put("protocol",2).put("mode",mode).put("rootScreen",rootScreenAvailable())
            .put("borrowingAvailable",com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED).put("vpn",BorrowVpnService.active).put("vpnMessage",BorrowVpnService.message).put("idleWorkers",hub.idleWorkers())
            .put("iphoneStage",com.shilapi.xcertplay.ConnectionDiagnostics.stage).put("iphoneError",com.shilapi.xcertplay.ConnectionDiagnostics.lastFailure?:"")
            .put("error",error).put("vpnConsentRequired",pendingVpn).put("txBytes",stats[1]).put("rxBytes",stats[3]).put("test",TunnelDiagnostics.latest)
        state.put("fixedEntry",BorrowVpnService.accessActive).put("fixedEntryMessage",BorrowVpnService.accessMessage)
        state.put("audio",ClientAudioRoute.snapshot()).put("stream",JSONObject().put("connected",stream.connected).put("slowDisconnects",stream.slowDisconnects.get()).put("sourceResets",stream.sourceResets.get()).put("queueRecoveries",stream.queueRecoveries.get()).put("sendFailures",stream.sendFailures.get()).put("lastSendFailure",stream.lastSendFailure).put("queueDepth",stream.queueDepth).put("staleFrames",stream.staleFrames.get()).put("nonReferenceDrops",stream.nonReferenceDrops.get()).put("lastWriteMs",stream.lastWriteMs).put("maxFrameBytes",stream.maxFrameBytes.get()).put("waitingKeyFrame",stream.waitingForKeyFrame).put("keyframeRequests",stream.keyframeRequests.get()))
        if(probeId!=null)state.put("probe",JSONObject().put("t0",probeId).put("t1",receivedNs).put("t2",System.nanoTime()))
        val bytes=state.toString().toByteArray(Charsets.UTF_8);check(bytes.size<=16384)
        synchronized(statusLock){val out=DataOutputStream(socket.getOutputStream());out.writeByte(128);out.writeShort(bytes.size);out.write(bytes);out.flush()}
    }catch(e:Exception){Log.w("KrPlay","Status response failed",e);Wire.close(socket)}}
    private val contacts=arrayOfNulls<AirPlayContact>(2)
    private fun touchCar(t:Wire.Touch){val changed=t.ids[0]
        for(i in t.ids.indices){val id=t.ids[i];var slot=contacts.indexOfFirst{it?.id==id};if(slot<0)slot=contacts.indexOfFirst{it==null};if(slot<0)continue
            contacts[slot]=AirPlayContact(id,t.xs[i].toDouble(),t.ys[i].toDouble(),t.action!=3 && !(t.action==1&&id==changed))
        }
        CarPlayBackgroundSession.snapshot()?.controller?.sendTouch(contacts.mapIndexed { index,c->c?:AirPlayContact(index,0.0,0.0,false) })
        for(i in contacts.indices)if(t.action==3 || t.action==1&&contacts[i]?.id==changed)contacts[i]=null
    }
    private fun cancelTouch(){CarPlayBackgroundSession.snapshot()?.controller?.sendTouch(emptyList());contacts.fill(null);if(mode==1)box?.touch(Wire.Touch(3,intArrayOf(0),floatArrayOf(0f),floatArrayOf(0f)))}
    fun requestIdr(){stream.requestKeyFrame()}
    private fun emitIdr(){
        if(mode==1){if(keyframeRequests.acquire(System.nanoTime()))box?.requestKeyFrame()}
        else recovery[carType]?.let{if(keyframeRequests.acquire(System.nanoTime()))runCatching{it()}}
    }

    /** Combine the existing upstream consumer without modifying shared/ or decrypt callbacks. */
    private val generations=java.util.concurrent.atomic.AtomicLong()
    fun wrap(delegate:EncodedVideoSink,w:Int,h:Int):EncodedVideoSink {
        val generation=generations.incrementAndGet()
        synchronized(this){carConfig=null;recovery.clear();if(mode==0)stream.resetSource(w,h)}
        return object:EncodedVideoSink {
        private val ownedTypes=mutableSetOf<Int>()
        override fun createFrameDecoder(key:ByteArray)=delegate.createFrameDecoder(key)
        override fun recovery(type:Int,request:()->Unit){ownedTypes+=type;if(generation==generations.get())recovery[type]=request;delegate.recovery(type,request)}
        override fun configure(type:Int,codec:VideoCodec,data:ByteArray){delegate.configure(type,codec,data)
            val csd=if(codec==VideoCodec.H265)MediaCodecSupport.hevcCodecSpecificData(data)else{val(sps,pps)=MediaCodecSupport.avcParameterSets(data);if(sps.isEmpty()||pps.isEmpty())byteArrayOf()else Wire.annex(listOf(sps,pps))}
            synchronized(this@ZeekrBridgeManager){if(generation!=generations.get())return;carType=type;carHevc=codec==VideoCodec.H265;carWidth=w;carHeight=h
                if(carConfig?.contentEquals(csd)!=true){carConfig=csd;if(mode==0){stream.resetSource(w,h);stream.pushVideoNalu(carHevc,0,csd,0,csd.size)}}
            }
        }
        override fun frame(type:Int,data:ByteArray,presentationTimeUs:Long):Boolean{synchronized(this@ZeekrBridgeManager){if(generation==generations.get()&&type==carType&&mode==0)stream.pushVideoNalu(carHevc,presentationTimeUs,data,0,data.size)};return delegate.frame(type,data,presentationTimeUs)||stream.connected}
        override fun frame(type:Int,frame:CompressedVideoFrame,pts:Long,receivedAtNs:Long,decryptNs:Long):Boolean{
            synchronized(this@ZeekrBridgeManager){if(generation==generations.get()&&type==carType&&mode==0){val bytes=frame.toByteArray();stream.pushVideoNalu(carHevc,pts,bytes,0,bytes.size,receivedAtNs)}}
            return delegate.frame(type,frame,pts,receivedAtNs,decryptNs)||stream.connected||mode==1
        }
        override fun stopped(type:Int){delegate.stopped(type);if(generation==generations.get() && type in ownedTypes)recovery.remove(type);if(generation==generations.get() && carType==type){carConfig=null;carType=-1;if(mode==0)stream.resetSource()}}
    }
    }
}
