import com.krplay.protocol.Socks5;
import com.krplay.protocol.Wire;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/** Desktop vehicle-peer fixture for emulator VPN tests; no authentication assets required. */
public final class TunnelWorkerHarness {
    private static void pipe(Socket from,Socket to){try{byte[] b=new byte[32768];int n;while((n=from.getInputStream().read(b))>=0){to.getOutputStream().write(b,0,n);to.getOutputStream().flush();}to.shutdownOutput();}catch(Exception e){Wire.close(from);Wire.close(to);}}
    public static void main(String[] args)throws Exception{
        int port=Integer.parseInt(args[0]);ExecutorService pool=Executors.newFixedThreadPool(8);
        for(int j=0;j<8;j++)pool.submit(()->{while(true){Socket worker=new Socket(),remote=null;try{
            Wire.socket(worker);worker.connect(new InetSocketAddress("127.0.0.1",port),1500);
            remote=Socks5.negotiate(new DataInputStream(worker.getInputStream()),new DataOutputStream(worker.getOutputStream()),(host,p)->{
                System.out.println("Vehicle peer CONNECT "+host+":"+p);Socket s=new Socket();Wire.socket(s);s.connect(new InetSocketAddress(host,p),5000);return s;
            },()->{});
            if(remote==null)continue;final Socket r=remote;Thread t=new Thread(()->pipe(worker,r));t.start();pipe(r,worker);t.join();
        }catch(Exception e){System.err.println("Vehicle peer: "+e);try{Thread.sleep(500);}catch(InterruptedException ignored){return;}}finally{Wire.close(worker);Wire.close(remote);}}});
    }
}
