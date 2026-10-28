package com.krplay.client;
import com.krplay.protocol.*;
import android.media.*;
import android.os.*;
import android.view.Surface;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Network reads and codec draining are independent; a silent socket cannot hold a rendered frame. */
final class NaluStreamPlayer implements AutoCloseable {
    interface Size {void changed(int w,int h);}
    private final String host;private final Runnable idr;private final Consumer<String> status;private final Size size;
    final PerformanceStats clock;
    private volatile boolean running;private volatile Socket socket;private volatile Surface surface;private volatile int surfaceEpoch,connectionEpoch;
    private volatile SoftwareVideoDecoder software;private boolean softwareFailed;
    private Thread reader,codecThread;private volatile MediaCodec decoder;private String mime="";private int width=1920,height=1080,epoch=-1,connection=-1;
    private byte[] csd;private volatile boolean waitKey=true,networkWaitingForKey;private int recovery=-1;private long codecRetryAfterNs;
    private final VideoPolicy.KeyframeRequests keyframeRequests=new VideoPolicy.KeyframeRequests();
    private final MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
    private static final class Packet {final byte[] bytes=new byte[Wire.MAX_FRAME];int type,n,connection,recovery;long pts,received;}
    private final ArrayBlockingQueue<Packet> free=new ArrayBlockingQueue<>(14),queue=new ArrayBlockingQueue<>(12);
    private final Map<Long,Timing> pending=new ConcurrentHashMap<>();
    private static final class Timing {long ingress,send,receive,submit;}
    private HandlerThread renderThread;
    final AtomicLong receivedBytes=new AtomicLong(),receivedFrames=new AtomicLong(),renderedFrames=new AtomicLong(),reconnects=new AtomicLong(),overflows=new AtomicLong();
    final AtomicLong keyframeRequestCount=new AtomicLong(),codecStalls=new AtomicLong();
    private volatile long lastVideoReceiveNs,lastRenderNs,connectedAtNs;
    volatile String lastRecoveryReason="无";
    volatile String decoderTuning="等待配置";
    volatile String codec="等待硬解码器";volatile double bridgeMs=Double.NaN,decodeMs=Double.NaN,transferMs=Double.NaN,serverQueueMs=Double.NaN;
    volatile double decodeReadyMs=Double.NaN,bridgeReadyMs=Double.NaN;volatile int queueDepth;volatile double renderTimestampDeltaMs=Double.NaN;private long lastPts,lastCodecProgressNs;
    final AtomicLong catchupFrames=new AtomicLong(),decoderResets=new AtomicLong(),inputWaits=new AtomicLong(),nonReferenceDrops=new AtomicLong();
    NaluStreamPlayer(String h,Runnable r,Consumer<String> s,Size z,PerformanceStats c){host=h;idr=r;status=s;size=z;clock=c;for(int i=0;i<14;i++)free.offer(new Packet());}
    void surface(Surface s){if(surface==s)return;surface=s;surfaceEpoch++;Wire.close(socket);}
    void start(){if(running)return;running=true;renderThread=new HandlerThread("KrPlay-Render-Timestamps");renderThread.start();codecThread=new Thread(this::decodeLoop,"KrPlay-Codec");codecThread.start();reader=new Thread(this::loop,"KrPlay-Video-Read");reader.start();}
    private void loop(){android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);while(running){Socket s=new Socket();try{
        LocalTransport.bind(s);Wire.videoSocket(s);s.connect(new InetSocketAddress(host,8081),1500);com.krplay.transport.VideoSocketOptions.configure(s);connectedAtNs=System.nanoTime();socket=s;s.setSoTimeout(1000);int generation=++connectionEpoch;requestKeyFrame();
        int recoveryGeneration=0;networkWaitingForKey=true;
        DataInputStream in=new DataInputStream(new BufferedInputStream(s.getInputStream(),65536));
        while(running){int type;try{type=in.readUnsignedByte();}catch(SocketTimeoutException idle){if(networkWaitingForKey)requestKeyFrame();continue;}s.setSoTimeout(3000);long pts=in.readLong();int n=in.readInt();Wire.validateHeader(type,pts,n);
            Packet p=free.take();
            boolean handed=false;try{p.type=type;p.pts=pts;p.n=n;p.connection=generation;p.recovery=recoveryGeneration;in.readFully(p.bytes,0,n);s.setSoTimeout(1000);p.received=System.nanoTime();receivedBytes.addAndGet(n+13L);
                boolean video=VideoPolicy.videoPacket(type);
                boolean key=false;if(video){lastVideoReceiveNs=p.received;receivedFrames.incrementAndGet();int offset=type==20||type==21?16:0;key=randomAccess(type==1||type==21,p.bytes,offset,n-offset);}
                if(video&&networkWaitingForKey&&!key){requestKeyFrame();continue;}
                // Metadata must survive congestion. Dropping a frame invalidates subsequent
                // references, but does not invalidate the TCP connection or cached codec data.
                if(video){
                    if(!queue.offer(p)){
                        int offset=type==20?16:0;
                        if((type==0||type==20)&&Wire.discardableAvc(p.bytes,offset,n-offset)){nonReferenceDrops.incrementAndGet();continue;}
                        overflows.incrementAndGet();recoveryGeneration++;networkWaitingForKey=true;
                        // Only the reader produces packets. Preserve configuration/resolution,
                        // evict queued reference frames together, then restart at a new IDR.
                        for(Packet old:queue.toArray(new Packet[0]))if(VideoPolicy.videoPacket(old.type)&&queue.remove(old))free.offer(old);
                        p.recovery=recoveryGeneration;lastRecoveryReason="客户端积压已清空，等待关键帧";
                        requestKeyFrame();if(!key)continue;
                        if(!queue.offer(p))continue;
                    }
                    if(key)networkWaitingForKey=false;
                }else queue.put(p);
                handed=true;queueDepth=queue.size();
            }finally{if(!handed)free.offer(p);}
        }
    }catch(Exception e){if(running){reconnects.incrementAndGet();lastRecoveryReason=failure(e);android.util.Log.w("KrPlay-Video","Network recovery: "+e.getClass().getSimpleName()+" "+e.getMessage());status.accept("等待设备视频 · "+failure(e));}}finally{socket=null;Wire.close(s);}if(running)try{Thread.sleep(250);}catch(InterruptedException ignored){}}}
    private void decodeLoop(){android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
        while(running){Packet p=null;try{drain();p=queue.poll(4,TimeUnit.MILLISECONDS);if(p==null){if(waitKey&&csd!=null&&surface!=null&&surface.isValid())requestKeyFrame();continue;}queueDepth=queue.size();
            if(connection!=p.connection){reset();csd=null;mime="";connection=p.connection;recovery=p.recovery;codecRetryAfterNs=0;}
            if(recovery!=p.recovery){waitKey=true;recovery=p.recovery;}
            if(p.type==4){int w=((p.bytes[0]&255)<<8)|(p.bytes[1]&255),h=((p.bytes[2]&255)<<8)|(p.bytes[3]&255);if(w<320||h<240||w>4096||h>4096)throw new IOException("Invalid resolution");if(w!=width||h!=height){width=w;height=h;reset();csd=null;}size.changed(w,h);}
            else if(p.type!=16&&p.n>0){boolean timed=p.type==20||p.type==21;String m=(p.type==1||p.type==3||p.type==21)?"video/hevc":"video/avc";boolean config=p.type==2||p.type==3;
                if(!m.equals(mime)){reset();csd=null;mime=m;}
                if(config){byte[] c=Arrays.copyOf(p.bytes,p.n);if(!Arrays.equals(c,csd)){reset();csd=c;}continue;}
                if(surface==null||!surface.isValid()||csd==null)continue;
                int off=timed?16:0,n=p.n-off;boolean key=timed?randomAccess(m.equals("video/hevc"),p.bytes,off,n):Wire.randomAccess(m.equals("video/hevc"),p.bytes,n);
                if(epoch!=surfaceEpoch)reset();
                if(waitKey&&!key){requestKeyFrame();continue;}
                if(decoder==null&&software==null){if(System.nanoTime()<codecRetryAfterNs){requestKeyFrame();continue;}create(m);}
                if(software!=null){
                    long now=System.nanoTime(),pts=Math.max(now/1000,lastPts+1);lastPts=pts;
                    Timing t=new Timing();t.submit=now;t.receive=p.received;
                    if(timed){ByteBuffer v=ByteBuffer.wrap(p.bytes,0,16);t.ingress=v.getLong();t.send=v.getLong();}
                    if(pending.size()>64)pending.clear();pending.put(pts,t);
                    long ready=software.submit(p.bytes,off,n,pts);long done=System.nanoTime();
                    Timing output=ready==pts?t:pending.get(ready);if(output!=null){decodeReadyMs=(done-output.submit)/1e6;bridgeReadyMs=clock.bridgeMs(output.ingress,done);}
                    waitKey=false;continue;
                }
                int idx=decoder.dequeueInputBuffer(0),attempts=0;
                long started=System.nanoTime();lastCodecProgressNs=started;boolean warned=false;
                while(idx<0 && running){
                    drain();idx=decoder.dequeueInputBuffer(1000);attempts++;
                    long now=System.nanoTime();
                    if(!warned && now-started>=VideoPolicy.INPUT_WARN_NS){inputWaits.incrementAndGet();warned=true;}
                    if(idx<0 && VideoPolicy.inputStalled(now,lastCodecProgressNs,attempts))break;
                    if(epoch!=surfaceEpoch || p.connection!=connectionEpoch)break;
                }
                if(!running)break;
                if(epoch!=surfaceEpoch || p.connection!=connectionEpoch){reset();continue;}
                if(idx<0){reset();lastRecoveryReason="解码器持续无进展超过 750ms";codecStalls.incrementAndGet();requestKeyFrame();continue;}
                ByteBuffer b=decoder.getInputBuffer(idx);if(b==null||b.capacity()<n)throw new IOException("Decoder input too small");b.clear();b.put(p.bytes,off,n);
                long now=System.nanoTime(),pts=Math.max(now/1000,lastPts+1);lastPts=pts;
                if(pending.size()>64)pending.clear();Timing t=new Timing();t.submit=now;t.receive=p.received;if(timed){ByteBuffer v=ByteBuffer.wrap(p.bytes,0,16);t.ingress=v.getLong();t.send=v.getLong();}pending.put(pts,t);
                decoder.queueInputBuffer(idx,0,n,pts,0);lastCodecProgressNs=now;waitKey=false;drain();
            }
        }catch(InterruptedException e){break;}catch(Exception e){if(software!=null)softwareFailed=true;lastRecoveryReason=failure(e);android.util.Log.w("KrPlay-Video","Codec recovery: "+e.getClass().getSimpleName()+" "+e.getMessage());reset();codecRetryAfterNs=System.nanoTime()+500_000_000L;requestKeyFrame();status.accept("视频恢复 · "+failure(e));}finally{if(p!=null)free.offer(p);}}
        reset();Packet p;while((p=queue.poll())!=null)free.offer(p);
    }
    private boolean randomAccess(boolean hevc,byte[] data,int offset,int n){for(int i=offset;i+3<offset+n;i++)if(data[i]==0&&data[i+1]==0){int k=data[i+2]==1?3:data[i+2]==0&&data[i+3]==1?4:0;if(k>0&&i+k<offset+n&&Wire.key(hevc,hevc?(data[i+k]>>1)&63:data[i+k]&31))return true;}return false;}
    private void requestKeyFrame(){if(keyframeRequests.acquire(System.nanoTime(),clock.rttMs()>100)){keyframeRequestCount.incrementAndGet();idr.run();}}
    boolean waitingForKeyFrame(){return waitKey||networkWaitingForKey;}
    long videoIdleMs(){return lastVideoReceiveNs==0?-1:(System.nanoTime()-lastVideoReceiveNs)/1_000_000L;}
    long renderIdleMs(){return lastRenderNs==0?-1:(System.nanoTime()-lastRenderNs)/1_000_000L;}
    void serverHasVideoClient(boolean connected){
        Socket current=socket;
        // A rebuilt fixed-entry VPN can lose the old TCP close/reset. Silence alone is
        // normal for static CarPlay screens; reconnect only on explicit server evidence.
        if(!connected&&current!=null&&!current.isClosed()&&System.nanoTime()-connectedAtNs>2_000_000_000L){
            android.util.Log.w("KrPlay-Video","Server no longer owns video connection; rebuilding stale TCP socket");
            Wire.close(current);
        }
    }
    private static String failure(Exception e){String message=e.getMessage();return e.getClass().getSimpleName()+(message==null?"":" · "+message);}
    private void create(String m)throws IOException {
        if(!softwareFailed&&SoftwareVideoDecoder.AVAILABLE){
            try {
                software=new SoftwareVideoDecoder(m.equals("video/hevc"),surface,csd,(owner,pts)->{
                    if(owner!=software)return;
                    long now=System.nanoTime();lastRenderNs=now;renderedFrames.incrementAndGet();
                    Timing t=pending.remove(pts);
                    for(Long old:pending.keySet())if(old<pts&&pending.remove(old)!=null)catchupFrames.incrementAndGet();
                    if(t!=null){decodeMs=(now-t.submit)/1e6;bridgeMs=clock.bridgeMs(t.ingress,now);clock.latency(bridgeMs);
                        if(t.send>0&&clock.synced()){transferMs=Math.max(0,(t.receive-(t.send-clock.offsetNs()))/1e6);serverQueueMs=t.ingress>0?Math.max(0,(t.send-t.ingress)/1e6):Double.NaN;}}
                });
                epoch=surfaceEpoch;codec="FFmpeg "+(m.equals("video/hevc")?"HEVC":"H.264");
                decoderTuning="ARM64 NEON / slice 4线程 / LOW_DELAY / 最新帧异步提交";
                status.accept("已连接 · "+width+" × "+height+" · "+codec);return;
            }catch(RuntimeException failure){softwareFailed=true;android.util.Log.w("KrPlay-Video","Software init failed; falling back to MediaCodec",failure);}
        }
        try{create(m,true);decoderTuning=(softwareFailed?"软解失败回退 / ":"")+"低延迟 / operating-rate 120 已请求";}
        catch(RuntimeException unsupported){
            android.util.Log.w("KrPlay-Video","Optional decoder tuning rejected; retrying baseline",unsupported);
            create(m,false);decoderTuning="可选参数被拒绝，使用兼容配置";
        }
    }
    private void create(String m,boolean tune)throws IOException {
        MediaFormat f=MediaFormat.createVideoFormat(m,width,height);f.setInteger(MediaFormat.KEY_PRIORITY,0);if(tune)f.setInteger(MediaFormat.KEY_OPERATING_RATE,120);f.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,Wire.MAX_FRAME);
        if(tune&&Build.VERSION.SDK_INT>=30)f.setInteger(MediaFormat.KEY_LOW_LATENCY,1);
        List<byte[]> sets=Wire.nals(csd,0,csd.length);if(m.equals("video/avc")){for(byte[] set:sets){int t=Wire.nalType(false,set);if(t==7||t==8)f.setByteBuffer(t==7?"csd-0":"csd-1",ByteBuffer.wrap(Wire.annex(Collections.singletonList(set))));}}else f.setByteBuffer("csd-0",ByteBuffer.wrap(csd));
        String name=null;for(MediaCodecInfo c:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()){if(c.isEncoder()||!c.isHardwareAccelerated())continue;for(String type:c.getSupportedTypes())if(type.equals(m)){name=c.getName();break;}if(name!=null)break;}if(name==null)throw new IOException("No hardware decoder for "+m);
        MediaCodec c=MediaCodec.createByCodecName(name);
        try{if(tune&&(name.startsWith("c2.qti.")||name.startsWith("OMX.qcom."))){
            f.setInteger("vendor.qti-ext-dec-low-latency.enable",1);
            if(Build.VERSION.SDK_INT>=31 && c.getSupportedVendorParameters().contains("vendor.qti-ext-dec-picture-order.enable"))
                f.setInteger("vendor.qti-ext-dec-picture-order.enable",1);
        }
        c.configure(f,surface,null,0);c.setOnFrameRenderedListener((owner,pts,nano)->{if(owner!=decoder)return;lastRenderNs=System.nanoTime();renderedFrames.incrementAndGet();Timing t=pending.remove(pts);if(t!=null){renderTimestampDeltaMs=(nano-t.submit)/1e6;long callback=System.nanoTime();long rendered=VideoPolicy.renderTime(t.submit,nano,callback);decodeMs=(rendered-t.submit)/1e6;bridgeMs=clock.bridgeMs(t.ingress,rendered);clock.latency(bridgeMs);if(t.send>0&&clock.synced()){transferMs=Math.max(0,(t.receive-(t.send-clock.offsetNs()))/1e6);serverQueueMs=t.ingress>0?Math.max(0,(t.send-t.ingress)/1e6):Double.NaN;}}},new Handler(renderThread.getLooper()));
            c.start();decoder=c;epoch=surfaceEpoch;codec=name;status.accept("已连接 · "+width+" × "+height+" · "+name);
        }catch(RuntimeException e){c.release();throw e;}
    }
    private boolean queuedVideo(){for(Packet p:queue)if(VideoPolicy.videoPacket(p.type))return true;return false;}
    private void releaseOutput(int index,long pts,boolean render){
        if(!render){pending.remove(pts);catchupFrames.incrementAndGet();}
        decoder.releaseOutputBuffer(index,render);
    }
    private void drain(){
        if(decoder==null)return;
        int latest=-1;long latestPts=0;
        // Feed every reference frame, but release obsolete decoded outputs without
        // presenting them. Always present the last output once backlog is consumed.
        for(int count=0;count<64;count++){
            int i=decoder.dequeueOutputBuffer(info,0);
            if(i==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED||i==MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED)continue;
            if(i<0)break;
            lastCodecProgressNs=System.nanoTime();
            if(latest>=0)releaseOutput(latest,latestPts,false);
            latest=i;latestPts=info.presentationTimeUs;
            Timing t=pending.get(latestPts);if(t!=null){decodeReadyMs=(lastCodecProgressNs-t.submit)/1e6;bridgeReadyMs=clock.bridgeMs(t.ingress,lastCodecProgressNs);}
        }
        if(latest>=0)releaseOutput(latest,latestPts,!queuedVideo());
    }
    private void reset(){if(software!=null){SoftwareVideoDecoder old=software;software=null;old.close();decoderResets.incrementAndGet();}waitKey=true;pending.clear();if(decoder!=null){MediaCodec d=decoder;decoder=null;decoderResets.incrementAndGet();try{d.stop();}catch(RuntimeException ignored){}try{d.release();}catch(RuntimeException ignored){}}}
    public void close(){running=false;Wire.close(socket);if(reader!=null)reader.interrupt();if(codecThread!=null)codecThread.interrupt();if(renderThread!=null)renderThread.quitSafely();}
}
