package com.zeekr.bridge
import android.content.Context
/** Fixed entry is distinct from forwarding all device Internet traffic. */
object NetworkAccessSettings {
    private fun prefs(c:Context)=c.getSharedPreferences("krplay-network",Context.MODE_PRIVATE)
    fun automatic(c:Context)=prefs(c).getBoolean("automatic_fixed_entry",true)
    fun automatic(c:Context,on:Boolean){prefs(c).edit().putBoolean("automatic_fixed_entry",on).apply()}
    fun asked(c:Context)=prefs(c).getBoolean("consent_asked",false)
    fun markAsked(c:Context){prefs(c).edit().putBoolean("consent_asked",true).apply()}
    fun rootNetworkEnabled(c:Context)=prefs(c).getBoolean("root_network_enabled",false)
    fun rootNetworkEnabled(c:Context,on:Boolean){prefs(c).edit().putBoolean("root_network_enabled",on).apply()}
    fun rootModeChoiceAsked(c:Context)=prefs(c).getBoolean("root_mode_choice_asked",false)
    fun markRootModeChoiceAsked(c:Context){prefs(c).edit().putBoolean("root_mode_choice_asked",true).apply()}
}
