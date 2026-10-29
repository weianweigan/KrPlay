package com.shilapi.xcertplay

import com.zeekr.bridge.BorrowVpnService
import com.zeekr.bridge.NetworkAccessSettings
import com.zeekr.bridge.RootNetworkManager
import com.zeekr.bridge.RootUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class RootNetworkTest {

    @After
    fun tearDown() {
        RootUtils.isRootOverride = null
    }

    @Test
    fun rootNetworkSettingsDefaultsAndPersistence() {
        val c = RuntimeEnvironment.getApplication()
        assertFalse(NetworkAccessSettings.rootNetworkEnabled(c))
        assertFalse(NetworkAccessSettings.rootModeChoiceAsked(c))

        NetworkAccessSettings.rootNetworkEnabled(c, true)
        assertTrue(NetworkAccessSettings.rootNetworkEnabled(c))

        NetworkAccessSettings.markRootModeChoiceAsked(c)
        assertTrue(NetworkAccessSettings.rootModeChoiceAsked(c))

        NetworkAccessSettings.rootNetworkEnabled(c, false)
        assertFalse(NetworkAccessSettings.rootNetworkEnabled(c))
    }

    @Test
    fun borrowVpnServiceExposesAccessStateModification() {
        BorrowVpnService.setAccessState(true, "测试固定入口已就绪")
        assertTrue(BorrowVpnService.accessActive)
        assertEquals("测试固定入口已就绪", BorrowVpnService.accessMessage)

        BorrowVpnService.setAccessState(false, "固定入口未开启")
        assertFalse(BorrowVpnService.accessActive)
        assertEquals("固定入口未开启", BorrowVpnService.accessMessage)
    }

    @Test
    fun rootUtilsAvailableReturnsBooleanWithoutException() {
        RootUtils.isRootOverride = null
        assertFalse(RootUtils.isRootAvailable()) // In Robolectric, defaults to false

        RootUtils.isRootOverride = true
        assertTrue(RootUtils.isRootAvailable())
    }

    @Test
    fun rootDeviceLaunchesModeSelectionDialogOnFirstLaunch() {
        RootUtils.isRootOverride = true
        val c = RuntimeEnvironment.getApplication()
        NetworkAccessSettings.automatic(c, true)
        NetworkAccessSettings.rootNetworkEnabled(c, false)
        c.getSharedPreferences("krplay-network", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("root_mode_choice_asked", false)
            .apply()

        val controller = Robolectric.buildActivity(DiPlayActivity::class.java).setup()
        try {
            val dialog = ShadowAlertDialog.getLatestDialog()
            assertNotNull("Root device should pop up mode selection dialog on first launch", dialog)
            assertTrue(dialog.isShowing)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
