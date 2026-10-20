// SPDX-License-Identifier: AGPL-3.0-only
package com.shilapi.xcertplay.settings

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.shilapi.xcertplay.host.R

@Composable
internal fun HelpSettingsScreen(state: SettingsState, actions: SettingsActions) {
    SettingsGroup(R.string.settings_permissions_help) {
        SettingsDescription(R.string.settings_permissions_description)
        SettingsButton(R.string.settings_app_permissions) { actions.command(SettingsCommand.APP_PERMISSIONS) }
        SettingsButton(R.string.settings_bluetooth_settings) { actions.command(SettingsCommand.BLUETOOTH_SETTINGS) }
        SettingsButton(R.string.settings_wireless_help) { actions.command(SettingsCommand.WIRELESS_HELP) }
    }
    SettingsGroup(R.string.settings_about_diagnostics) {
        val context = androidx.compose.ui.platform.LocalContext.current
        SettingsButton(R.string.settings_check_updates) { com.krplay.update.AppUpdates.open(context) }
        Text(
            "${settingsString(R.string.app_name)} · ${state.runtime.version}",
            style = MaterialTheme.typography.titleMedium,
        )
        SettingsButton(
            if (state.runtime.exporting) R.string.settings_exporting else R.string.settings_export,
            !state.runtime.exporting,
        ) {
            actions.command(SettingsCommand.EXPORT_REPORT)
        }
        SettingsDescription(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) R.string.settings_export_downloads
            else R.string.settings_export_document
        )
        SettingsDescription(R.string.settings_licenses)
    }
    SettingsGroup(R.string.settings_open_source) {
        val context = androidx.compose.ui.platform.LocalContext.current
        SettingsDescription(R.string.settings_source_offer)
        SettingsRow(
            settingsString(R.string.settings_version_source),
            com.krplay.distribution.SourceDistribution.versionLabel(context),
            "settings:version-source",
        ) { actions.command(SettingsCommand.VERSION_SOURCE) }
        SettingsButton(R.string.settings_license_texts) { actions.command(SettingsCommand.LICENSES) }

        SettingsRow(
            settingsString(R.string.settings_source),
            "github.com/weianweigan/KrPlay",
            "settings:source",
        ) {
            actions.command(SettingsCommand.SOURCE)
        }
    }
}
