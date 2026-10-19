package com.krplay.client;
import com.krplay.protocol.Wire;
import android.view.MotionEvent;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

final class TouchControlClient implements AutoCloseable {
    private final String host; private final Consumer<String> status; private final Consumer<JSONObject> remote;
    private final ArrayBlockingQueue<byte[]> queue=new ArrayBlockingQueue<>(128);
    private volatile boolean running; private volatile Socket socket; private Thread thread;
    private volatile int mode,width=1920,height=1080;
    TouchControlClient(String h,Consumer<String> s,Consumer<JSONObject> r){host=h;status=s;remote=r;}
    void start(){if(running)return;running=true;thread=new Thread(this::loop,"KrPlay-Control");thread.start();}
    private interface Packet {void write(DataOutputStream out) throws IOException;}
    private byte[] packet(Packet f){try{ByteArrayOutputStream b=new ByteArrayOutputStream();f.write(new DataOutputStream(b));return b.toByteArray();}catch(IOException e){throw new IllegalStateException(e);}}
    private void send(byte[] b){if(!running || socket==null)return;if(!queue.offer(b)){Wire.close(socket);queue.clear();}}
    void mode(int m){mode=m;queue.clear();send(new byte[]{1,(byte)m});idr();}
    void probe(){send(packet(o->{o.writeByte(9);o.writeLong(System.nanoTime());}));}
    void query(){send(new byte[]{6});}
    void test(boolean speed){send(new byte[]{7,(byte)(speed?1:0)});}
    void vpn(boolean enabled){send(new byte[]{8,(byte)(enabled?1:0)});}
    void idr(){send(new byte[]{4,0});}
    void size(int w,int h){width=w;height=h;send(surface());}
    private byte[] surface(){return packet(o->{o.writeByte(5);o.writeShort(width);o.writeShort(height);o.writeByte(60);});}
    void key(int code){send(packet(o->{o.writeByte(3);o.writeByte(0);o.writeInt(code);o.writeByte(3);o.writeByte(1);o.writeInt(code);}));}
    boolean touch(MotionEvent e,int w,int h){
        if(w<=0 || h<=0 || e.getPointerCount()>10)return false;
        int masked=e.getActionMasked(),index=e.getActionIndex(),n=e.getPointerCount();
        // v1 has no actionIndex. Put the changed pointer first, preserving its ID.
        int a=(masked==0 || masked==5)?0:(masked==1 || masked==6)?1:masked==2?2:3;
        int[] ids=new int[n];float[] xs=new float[n],ys=new float[n];
        for(int j=0;j<n;j++){int k=j==0?index:j<=index?j-1:j;ids[j]=e.getPointerId(k);xs[j]=Math.max(0,Math.min(1,e.getX(k)/w));ys[j]=Math.max(0,Math.min(1,e.getY(k)/h));}
        Wire.Touch t=new Wire.Touch(a,ids,xs,ys);send(packet(t::write));return true;
    }
    private void loop(){while(running){Socket s=new Socket();try{
        LocalTransport.bind(s);Wire.socket(s);s.connect(new InetSocketAddress(host,8082),1500);socket=s;queue.clear();s.setSoTimeout(15000);new Thread(()->readStatus(s),"KrPlay-Status").start();
        DataOutputStream out=new DataOutputStream(s.getOutputStream());out.write(new byte[]{1,(byte)mode});out.write(surface());out.write(new byte[]{4,0,6});out.flush();status.accept("控制已连接");
        while(running){byte[] b=queue.poll(1,TimeUnit.SECONDS);if(b!=null){out.write(b);out.flush();}else{out.writeByte(9);out.writeLong(System.nanoTime());out.flush();}}
    }catch(Exception e){if(running)status.accept("等待设备控制连接");}finally{socket=null;Wire.close(s);queue.clear();}if(running)try{Thread.sleep(500);}catch(InterruptedException ignored){}}}
    private void readStatus(Socket s){try{DataInputStream input=new DataInputStream(s.getInputStream());while(running&&socket==s){if(input.readUnsignedByte()!=128)throw new IOException("Unknown status packet");int n=input.readUnsignedShort();if(n<2||n>16384)throw new IOException("Invalid status length");byte[] data=new byte[n];input.readFully(data);JSONObject json=new JSONObject(new String(data,StandardCharsets.UTF_8));json.put("receivedNs",System.nanoTime());if(socket==s)remote.accept(json);}}catch(Exception ignored){}finally{Wire.close(s);}}
    public void close(){running=false;Wire.close(socket);if(thread!=null)thread.interrupt();queue.clear();}
}
