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

/** Network reads, PCM playback and microphone capture have independent workers. */
final class ClientAudio implements AutoCloseable {
    private final Context context;private final String host;private final int bufferMs;
    private volatile boolean running;private volatile Socket socket;private volatile AudioRecord recorder;
    private volatile int generation;private volatile String error="",state="等待音频通道",playbackSummary="等待 PCM";
    private Thread thread,mic;
    private volatile int negotiatedVersion;
    final AtomicLong playedBytes=new AtomicLong(),micBytes=new AtomicLong(),droppedPcm=new AtomicLong();
    private final AtomicLong renderedFrames=new AtomicLong(),underruns=new AtomicLong(),rebuffers=new AtomicLong(),connections=new AtomicLong(),recoveries=new AtomicLong();
    ClientAudio(Context c,String h){context=c;host=h;bufferMs=AudioPolicy.bufferMs(c.getSharedPreferences("audio",0).getInt("musicBufferMs",AudioPolicy.DEFAULT_BUFFER_MS));}
    private static final class Track {
        final AudioPlayoutQueue lane;final AudioTrack audio;final AudioProgress progress=new AudioProgress();
        final int startFrames,capacityFrames;boolean playing;int lastUnderruns;
        Track(AudioPlayoutQueue l,AudioTrack a,int start){lane=l;audio=a;capacityFrames=a.getBufferSizeInFrames();startFrames=AudioProgress.startFrames(start,capacityFrames);}
    }
    void start(){running=true;thread=new Thread(this::loop,"KrPlay-Audio");thread.start();}
    private void loop(){boolean legacy=false;while(running){Socket s=new Socket();
        final Map<Integer,AudioPlayoutQueue> lanes=new LinkedHashMap<>();
        java.util.concurrent.atomic.AtomicBoolean active=new java.util.concurrent.atomic.AtomicBoolean(true);
        Thread player=null,feedback=null;boolean established=false;
        try{
            LocalTransport.bind(s);Wire.socket(s);s.connect(new InetSocketAddress(host,Ports.AUDIO),1500);socket=s;
            DataOutputStream out=new DataOutputStream(s.getOutputStream());DataInputStream in=new DataInputStream(new BufferedInputStream(s.getInputStream(),32768));
            int version=legacy?AudioWire.VERSION:AudioWire.BUFFERED_VERSION;
            out.writeInt(AudioWire.MAGIC);out.writeByte(version);out.writeBoolean(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED);
            if(!legacy)out.writeInt(bufferMs);out.flush();
            if(!legacy){s.setSoTimeout(2000);if(in.readUnsignedByte()!=AudioWire.HELLO_V2||in.readUnsignedByte()!=version)throw new IOException("Audio v2 negotiation failed");s.setSoTimeout(0);}
            established=true;negotiatedVersion=version;connections.incrementAndGet();state="客户端音频已连接";error="";
            player=new Thread(()->playback(s,out,lanes,active,version),"KrPlay-Audio-Playback");player.start();
            if(version==AudioWire.BUFFERED_VERSION){feedback=new Thread(()->feedback(s,out,lanes,active),"KrPlay-Audio-Feedback");feedback.start();}
            while(running&&socket==s){int op=in.readUnsignedByte();
                if(op==1||op==AudioWire.PCM_V2){
                    if(op==AudioWire.PCM_V2&&version!=AudioWire.BUFFERED_VERSION)throw new IOException("Unexpected audio version");
                    int type=in.readInt(),epoch=op==1?1:in.readInt(),purpose=op==1?(type==102?AudioPolicy.MUSIC:AudioPolicy.NAVIGATION):in.readUnsignedByte();
                    int rate=in.readInt(),channels=in.readUnsignedByte();long firstFrame=op==1?-1:in.readLong();int n=in.readUnsignedShort();AudioWire.format(rate,channels,n);
                    byte[] bytes=new byte[n];in.readFully(bytes);
                    synchronized(lanes){AudioPlayoutQueue lane=lanes.get(type);
                        if(lane==null||lane.epoch!=epoch||lane.rate!=rate||lane.channels!=channels||lane.purpose!=purpose){
                            if(lane!=null)lane.cancel();if(lanes.size()>=4&&!lanes.containsKey(type))throw new IOException("Too many audio streams");
                            lane=new AudioPlayoutQueue(type,epoch,purpose,rate,channels,bufferMs);lanes.put(type,lane);
                        }
                        lane.offer(firstFrame<0?lane.received():firstFrame,bytes,System.nanoTime());
                    }
                }else if(op==2){int id=in.readInt(),rate=in.readInt(),channels=in.readUnsignedByte();AudioWire.format(rate,channels,0);startMic(s,out,id,rate,channels);}
                else if(op==3){int id=in.readInt();if(generation==id)stopMic();}
                else if(op==4||op==AudioWire.END_V2){int type=in.readInt(),epoch=op==4?-1:in.readInt();boolean drain=op!=4&&in.readBoolean();
                    synchronized(lanes){AudioPlayoutQueue lane=lanes.get(type);if(lane!=null&&(epoch<0||lane.epoch==epoch)){
                        if(drain)lane.ending=true;else{lane.cancel();lanes.remove(type);}
                    }}
                }else throw new IOException("Unknown audio packet");
            }
        }catch(Exception e){if(running){state="等待音频通道";if(error.isEmpty())error=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();
            if(!established&&!legacy)legacy=true;else recoveries.incrementAndGet();
        }}finally{stopMic();socket=null;Wire.close(s);active.set(false);
            synchronized(lanes){for(AudioPlayoutQueue lane:lanes.values()){if(running)droppedPcm.addAndGet(lane.outstanding());lane.cancel();}lanes.clear();}
            if(player!=null){player.interrupt();try{player.join();}catch(InterruptedException ignored){}}
            if(feedback!=null){feedback.interrupt();try{feedback.join();}catch(InterruptedException ignored){}}
        }
        if(running)try{Thread.sleep(500);}catch(InterruptedException ignored){}
    }}
    private void feedback(Socket s,DataOutputStream out,Map<Integer,AudioPlayoutQueue> lanes,java.util.concurrent.atomic.AtomicBoolean active){
        Map<AudioPlayoutQueue,Long> reported=new IdentityHashMap<>();
        try{while(running&&active.get()){
            List<AudioPlayoutQueue> snapshot;synchronized(lanes){snapshot=new ArrayList<>(lanes.values());}
            reported.keySet().retainAll(snapshot);
            for(AudioPlayoutQueue lane:snapshot){long played=lane.playedFrames();if(lane.cancelled||played==reported.getOrDefault(lane,0L))continue;
                synchronized(out){out.writeByte(AudioWire.PROGRESS);out.writeInt(lane.type);out.writeInt(lane.epoch);out.writeLong(played);out.flush();}
                reported.put(lane,played);
            }
            Thread.sleep(50);
        }}catch(InterruptedException ignored){}catch(IOException e){if(active.get())Wire.close(s);}
    }
    private Track createTrack(AudioPlayoutQueue lane)throws IOException {
        int mask=lane.channels==1?AudioFormat.CHANNEL_OUT_MONO:AudioFormat.CHANNEL_OUT_STEREO;
        int min=AudioTrack.getMinBufferSize(lane.rate,mask,AudioFormat.ENCODING_PCM_16BIT);if(min<=0)throw new IOException("Unsupported PCM output");
        int usage=lane.purpose==AudioPolicy.PHONE?AudioAttributes.USAGE_VOICE_COMMUNICATION:lane.purpose==AudioPolicy.ASSISTANT?AudioAttributes.USAGE_ASSISTANT:lane.purpose==AudioPolicy.NAVIGATION?AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE:AudioAttributes.USAGE_MEDIA;
        AudioTrack.Builder builder=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(usage).setContentType(lane.purpose==AudioPolicy.MUSIC?AudioAttributes.CONTENT_TYPE_MUSIC:AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(new AudioFormat.Builder().setSampleRate(lane.rate).setChannelMask(mask).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(Math.max(min,AudioPolicy.frames(lane.rate,AudioPolicy.trackMs(lane.purpose,bufferMs))*lane.frameBytes));
        if(lane.purpose!=AudioPolicy.MUSIC)builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
        AudioTrack audio=builder.build();
        if(audio.getState()!=AudioTrack.STATE_INITIALIZED){audio.release();throw new IOException("AudioTrack initialization failed");}
        Track track=new Track(lane,audio,AudioPolicy.frames(lane.rate,AudioPolicy.startMs(lane.purpose,bufferMs)));
        if(android.os.Build.VERSION.SDK_INT>=31)audio.setStartThresholdInFrames(track.startFrames);
        android.util.Log.i("KrPlay-Audio","PCM type="+lane.type+" epoch="+lane.epoch+" purpose="+lane.purpose+" rate="+lane.rate+" channels="+lane.channels+" targetMs="+bufferMs+" actualStartMs="+(track.startFrames*1000L/lane.rate)+" trackCapacityMs="+(track.capacityFrames*1000L/lane.rate));
        return track;
    }
    private void playback(Socket s,DataOutputStream out,Map<Integer,AudioPlayoutQueue> lanes,java.util.concurrent.atomic.AtomicBoolean active,int version){
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
        Map<Integer,Track> tracks=new LinkedHashMap<>();long lastSummary=0;
        try{while(running&&active.get()){
            List<AudioPlayoutQueue> current;synchronized(lanes){current=new ArrayList<>(lanes.values());}
            Iterator<Track> old=tracks.values().iterator();while(old.hasNext()){Track t=old.next();if(t.lane.cancelled||!current.contains(t.lane)){t.audio.release();old.remove();}}
            long now=System.nanoTime();
            for(AudioPlayoutQueue lane:current){if(lane.cancelled)continue;Track t=tracks.get(lane.type);
                if(t==null){t=createTrack(lane);tracks.put(lane.type,t);}
                long before=t.progress.played(),played=t.progress.update(t.audio.getPlaybackHeadPosition());lane.played(played);renderedFrames.addAndGet(played-before);
                int under=t.audio.getUnderrunCount();int increase=Math.max(0,under-t.lastUnderruns);underruns.addAndGet(increase);t.lastUnderruns=under;
                if(t.playing&&increase>0&&t.progress.queued()==0&&lane.softwareFrames()==0&&!lane.ending){
                    t.audio.pause();t.playing=false;rebuffers.incrementAndGet();
                }
                AudioPlayoutQueue.Chunk chunk=lane.peek();
                if(chunk!=null&&!lane.cancelled){
                    int n=t.audio.write(chunk.data,chunk.offset,chunk.data.length-chunk.offset,AudioTrack.WRITE_NON_BLOCKING);
                    if(n<0)throw new IOException("AudioTrack write failed "+n);
                    if(n>0){if(!lane.cancelled)lane.consume(chunk,n);t.progress.written(n/lane.frameBytes);playedBytes.addAndGet(n);}
                }
                if(!t.playing&&AudioProgress.ready(t.progress.queued(),t.startFrames,lane.ending,now-lane.lastArrivalNs)){
                    t.audio.play();t.playing=true;
                }
                if(lane.ending&&lane.softwareFrames()==0&&t.progress.queued()==0){synchronized(lanes){if(lanes.get(lane.type)==lane){lane.cancel();lanes.remove(lane.type);}}}
            }
            if(now-lastSummary>=1_000_000_000L){StringBuilder report=new StringBuilder();for(Track t:tracks.values()){
                AudioPlayoutQueue l=t.lane;report.append("流 ").append(l.type).append(l.purpose==AudioPolicy.MUSIC?" 音乐":" 语音").append(t.playing?" 播放":" 预缓冲")
                    .append(" · PCM ").append(l.rate).append(" Hz / ").append(l.channels).append("ch / 16bit")
                    .append(" · 实际预缓冲 ").append(t.startFrames*1000L/l.rate).append("ms · 待播放 ").append(l.outstanding()*1000L/l.rate).append("ms\n");}
                playbackSummary=report.length()==0?"等待 PCM":report.toString().trim();lastSummary=now;
            }
            Thread.sleep(2);
        }}catch(InterruptedException ignored){}catch(Exception e){if(active.get()){error="播放："+e.getMessage();Wire.close(s);}}
        finally{for(Track t:tracks.values())t.audio.release();}
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
    String summary(){return state+" · 音频协议 "+negotiatedVersion+(negotiatedVersion==1?"（旧服务端，无法反馈播放进度）":"")+" · 音乐缓冲设置 "+bufferMs+"ms\n"
        +"写入 "+playedBytes.get()/1024+" KiB · 已播放 "+renderedFrames.get()+" 帧 · 麦克风 "+micBytes.get()/1024+" KiB"
        +" · 恢复放弃PCM帧 "+droppedPcm.get()+" · 欠载 "+underruns.get()+" · 重缓冲 "+rebuffers.get()+" · 音频连接 "+connections.get()+" · 音频恢复 "+recoveries.get()+"\n"+playbackSummary+(error.isEmpty()?"":"\n"+error);}
    public void close(){running=false;stopMic();Wire.close(socket);if(thread!=null)thread.interrupt();}
}
