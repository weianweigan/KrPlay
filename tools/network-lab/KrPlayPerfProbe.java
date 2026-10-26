/** Android shell-side TCP probe. No adb forwarding; cross-check with the in-app bound path. */
import java.io.*;import java.net.*;import java.util.*;
public class KrPlayPerfProbe {
 public static void main(String[] args)throws Exception{
  String host=args[0];boolean speed=args.length>1;
  try(Socket s=new Socket()){
   s.setTcpNoDelay(true);s.setSendBufferSize(512*1024);s.setReceiveBufferSize(512*1024);s.connect(new InetSocketAddress(host,8084),2000);s.setSoTimeout(8000);
   System.out.printf(Locale.US,"local=%s remote=%s sendBuffer=%d receiveBuffer=%d ",s.getLocalAddress().getHostAddress(),s.getInetAddress().getHostAddress(),s.getSendBufferSize(),s.getReceiveBufferSize());
   DataInputStream in=new DataInputStream(s.getInputStream());DataOutputStream out=new DataOutputStream(s.getOutputStream());double[] rtt=new double[40];
   out.writeByte(4);out.flush();if(in.readInt()!=0x4b52504c||in.readUnsignedByte()!=1)throw new IOException("Not a KrPlay probe");for(int port:new int[]{8081,8082,8083,7000,8084})if(in.readUnsignedShort()!=port)throw new IOException("Unexpected port map");
   for(int i=0;i<rtt.length;i++){long t=System.nanoTime();out.writeByte(1);out.writeLong(t);out.flush();if(in.readLong()!=t)throw new IOException("Echo mismatch");rtt[i]=(System.nanoTime()-t)/1e6;Thread.sleep(50);}Arrays.sort(rtt);
   System.out.printf(Locale.US,"host=%s rttMedianMs=%.3f rttP95Ms=%.3f rttMaxMs=%.3f",host,(rtt[19]+rtt[20])/2,rtt[37],rtt[39]);
   if(speed){int n=8*1024*1024;byte[] b=new byte[32768];out.writeByte(2);out.writeInt(n);out.flush();long t=System.nanoTime();for(int remaining=n;remaining>0;){int k=in.read(b,0,Math.min(remaining,b.length));if(k<0)throw new EOFException();remaining-=k;}double down=n*8000.0/(System.nanoTime()-t);
    out.writeByte(3);out.writeInt(n);t=System.nanoTime();for(int off=0;off<n;off+=b.length)out.write(b,0,Math.min(b.length,n-off));out.flush();if(in.readUnsignedByte()!=1)throw new IOException("Upload failed");double up=n*8000.0/(System.nanoTime()-t);System.out.printf(Locale.US," downMbps=%.2f upMbps=%.2f",down,up);
   }System.out.println();
  }
 }
}
