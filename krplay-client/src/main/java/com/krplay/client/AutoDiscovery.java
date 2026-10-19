package com.krplay.client;
import android.content.Context;
import android.net.*;
import com.krplay.protocol.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
/** Probe identities without claiming the exclusive control socket. No subnet scanning. */
final class AutoDiscovery implements AutoCloseable {
    private final Context context;private final String saved;private final BiConsumer<String,Network> found;
    private final AtomicBoolean done=new AtomicBoolean();private final Set<Socket> sockets=ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();private final ExecutorService pool=Executors.newFixedThreadPool(4);
    AutoDiscovery(Context c,String s,BiConsumer<String,Network> f){context=c;saved=s;found=f;}
    void start(){timer.scheduleWithFixedDelay(this::round,0,3,TimeUnit.SECONDS);}
    private static final class Candidate {final String host;final Network network;Candidate(String h,Network n){host=h;network=n;}}
    private List<Candidate> candidates(){LinkedHashMap<String,Candidate> list=new LinkedHashMap<>();
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        for(Network n:cm.getAllNetworks()){NetworkCapabilities caps=cm.getNetworkCapabilities(n);if(caps==null||(!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)&&!caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)))continue;
            add(list,Discovery.FIXED_HOST,n);LinkProperties lp=cm.getLinkProperties(n);if(lp==null)continue;
            for(RouteInfo r:lp.getRoutes()){InetAddress gateway=r.getGateway();if(gateway instanceof Inet4Address&&!gateway.isAnyLocalAddress())add(list,gateway.getHostAddress(),n);}
            if(android.os.Build.VERSION.SDK_INT>=30&&lp.getDhcpServerAddress()!=null)add(list,lp.getDhcpServerAddress().getHostAddress(),n);
            if(saved!=null&&!saved.isEmpty())add(list,saved,n);add(list,"100.96.0.1",n);
        }
        // A vehicle VM may expose only its default virtual network, with no Wi-Fi capability.
        if(list.isEmpty()){add(list,Discovery.FIXED_HOST,null);if(saved!=null&&!saved.isEmpty())add(list,saved,null);add(list,"100.96.0.1",null);}
        return new ArrayList<>(list.values());
    }
    private void add(Map<String,Candidate> list,String host,Network n){list.put(host+"@"+n,new Candidate(host,n));}
    private void round(){if(done.get())return;try{for(Candidate endpoint:candidates())pool.execute(()->probe(endpoint));}catch(RuntimeException ignored){}}
    private void probe(Candidate endpoint){String host=endpoint.host;if(done.get())return;Socket s=new Socket();sockets.add(s);try{
        if(endpoint.network!=null)endpoint.network.bindSocket(s);Wire.socket(s);s.connect(new InetSocketAddress(host,Ports.PROBE),900);s.setSoTimeout(900);DataOutputStream out=new DataOutputStream(s.getOutputStream());out.writeByte(4);out.flush();
        if(Discovery.verify(new DataInputStream(s.getInputStream()))&&done.compareAndSet(false,true)){found.accept(host,endpoint.network);timer.shutdown();pool.shutdown();}
    }catch(Exception ignored){}finally{sockets.remove(s);Wire.close(s);}}
    public void close(){done.set(true);timer.shutdownNow();pool.shutdownNow();for(Socket s:sockets)Wire.close(s);sockets.clear();}
}
