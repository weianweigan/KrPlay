package com.zeekr.bridge

import android.content.Context
import android.util.Log
import java.io.DataOutputStream
import java.util.concurrent.Executors

/**
 * Manages system-level network configuration via Root (su) for the fixed entry host (6.6.6.6)
 * and Tesla host (192.168.229.229) without taking up the Android VpnService slot.
 */
object RootNetworkManager {
    private const val TAG = "KrPlay-RootNet"
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile var isRunning: Boolean = false; private set

    fun start(context: Context, onComplete: ((Boolean, String) -> Unit)? = null) {
        executor.execute {
            try {
                // Ensure any running BorrowVpnService is stopped so both don't conflict
                context.stopService(android.content.Intent(context, BorrowVpnService::class.java))

                val script = """
                    ip addr add 6.6.6.6/32 dev lo 2>/dev/null || true
                    ip addr add 192.168.229.229/32 dev lo 2>/dev/null || true
                    sysctl -w net.ipv4.conf.all.route_localnet=1 2>/dev/null || true
                    sysctl -w net.ipv4.ip_forward=1 2>/dev/null || true
                    for iface in $(ip -o link show 2>/dev/null | awk -F': ' '{print $2}' | grep -E '^(wlan|ap|swlan|eth)'); do
                        ip addr add 6.6.6.6/32 dev "${'$'}iface" 2>/dev/null || true
                        ip addr add 192.168.229.229/32 dev "${'$'}iface" 2>/dev/null || true
                    done
                    iptables -t nat -N KRPLAY_ROOT_PRE 2>/dev/null || true
                    iptables -t nat -F KRPLAY_ROOT_PRE 2>/dev/null || true
                    iptables -t nat -A KRPLAY_ROOT_PRE -d 6.6.6.6 -p tcp -m multiport --dports 8081,8082,8083,8084,8085 -j REDIRECT 2>/dev/null || true
                    iptables -t nat -A KRPLAY_ROOT_PRE -d 192.168.229.229 -p tcp -m multiport --dports 8081,8082,8083,8084,8085 -j REDIRECT 2>/dev/null || true
                    iptables -t nat -C PREROUTING -j KRPLAY_ROOT_PRE 2>/dev/null || iptables -t nat -I PREROUTING -j KRPLAY_ROOT_PRE 2>/dev/null || true
                """.trimIndent()

                val process = ProcessBuilder("su", "-c", "sh").start()
                DataOutputStream(process.outputStream).use { out ->
                    out.write(script.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    isRunning = true
                    BorrowVpnService.setAccessState(true, "固定入口已就绪（Root 系统级网络）· 6.6.6.6:8084")
                    ZeekrBridgeManager.publishStatus()
                    Log.i(TAG, "Root network configuration applied successfully")
                    onComplete?.invoke(true, "Root 系统级网络配置成功 · 固定入口就绪")
                } else {
                    isRunning = false
                    val err = process.errorStream.bufferedReader().readText().trim()
                    Log.e(TAG, "Root network configuration failed with exit code $exitCode: $err")
                    BorrowVpnService.setAccessState(false, "Root 网络配置失败: exit $exitCode")
                    ZeekrBridgeManager.publishStatus()
                    onComplete?.invoke(false, "Root 网络配置失败: $err")
                }
            } catch (e: Exception) {
                isRunning = false
                Log.e(TAG, "Error executing root network configuration", e)
                BorrowVpnService.setAccessState(false, "Root 网络配置异常: ${e.message}")
                ZeekrBridgeManager.publishStatus()
                onComplete?.invoke(false, "Root 网络异常: ${e.message}")
            }
        }
    }

    fun stop(onComplete: ((Boolean) -> Unit)? = null) {
        executor.execute {
            try {
                val script = """
                    ip addr del 6.6.6.6/32 dev lo 2>/dev/null || true
                    ip addr del 192.168.229.229/32 dev lo 2>/dev/null || true
                    for iface in $(ip -o link show 2>/dev/null | awk -F': ' '{print $2}' | grep -E '^(wlan|ap|swlan|eth)'); do
                        ip addr del 6.6.6.6/32 dev "${'$'}iface" 2>/dev/null || true
                        ip addr del 192.168.229.229/32 dev "${'$'}iface" 2>/dev/null || true
                    done
                    iptables -t nat -D PREROUTING -j KRPLAY_ROOT_PRE 2>/dev/null || true
                    iptables -t nat -F KRPLAY_ROOT_PRE 2>/dev/null || true
                    iptables -t nat -X KRPLAY_ROOT_PRE 2>/dev/null || true
                """.trimIndent()

                val process = ProcessBuilder("su", "-c", "sh").start()
                DataOutputStream(process.outputStream).use { out ->
                    out.write(script.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
                process.waitFor()
                isRunning = false
                BorrowVpnService.setAccessState(false, "固定入口未开启")
                ZeekrBridgeManager.publishStatus()
                Log.i(TAG, "Root network configuration cleaned up")
                onComplete?.invoke(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up root network configuration", e)
                isRunning = false
                BorrowVpnService.setAccessState(false, "固定入口未开启")
                ZeekrBridgeManager.publishStatus()
                onComplete?.invoke(false)
            }
        }
    }
}
