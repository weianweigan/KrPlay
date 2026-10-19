package com.zeekr.bridge

import com.krplay.protocol.HttpBody
import org.json.JSONObject
import java.io.BufferedInputStream
import java.net.*
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Tests explicitly traverse the reverse SOCKS channel, never silently using local mobile data. */
object TunnelDiagnostics {
    private val executor=Executors.newSingleThreadExecutor()
    private val busy=AtomicBoolean()
    @Volatile private var socket:Socket?=null
    @Volatile var latest=JSONObject().put("state","idle");private set
    fun start(speed:Boolean){if(!busy.compareAndSet(false,true))return
        latest=JSONObject().put("state","running").put("kind",if(speed)"speed" else "connectivity");ZeekrBridgeManager.publishStatus()
        executor.execute {
            var transport:Socket?=null
            try{
                check(ZeekrBridgeManager.idleWorkers()>0){"尚未连接 KrPlay-Client，无法检测车载通道"}
                val host="speed.cloudflare.com";val bytes=if(speed)8*1024*1024 else 1024
                val start=System.nanoTime()
                val proxy=Socket(Proxy(Proxy.Type.SOCKS,InetSocketAddress("127.0.0.1",1080)))
                transport=proxy;socket=proxy;proxy.soTimeout=8000
                proxy.connect(InetSocketAddress.createUnresolved(host,443),5000)
                val tls=SSLSocketFactory.getDefault().let { it as SSLSocketFactory }.createSocket(proxy,host,443,true) as SSLSocket
                transport=tls;socket=tls;tls.soTimeout=8000
                val params=tls.sslParameters;params.endpointIdentificationAlgorithm="HTTPS";tls.sslParameters=params;tls.startHandshake()
                val latency=(System.nanoTime()-start)/1_000_000.0
                val request="GET /__down?bytes=$bytes HTTP/1.1\r\nHost: $host\r\nAccept-Encoding: identity\r\nCache-Control: no-cache\r\nConnection: close\r\n\r\n"
                tls.outputStream.write(request.toByteArray(Charsets.US_ASCII));tls.outputStream.flush()
                val transferStart=System.nanoTime();var lastReport=0L
                val count=HttpBody.read(BufferedInputStream(tls.inputStream),bytes.toLong()){n->
                    check(busy.get()){ "检测已取消" }
                    val now=System.nanoTime();if(now-transferStart>20_000_000_000L)throw java.io.IOException("测速超时")
                    if(speed&&now-lastReport>300_000_000L){lastReport=now;latest=JSONObject().put("state","running").put("kind","speed").put("bytes",n).put("mbps",n*8.0/(now-transferStart)*1000);ZeekrBridgeManager.publishStatus()}
                }
                val duration=(System.nanoTime()-transferStart)/1_000_000_000.0
                check(count==bytes.toLong()){ "下载未完成，不能计算有效测速结果" }
                latest=JSONObject().put("state","success").put("kind",if(speed)"speed" else "connectivity")
                    .put("route","reverse-socks").put("latencyMs",latency).put("bytes",count).put("seconds",duration)
                    .put("mbps",count*8.0/duration/1_000_000.0).put("endpoint",host)
            }catch(e:Exception){latest=JSONObject().put("state","failed").put("error",e.message?:"检测失败")}
            finally{runCatching{transport?.close()};socket=null;busy.set(false);ZeekrBridgeManager.publishStatus()}
        }
    }
    fun description():String {val r=latest;return when(r.optString("state")){"success"->if(r.optString("kind")=="speed")String.format(java.util.Locale.US,"下载 %.2f Mbps · TLS %.0f ms · %.2f MiB",r.optDouble("mbps"),r.optDouble("latencyMs"),r.optLong("bytes")/1048576.0)else "借网通道可用 · TLS ${r.optDouble("latencyMs").toInt()} ms";"failed"->"检测失败：${r.optString("error")}";"running"->"正在检测车载通道…";else->"尚未检测借网通道"}}
    fun cancel(){runCatching{socket?.close()}}
}
