package com.shilapi.xcertplay.orchestration
import com.shilapi.xcertplay.transport.Iap2IdentificationConfig
import org.junit.Assert.*
import org.junit.Test
class WiredHotspotIndependenceTest {
    private val identity=Iap2IdentificationConfig("test","test","test","1","1","1",3)
    @Test fun wiredDoesNotRequireHotspotNameOrPassword(){
        val config=CarPlayRuntimeConfig(mfiTarget=MfiTarget.LOCAL,identification=identity)
        assertEquals(CarPlayTransport.WIRED,config.transport);assertNull(config.manualHotspotSsid)
    }
    @Test fun wirelessCannotStartWithMissingHotspotConfiguration(){
        assertThrows(IllegalArgumentException::class.java){CarPlayRuntimeConfig(mfiTarget=MfiTarget.LOCAL,identification=identity,transport=CarPlayTransport.WIRELESS)}
    }
}
