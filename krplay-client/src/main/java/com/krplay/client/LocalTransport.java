package com.krplay.client;
import android.content.Context;
import android.net.*;
import java.io.IOException;
import java.net.Socket;
/** LAN binding must never be applied to the independent vehicle Internet sockets. */
final class LocalTransport {
    static volatile Network network;
    static Network preferred(Context context){ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);Network ethernet=null;
        for(Network n:cm.getAllNetworks()){NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c==null)continue;if(c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))return n;if(c.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))ethernet=n;}return ethernet;
    }
    static void bind(Socket s)throws IOException {Network n=network;if(n!=null)n.bindSocket(s);}
}
