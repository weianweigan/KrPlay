package com.krplay.client;
import com.krplay.protocol.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
final class LinkDiagnostics implements AutoCloseable {
    private final AtomicBoolean busy=new AtomicBoolean();private volatile Socket socket;private volatile boolean closed;
    void run(String host,boolean speed,Consumer<String> report){if(closed||!busy.compareAndSet(false,true))return;
        new Thread(()->{Socket s=new Socket();socket=s;try{report.accept(speed?"局域网双向测速中 · 各 8 MiB，可能短暂影响画面":"正在检测局域网延迟…");
            LocalTransport.bind(s);Wire.socket(s);s.connect(new InetSocketAddress(host,Ports.PROBE),1500);s.setSoTimeout(5000);DataInputStream in=new DataInputStream(s.getInputStream());DataOutputStream out=new DataOutputStream(s.getOutputStream());
            double[] rtt=new double[20];for(int i=0;i<rtt.length;i++){long t=System.nanoTime();out.writeByte(1);out.writeLong(t);out.flush();if(in.readLong()!=t)throw new IOException("Echo mismatch");rtt[i]=(System.nanoTime()-t)/1e6;Thread.sleep(30);}Arrays.sort(rtt);
            String text=String.format(Locale.US,"局域网 RTT 中位 %.2f ms · P95 %.2f ms",rtt[10],rtt[18]);
            if(speed){byte[] buf=new byte[32768];int n=8*1024*1024;out.writeByte(2);out.writeInt(n);out.flush();long t=System.nanoTime();int remaining=n;while(remaining>0){int k=in.read(buf,0,Math.min(remaining,buf.length));if(k<0)throw new EOFException();remaining-=k;}double down=n*8000.0/(System.nanoTime()-t);
                out.writeByte(3);out.writeInt(n);t=System.nanoTime();for(int off=0;off<n;off+=buf.length)out.write(buf,0,Math.min(buf.length,n-off));out.flush();if(in.readUnsignedByte()!=1)throw new IOException("Upload incomplete");double up=n*8000.0/(System.nanoTime()-t);
                text+=String.format(Locale.US,"\n接收端 → 客户端 %.2f Mbps\n客户端 → 接收端 %.2f Mbps",down,up);}
            if(!closed)report.accept(text);
        }catch(Exception e){if(!closed)report.accept("局域网测试失败："+e.getClass().getSimpleName()+" · "+e.getMessage());}finally{Wire.close(s);socket=null;busy.set(false);}},"KrPlay-LAN-Test").start();
    }
    public void close(){closed=true;Wire.close(socket);}
}
