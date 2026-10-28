package com.shilapi.xcertplay
import com.krplay.protocol.AudioWire
import com.zeekr.bridge.ClientAudioRoute
import com.shilapi.xcertplay.airplay.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.*
import java.io.*
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[29])
class ClientAudioRouteTest {
    @Test fun defaultsAreClientAndExplicitLocalOrDisabledChoicesPersist(){
        val app=RuntimeEnvironment.getApplication();assertEquals(CarPlayAudioOutput.CLIENT,AirPlayPersistence.loadAudioOutput(app));assertEquals(CarPlayMicrophoneSource.CLIENT,AirPlayPersistence.loadMicrophoneSource(app))
        AirPlayPersistence.saveAudioOutput(app,CarPlayAudioOutput.ANDROID);AirPlayPersistence.saveMicrophoneSource(app,CarPlayMicrophoneSource.DISABLED)
        assertEquals(CarPlayAudioOutput.ANDROID,AirPlayPersistence.loadAudioOutput(app))
        val route=ClientAudioRoute(app,0);try{assertFalse(route.microphoneEnabled);assertNull(route.microphone(1,config()))}finally{route.close()}
    }
    private fun config()=MicrophoneConfig("speechrecognition",24000,1,96,20,InetAddress.getLoopbackAddress(),54321,ByteArray(32))
    @Test fun actualSocketTransfersNegotiatedPcmAndMicrophoneWithoutLocalFallback(){
        val app=RuntimeEnvironment.getApplication();val route=ClientAudioRoute(app,0)
        val source=route.microphone(3,config())!!
        try{
            assertEquals(0,source.read(ByteArray(2048))) // No peer: bounded wait, never opens host AudioRecord.
            Socket("127.0.0.1",route.localPort).use{s->s.soTimeout=2000;val out=DataOutputStream(s.getOutputStream());val input=DataInputStream(s.getInputStream())
                out.writeInt(AudioWire.MAGIC);out.writeByte(AudioWire.VERSION);out.writeBoolean(true);out.flush()
                assertEquals(2,input.readUnsignedByte());val id=input.readInt();assertEquals(24000,input.readInt());assertEquals(1,input.readUnsignedByte())
                val mic=byteArrayOf(1,2,3,4);out.writeByte(1);out.writeInt(id);out.writeShort(mic.size);out.write(mic);out.flush()
                val got=ByteArray(2048);var count=0;repeat(20){if(count==0)count=source.read(got)};assertEquals(4,count);assertArrayEquals(mic,got.copyOf(count))
                assertTrue(route.output(100,AudioFormat(AudioCodecKind.LPCM,44100,2,96),mic,0,mic.size))
                assertEquals(1,input.readUnsignedByte());assertEquals(100,input.readInt());assertEquals(44100,input.readInt());assertEquals(2,input.readUnsignedByte());assertEquals(4,input.readUnsignedShort());assertArrayEquals(mic,ByteArray(4).also{input.readFully(it)})
                val replacement=route.microphone(4,config())!!
                assertEquals(3,input.readUnsignedByte());assertEquals(id,input.readInt());assertEquals(-1,source.read(got))
                assertEquals(2,input.readUnsignedByte());val nextId=input.readInt();input.readInt();input.readUnsignedByte()
                route.microphoneStopped(3) // A stale Siri teardown must not stop the newer call input.
                out.writeByte(1);out.writeInt(nextId);out.writeShort(4);out.write(mic);out.flush()
                var nextCount=0;repeat(20){if(nextCount==0)nextCount=replacement.read(got)};assertEquals(4,nextCount)
                route.microphoneStopped(4);assertEquals(-1,replacement.read(got))
            }
        }finally{source.close();route.close()}
    }
    @Test fun bufferedPeerPreservesMusicAndFeedbackDoesNotBlockPromptOrCancellation(){
        val app=RuntimeEnvironment.getApplication();AirPlayPersistence.saveAudioOutput(app,CarPlayAudioOutput.CLIENT)
        val route=ClientAudioRoute(app,0)
        try { Socket("127.0.0.1",route.localPort).use { socket ->
            socket.soTimeout=2000
            val out=DataOutputStream(socket.getOutputStream());val input=DataInputStream(socket.getInputStream())
            out.writeInt(AudioWire.MAGIC);out.writeByte(AudioWire.BUFFERED_VERSION);out.writeBoolean(false);out.writeInt(300);out.flush()
            assertEquals(AudioWire.HELLO_V2,input.readUnsignedByte());assertEquals(2,input.readUnsignedByte())
            // HELLO precedes peer publication by a few instructions.
            repeat(100){if(!route.playbackAvailable)Thread.sleep(2)};assertTrue(route.playbackAvailable)
            val music=ByteArray(48000*4*3/2){(it*31).toByte()}
            assertTrue(route.output(102,AudioFormat(AudioCodecKind.LPCM,48000,2,96,"media"),music,0,music.size))
            Thread.sleep(600) // temporary receiver pause must not age out preserved music
            var epoch=0;var frames=0L
            val got=ByteArrayOutputStream()
            fun readPcm(expectedType:Int):Int {
                assertEquals(AudioWire.PCM_V2,input.readUnsignedByte());assertEquals(expectedType,input.readInt())
                val e=input.readInt();val purpose=input.readUnsignedByte();val rate=input.readInt();val channels=input.readUnsignedByte();val first=input.readLong();val n=input.readUnsignedShort()
                val bytes=ByteArray(n);input.readFully(bytes)
                if(expectedType==102){epoch=e;assertEquals(0,purpose);assertEquals(48000,rate);assertEquals(2,channels);assertEquals(frames,first);frames+=n/4;got.write(bytes)}
                else {assertEquals(3,purpose);assertArrayEquals(byteArrayOf(1,2,3,4),bytes)}
                return n
            }
            repeat(50){readPcm(102)} // 1000 ms total window, no credit yet
            socket.soTimeout=100
            try { input.readUnsignedByte();fail("music exceeded playback credit") } catch(_:SocketTimeoutException) {}
            socket.soTimeout=2000
            route.output(101,AudioFormat(AudioCodecKind.LPCM,24000,1,96,"default"),byteArrayOf(1,2,3,4),0,4)
            readPcm(101) // prompt bypasses blocked music window
            out.writeByte(AudioWire.PROGRESS);out.writeInt(102);out.writeInt(epoch);out.writeLong(24000);out.flush()
            repeat(25){readPcm(102)}
            assertArrayEquals(music,got.toByteArray())
            route.audioStopped(102)
            assertEquals(AudioWire.END_V2,input.readUnsignedByte());assertEquals(102,input.readInt());assertEquals(epoch,input.readInt());assertFalse(input.readBoolean())
            route.output(102,AudioFormat(AudioCodecKind.LPCM,48000,2,96,"media"),byteArrayOf(9,8,7,6),0,4)
            assertEquals(AudioWire.PCM_V2,input.readUnsignedByte());assertEquals(102,input.readInt());assertTrue(input.readInt()>epoch)
        }} finally {route.close()}
    }

}
