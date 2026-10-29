package com.zeekr.bridge

import android.os.Build
import java.io.File

object RootUtils {
    @Volatile var isRootOverride: Boolean? = null

    fun isRootAvailable(): Boolean {
        isRootOverride?.let { return it }
        if (Build.FINGERPRINT.contains("robolectric")) {
            return false
        }
        val paths = (System.getenv("PATH") ?: "").split(':') + listOf("/system/bin", "/system/xbin", "/sbin", "/debug_ramdisk")
        return paths.any { File(it, "su").canExecute() }
    }
}
