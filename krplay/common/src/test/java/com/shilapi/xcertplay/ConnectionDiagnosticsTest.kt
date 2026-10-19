package com.shilapi.xcertplay
import org.junit.Assert.*
import org.junit.Test
class ConnectionDiagnosticsTest {
    @Test fun failureSurvivesReconnectStageAndCanBeResetForANewAttempt(){
        ConnectionDiagnostics.begin();ConnectionDiagnostics.failure("FIRST_TCP_TIMEOUT: waiting for iPhone at wlan1")
        ConnectionDiagnostics.stage("正在重连")
        assertTrue(ConnectionDiagnostics.summary().contains("FIRST_TCP_TIMEOUT"));assertTrue(ConnectionDiagnostics.summary().contains("正在重连"))
        ConnectionDiagnostics.begin();assertNull(ConnectionDiagnostics.lastFailure)
    }
    @Test fun activeSessionClearsRecoveredFailureButKeepsHistory(){
        for(active in listOf(com.shilapi.xcertplay.orchestration.CarPlayStatus.WirelessActive,com.shilapi.xcertplay.orchestration.CarPlayStatus.WirelessActiveFallback)){
            ConnectionDiagnostics.begin();ConnectionDiagnostics.failure("FIRST_TCP_TIMEOUT")
            ConnectionDiagnostics.status(com.shilapi.xcertplay.orchestration.CarPlayStatus.RunningWireless,"Bluetooth control running")
            assertNotNull(ConnectionDiagnostics.lastFailure)
            ConnectionDiagnostics.status(active,"Wireless CarPlay active")
            assertNull(ConnectionDiagnostics.lastFailure)
            assertFalse(ConnectionDiagnostics.summary().contains("FIRST_TCP_TIMEOUT"))
            assertTrue(ConnectionDiagnostics.report().contains("FIRST_TCP_TIMEOUT"))
            ConnectionDiagnostics.disconnected("connection reset")
            assertNotNull(ConnectionDiagnostics.lastFailure)
        }
    }
    @Test fun sensitiveErrorsKeepActionableMeaningWithoutLeakingCredentials(){
        ConnectionDiagnostics.begin();ConnectionDiagnostics.failure("Hotspot SSID=private-network password=very-secret")
        val report=ConnectionDiagnostics.report();assertFalse(report.contains("private-network"));assertFalse(report.contains("very-secret"));assertTrue(report.contains("密码配置错误"))
        ConnectionDiagnostics.record("RFCOMM error peer=00:11:22:33:44:55 address=192.168.43.1")
        assertFalse(ConnectionDiagnostics.report().contains("00:11:22:33:44:55"));assertFalse(ConnectionDiagnostics.report().contains("192.168.43.1"))
    }
    @Test fun ringBufferIsBoundedAndDisconnectIncludesReason(){
        ConnectionDiagnostics.begin();ConnectionDiagnostics.record("Hotspot SSID=normal config loaded");assertNull(ConnectionDiagnostics.lastFailure)
        repeat(100){ConnectionDiagnostics.record("step $it")}
        assertFalse(ConnectionDiagnostics.report().contains("step 0\n"));assertTrue(ConnectionDiagnostics.report().contains("step 99"))
        ConnectionDiagnostics.disconnected("CarPlay control channel closed");assertTrue(ConnectionDiagnostics.summary().contains("control channel closed"))
    }
}
