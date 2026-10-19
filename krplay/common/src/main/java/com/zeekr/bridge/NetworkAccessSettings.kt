package com.zeekr.bridge
import android.content.Context
/** Fixed entry is distinct from forwarding all device Internet traffic. */
object NetworkAccessSettings {
    private fun prefs(c:Context)=c.getSharedPreferences("krplay-network",Context.MODE_PRIVATE)
    fun automatic(c:Context)=prefs(c).getBoolean("automatic_fixed_entry",true)
    fun automatic(c:Context,on:Boolean){prefs(c).edit().putBoolean("automatic_fixed_entry",on).apply()}
    fun asked(c:Context)=prefs(c).getBoolean("consent_asked",false)
    fun markAsked(c:Context){prefs(c).edit().putBoolean("consent_asked",true).apply()}
}
