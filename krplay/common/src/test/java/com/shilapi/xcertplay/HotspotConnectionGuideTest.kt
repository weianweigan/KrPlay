package com.shilapi.xcertplay
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.shilapi.xcertplay.orchestration.WirelessHotspotMode
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[29])
class HotspotConnectionGuideTest {
    @Test fun disabledHotspotAlwaysGuidesToSystemSettings(){
        assertEquals(HotspotConnectionGuide.Issue.DISABLED,HotspotConnectionGuide.issue(false,"Test","password"))
        assertEquals(HotspotConnectionGuide.Issue.DISABLED,HotspotConnectionGuide.issue(false,"",""))
    }
    @Test fun absentOrInvalidConfigurationRequiresSetup(){
        for(enabled in listOf(true,null)) {
            assertEquals(HotspotConnectionGuide.Issue.CONFIGURATION,HotspotConnectionGuide.issue(enabled,"",""))
            assertEquals(HotspotConnectionGuide.Issue.CONFIGURATION,HotspotConnectionGuide.issue(enabled,"Test","short"))
        }
    }
    @Test fun validOrOpenHotspotAndUnobservableSwitchCanProceed(){
        for(enabled in listOf(true,null))for(password in listOf("password",""))assertNull(HotspotConnectionGuide.issue(enabled,"Test",password))
    }
    @Test fun oldModeChoicesMigrateWithoutChangingCredentials(){
        val app=RuntimeEnvironment.getApplication();val prefs=app.getSharedPreferences("xcertplay_airplay",0)
        for(old in listOf("WIFI_P2P","LOCAL_ONLY_HOTSPOT","MANUAL","unknown")){
            prefs.edit().putString("wireless_hotspot_mode",old).commit()
            AirPlayPersistence.saveManualHotspotSsid(app," My hotspot ");AirPlayPersistence.saveManualHotspotPassphrase(app," password ")
            assertEquals(WirelessHotspotMode.MANUAL,AirPlayPersistence.loadWirelessHotspotMode(app))
            assertEquals("MANUAL",prefs.getString("wireless_hotspot_mode",null))
            assertEquals(" My hotspot ",AirPlayPersistence.loadManualHotspotSsid(app));assertEquals(" password ",AirPlayPersistence.loadManualHotspotPassphrase(app))
        }
    }
}
