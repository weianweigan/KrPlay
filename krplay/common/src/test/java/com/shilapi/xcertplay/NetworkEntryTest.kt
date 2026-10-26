package com.shilapi.xcertplay
import com.zeekr.bridge.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[29])
class NetworkEntryTest {
    @Test fun startupDefaultsToFixedEntryAndConsentIsNotRepeatedAfterDenial(){
        val c=RuntimeEnvironment.getApplication();assertTrue(NetworkAccessSettings.automatic(c));assertFalse(NetworkAccessSettings.asked(c))
        NetworkAccessSettings.markAsked(c);assertTrue(NetworkAccessSettings.asked(c))
        NetworkAccessSettings.automatic(c,false);assertFalse(NetworkAccessSettings.automatic(c))
    }
    @Test fun launcherRequestsConsentOnceAndDoesNotRepeatAfterCancellation(){
        org.robolectric.shadows.ShadowVpnService.setPrepareResult(android.content.Intent("test.vpn.CONSENT"))
        val first=org.robolectric.Robolectric.buildActivity(DiPlayActivity::class.java).setup()
        try{val launched=org.robolectric.Shadows.shadowOf(first.get()).nextStartedActivity
            assertEquals("test.vpn.CONSENT",launched.action)
            assertTrue(NetworkAccessSettings.asked(first.get()))
            org.robolectric.Shadows.shadowOf(first.get()).receiveResult(launched,android.app.Activity.RESULT_CANCELED,null)
        }finally{first.pause().stop().destroy()}
        val second=org.robolectric.Robolectric.buildActivity(DiPlayActivity::class.java).setup()
        try{assertNull(org.robolectric.Shadows.shadowOf(second.get()).nextStartedActivity)}finally{second.pause().stop().destroy()}
    }
    @Test fun disabledBorrowingProbeNeverStartsExternalDownload(){
        TunnelDiagnostics.start(true)
        assertEquals("disabled",TunnelDiagnostics.latest.getString("state"))
    }
    @Test fun fixedEntryDoesNotCaptureDefaultRoutesDnsOrEnableInternetProxy(){
        val p=NetworkVpnPlan.forBorrowing(false);assertFalse(p.forwarding);assertTrue(p.dns.isEmpty())
        assertTrue(p.routes.all{it.prefix==32});assertEquals(listOf(VpnNetwork("192.168.229.229",32)),p.routes)
        // Stale callers cannot capture default routes while borrowing is paused.
        val borrow=NetworkVpnPlan.forBorrowing(true);assertFalse(borrow.forwarding);assertTrue(borrow.dns.isEmpty());assertEquals(p,borrow)
    }
}
