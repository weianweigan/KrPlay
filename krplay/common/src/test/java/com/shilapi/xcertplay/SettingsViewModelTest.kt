package com.shilapi.xcertplay

import androidx.lifecycle.SavedStateHandle
import com.shilapi.xcertplay.settings.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SettingsViewModelTest {
    private val app
        get() = RuntimeEnvironment.getApplication()

    @After
    fun cleanup() {
    }

    @Test
    fun restoredNavigationContainsScrollPositionsButNoCredentials() {
        val handle = SavedStateHandle()
        val model = SettingsViewModel(app, handle)
        model.saveCredentials(" CarPlay ", " password ")
        model.open(SettingsCategory.AUDIO)
        model.rememberScroll(null, 160)
        model.rememberScroll(SettingsCategory.AUDIO, 240)
        val saved = handle.keys().associateWith { handle.get<Any?>(it) }
        assertEquals(setOf("settingsCategory", "scroll:index", "scroll:AUDIO"), saved.keys)
        val restored = SettingsViewModel(app, SavedStateHandle(saved))
        assertEquals(SettingsCategory.AUDIO, restored.state.value.category)
        assertEquals(160, restored.scrollPosition(null))
        assertEquals(240, restored.scrollPosition(SettingsCategory.AUDIO))
        assertEquals(" CarPlay ", restored.state.value.ssid)
        assertEquals(" password ", restored.state.value.password)
    }

    @Test
    fun invalidCredentialsLeaveBothPersistedFieldsUntouched() {
        val model = SettingsViewModel(app, SavedStateHandle())
        model.saveCredentials(" Original ", " password ")
        assertTrue(runCatching { model.saveCredentials(" Changed ", "short") }.isFailure)
        assertEquals(" Original ", AirPlayPersistence.loadManualHotspotSsid(app))
        assertEquals(" password ", AirPlayPersistence.loadManualHotspotPassphrase(app))
        assertTrue(runCatching { model.saveCredentials("界".repeat(11), "otherpassword") }.isFailure)
        assertEquals(" Original ", model.state.value.ssid)
    }

    @Test
    fun mediaSettingsKeepMicrophoneDisabledAndRequestTheAppropriateEffects() {
        val model = SettingsViewModel(app, SavedStateHandle())
        assertTrue(
            model.choose(ChoiceSetting.MICROPHONE, CarPlayMicrophoneSource.DISABLED.ordinal)
                is SettingsEffect.Reconnect
        )
        model.choose(ChoiceSetting.AUDIO_BUFFER, 2)
        assertEquals(CarPlayMicrophoneSource.DISABLED, AirPlayPersistence.loadMicrophoneSource(app))
        assertEquals(
            CarPlayMicrophoneSource.DISABLED.ordinal,
            model.state.value.selected(ChoiceSetting.MICROPHONE),
        )
        assertEquals(SettingsEffect.None, model.choose(ChoiceSetting.LOCATION, 1))
        assertTrue(AirPlayPersistence.loadLocationReportingEnabled(app))
    }

    @Test
    fun removedSavedCategoryFallsBackToIndexWithoutRevivingBrowserOptions() {
        for(name in listOf("WEB","BROWSER","TESLA","AUTHENTICATION")) {
            val model=SettingsViewModel(app,SavedStateHandle(mapOf("settingsCategory" to name)))
            assertNull(model.state.value.category)
            assertEquals(setOf("CONNECTION","DISPLAY","AUDIO","VEHICLE","HELP"),SettingsCategory.entries.map{it.name}.toSet())
        }
    }
}
