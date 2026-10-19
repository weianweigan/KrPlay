package com.shilapi.xcertplay

import com.shilapi.xcertplay.orchestration.ManualHotspotValidation

/** A hidden OEM hotspot API is not evidence that the hotspot is off. */
internal object HotspotConnectionGuide {
    enum class Issue { DISABLED, CONFIGURATION }
    fun issue(enabled: Boolean?, ssid: String, password: String): Issue? = when {
        enabled == false -> Issue.DISABLED
        ManualHotspotValidation.validate(ssid, password) != null -> Issue.CONFIGURATION
        else -> null
    }
}
