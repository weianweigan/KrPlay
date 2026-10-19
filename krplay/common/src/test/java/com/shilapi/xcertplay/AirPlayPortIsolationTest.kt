package com.shilapi.xcertplay

import com.shilapi.xcertplay.airplay.AirPlayConfig
import com.zeekr.bridge.ReverseSocks5Hub
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[29])
class AirPlayPortIsolationTest {
    @Test fun realHostConfigurationDoesNotUseTheReverseHubListenerPort() {
        val app=RuntimeEnvironment.getApplication()
        val identity=SyntheticMfiIdentity.create()
        MfiAssetStore(app.noBackupFilesDir).installFiles({identity.key.inputStream()},{identity.certificate.inputStream()})
        AirPlayPersistence.saveWirelessEnabled(app,false)
        val host=Robolectric.buildActivity(CarPlayHostActivity::class.java).get()
        val type=host.javaClass
        type.getDeclaredMethod("loadPersistedSettings").apply{isAccessible=true}.invoke(host)
        type.getDeclaredField("airPlayIdentity").apply{isAccessible=true}.set(host,AirPlayPersistence.loadIdentity(app))
        val sizeType=type.declaredClasses.single{it.simpleName=="DisplaySize"}
        val size=sizeType.getDeclaredConstructor(Int::class.javaPrimitiveType,Int::class.javaPrimitiveType).apply{isAccessible=true}.newInstance(1280,720)
        val advertised=type.getDeclaredMethod("createAirPlayConfig",sizeType).apply{isAccessible=true}.invoke(host,size) as AirPlayConfig
        val hub=ReverseSocks5Hub()
        val workerPort=hub.javaClass.getDeclaredField("workerPort").apply{isAccessible=true}.getInt(hub)
        assertNotEquals("Wildcard reverse hub binding must not block the advertised AirPlay listener",workerPort,advertised.port)
        assertTrue(advertised.port in 1024..65535)
        assertEquals(7000,workerPort) // Existing vehicle clients retain their deployed port.
    }
}
