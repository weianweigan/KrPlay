package com.shilapi.xcertplay
import android.content.Context
import com.shilapi.xcertplay.media.EncodedVideoSink
import com.shilapi.xcertplay.airplay.VideoCodec
/** Native projection status, independent of a web service. */
internal object ProjectionState {
    @Volatile var videoActive=false
    fun stage(context:Context)=ConnectionDiagnostics.stage
    private val executor=java.util.concurrent.Executors.newSingleThreadExecutor()
    fun refreshAppearance(context:Context){val controller=CarPlayBackgroundSession.snapshot()?.controller?:return
        executor.execute{if(CarPlayBackgroundSession.snapshot()?.controller===controller)controller.setNightMode(AirPlayPersistence.loadAppearance(context).night(isDarkMode(context.resources.configuration.uiMode)))}
    }
}
internal object NativeVideoConsumer:EncodedVideoSink {
    override fun configure(type:Int,codec:VideoCodec,data:ByteArray)=Unit
    override fun recovery(type:Int,request:()->Unit)=Unit
    override fun stopped(type:Int)=Unit
    override fun frame(type:Int,data:ByteArray,presentationTimeUs:Long)=true
}
