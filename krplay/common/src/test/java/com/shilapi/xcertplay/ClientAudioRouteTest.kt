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
}
