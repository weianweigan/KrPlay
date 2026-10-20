package com.shilapi.xcertplay

import android.os.Handler
import com.zeekr.bridge.ZeekrBridgeManager
import java.net.Socket
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ClientDisplayPriorityTest {
    private val host = Robolectric.buildActivity(CarPlayHostActivity::class.java).get()
    private val type = host.javaClass
    private val control = ZeekrBridgeManager.javaClass.getDeclaredField("control").apply { isAccessible = true }
    private fun set(name: String, value: Any?) = type.getDeclaredField(name).apply { isAccessible = true }.set(host, value)
    private fun get(name: String) = type.getDeclaredField(name).apply { isAccessible = true }.get(host)
    private fun size(w: Int, h: Int): Any {
        val sizeType = type.declaredClasses.single { it.simpleName == "DisplaySize" }
        return sizeType.getDeclaredConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.newInstance(w, h)
    }
    private fun adaptive(value: Boolean) {
        RuntimeEnvironment.getApplication().getSharedPreferences("xcertplay_airplay", 0)
            .edit().putBoolean("adaptive_browser_size", value).commit()
    }
    private fun nativeSize(w: Int, h: Int) {
        type.getDeclaredMethod("scheduleDisplaySize", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .apply { isAccessible = true }.invoke(host, w, h)
    }
    @After fun cleanup() {
        (control.get(ZeekrBridgeManager) as? Socket)?.close()
        control.set(ZeekrBridgeManager, null)
        (get("mainHandler") as Handler).removeCallbacksAndMessages(null)
        adaptive(true)
    }
    @Test fun nativeSurfaceCannotOverrideConnectedClientViewport() {
        adaptive(true);control.set(ZeekrBridgeManager, Socket())
        set("activeDisplaySize", size(1696, 1600))
        nativeSize(2560, 1600)
        assertNull(get("pendingDisplaySize"))
    }
    @Test fun repeatedClientViewportCancelsAnAlreadyPendingNativeRestart() {
        adaptive(true);set("activeDisplaySize", size(1696, 1600))
        nativeSize(2560, 1600);assertNotNull(get("pendingDisplaySize"))
        type.getDeclaredMethod("scheduleClientDisplaySize", Int::class.javaObjectType, Int::class.javaObjectType)
            .apply { isAccessible = true }.invoke(host, 1696, 1600)
        assertNull(get("pendingDisplaySize"))
    }
    @Test fun standaloneReceiverStillFollowsItsNativeSurface() {
        adaptive(true);control.set(ZeekrBridgeManager, null)
        set("activeDisplaySize", size(1696, 1600));nativeSize(2560, 1600)
        assertEquals(size(2560, 1600), get("pendingDisplaySize"))
    }
    @Test fun disablingAdaptiveSizeKeepsNativeSizingWithAClientConnected() {
        adaptive(false);control.set(ZeekrBridgeManager, Socket())
        set("activeDisplaySize", size(1696, 1600));nativeSize(2560, 1600)
        assertEquals(size(2560, 1600), get("pendingDisplaySize"))
    }
}
