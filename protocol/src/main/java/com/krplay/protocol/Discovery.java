package com.krplay.protocol;
import java.io.*;
/** Identity/port negotiation, independent of browser services or an existing control owner. */
public final class Discovery {
    private Discovery() {}
    public static final String FIXED_HOST="6.6.6.6";
    public static final int MAGIC=0x4b52504c,VERSION=1;
    public static void response(DataOutputStream out)throws IOException {
        out.writeInt(MAGIC);out.writeByte(VERSION);for(int p:new int[]{8081,8082,Ports.AUDIO,Ports.REVERSE_WORKERS,Ports.PROBE})out.writeShort(p);out.flush();
    }
    public static boolean verify(DataInputStream in)throws IOException {
        if(in.readInt()!=MAGIC||in.readUnsignedByte()!=VERSION)return false;
        for(int p:new int[]{8081,8082,Ports.AUDIO,Ports.REVERSE_WORKERS,Ports.PROBE})if(in.readUnsignedShort()!=p)return false;
        return true;
    }
}
