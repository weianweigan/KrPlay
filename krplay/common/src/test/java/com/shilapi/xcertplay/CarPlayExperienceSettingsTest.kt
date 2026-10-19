package com.shilapi.xcertplay
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[29])
class CarPlayExperienceSettingsTest {
    @Test fun legacyBrowserMicrophoneUsesNativeClientDefaultInsteadOfDisabled(){
        val app=RuntimeEnvironment.getApplication()
        app.getSharedPreferences("xcertplay_airplay",0).edit().putString("microphone_source","BROWSER").putBoolean("browser_microphone",true).commit()
        assertEquals(CarPlayMicrophoneSource.CLIENT,AirPlayPersistence.loadMicrophoneSource(app))
        AirPlayPersistence.saveMicrophoneSource(app,CarPlayMicrophoneSource.DISABLED)
        assertEquals(CarPlayMicrophoneSource.DISABLED,AirPlayPersistence.loadMicrophoneSource(app))
    }
    @Test fun clientAdaptivePreferencePreservesExistingUserChoice(){
        val app=RuntimeEnvironment.getApplication()
        app.getSharedPreferences("xcertplay_airplay",0).edit().putBoolean("adaptive_browser_size",false).commit()
        assertFalse(AirPlayPersistence.loadAdaptiveClientSize(app))
        AirPlayPersistence.saveAdaptiveClientSize(app,true);assertTrue(AirPlayPersistence.loadAdaptiveClientSize(app))
    }
}
