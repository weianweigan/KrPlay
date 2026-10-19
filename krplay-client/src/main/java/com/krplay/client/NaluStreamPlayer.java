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
    private Thread reader,codecThread;private volatile MediaCodec decoder;private String mime="";private int width=1920,height=1080,epoch=-1,connection=-1;
    private byte[] csd;private boolean waitKey=true;
    private final MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
    private static final class Packet {final byte[] bytes=new byte[Wire.MAX_FRAME];int type,n,connection;long pts,received;}
    private final ArrayBlockingQueue<Packet> free=new ArrayBlockingQueue<>(6),queue=new ArrayBlockingQueue<>(4);
    private final Map<Long,Timing> pending=new ConcurrentHashMap<>();
    private static final class Timing {long ingress,send,receive,submit;}
    private HandlerThread renderThread;
    final AtomicLong receivedBytes=new AtomicLong(),receivedFrames=new AtomicLong(),renderedFrames=new AtomicLong(),reconnects=new AtomicLong(),overflows=new AtomicLong();
    volatile String codec="等待硬解码器";volatile double bridgeMs=Double.NaN,decodeMs=Double.NaN,transferMs=Double.NaN,serverQueueMs=Double.NaN;
    volatile double decodeReadyMs=Double.NaN,bridgeReadyMs=Double.NaN;volatile int queueDepth;volatile double renderTimestampDeltaMs=Double.NaN;private long lastPts;
    NaluStreamPlayer(String h,Runnable r,Consumer<String> s,Size z,PerformanceStats c){host=h;idr=r;status=s;size=z;clock=c;for(int i=0;i<6;i++)free.offer(new Packet());}
    void surface(Surface s){if(surface==s)return;surface=s;surfaceEpoch++;Wire.close(socket);}
    void start(){if(running)return;running=true;renderThread=new HandlerThread("KrPlay-Render-Timestamps");renderThread.start();codecThread=new Thread(this::decodeLoop,"KrPlay-Codec");codecThread.start();reader=new Thread(this::loop,"KrPlay-Video-Read");reader.start();}
    private void loop(){android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);while(running){Socket s=new Socket();try{
        LocalTransport.bind(s);Wire.socket(s);s.connect(new InetSocketAddress(host,8081),1500);socket=s;s.setSoTimeout(1000);int generation=++connectionEpoch;idr.run();
        DataInputStream in=new DataInputStream(new BufferedInputStream(s.getInputStream(),65536));
        while(running){int type;try{type=in.readUnsignedByte();}catch(SocketTimeoutException idle){continue;}s.setSoTimeout(3000);long pts=in.readLong();int n=in.readInt();Wire.validateHeader(type,pts,n);
            Packet p=free.poll();if(p==null)throw new IOException("Decode pool exhausted");
            boolean handed=false;try{p.type=type;p.pts=pts;p.n=n;p.connection=generation;in.readFully(p.bytes,0,n);s.setSoTimeout(1000);p.received=System.nanoTime();receivedBytes.addAndGet(n+13L);
                if(type==0||type==1||type==20||type==21)receivedFrames.incrementAndGet();
                if(!queue.offer(p)){overflows.incrementAndGet();throw new IOException("Decoder backlog; requesting fresh keyframe");}handed=true;queueDepth=queue.size();
            }finally{if(!handed)free.offer(p);}
        }
    }catch(Exception e){if(running){reconnects.incrementAndGet();android.util.Log.w("KrPlay-Video","Network recovery: "+e.getClass().getSimpleName()+" "+e.getMessage());status.accept("等待设备视频 · "+e.getClass().getSimpleName());}}finally{socket=null;Wire.close(s);}if(running)try{Thread.sleep(250);}catch(InterruptedException ignored){}}}
    private void decodeLoop(){android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
        while(running){Packet p=null;try{drain();p=queue.poll(4,TimeUnit.MILLISECONDS);if(p==null)continue;queueDepth=queue.size();
            if(connection!=p.connection){reset();csd=null;mime="";connection=p.connection;}
            if(p.type==4){int w=((p.bytes[0]&255)<<8)|(p.bytes[1]&255),h=((p.bytes[2]&255)<<8)|(p.bytes[3]&255);if(w<320||h<240||w>4096||h>4096)throw new IOException("Invalid resolution");if(w!=width||h!=height){width=w;height=h;reset();csd=null;}size.changed(w,h);}
            else if(p.type!=16&&p.n>0){boolean timed=p.type==20||p.type==21;String m=(p.type==1||p.type==3||p.type==21)?"video/hevc":"video/avc";boolean config=p.type==2||p.type==3;
                if(!m.equals(mime)){reset();csd=null;mime=m;}
                if(config){byte[] c=Arrays.copyOf(p.bytes,p.n);if(!Arrays.equals(c,csd)){reset();csd=c;}continue;}
                if(surface==null||!surface.isValid()||csd==null)continue;
                int off=timed?16:0,n=p.n-off;boolean key=timed?randomAccess(m.equals("video/hevc"),p.bytes,off,n):Wire.randomAccess(m.equals("video/hevc"),p.bytes,n);
                if(decoder==null||epoch!=surfaceEpoch){reset();create(m);idr.run();}
                if(waitKey&&!key)continue;
                int idx=decoder.dequeueInputBuffer(0);long deadline=System.nanoTime()+20_000_000L;while(idx<0&&System.nanoTime()<deadline){drain();idx=decoder.dequeueInputBuffer(1000);}
                if(idx<0){reset();idr.run();overflows.incrementAndGet();continue;}
                ByteBuffer b=decoder.getInputBuffer(idx);if(b==null||b.capacity()<n)throw new IOException("Decoder input too small");b.clear();b.put(p.bytes,off,n);
                long now=System.nanoTime(),pts=Math.max(now/1000,lastPts+1);lastPts=pts;
                if(pending.size()>64)pending.clear();Timing t=new Timing();t.submit=now;t.receive=p.received;if(timed){ByteBuffer v=ByteBuffer.wrap(p.bytes,0,16);t.ingress=v.getLong();t.send=v.getLong();}pending.put(pts,t);
                decoder.queueInputBuffer(idx,0,n,pts,0);waitKey=false;drain();
            }
        }catch(InterruptedException e){break;}catch(Exception e){android.util.Log.w("KrPlay-Video","Codec recovery: "+e.getClass().getSimpleName()+" "+e.getMessage());reset();Wire.close(socket);status.accept("视频恢复 · "+e.getClass().getSimpleName());}finally{if(p!=null)free.offer(p);}}
        reset();Packet p;while((p=queue.poll())!=null)free.offer(p);
    }
    private boolean randomAccess(boolean hevc,byte[] data,int offset,int n){for(int i=offset;i+3<offset+n;i++)if(data[i]==0&&data[i+1]==0){int k=data[i+2]==1?3:data[i+2]==0&&data[i+3]==1?4:0;if(k>0&&i+k<offset+n&&Wire.key(hevc,hevc?(data[i+k]>>1)&63:data[i+k]&31))return true;}return false;}
    private void create(String m)throws IOException {
        MediaFormat f=MediaFormat.createVideoFormat(m,width,height);f.setInteger(MediaFormat.KEY_PRIORITY,0);f.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,Wire.MAX_FRAME);
        if(Build.VERSION.SDK_INT>=30)f.setInteger(MediaFormat.KEY_LOW_LATENCY,1);f.setInteger("vendor.qti-ext-dec-low-latency.enable",1);
        List<byte[]> sets=Wire.nals(csd,0,csd.length);if(m.equals("video/avc")){for(byte[] set:sets){int t=Wire.nalType(false,set);if(t==7||t==8)f.setByteBuffer(t==7?"csd-0":"csd-1",ByteBuffer.wrap(Wire.annex(Collections.singletonList(set))));}}else f.setByteBuffer("csd-0",ByteBuffer.wrap(csd));
        String name=null;for(MediaCodecInfo c:new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()){if(c.isEncoder()||!c.isHardwareAccelerated())continue;for(String type:c.getSupportedTypes())if(type.equals(m)){name=c.getName();break;}if(name!=null)break;}if(name==null)throw new IOException("No hardware decoder for "+m);
        MediaCodec c=MediaCodec.createByCodecName(name);try{c.configure(f,surface,null,0);c.setOnFrameRenderedListener((owner,pts,nano)->{if(owner!=decoder)return;renderedFrames.incrementAndGet();Timing t=pending.remove(pts);if(t!=null){renderTimestampDeltaMs=(nano-t.submit)/1e6;long callback=System.nanoTime();long rendered=VideoPolicy.renderTime(t.submit,nano,callback);decodeMs=(rendered-t.submit)/1e6;bridgeMs=clock.bridgeMs(t.ingress,rendered);clock.latency(bridgeMs);if(t.send>0&&clock.synced()){transferMs=Math.max(0,(t.receive-(t.send-clock.offsetNs()))/1e6);serverQueueMs=t.ingress>0?Math.max(0,(t.send-t.ingress)/1e6):Double.NaN;}}},new Handler(renderThread.getLooper()));
            c.start();decoder=c;epoch=surfaceEpoch;codec=name;status.accept("已连接 · "+width+" × "+height+" · "+name);
        }catch(RuntimeException e){c.release();throw e;}
    }
    private void drain(){if(decoder==null)return;int i;while((i=decoder.dequeueOutputBuffer(info,0))>=0){Timing t=pending.get(info.presentationTimeUs);if(t!=null){long now=System.nanoTime();decodeReadyMs=(now-t.submit)/1e6;bridgeReadyMs=clock.bridgeMs(t.ingress,now);}decoder.releaseOutputBuffer(i,true);}}
    private void reset(){waitKey=true;pending.clear();if(decoder!=null){MediaCodec d=decoder;decoder=null;try{d.stop();}catch(RuntimeException ignored){}try{d.release();}catch(RuntimeException ignored){}}}
    public void close(){running=false;Wire.close(socket);if(reader!=null)reader.interrupt();if(codecThread!=null)codecThread.interrupt();if(renderThread!=null)renderThread.quitSafely();}
}
