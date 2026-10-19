package com.krplay.client;
import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.*;
import android.media.audiofx.AcousticEchoCanceler;
import com.krplay.protocol.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
/** Mic is opened only for an explicit CarPlay request and closed on stop/disconnect/background. */
final class ClientAudio implements AutoCloseable {
    private final Context context;private final String host;
    private volatile boolean running;private volatile Socket socket;private volatile AudioRecord recorder;
    private volatile int generation;private volatile String error="",state="等待音频通道";
    private final Map<Integer,Track> tracks=new HashMap<>();private Thread thread,mic;
    final AtomicLong playedBytes=new AtomicLong(),micBytes=new AtomicLong();
    ClientAudio(Context c,String h){context=c;host=h;}
    private static final class Track {final int rate,channels;final AudioTrack audio;Track(int r,int c,AudioTrack a){rate=r;channels=c;audio=a;}}
    void start(){running=true;thread=new Thread(this::loop,"KrPlay-Audio");thread.start();}
    private void loop(){while(running){Socket s=new Socket();try{
        LocalTransport.bind(s);Wire.socket(s);s.connect(new InetSocketAddress(host,Ports.AUDIO),1500);socket=s;
        DataOutputStream out=new DataOutputStream(s.getOutputStream());out.writeInt(AudioWire.MAGIC);out.writeByte(AudioWire.VERSION);out.writeBoolean(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED);out.flush();state="客户端音频已连接";error="";
        DataInputStream in=new DataInputStream(new BufferedInputStream(s.getInputStream(),8192));byte[] data=new byte[AudioWire.MAX_PCM];
        while(running&&socket==s){int op=in.readUnsignedByte();if(op==1){int type=in.readInt(),rate=in.readInt(),channels=in.readUnsignedByte(),n=in.readUnsignedShort();AudioWire.format(rate,channels,n);in.readFully(data,0,n);play(type,rate,channels,data,n);}
            else if(op==2){int id=in.readInt(),rate=in.readInt(),channels=in.readUnsignedByte();AudioWire.format(rate,channels,0);startMic(s,out,id,rate,channels);}
            else if(op==3){int id=in.readInt();if(generation==id)stopMic();}
            else if(op==4){Track t=tracks.remove(in.readInt());if(t!=null)t.audio.release();}
            else throw new IOException("Unknown audio packet");
        }
    }catch(Exception e){if(running){state="等待音频通道";error=e.getClass().getSimpleName();}}finally{stopMic();socket=null;Wire.close(s);for(Track t:tracks.values())t.audio.release();tracks.clear();}
        if(running)try{Thread.sleep(500);}catch(InterruptedException ignored){}
    }}
    private void play(int type,int rate,int channels,byte[] pcm,int n)throws IOException {
        Track t=tracks.get(type);if(t==null||t.rate!=rate||t.channels!=channels){if(t!=null)t.audio.release();if(tracks.size()>=4&&!tracks.containsKey(type))throw new IOException("Too many audio streams");
            int mask=channels==1?AudioFormat.CHANNEL_OUT_MONO:AudioFormat.CHANNEL_OUT_STEREO;
            int min=AudioTrack.getMinBufferSize(rate,mask,AudioFormat.ENCODING_PCM_16BIT);if(min<=0)throw new IOException("Unsupported playback format");
            AudioTrack a=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setChannelMask(mask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(Math.max(min,rate*channels*2/25)).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build();a.play();t=new Track(rate,channels,a);tracks.put(type,t);
        }
        int off=0;while(off<n&&running){int k=t.audio.write(pcm,off,n-off,AudioTrack.WRITE_BLOCKING);if(k<=0)throw new IOException("AudioTrack write failed "+k);off+=k;playedBytes.addAndGet(k);}
    }
    private synchronized void startMic(Socket s,DataOutputStream out,int id,int rate,int channels){stopMic();generation=id;
        if(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){micError(out,id,1);return;}
        mic=new Thread(()->capture(s,out,id,rate,channels),"KrPlay-Mic");mic.start();
    }
    private void capture(Socket s,DataOutputStream out,int id,int rate,int channels){AudioRecord r=null;AcousticEchoCanceler echo=null;try{
        int mask=channels==1?AudioFormat.CHANNEL_IN_MONO:AudioFormat.CHANNEL_IN_STEREO;int min=AudioRecord.getMinBufferSize(rate,mask,AudioFormat.ENCODING_PCM_16BIT);
        if(min<=0){micError(out,id,2);return;}
        r=new AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION).setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setChannelMask(mask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(Math.max(min,rate*channels*2/10)).build();
        synchronized(this){if(!running||socket!=s||generation!=id){r.release();return;}recorder=r;}
        if(AcousticEchoCanceler.isAvailable()){echo=AcousticEchoCanceler.create(r.getAudioSessionId());if(echo!=null)echo.setEnabled(true);}
        r.startRecording();state="客户端麦克风采集中";byte[] frame=new byte[Math.min(AudioWire.MAX_PCM,rate*channels*2/50)];
        while(running&&socket==s&&generation==id){int n=r.read(frame,0,frame.length,AudioRecord.READ_BLOCKING);if(n<=0){if(generation==id)micError(out,id,3);break;}synchronized(out){out.writeByte(1);out.writeInt(id);out.writeShort(n);out.write(frame,0,n);out.flush();}micBytes.addAndGet(n);}
    }catch(Exception e){if(generation==id&&socket==s){error="麦克风："+e.getClass().getSimpleName();micError(out,id,3);}}
        finally{if(echo!=null)echo.release();synchronized(this){if(recorder==r)recorder=null;}if(r!=null)r.release();if(generation==id&&socket==s)state="客户端音频已连接";}
    }
    private void micError(DataOutputStream out,int id,int code){error=code==1?"请允许客户端麦克风权限":code==2?"不支持请求的录音格式":"麦克风读取失败";try{synchronized(out){out.writeByte(2);out.writeInt(id);out.writeByte(code);out.flush();}}catch(IOException ignored){}}
    private synchronized void stopMic(){generation++;state=running&&socket!=null?"客户端音频已连接":"音频通道已停止";AudioRecord r=recorder;recorder=null;if(r!=null)try{r.stop();}catch(RuntimeException ignored){};}
    String summary(){return state+" · 播放 "+playedBytes.get()/1024+" KiB · 麦克风 "+micBytes.get()/1024+" KiB"+(error.isEmpty()?"":"\n"+error);}
    public void close(){running=false;stopMic();Wire.close(socket);if(thread!=null)thread.interrupt();}
}
