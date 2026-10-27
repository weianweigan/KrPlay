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
    final AtomicLong playedBytes=new AtomicLong(),micBytes=new AtomicLong(),droppedPcm=new AtomicLong();
    ClientAudio(Context c,String h){context=c;host=h;}
    private static final class Track {final int rate,channels;final AudioTrack audio;boolean fade=true;final short[] last;Track(int r,int c,AudioTrack a){rate=r;channels=c;audio=a;last=new short[c];}}
    private static final class Pcm {
        final int type,rate,channels;final byte[] data;
        Pcm(int t,int r,int c,byte[] b){type=t;rate=r;channels=c;data=b;}
    }
    void start(){running=true;thread=new Thread(this::loop,"KrPlay-Audio");thread.start();}
    private void loop(){while(running){Socket s=new Socket();RealtimeAudioQueue<Pcm> playback=new RealtimeAudioQueue<>();
        java.util.concurrent.atomic.AtomicBoolean playing=new java.util.concurrent.atomic.AtomicBoolean(true);
        Thread player=null;try{
        LocalTransport.bind(s);Wire.socket(s);s.connect(new InetSocketAddress(host,Ports.AUDIO),1500);socket=s;
        DataOutputStream out=new DataOutputStream(s.getOutputStream());out.writeInt(AudioWire.MAGIC);out.writeByte(AudioWire.VERSION);out.writeBoolean(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED);out.flush();state="客户端音频已连接";error="";
        player=new Thread(()->playback(s,playback,playing),"KrPlay-Audio-Playback");player.start();
        DataInputStream in=new DataInputStream(new BufferedInputStream(s.getInputStream(),8192));byte[] data=new byte[AudioWire.MAX_PCM];
        while(running&&socket==s){int op=in.readUnsignedByte();if(op==1){int type=in.readInt(),rate=in.readInt(),channels=in.readUnsignedByte(),n=in.readUnsignedShort();AudioWire.format(rate,channels,n);in.readFully(data,0,n);if(!playback.offer(new Pcm(type,rate,channels,Arrays.copyOf(data,n)),System.nanoTime(),Math.max(1,n*1_000_000_000L/(rate*channels*2))))throw new IOException("Audio controls congested");}
            else if(op==2){int id=in.readInt(),rate=in.readInt(),channels=in.readUnsignedByte();AudioWire.format(rate,channels,0);startMic(s,out,id,rate,channels);}
            else if(op==3){int id=in.readInt();if(generation==id)stopMic();}
            else if(op==4){if(!playback.offer(new Pcm(in.readInt(),0,0,null),System.nanoTime(),0))throw new IOException("Audio controls congested");}
            else throw new IOException("Unknown audio packet");
        }
    }catch(Exception e){if(running){state="等待音频通道";error=e.getClass().getSimpleName();}}finally{stopMic();socket=null;Wire.close(s);playing.set(false);playback.clear();
        if(player!=null){player.interrupt();try{player.join();}catch(InterruptedException ignored){}}
        }
        if(running)try{Thread.sleep(500);}catch(InterruptedException ignored){}
    }}
    private void playback(Socket s,RealtimeAudioQueue<Pcm> queue,java.util.concurrent.atomic.AtomicBoolean active){
        long dropped=0;
        try {while(running && active.get()){
            RealtimeAudioQueue.Entry<Pcm> e=queue.poll(20);if(e==null)continue;
            long next=queue.dropped();boolean discontinuity=next!=dropped;
            droppedPcm.addAndGet(next-dropped);dropped=next;
            Pcm p=e.value;
            if(discontinuity)flushTracks();
            if(p.data==null){Track t=tracks.remove(p.type);if(t!=null)t.audio.release();continue;}
            if(System.nanoTime()-e.enqueuedNs>RealtimeAudioQueue.MAX_AGE_NS){droppedPcm.incrementAndGet();flushTracks();continue;}
            play(p.type,p.rate,p.channels,p.data,p.data.length,e.enqueuedNs,active);
        }}catch(InterruptedException ignored){}catch(Exception e){if(active.get()){error="播放："+e.getClass().getSimpleName();Wire.close(s);}}
        finally{for(Track t:tracks.values())t.audio.release();tracks.clear();}
    }
    private void flushTracks(){for(Track t:tracks.values())flushTrack(t);}
    private void flushTrack(Track t){
        t.audio.pause();t.audio.flush();t.fade=true;t.audio.play();
        int frames=Math.max(1,t.rate/200);byte[] ramp=new byte[frames*t.channels*2];
        for(int f=0;f<frames;f++)for(int c=0;c<t.channels;c++){int v=t.last[c]*(frames-1-f)/frames,i=(f*t.channels+c)*2;ramp[i]=(byte)v;ramp[i+1]=(byte)(v>>8);}
        t.audio.write(ramp,0,ramp.length,AudioTrack.WRITE_NON_BLOCKING);Arrays.fill(t.last,(short)0);
    }
    private void play(int type,int rate,int channels,byte[] pcm,int n,long received,java.util.concurrent.atomic.AtomicBoolean active)throws IOException,InterruptedException {
        Track t=tracks.get(type);if(t==null||t.rate!=rate||t.channels!=channels){if(t!=null)t.audio.release();if(tracks.size()>=4&&!tracks.containsKey(type))throw new IOException("Too many audio streams");
            int mask=channels==1?AudioFormat.CHANNEL_OUT_MONO:AudioFormat.CHANNEL_OUT_STEREO;
            int min=AudioTrack.getMinBufferSize(rate,mask,AudioFormat.ENCODING_PCM_16BIT);if(min<=0)throw new IOException("Unsupported playback format");
            AudioTrack a=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(rate).setChannelMask(mask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(Math.max(min,rate*channels*2/25)).setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY).build();a.play();t=new Track(rate,channels,a);tracks.put(type,t);
        }
        // Fade in after a flush; no stale PCM may block the socket reader.
        if(t.fade){int frames=Math.min(n/(channels*2),Math.max(1,rate/200));
            for(int f=0;f<frames;f++)for(int c=0;c<channels;c++){int i=(f*channels+c)*2;int sample=(short)((pcm[i]&255)|(pcm[i+1]<<8));sample=sample*f/frames;pcm[i]=(byte)sample;pcm[i+1]=(byte)(sample>>8);}t.fade=false;}
        int off=0;while(off<n&&running&&active.get()){
            if(System.nanoTime()-received>RealtimeAudioQueue.MAX_AGE_NS){droppedPcm.incrementAndGet();flushTrack(t);break;}
            int k=t.audio.write(pcm,off,n-off,AudioTrack.WRITE_NON_BLOCKING);
            if(k<0)throw new IOException("AudioTrack write failed "+k);
            if(k==0){Thread.sleep(2);continue;}off+=k;playedBytes.addAndGet(k);
            if(k>=channels*2)for(int c=0;c<channels;c++){int i=off-channels*2+c*2;t.last[c]=(short)((pcm[i]&255)|(pcm[i+1]<<8));}
        }
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
    String summary(){return state+" · 播放 "+playedBytes.get()/1024+" KiB · 麦克风 "+micBytes.get()/1024+" KiB · 丢弃过期PCM "+droppedPcm.get()+(error.isEmpty()?"":"\n"+error);}
    public void close(){running=false;stopMic();Wire.close(socket);if(thread!=null)thread.interrupt();}
}
