package com.shilapi.xcertplay

import androidx.compose.ui.test.*
import com.shilapi.xcertplay.orchestration.WirelessHotspotMode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "zh-rCN-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ManualHotspotSettingsTest : ComposeSettingsTest() {
    private val context
        get() = RuntimeEnvironment.getApplication()

    private val password = " password "

    private fun configureManual(ssid: String = "Original") {
        AirPlayPersistence.saveManualHotspotSsid(context, ssid)
        AirPlayPersistence.saveManualHotspotPassphrase(context, password)
        AirPlayPersistence.saveWirelessHotspotMode(context, WirelessHotspotMode.MANUAL)
    }

    private fun withSettings(block: () -> Unit) {
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java).setup()
        try {
            SettingsNavigation.open(controller.get(), "iPhone 连接", compose)
            block()
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private fun selectManual() {
        clickTag("settings:hotspot-credentials")
    }

    private fun credentials(ssid: String) {
        compose.onNodeWithTag("hotspot:ssid").performTextReplacement(ssid)
        compose.onNodeWithTag("hotspot:password").performTextReplacement(password)
    }

    @Test
    fun onlySystemHotspotConfigurationIsExposed() = withSettings {
        compose.onNodeWithTag("choice:WIRELESS").assertDoesNotExist()
        compose.onNodeWithText("Wi-Fi Direct · 默认").assertDoesNotExist()
        compose.onNodeWithTag("settings:hotspot-credentials").assertExists()
        compose.onNodeWithTag("text:SSID").assertExists()
    }

    @Test
    fun firstSetupPreservesSsidAndPasswordWhitespaceAcrossReopening() {
        val ssid = " CarPlayTest "
        withSettings {
            selectManual()
            credentials(ssid)
            saveInput()
            assertEquals(
                WirelessHotspotMode.MANUAL,
                AirPlayPersistence.loadWirelessHotspotMode(context),
            )
            assertEquals(ssid, AirPlayPersistence.loadManualHotspotSsid(context))
            assertEquals(password, AirPlayPersistence.loadManualHotspotPassphrase(context))
        }
        withSettings {
            clickTag("text:SSID")
            compose.onNodeWithTag("settings:text-input").assertTextContains(ssid)
            cancelInput()
        }
    }

    @Test
    fun editingPreservesLeadingTrailingAndMultibyteSsidWhitespace() {
        configureManual()
        withSettings {
            for (ssid in
                listOf("CarPlayTest ", " CarPlayTest", " CarPlayTest ", " 车机热点 ", "Normal")) {
                clickTag("text:SSID")
                input(ssid)
                saveInput()
                assertEquals(ssid, AirPlayPersistence.loadManualHotspotSsid(context))
            }
        }
    }

    @Test
    fun validationCountsWhitespaceAndUtf8BytesAndRejectsBlankAndNulNames() {
        configureManual()
        withSettings {
            val boundary = " " + "x".repeat(30) + " "
            clickTag("text:SSID")
            input(boundary)
            saveInput()
            for (invalid in
                listOf("", "   ", "x".repeat(32) + " ", "界".repeat(11), "Test\u0000SSID")) {
                clickTag("text:SSID")
                input(invalid)
                saveInput()
                assertEquals(boundary, AirPlayPersistence.loadManualHotspotSsid(context))
                compose.onNodeWithTag("settings:text-input").assertExists()
                cancelInput()
            }
        }
    }

    @Test
    fun invalidFirstSetupDoesNotSaveCredentials() = withSettings {
        for (invalid in listOf("   ", "x".repeat(32) + " ", "Test\u0000SSID")) {
            selectManual()
            credentials(invalid)
            saveInput()
            assertEquals(
                WirelessHotspotMode.MANUAL,
                AirPlayPersistence.loadWirelessHotspotMode(context),
            )
            assertEquals("", AirPlayPersistence.loadManualHotspotSsid(context))
            assertEquals("", AirPlayPersistence.loadManualHotspotPassphrase(context))
            cancelInput()
        }
    }

    @Test
    fun cancellingFirstSetupDoesNotSaveEitherCredential() = withSettings {
        selectManual()
        credentials(" CarPlayTest ")
        cancelInput()
        assertEquals(
            WirelessHotspotMode.MANUAL,
            AirPlayPersistence.loadWirelessHotspotMode(context),
        )
        assertEquals("", AirPlayPersistence.loadManualHotspotSsid(context))
        assertEquals("", AirPlayPersistence.loadManualHotspotPassphrase(context))
    }

    @Test
    fun cancellingAnEditPreservesThePreviousSsidAndPasswordsRemainUntrimmed() {
        configureManual(" Original ")
        withSettings {
            clickTag("text:SSID")
            input(" Changed ")
            cancelInput()
            assertEquals(" Original ", AirPlayPersistence.loadManualHotspotSsid(context))
            clickTag("text:PASSWORD")
            input(" new password ")
            saveInput()
            assertEquals(" new password ", AirPlayPersistence.loadManualHotspotPassphrase(context))
        }
    }
}
