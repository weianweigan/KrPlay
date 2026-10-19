package com.krplay.protocol;
import java.io.*;
/** Dedicated PCM channel. No audio samples share the video or touch socket. */
public final class AudioWire {
    private AudioWire() {}
    public static final int MAGIC=0x4b525041, VERSION=1, MAX_PCM=8192;
    public static void format(int rate,int channels,int bytes)throws IOException {
        if(rate<8000||rate>96000||channels<1||channels>2||bytes<0||bytes>MAX_PCM||bytes%(channels*2)!=0)throw new IOException("Invalid PCM format/length");
    }
    public static void pcm(DataOutputStream o,int type,int rate,int channels,byte[] data,int off,int n)throws IOException {
        format(rate,channels,n);if(off<0||off>data.length-n)throw new IOException("PCM bounds");
        o.writeByte(1);o.writeInt(type);o.writeInt(rate);o.writeByte(channels);o.writeShort(n);o.write(data,off,n);
    }
}
