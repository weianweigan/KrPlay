import com.zeekr.bridge.RawTcpNaluServer
import com.zeekr.bridge.ReverseSocks5Hub
import com.krplay.protocol.Wire
import java.io.*
import java.net.*
import java.util.concurrent.atomic.AtomicInteger

fun main(){
    val idrs=AtomicInteger();val stream=RawTcpNaluServer(19081){idrs.incrementAndGet()}
    val config=byteArrayOf(0,0,0,1,0x67,1,0,0,0,1,0x68,2)
    val key=byteArrayOf(0,0,1,0x65,3)
    fun packet(input:DataInputStream):Pair<Wire.Frame,ByteArray>{val f=Wire.header(input);val b=ByteArray(f.size);input.readFully(b);return f to b}
    stream.start();stream.pushVideoNalu(false,0,config,0,config.size);stream.pushVideoNalu(false,9,key,0,key.size)
    Socket("127.0.0.1",19081).use { s->s.soTimeout=2000;val input=DataInputStream(s.getInputStream());check(packet(input).first.type==4);val csd=packet(input);check(csd.first.type==2);check(Wire.nals(csd.second,0,csd.second.size).size==2);check(packet(input).first.type==0)
        stream.pushVideoNalu(false,10,key,0,key.size);check(packet(input).first.type==2);check(packet(input).first.pts==10L)
        stream.timingEnabled=true
        val ingress=System.nanoTime();stream.pushVideoNalu(false,12,key,0,key.size,ingress)
        check(packet(input).first.type==2)
        val timed=packet(input);check(timed.first.type==20);val timing=DataInputStream(ByteArrayInputStream(timed.second));check(timing.readLong()==ingress);check(timing.readLong()>=ingress);check(timing.readBytes().contentEquals(key))
        stream.timingEnabled=false
        val large=ByteArray(Wire.MAX_FRAME);large[2]=1;large[3]=0x65;stream.pushVideoNalu(false,11,large,0,large.size);check(packet(input).first.type==2);check(packet(input).second.size==Wire.MAX_FRAME)
        stream.resetSource(1280,720);check(input.read()==-1)
    }
    check(idrs.get()<=1){"Cached/live IDRs must coalesce redundant recovery requests"};stream.stop();stream.start();stream.stop()
    // A brief Wi-Fi/receiver pause during a large IDR must not trigger a reconnect.
    val paused=RawTcpNaluServer(19085);paused.start()
    try {
        Socket().use { s ->
            s.receiveBufferSize=64*1024;s.connect(InetSocketAddress("127.0.0.1",19085));s.soTimeout=3000
            val input=DataInputStream(s.getInputStream());check(packet(input).first.type==4)
            paused.pushVideoNalu(false,0,config,0,config.size);check(packet(input).first.type==2)
            val large=ByteArray(Wire.MAX_FRAME);large[2]=1;large[3]=0x65
            paused.pushVideoNalu(false,20,large,0,large.size)
            Thread.sleep(600)
            check(paused.slowDisconnects.get()==0L){"A 600ms receiver pause must preserve the video connection"}
            check(packet(input).first.type==2)
            val resumed=packet(input);check(resumed.first.pts==20L&&resumed.second.size==Wire.MAX_FRAME)
            check(paused.slowDisconnects.get()==0L)
        }
    } finally {paused.stop()}
    // Reproduce the freeze: the first request has no effect and the source sends no
    // further frames. A waiting stream must retry without requiring a TCP reconnect.
    val retryRequests=AtomicInteger();val retry=RawTcpNaluServer(19086){retryRequests.incrementAndGet()}
    retry.start();retry.pushVideoNalu(false,0,config,0,config.size)
    try {
        Socket("127.0.0.1",19086).use { s ->
            s.soTimeout=3000;val input=DataInputStream(s.getInputStream())
            check(packet(input).first.type==4);check(packet(input).first.type==2)
            val deadline=System.nanoTime()+3_000_000_000L
            while(retryRequests.get()<2 && System.nanoTime()<deadline)Thread.sleep(10)
            check(retryRequests.get()>=2){"An ignored/coalesced keyframe request must not freeze a silent stream"}
            check(retryRequests.get()<=3){"Waiting for a keyframe must not create a request storm"}
            check(retry.waitingForKeyFrame)
            retry.pushVideoNalu(false,30,key,0,key.size)
            check(packet(input).first.type==2);check(packet(input).first.pts==30L)
            check(!retry.waitingForKeyFrame)
            val count=retryRequests.get();Thread.sleep(1100)
            check(retryRequests.get()==count){"Stop retries after a keyframe arrives"}
            val delta=byteArrayOf(0,0,1,0x41,3)
            retry.pushVideoNalu(false,31,delta,0,delta.size);check(packet(input).first.pts==31L)
            check(retry.sendFailures.get()==0L&&retry.slowDisconnects.get()==0L)
        }
    } finally {retry.stop()}
    // Partial 2MiB frames survive a paused receiver. Requests are coalesced while
    // that frame is in flight, and queued dependent frames are evicted as a group.
    val congestionRequests=AtomicInteger()
    val congested=RawTcpNaluServer(19087){congestionRequests.incrementAndGet()}
    congested.start();congested.pushVideoNalu(false,0,config,0,config.size)
    try {
        Socket().use { s ->
            s.receiveBufferSize=32*1024;s.connect(InetSocketAddress("127.0.0.1",19087));s.soTimeout=4000
            val input=DataInputStream(s.getInputStream());check(packet(input).first.type==4);check(packet(input).first.type==2)
            val large=ByteArray(Wire.MAX_FRAME){42};large[0]=0;large[1]=0;large[2]=1;large[3]=0x65
            congested.pushVideoNalu(false,100,large,0,large.size)
            check(packet(input).first.type==2)
            val header=Wire.header(input);check(header.pts==100L)
            val before=congestionRequests.get()
            repeat(100){congested.requestKeyFrame()}
            val delta=byteArrayOf(0,0,1,0x41,3)
            congested.pushVideoNalu(false,101L,delta,0,delta.size)
            Thread.sleep(80)
            check(congestionRequests.get()==before){"No IDR generation into a partially blocked TCP frame"}
            check(congested.queueRecoveries.get()>=1){"A blocked write must flush before the old 250ms threshold, even with only one queued frame"}
            val payload=ByteArray(header.size);input.readFully(payload);check(payload.contentEquals(large)){"Partial write must retain exact frame bytes"}
            val deadline=System.nanoTime()+3_000_000_000L
            while(congestionRequests.get()==before && System.nanoTime()<deadline)Thread.sleep(10)
            check(congestionRequests.get()>before){"Deferred request must run after congestion clears"}
            congested.pushVideoNalu(false,200,key,0,key.size)
            check(packet(input).first.type==2);check(packet(input).first.pts==200L)
            check(congested.sendFailures.get()==0L&&congested.slowDisconnects.get()==0L)
        }
    } finally {congested.stop()}
    val probe=com.zeekr.bridge.LinkProbeServer(19084);probe.start()
    Socket("127.0.0.1",19084).use{s->s.soTimeout=2000;val out=DataOutputStream(s.getOutputStream());val input=DataInputStream(s.getInputStream())
        out.writeByte(4);out.flush();check(com.krplay.protocol.Discovery.verify(input))
        out.writeByte(1);out.writeLong(123);out.flush();check(input.readLong()==123L)
        out.writeByte(2);out.writeInt(4096);out.flush();input.readFully(ByteArray(4096))
        out.writeByte(3);out.writeInt(4096);out.write(ByteArray(4096));out.flush();check(input.readUnsignedByte()==1)
        out.writeByte(2);out.writeInt(8388609);out.flush();check(input.read()==-1)
    };probe.close()
    val hub=ReverseSocks5Hub(19700,19080,19053);hub.start()
    Socket("127.0.0.1",19700).use { worker->worker.soTimeout=3000
        Socket("127.0.0.1",19080).use { app->app.soTimeout=3000;val content="test half-close".toByteArray();app.getOutputStream().write(content);app.shutdownOutput();val copied=worker.getInputStream().readBytes();check(copied.contentEquals(content));worker.getOutputStream().write("response".toByteArray());worker.shutdownOutput();check(String(app.getInputStream().readBytes())=="response") }
    }
    val stale=Socket("127.0.0.1",19700)
    val deadline=System.nanoTime()+2_000_000_000L
    while(hub.idleWorkers()==0&&System.nanoTime()<deadline)Thread.sleep(10)
    check(hub.idleWorkers()==1);stale.close()
    val staleDeadline=System.nanoTime()+2_000_000_000L
    while(hub.idleWorkers()>0&&System.nanoTime()<staleDeadline)Thread.sleep(10)
    check(hub.idleWorkers()==0){"Disconnected idle workers must not be reported as usable"}
    hub.stop();Thread.sleep(50);hub.start();hub.stop()
    println("Bridge integration passed: CSD/IDR replay, 2MiB frame, brief receiver stall, source reset, restart and reverse half-close relay")
}
