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
    check(idrs.get()>0);stream.stop();stream.start();stream.stop()
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
    println("Bridge integration passed: CSD/IDR replay, 2MiB frame, source reset, restart and reverse half-close relay")
}
