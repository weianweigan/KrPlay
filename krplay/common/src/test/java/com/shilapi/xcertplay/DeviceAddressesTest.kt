package com.shilapi.xcertplay
import com.shilapi.xcertplay.connection.DeviceAddresses
import java.net.InetAddress
import java.net.Inet4Address
import org.junit.Assert.*
import org.junit.Test
class DeviceAddressesTest {
    private fun usable(ip:String)=DeviceAddresses.isLanAddress(InetAddress.getByName(ip) as Inet4Address)
    @Test fun privateHotspotAndOriginalSharedAliasRemainVisible(){
        for(ip in listOf("192.168.43.1","192.168.1.20","10.0.0.1","172.16.1.1","100.96.0.1"))assertTrue(ip,usable(ip))
    }
    @Test fun publicLoopbackLinkLocalAndMulticastAreNotAdvertisedAsLan(){
        for(ip in listOf("8.8.8.8","127.0.0.1","169.254.1.2","224.0.0.1","100.63.255.255","100.128.0.1"))assertFalse(ip,usable(ip))
    }
}
