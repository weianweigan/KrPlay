package com.krplay.client;
import android.content.Context;
import android.net.*;
import com.krplay.protocol.Wire;
import com.krplay.protocol.Socks5;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

final class ReverseSocks5Worker implements AutoCloseable {
    private final String host;private final ConnectivityManager cm;private final Consumer<String> status;
    private volatile boolean running;private final Set<Socket> sockets=ConcurrentHashMap.newKeySet();
    private final AtomicInteger idle=new AtomicInteger(),total=new AtomicInteger();private Thread maintainer;
    ReverseSocks5Worker(Context c,String h,Consumer<String> s){host=h;cm=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);status=s;}
    void start(){if(running)return;running=true;maintainer=new Thread(()->{while(running){while(running&&idle.get()<8&&total.get()<32){idle.incrementAndGet();total.incrementAndGet();new Thread(this::worker,"KrPlay-SOCKS").start();}try{Thread.sleep(300);}catch(InterruptedException ignored){}}},"KrPlay-SOCKS-Pool");maintainer.start();}
    private Network internet()throws IOException{
        for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c!=null&&c.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))return n;}
        Network n=cm.getActiveNetwork();NetworkCapabilities c=n==null?null:cm.getNetworkCapabilities(n);
        if(c==null||!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)||!c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))throw new IOException("没有可用的车载外网");return n;
    }
    private Socket connectInternet(String target,int port)throws IOException {
        Network network=internet();InetAddress[] addresses=network.getAllByName(target);IOException failure=null;
        for(InetAddress address:addresses){Socket candidate=network.getSocketFactory().createSocket();sockets.add(candidate);try{Wire.socket(candidate);candidate.connect(new InetSocketAddress(address,port),5000);return candidate;}catch(IOException e){failure=e;sockets.remove(candidate);Wire.close(candidate);}}
        throw failure==null?new IOException("DNS returned no addresses"):failure;
    }
    private void worker(){java.util.concurrent.atomic.AtomicBoolean waiting=new java.util.concurrent.atomic.AtomicBoolean(true);Socket box=new Socket(),remote=null;sockets.add(box);try{
        LocalTransport.bind(box);Wire.socket(box);box.connect(new InetSocketAddress(host,7000),2000);DataInputStream in=new DataInputStream(box.getInputStream());DataOutputStream out=new DataOutputStream(box.getOutputStream());
        remote=Socks5.negotiate(in,out,this::connectInternet,()->{idle.decrementAndGet();waiting.set(false);try{box.setSoTimeout(10000);}catch(SocketException ignored){}});
        if(remote==null)return;box.setSoTimeout(0);final Socket r=remote;
        Thread upstream=new Thread(()->pipe(box,r),"KrPlay-SOCKS-Up");upstream.start();pipe(r,box);upstream.join();
    }catch(Exception e){if(running)try{Thread.sleep(800);}catch(InterruptedException ignored){}}finally{if(waiting.get())idle.decrementAndGet();sockets.remove(box);if(remote!=null)sockets.remove(remote);Wire.close(box);Wire.close(remote);total.decrementAndGet();}}
    private void pipe(Socket from,Socket to){try{byte[] b=new byte[32768];InputStream in=from.getInputStream();OutputStream out=to.getOutputStream();int n;while(running&&(n=in.read(b))!=-1){out.write(b,0,n);out.flush();}to.shutdownOutput();}catch(IOException e){Wire.close(from);Wire.close(to);}}
    public void close(){running=false;if(maintainer!=null)maintainer.interrupt();for(Socket s:sockets)Wire.close(s);}
}
