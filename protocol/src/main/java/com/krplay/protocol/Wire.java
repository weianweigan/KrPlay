package com.krplay.protocol;
import java.io.*;
import java.net.*;
import java.util.*;

/** KrPlay v1: network byte order, 13-byte frame header, bounded payload. */
public final class Wire {
    public static final int MAX_FRAME = 2 * 1024 * 1024;
    private Wire() {}
    public static void socket(Socket s) throws SocketException {
        s.setTcpNoDelay(true); s.setKeepAlive(true);
        s.setSendBufferSize(512 * 1024); s.setReceiveBufferSize(512 * 1024);
        try { s.setTrafficClass(0x10); } catch (SocketException ignored) { }
    }
    /** Best-effort CS4 marking; the AP/host may ignore it. Buffers are kernel hints. */
    public static void videoSocket(Socket s) throws SocketException {
        socket(s);
        try { s.setTrafficClass(0x80); } catch (SocketException ignored) { }
    }
    public static final class Frame {
        public final int type, size; public final long pts;
        public Frame(int t, long p, int n) { type=t; pts=p; size=n; }
    }
    public static Frame header(DataInputStream in) throws IOException {
        return validateHeader(in.readUnsignedByte(),in.readLong(),in.readInt());
    }
    public static Frame validateHeader(int type,long pts,int n)throws IOException {
        if(n<0 || n>MAX_FRAME) throw new IOException("Invalid frame length: "+n);
        if(type!=0 && type!=1 && type!=2 && type!=3 && type!=4 && type!=16 && type!=20 && type!=21) throw new IOException("Unknown frame type");
        if(type==4 && n!=4 || type==16 && n%4!=0 || (type==20||type==21)&&n<16) throw new IOException("Malformed frame");
        return new Frame(type,pts,n);
    }
    public static void frame(DataOutputStream out,int type,long pts,byte[] data,int offset,int n) throws IOException {
        if(n<0 || n>MAX_FRAME || offset<0 || offset>data.length-n) throw new IOException("Invalid frame bounds");
        out.writeByte(type); out.writeLong(pts); out.writeInt(n); out.write(data,offset,n);
    }
    public static final class Touch {
        public final int action; public final int[] ids; public final float[] xs,ys;
        public Touch(int a,int[] i,float[] x,float[] y) { action=a; ids=i; xs=x; ys=y; validate(); }
        private void validate() {
            if(action<0 || action>3 || ids.length<1 || ids.length>10 || xs.length!=ids.length || ys.length!=ids.length) throw new IllegalArgumentException("Invalid touch");
            Set<Integer> unique=new HashSet<>();
            for(int j=0;j<ids.length;j++) if(ids[j]<0 || ids[j]>31 || !unique.add(ids[j]) || !Float.isFinite(xs[j]) || !Float.isFinite(ys[j]) || xs[j]<0 || xs[j]>1 || ys[j]<0 || ys[j]>1) throw new IllegalArgumentException("Invalid contact");
        }
        public void write(DataOutputStream out) throws IOException {
            out.writeByte(2); out.writeByte(action); out.writeByte(ids.length);
            for(int j=0;j<ids.length;j++) { out.writeByte(ids[j]);out.writeFloat(xs[j]);out.writeFloat(ys[j]); }
        }
        public static Touch read(DataInputStream in) throws IOException {
            int a=in.readUnsignedByte(),n=in.readUnsignedByte();
            if(n<1 || n>10) throw new IOException("Invalid pointer count");
            int[] i=new int[n];float[] x=new float[n],y=new float[n];
            for(int j=0;j<n;j++){i[j]=in.readUnsignedByte();x[j]=in.readFloat();y[j]=in.readFloat();}
            try { return new Touch(a,i,x,y); } catch(IllegalArgumentException e){ throw new IOException(e); }
        }
    }
    /** Split every Annex-B NAL, including 3-byte start codes and mixed config+IDR AUs. */
    public static List<byte[]> nals(byte[] data,int offset,int size) {
        if(offset<0 || size<0 || offset>data.length-size) throw new IllegalArgumentException("Invalid NAL bounds");
        List<byte[]> result=new ArrayList<>();int start=-1,end=offset+size;
        for(int j=offset;j+2<end;j++) {
            int prefix=data[j]==0 && data[j+1]==0 ? (data[j+2]==1?3:(j+3<end && data[j+2]==0 && data[j+3]==1?4:0)):0;
            if(prefix>0){if(start>=0 && start<j)result.add(Arrays.copyOfRange(data,start,j));start=j+prefix;j+=prefix-1;}
        }
        if(start>=0 && start<end)result.add(Arrays.copyOfRange(data,start,end));return result;
    }
    public static boolean randomAccess(boolean hevc,byte[] data,int size) {
        if(size<0 || size>data.length)throw new IllegalArgumentException("Invalid NAL bounds");
        for(int i=0;i+3<size;i++)if(data[i]==0 && data[i+1]==0){int prefix=data[i+2]==1?3:(data[i+2]==0&&data[i+3]==1?4:0);
            if(prefix>0 && i+prefix<size){int type=hevc?(data[i+prefix]>>1)&63:data[i+prefix]&31;if(key(hevc,type))return true;i+=prefix;}}
        return false;
    }
    /** Only positively identified non-reference AVC AUs may be discarded compressed.
     * HEVC, partitioned AVC and unknown NAL types conservatively retain references. */
    public static boolean discardableAvc(byte[] data,int offset,int size) {
        if(offset<0||size<0||offset>data.length-size)return false;
        boolean slice=false;int end=offset+size;
        for(int i=offset;i+3<end;i++)if(data[i]==0&&data[i+1]==0){
            int prefix=data[i+2]==1?3:(i+3<end&&data[i+2]==0&&data[i+3]==1?4:0);
            if(prefix==0)continue;if(i+prefix>=end)return false;
            int header=data[i+prefix]&255,type=header&31;
            if((header&0x80)!=0)return false;
            if(type==1){if((header&0x60)!=0)return false;slice=true;}
            else if(type!=6&&type!=9&&type!=12)return false;
            i+=prefix;
        }
        return slice;
    }
    public static int nalType(boolean hevc,byte[] n){ return n.length==0?-1:hevc?(n[0]>>1)&63:n[0]&31; }
    public static boolean key(boolean hevc,int type){return hevc?type>=16 && type<=21:type==5;}
    public static byte[] annex(List<byte[]> nals){ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] n:nals){out.write(0);out.write(0);out.write(0);out.write(1);out.write(n,0,n.length);}return out.toByteArray();}
    public static void close(Closeable s){if(s!=null)try{s.close();}catch(IOException ignored){}}
}
