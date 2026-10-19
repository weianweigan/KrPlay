package com.shilapi.xcertplay

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.ui.test.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.i18n.UiException
import com.shilapi.xcertplay.i18n.UiText
import com.shilapi.xcertplay.i18n.userMessage
import com.shilapi.xcertplay.settings.ChoiceSetting
import com.shilapi.xcertplay.shared.R as UiR
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], qualifiers = "en-rUS-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LocalizationTest : ComposeSettingsTest() {
    private val context
        get() = RuntimeEnvironment.getApplication()

    private fun localized(tag: String): Context =
        context.createConfigurationContext(
            Configuration(context.resources.configuration).apply {
                setLocales(LocaleList(Locale.forLanguageTag(tag)))
            }
        )

    @Test
    fun systemLocaleSelectsResourcesAndUnsupportedLocalesFallBackToEnglish() {
        assertEquals("Settings", localized("en-US").getString(R.string.settings_title))
        assertEquals("设置", localized("zh-CN").getString(R.string.settings_title))
        assertEquals("設定", localized("zh-TW").getString(R.string.settings_title))
        assertEquals("設定", localized("zh-HK").getString(R.string.settings_title))
        assertEquals("Settings", localized("fr-FR").getString(R.string.settings_title))
        assertEquals("Audio & microphone", localized("en").getString(R.string.settings_audio))
        assertEquals("音訊與麥克風", localized("zh-TW").getString(R.string.settings_audio))
    }

}
