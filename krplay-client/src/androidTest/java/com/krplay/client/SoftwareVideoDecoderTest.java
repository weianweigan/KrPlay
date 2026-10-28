package com.krplay.client;

import android.test.InstrumentationTestCase;
import android.media.*;
import android.graphics.PixelFormat;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/** Device-only test: encode a real AU, decode through JNI, and read the posted RGBA buffer. */
public class SoftwareVideoDecoderTest extends InstrumentationTestCase {
    public void testAvcNativeDecodeAndSurface() throws Exception { roundTrip("video/avc"); }
    public void testHevcNativeDecodeAndSurface() throws Exception { roundTrip("video/hevc"); }
    private void roundTrip(String mime) throws Exception {
        assertTrue("ARM64 FFmpeg libraries must load",SoftwareVideoDecoder.AVAILABLE);
        final int w=320,h=240;
        MediaCodec encoder=MediaCodec.createEncoderByType(mime);
        ImageReader imageReader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2);
        SoftwareVideoDecoder decoder=null;
        try {
            MediaFormat format=MediaFormat.createVideoFormat(mime,w,h);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            format.setInteger(MediaFormat.KEY_BIT_RATE,500000);format.setInteger(MediaFormat.KEY_FRAME_RATE,30);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
            encoder.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);encoder.start();
            int input=encoder.dequeueInputBuffer(1_000_000);assertTrue(input>=0);
            Image yuv=encoder.getInputImage(input);assertNotNull(yuv);
            Image.Plane[] planes=yuv.getPlanes();
            for(int k=0;k<3;k++) {
                ByteBuffer b=planes[k].getBuffer();int rows=k==0?h:h/2,cols=k==0?w:w/2;
                for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)
                    b.put(y*planes[k].getRowStride()+x*planes[k].getPixelStride(),(byte)(k==0?100:128));
            }
            encoder.queueInputBuffer(input,0,w*h*3/2,1000,0);
            MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();byte[] config=null,frame=null;
            long end=System.nanoTime()+5_000_000_000L;
            while(frame==null&&System.nanoTime()<end) {
                int output=encoder.dequeueOutputBuffer(info,10000);
                if(output==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat f=encoder.getOutputFormat();java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
                    for(String key:new String[]{"csd-0","csd-1"})if(f.containsKey(key)){
                        ByteBuffer b=f.getByteBuffer(key);byte[] data=new byte[b.remaining()];b.get(data);bytes.write(data);
                    }config=bytes.toByteArray();
                } else if(output>=0) {
                    ByteBuffer b=encoder.getOutputBuffer(output);b.position(info.offset);b.limit(info.offset+info.size);
                    byte[] data=new byte[info.size];b.get(data);
                    if((info.flags&MediaCodec.BUFFER_FLAG_CODEC_CONFIG)!=0)config=data;else if(info.size>0)frame=data;
                    encoder.releaseOutputBuffer(output,false);
                }
            }
            assertNotNull("Encoder CSD",config);assertNotNull("Encoder keyframe",frame);
            AtomicLong posted=new AtomicLong();decoder=new SoftwareVideoDecoder(mime.equals("video/hevc"),imageReader.getSurface(),config,(owner,pts)->posted.set(pts));
            assertEquals(123L,decoder.submit(frame,0,frame.length,123));
            end=System.nanoTime()+3_000_000_000L;
            while(posted.get()==0&&System.nanoTime()<end)Thread.sleep(5);
            assertEquals(123L,posted.get());
            try(Image image=imageReader.acquireLatestImage()) {
                assertNotNull("ANativeWindow must post a buffer",image);assertEquals(w,image.getWidth());assertEquals(h,image.getHeight());
                int gray=image.getPlanes()[0].getBuffer().get(0)&255;
                assertTrue("Decoded luma must reach RGBA output: "+gray,gray>70&&gray<130);
            }
            // Stop consuming Surface buffers while decode keeps replacing the latest frame.
            for(int i=0;i<20;i++)decoder.submit(frame,0,frame.length,200+i);
            end=System.nanoTime()+3_000_000_000L;
            while(posted.get()!=219&&System.nanoTime()<end){
                try(Image ignored=imageReader.acquireLatestImage()) { }
                Thread.sleep(5);
            }
            assertEquals("Rendering must catch up to the latest decoded frame",219L,posted.get());
        } finally {if(decoder!=null)decoder.close();imageReader.close();encoder.release();}
    }
}
