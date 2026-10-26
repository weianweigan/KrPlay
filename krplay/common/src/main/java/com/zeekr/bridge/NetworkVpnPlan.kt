package com.zeekr.bridge
internal data class VpnNetwork(val address:String,val prefix:Int)
internal data class NetworkVpnPlan(val addresses:List<VpnNetwork>,val routes:List<VpnNetwork>,val dns:List<String>,val forwarding:Boolean){
    companion object {
        fun forBorrowing(borrow:Boolean)=if(borrow && com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED)NetworkVpnPlan(
            listOf(VpnNetwork("198.18.0.1",30),VpnNetwork("fd00:6b72::1",64)),
            listOf(VpnNetwork("0.0.0.0",0),VpnNetwork("::",0)),listOf("198.18.0.2"),true)
        else NetworkVpnPlan(listOf(VpnNetwork("192.168.229.229",32)),listOf(VpnNetwork("192.168.229.229",32)),emptyList(),false)
    }
}
