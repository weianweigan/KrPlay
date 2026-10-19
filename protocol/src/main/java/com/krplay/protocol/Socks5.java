package com.krplay.protocol;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/** RFC 1928 no-authentication TCP CONNECT, with deterministic failure responses. */
public final class Socks5 {
    public interface Connector { Socket connect(String host,int port) throws IOException; }
    private Socks5() {}
    public static Socket negotiate(DataInputStream in,DataOutputStream out,Connector connector,Runnable assigned)throws IOException {
        int version=in.readUnsignedByte();assigned.run();if(version!=5)throw new IOException("Bad SOCKS version");
        int count=in.readUnsignedByte();boolean noAuth=false;for(int i=0;i<count;i++)if(in.readUnsignedByte()==0)noAuth=true;
        out.write(new byte[]{5,(byte)(noAuth?0:255)});out.flush();if(!noAuth)return null;
        if(in.readUnsignedByte()!=5)throw new IOException("Bad request version");
        int cmd=in.readUnsignedByte(),reserved=in.readUnsignedByte(),type=in.readUnsignedByte();
        if(cmd!=1 || reserved!=0){reply(out,7);return null;}
        String target;byte[] addr;
        if(type==1||type==4){addr=new byte[type==1?4:16];in.readFully(addr);target=InetAddress.getByAddress(addr).getHostAddress();}
        else if(type==3){int n=in.readUnsignedByte();if(n==0){reply(out,8);return null;}addr=new byte[n];in.readFully(addr);target=new String(addr,StandardCharsets.US_ASCII);}
        else{reply(out,8);return null;}
        int port=in.readUnsignedShort();if(port==0){reply(out,1);return null;}
        Socket socket;
        try{socket=connector.connect(target,port);}catch(IOException e){reply(out,4);return null;}
        try{reply(out,0);return socket;}catch(IOException e){Wire.close(socket);throw e;}
    }
    private static void reply(DataOutputStream out,int code)throws IOException{out.write(new byte[]{5,(byte)code,0,1,0,0,0,0,0,0});out.flush();}
}
