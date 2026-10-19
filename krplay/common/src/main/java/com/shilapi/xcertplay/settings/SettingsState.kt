// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay.settings

import androidx.annotation.StringRes
import com.shilapi.xcertplay.CarPlaySafeMargins
import com.shilapi.xcertplay.host.R

internal enum class SettingsCategory(
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int,
) {
    CONNECTION(R.string.settings_connection, R.string.settings_connection_summary),
    DISPLAY(R.string.settings_display, R.string.settings_display_summary),
    AUDIO(R.string.settings_audio, R.string.settings_audio_summary),
    VEHICLE(R.string.settings_vehicle, R.string.settings_vehicle_summary),
    HELP(R.string.settings_help, R.string.settings_help_summary),
}

internal enum class ChoiceSetting {
    APPEARANCE,
    DISPLAY_SIZE,
    RESOLUTION,
    FPS,
    AUDIO_BUFFER,
    AUDIO_OUTPUT,
    MICROPHONE,
    LOCATION,
}

internal enum class ToggleSetting {
    AUTO_CONNECT,
    AUTO_START,
    ADAPTIVE_SIZE,
    HEVC,
    RIGHT_HAND,
    NAVIGATION,
}

internal enum class TextSetting {
    OEM_NAME,
    SSID,
    PASSWORD,
}

internal enum class SettingsCommand {
    SELECT_PHONE,
    HOTSPOT_SETTINGS,
    SELECT_ICON,
    APP_PERMISSIONS,
    BLUETOOTH_SETTINGS,
    WIRELESS_HELP,
    EXPORT_REPORT,
    SOURCE,
    VERSION_SOURCE,
    LICENSES,
}

/** Immutable preferences and live service status. Credentials never enter SavedStateHandle. */
internal data class SettingsState(
    val category: SettingsCategory? = null,
    val choices: Map<ChoiceSetting, Int> = emptyMap(),
    val toggles: Map<ToggleSetting, Boolean> = emptyMap(),
    val phoneName: String = "",
    val manufacturer: String = "",
    val iconPath: String? = null,
    val iconVersion: Long = 0,
    val margins: CarPlaySafeMargins? = null,
    val legacySafeArea: Boolean = false,
    val ssid: String = "",
    val password: String = "",
    val navigationAvailable: Boolean = false,
    val runtime: SettingsRuntime = SettingsRuntime(),
) {
    fun selected(setting: ChoiceSetting) = choices.getValue(setting)

    fun checked(setting: ToggleSetting) = toggles.getValue(setting)
}

internal data class SettingsRuntime(
    val authenticationError: String? = null,
    val exporting: Boolean = false,
    val audioStatus: String = "",
    val microphoneStatus: String = "",
    val version: String = "",
)

/** The Activity executes platform operations; composables only describe UI and emit actions. */
internal class SettingsActions(
    val choice: (ChoiceSetting, Int) -> Unit,
    val toggle: (ToggleSetting, Boolean) -> Unit,
    val text: (TextSetting, String) -> String?,
    val credentials: (String, String) -> String?,
    val margins: (CarPlaySafeMargins) -> Unit,
    val resetBranding: (Boolean) -> Unit,
    val command: (SettingsCommand) -> Unit,
)
