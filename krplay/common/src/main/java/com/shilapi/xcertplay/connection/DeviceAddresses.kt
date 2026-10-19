package com.shilapi.xcertplay.connection
import android.content.Context
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
/** Raw device addresses for the native client; no URL, certificate, or browser pairing. */
internal object DeviceAddresses {
    data class Entry(val interfaceName:String,val address:String)
    fun isLanAddress(address:Inet4Address):Boolean {
        val bytes=address.address;val shared=(bytes[0].toInt() and 255)==100 && (bytes[1].toInt() and 255) in 64..127
        return (address.isSiteLocalAddress||shared)&&!address.isLinkLocalAddress&&!address.isLoopbackAddress&&!address.isMulticastAddress
    }
    fun discover(context:Context):List<Entry> = runCatching {
        Collections.list(NetworkInterface.getNetworkInterfaces()).flatMap { iface ->
            if(!iface.isUp||iface.isLoopback)emptyList()else Collections.list(iface.inetAddresses).filterIsInstance<Inet4Address>()
                .filter{isLanAddress(it) && !iface.name.startsWith("tun") && !iface.name.startsWith("rmnet")}
                .map{Entry(iface.name,it.hostAddress!!)}
        }.distinct().sortedBy{it.interfaceName}
    }.getOrDefault(emptyList())
}
