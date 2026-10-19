// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import com.shilapi.xcertplay.*
import com.shilapi.xcertplay.airplay.CarPlaySize
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.media.MediaAudioBuffer
import com.shilapi.xcertplay.orchestration.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal sealed interface SettingsEffect {
    data object None:SettingsEffect
    data class Reconnect(val wireless:Boolean?=null,val stopBeforeConnect:Boolean=false):SettingsEffect
}
internal class SettingsViewModel(application:Application,private val savedState:SavedStateHandle):AndroidViewModel(application){
    private val app=application
    private val mutableState=MutableStateFlow(SettingsState(category=savedState.get<String>("settingsCategory")?.let{n->SettingsCategory.entries.firstOrNull{it.name==n}}))
    val state=mutableState.asStateFlow()
    init{reload()}
    fun open(category:SettingsCategory?){savedState["settingsCategory"]=category?.name;mutableState.value=mutableState.value.copy(category=category)}
    fun scrollPosition(category:SettingsCategory?)=savedState.get<Int>("scroll:${category?.name?:"index"}")?:0
    fun rememberScroll(category:SettingsCategory?,position:Int){savedState["scroll:${category?.name?:"index"}"]=position}
    fun updateRuntime(runtime:SettingsRuntime){mutableState.value=state.value.copy(runtime=runtime)}
    fun reload(){mutableState.value=state.value.copy(
        choices=mapOf(ChoiceSetting.APPEARANCE to AirPlayPersistence.loadAppearance(app).ordinal,
            ChoiceSetting.DISPLAY_SIZE to CarPlaySize.fromWidthMillimeters(AirPlayPersistence.loadWidthPhysicalMm(app)).ordinal,
            ChoiceSetting.RESOLUTION to listOf(10,8,6).indexOf(AirPlayPersistence.loadDisplayScaleTenths(app)).coerceAtLeast(0),
            ChoiceSetting.FPS to if(AirPlayPersistence.loadFps(app)==60)1 else 0,
            ChoiceSetting.AUDIO_BUFFER to MediaAudioBuffer.presets.indexOf(AirPlayPersistence.loadMediaBufferMillis(app)).coerceAtLeast(0),
            ChoiceSetting.AUDIO_OUTPUT to AirPlayPersistence.loadAudioOutput(app).ordinal,
            ChoiceSetting.MICROPHONE to AirPlayPersistence.loadMicrophoneSource(app).ordinal,
            ChoiceSetting.LOCATION to if(AirPlayPersistence.loadLocationReportingEnabled(app))1 else 0),
        toggles=mapOf(ToggleSetting.AUTO_CONNECT to DiPlayPreferences.autoConnect(app),ToggleSetting.AUTO_START to AirPlayPersistence.loadAutoStartOnBoot(app),ToggleSetting.ADAPTIVE_SIZE to AirPlayPersistence.loadAdaptiveClientSize(app),ToggleSetting.HEVC to AirPlayPersistence.loadHevcEnabled(app),ToggleSetting.RIGHT_HAND to AirPlayPersistence.loadRightHandDrive(app),ToggleSetting.NAVIGATION to false),
        phoneName=if(DiPlayPreferences.phoneAddress(app)==null)app.getString(R.string.settings_unselected)else DiPlayPreferences.phoneName(app),
        manufacturer=AirPlayPersistence.loadSimulatedManufacturerName(app),iconPath=AirPlayPersistence.loadCustomAirPlayIconFile(app)?.absolutePath,
        iconVersion=AirPlayPersistence.loadCustomAirPlayIconFile(app)?.lastModified()?:0,
        margins=AirPlayPersistence.loadSafeMargins(app),legacySafeArea=AirPlayPersistence.loadSafeAreaRect(app,1280,720)!=null,
        ssid=AirPlayPersistence.loadManualHotspotSsid(app),password=AirPlayPersistence.loadManualHotspotPassphrase(app),navigationAvailable=false)}
    fun choose(setting:ChoiceSetting,index:Int):SettingsEffect{if(state.value.selected(setting)==index)return SettingsEffect.None
        when(setting){
            ChoiceSetting.APPEARANCE->{require(index in CarPlayAppearance.entries.indices);AirPlayPersistence.saveAppearance(app,CarPlayAppearance.entries[index]);ProjectionState.refreshAppearance(app)}
            ChoiceSetting.DISPLAY_SIZE->AirPlayPersistence.saveWidthPhysicalMm(app,CarPlaySize.entries[index].widthMillimeters)
            ChoiceSetting.RESOLUTION->AirPlayPersistence.saveDisplayScaleTenths(app,listOf(10,8,6)[index])
            ChoiceSetting.FPS->{require(index in 0..1);AirPlayPersistence.saveFps(app,if(index==1)60 else 30)}
            ChoiceSetting.AUDIO_BUFFER->AirPlayPersistence.saveMediaBufferMillis(app,MediaAudioBuffer.presets[index])
            ChoiceSetting.AUDIO_OUTPUT->AirPlayPersistence.saveAudioOutput(app,CarPlayAudioOutput.entries[index])
            ChoiceSetting.MICROPHONE->AirPlayPersistence.saveMicrophoneSource(app,CarPlayMicrophoneSource.entries[index])
            ChoiceSetting.LOCATION->{require(index in 0..1);AirPlayPersistence.saveLocationReportingEnabled(app,index==1)}
        };reload();return when(setting){ChoiceSetting.APPEARANCE,ChoiceSetting.LOCATION->SettingsEffect.None;else->SettingsEffect.Reconnect()}
    }
    fun toggle(setting:ToggleSetting,checked:Boolean):SettingsEffect{when(setting){
        ToggleSetting.AUTO_CONNECT->DiPlayPreferences.saveAutoConnect(app,checked)
        ToggleSetting.AUTO_START->AirPlayPersistence.saveAutoStartOnBoot(app,checked)
        ToggleSetting.ADAPTIVE_SIZE->AirPlayPersistence.saveAdaptiveClientSize(app,checked)
        ToggleSetting.HEVC->AirPlayPersistence.saveHevcEnabled(app,checked)
        ToggleSetting.RIGHT_HAND->AirPlayPersistence.saveRightHandDrive(app,checked)
        ToggleSetting.NAVIGATION->Unit
    };reload();return SettingsEffect.None}
    fun edit(setting:TextSetting,value:String):SettingsEffect{when(setting){TextSetting.OEM_NAME->AirPlayPersistence.saveSimulatedManufacturerName(app,value.trim());TextSetting.SSID->saveCredentials(value,state.value.password);TextSetting.PASSWORD->saveCredentials(state.value.ssid,value)};reload();return SettingsEffect.None}
    fun saveCredentials(ssid:String,password:String){ManualHotspotValidation.validate(ssid,password)?.let{throw com.shilapi.xcertplay.i18n.UiException(it)};AirPlayPersistence.saveManualHotspotSsid(app,ssid);AirPlayPersistence.saveManualHotspotPassphrase(app,password);AirPlayPersistence.saveManualHotspotSecurity(app,ManualHotspotValidation.securityFor(password));AirPlayPersistence.saveManualHotspotBand(app,ManualHotspotBand.AUTO);AirPlayPersistence.saveManualHotspotChannel(app,0);reload()}
    fun saveMargins(margins:CarPlaySafeMargins):SettingsEffect{AirPlayPersistence.saveSafeMargins(app,margins);reload();return SettingsEffect.Reconnect()}
    fun resetBranding(includeName:Boolean){AirPlayPersistence.clearCustomAirPlayIcon(app);if(includeName)AirPlayPersistence.saveSimulatedManufacturerName(app,AirPlayPersistence.DEFAULT_SIMULATED_MANUFACTURER_NAME);reload()}
}
