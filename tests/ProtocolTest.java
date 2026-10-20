import com.krplay.protocol.*;
import java.io.*;
import java.net.*;
import java.util.*;

public final class ProtocolTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void invalid(byte[] header)throws Exception{try{Wire.header(new DataInputStream(new ByteArrayInputStream(header)));throw new AssertionError("Expected invalid header");}catch(IOException expected){}}
    static byte[] header(int type,int n)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);o.writeByte(type);o.writeLong(0x0102030405060708L);o.writeInt(n);return b.toByteArray();}
    static int socks(byte[] input,Socks5.Connector connector)throws Exception{ByteArrayOutputStream result=new ByteArrayOutputStream();Socket s=Socks5.negotiate(new DataInputStream(new ByteArrayInputStream(input)),new DataOutputStream(result),connector,()->{});Wire.close(s);byte[] out=result.toByteArray();return out.length==2?out[1]&255:out[3]&255;}
    public static void main(String[] args)throws Exception {
        ByteArrayOutputStream discoveryBytes=new ByteArrayOutputStream();Discovery.response(new DataOutputStream(discoveryBytes));
        check(Discovery.verify(new DataInputStream(new ByteArrayInputStream(discoveryBytes.toByteArray()))),"KrPlay identity and port negotiation");
        byte[] malformed=discoveryBytes.toByteArray();malformed[0]=0;check(!Discovery.verify(new DataInputStream(new ByteArrayInputStream(malformed))),"foreign service rejected");
        check(Arrays.equals(VideoPolicy.viewport(3200,2136,1),new int[]{1616,1080}),"1080p aspect and even codec dimensions");
        check(Arrays.equals(VideoPolicy.viewport(1920,1080,2),new int[]{1280,720}),"720p low latency cap");
        check(Arrays.equals(VideoPolicy.viewport(3200,2136,0),new int[]{3200,2136}),"native profile retained");
        check(VideoPolicy.renderTime(1000500,1000000,20_000_000)==20_000_000,"fabricated PTS is not zero decode latency");
        check(VideoPolicy.renderTime(1000000,15_000_000,20_000_000)==15_000_000,"valid hardware timestamp preserved");
        VideoPolicy.KeyframeRequests requests=new VideoPolicy.KeyframeRequests();
        check(requests.acquire(-2_000_000_000L),"first request accepts a negative monotonic clock origin");
        check(!requests.acquire(-1_999_000_000L),"concurrent reconnect/decoder requests coalesced");
        check(!requests.acquire(-1_000_000_001L),"keyframe request cooldown retained");
        check(requests.acquire(-1_000_000_000L),"missing keyframe can be requested again after one second");
        PerformanceStats stats=new PerformanceStats();
        for(int i=0;i<5;i++)check(stats.probe(1_000_000_000L,6_002_000_000L,6_003_000_000L,1_005_000_000L),"valid asymmetric clocks");
        check(Math.abs(stats.offsetNs()-5_000_000_000L)<2,"independent device clocks aligned");check(stats.synced()&&stats.rttMs()==4,"server processing excluded from RTT");
        check(stats.bridgeMs(6_000_000_000L,1_025_000_000L)==25,"bridge delay uses aligned monotonic clocks");check(Double.isNaN(stats.bridgeMs(0,1)),"cached frame has no latency claim");
        check(!stats.probe(10,20,15,30),"invalid server clock rejected");stats.reset();check(!stats.synced()&&Double.isNaN(stats.rttMs()),"new endpoint resets clock samples");
        invalid(header(20,15));check(Wire.header(new DataInputStream(new ByteArrayInputStream(header(21,100)))).size==100,"timed video extension bounded");
        AudioWire.format(24000,1,960);try{AudioWire.format(48000,2,3);throw new AssertionError("partial PCM frame");}catch(IOException expected){}
        try{AudioWire.format(48000,3,12);throw new AssertionError("unsupported channels");}catch(IOException expected){}
        byte[] data=new byte[Wire.MAX_FRAME];data[data.length-1]=7;ByteArrayOutputStream b=new ByteArrayOutputStream();Wire.frame(new DataOutputStream(b),0,123456789L,data,0,data.length);
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(b.toByteArray()));Wire.Frame f=Wire.header(in);check(f.size==Wire.MAX_FRAME&&f.pts==123456789L,"2MiB header roundtrip");byte[] got=new byte[f.size];in.readFully(got);check(Arrays.equals(data,got),"2MiB payload preserved");
        invalid(header(0,-1));invalid(header(0,Wire.MAX_FRAME+1));invalid(header(4,3));invalid(header(16,3));invalid(header(127,1));
        Wire.Touch touch=new Wire.Touch(1,new int[]{4,1},new float[]{1,0},new float[]{0,.5f});b.reset();touch.write(new DataOutputStream(b));in=new DataInputStream(new ByteArrayInputStream(b.toByteArray()));check(in.readUnsignedByte()==2,"touch command");Wire.Touch decoded=Wire.Touch.read(in);check(decoded.ids[0]==4&&decoded.action==1&&decoded.ys[1]==.5f,"changed pointer first preserved");
        try{new Wire.Touch(0,new int[]{1,1},new float[]{0,0},new float[]{0,0});throw new AssertionError("duplicate pointers");}catch(IllegalArgumentException expected){}
        try{new Wire.Touch(0,new int[]{1},new float[]{Float.NaN},new float[]{0});throw new AssertionError("NaN pointer");}catch(IllegalArgumentException expected){}
        List<byte[]> nals=Wire.nals(new byte[]{0,0,1,0x67,1,0,0,0,1,0x68,2,0,0,1,0x65,3},0,16);check(nals.size()==3,"all 3-/4-byte prefixes");check(Wire.key(false,Wire.nalType(false,nals.get(2))),"IDR after config");
        check(Wire.randomAccess(false,Wire.annex(nals),Wire.annex(nals).length),"allocation-free IDR scan");
        for(int type=16;type<=21;type++)check(Wire.key(true,type),"HEVC IRAP "+type);
        byte[] request={5,1,0,5,1,0,3,3,'a','b','c',0,80};
        check(socks(request,(h,p)->{check(h.equals("abc")&&p==80,"domain CONNECT");return new Socket();})==0,"SOCKS success");
        check(socks(request,(h,p)->{throw new IOException();})==4,"SOCKS connect failure");
        check(socks(new byte[]{5,1,2},(h,p)->{throw new AssertionError();})==255,"authentication method rejection");
        check(socks(new byte[]{5,1,0,5,3,0,1},(h,p)->{throw new AssertionError();})==7,"UDP explicitly rejected");
        byte[] ipv6={5,1,0,5,1,0,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,(byte)187};
        check(socks(ipv6,(h,p)->{check(p==443&&InetAddress.getByName(h).isLoopbackAddress(),"IPv6 CONNECT");return new Socket();})==0,"IPv6 supported");
        System.out.println("Protocol tests passed: max payload, malformed headers, touches, mixed NALs, SOCKS methods/CONNECT/IPv6/failures");
    }
}
