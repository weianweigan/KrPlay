package com.zeekr.bridge
import com.krplay.protocol.Wire
import java.net.*
import java.io.*
import java.util.concurrent.*

/** SOCKS TCP CONNECT only; DNS UDP is explicitly converted to DNS-over-TCP below. */
class ReverseSocks5Hub(private val workerPort:Int=com.krplay.protocol.Ports.REVERSE_WORKERS,private val localPort:Int=1080,private val dnsPort:Int=15353) {
    @Volatile private var running=false
    private val workers=LinkedBlockingDeque<Socket>(32)
    private val sockets=ConcurrentHashMap.newKeySet<Socket>()
    private val listeners=mutableListOf<ServerSocket>()
    private var dns:DatagramSocket?=null
    @Synchronized fun start(){if(running)return
        try{
            val remote=ServerSocket().apply{reuseAddress=true;bind(InetSocketAddress(this@ReverseSocks5Hub.workerPort))}.also{listeners.add(it)}
            val local=ServerSocket().apply{reuseAddress=true;bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"),this@ReverseSocks5Hub.localPort),16)}.also{listeners.add(it)}
            dns=DatagramSocket(null).apply{reuseAddress=true;bind(InetSocketAddress(InetAddress.getByName("127.0.0.1"),this@ReverseSocks5Hub.dnsPort))};running=true
            Thread({while(running){val s=try{remote.accept()}catch(_:Exception){break};if(!running){Wire.close(s);break};Wire.socket(s);sockets.add(s);if(!workers.offer(s)){sockets.remove(s);Wire.close(s)}}},"KrPlay-Workers").start()
            Thread({while(running){val s=try{local.accept()}catch(_:Exception){break};if(!running){Wire.close(s);break};if(sockets.size>=96){Wire.close(s);continue};sockets.add(s);Thread({bridge(s)},"KrPlay-SOCKS-Bridge").start()}},"KrPlay-SOCKS-Accept").start()
            Thread({dnsLoop()},"KrPlay-DNS").start()
            Thread({while(running&&!remote.isClosed){pruneWorkers();try{Thread.sleep(1000)}catch(_:InterruptedException){break}}},"KrPlay-Worker-Health").start()
        }catch(e:Exception){stop();throw e}
    }
    fun idleWorkers():Int=workers.size
    // Reserve each idle socket before probing so no bridge can read it concurrently.
    private fun pruneWorkers(){repeat(workers.size){val s=workers.poll()?:return
        var alive=false
        try{s.soTimeout=1;s.getInputStream().read()}catch(_:SocketTimeoutException){alive=true}catch(_:Exception){}
        if(alive&&running){s.soTimeout=0;if(workers.offer(s))return@repeat}
        sockets.remove(s);Wire.close(s)
    }}
    private fun bridge(app:Socket){var worker:Socket?=null
        try{Wire.socket(app);worker=workers.poll(3,TimeUnit.SECONDS)?:return;val w=worker
            val t=Thread({pipe(app,w)},"KrPlay-SOCKS-Up");t.start();pipe(w,app);t.join()
        }catch(e:Exception){System.err.println("KrPlay SOCKS bridge: $e")}finally{sockets.remove(app);Wire.close(app);worker?.let{sockets.remove(it);Wire.close(it)}}
    }
    private fun pipe(a:Socket,b:Socket){try{val buf=ByteArray(32768);val input=a.getInputStream();val output=b.getOutputStream();while(running){val n=input.read(buf);if(n<0)break;output.write(buf,0,n);output.flush()};b.shutdownOutput()}catch(_:Exception){Wire.close(a);Wire.close(b)}}
    private fun dnsLoop(){val pool=Executors.newFixedThreadPool(4);val slots=Semaphore(32)
        try { while(running){val packet=DatagramPacket(ByteArray(4096),4096);try{dns?.receive(packet)?:break}catch(_:Exception){break}
            if(!slots.tryAcquire())continue
            pool.execute {try{dnsQuery(packet)}finally{slots.release()}}
        }}finally{pool.shutdownNow()}
    }
    private fun dnsQuery(packet:DatagramPacket){val s=Socket();sockets.add(s)
        try{s.soTimeout=8000;s.connect(InetSocketAddress("127.0.0.1",localPort),1000);val out=DataOutputStream(s.getOutputStream());val input=DataInputStream(s.getInputStream())
            out.write(byteArrayOf(5,1,0));out.flush();check(input.readUnsignedByte()==5&&input.readUnsignedByte()==0)
            out.write(byteArrayOf(5,1,0,1,1,1,1,1,0,53));out.flush();check(input.readUnsignedByte()==5&&input.readUnsignedByte()==0);input.readUnsignedByte();when(input.readUnsignedByte()){1->input.readFully(ByteArray(6));4->input.readFully(ByteArray(18));else->error("Bad SOCKS response")}
            out.writeShort(packet.length);out.write(packet.data,0,packet.length);out.flush();val n=input.readUnsignedShort();check(n in 12..4096);val reply=ByteArray(n);input.readFully(reply);dns?.send(DatagramPacket(reply,n,packet.socketAddress))
        }catch(_:Exception){}finally{sockets.remove(s);Wire.close(s)}
    }
    @Synchronized fun stop(){running=false;listeners.forEach{Wire.close(it)};listeners.clear();dns?.close();dns=null;sockets.forEach{Wire.close(it)};sockets.clear();workers.clear()}
}
