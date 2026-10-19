package com.zeekr.bridge
import com.krplay.protocol.Ports
import com.krplay.protocol.Wire
import java.io.*
import java.net.*
/** Bounded LAN diagnostics on an independent socket; never blocks touch/video control. */
class LinkProbeServer(private val port:Int=Ports.PROBE):Closeable {
    @Volatile private var running=false
    private var server:ServerSocket?=null
    @Volatile private var peer:Socket?=null
    fun start(){val listener=ServerSocket(port);server=listener;running=true
        Thread({val buffer=ByteArray(32768){(it*31).toByte()};while(running){val s=try{listener.accept()}catch(_:Exception){break};peer=s
            try{Wire.socket(s);s.soTimeout=5000;val input=DataInputStream(s.getInputStream());val out=DataOutputStream(s.getOutputStream())
                var total=0
                while(running){when(input.readUnsignedByte()){
                    4->{com.krplay.protocol.Discovery.response(out)}
                    1->{out.writeLong(input.readLong());out.flush()}
                    2->{val n=input.readInt();require(n in 1..8388608&&total+n<=16777216);total+=n;var left=n;while(left>0){val k=minOf(left,buffer.size);out.write(buffer,0,k);left-=k};out.flush()}
                    3->{val n=input.readInt();require(n in 1..8388608&&total+n<=16777216);total+=n;var left=n;while(left>0){val k=input.read(buffer,0,minOf(left,buffer.size));if(k<0)throw EOFException();left-=k};out.writeByte(1);out.flush()}
                    else->throw IOException("Invalid probe")
                }}
            }catch(_:Exception){}finally{Wire.close(s);peer=null}
        }},"KrPlay-LAN-Probe").apply{isDaemon=true;start()}
    }
    override fun close(){running=false;Wire.close(server);Wire.close(peer);server=null}
}
