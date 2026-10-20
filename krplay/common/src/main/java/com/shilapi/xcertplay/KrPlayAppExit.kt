package com.shilapi.xcertplay

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.shilapi.xcertplay.network.CarPlayVpnService
import com.zeekr.bridge.BorrowVpnService
import com.zeekr.bridge.KrPlayConnectionService
import com.zeekr.bridge.TunnelDiagnostics
import com.zeekr.bridge.ZeekrBridgeManager

/**
 * Handles graceful and complete shutdown of all KrPlay services, connections, and processes.
 */
object KrPlayAppExit {

    fun stopAndExit(activity: Activity) {
        val appContext = activity.applicationContext

        // 1. Disconnect iPhone and clean up CarPlay background session
        runCatching {
            ConnectionDiagnostics.disconnected("用户手动中止服务")
            CarPlayBackgroundSession.stop()
            CarPlayBackgroundSession.clear()
        }

        // 2. Stop VPN services
        runCatching {
            appContext.stopService(Intent(appContext, BorrowVpnService::class.java))
        }
        runCatching {
            appContext.stopService(Intent(appContext, CarPlayVpnService::class.java))
        }
        runCatching {
            if (hev.htproxy.TProxyService.TProxyIsRunning()) {
                hev.htproxy.TProxyService.TProxyStopService()
            }
        }

        // 3. Stop App services and bridges
        runCatching {
            TunnelDiagnostics.cancel()
        }
        runCatching {
            ZeekrBridgeManager.stop()
        }
        runCatching {
            appContext.stopService(Intent(appContext, KrPlayConnectionService::class.java))
        }
        runCatching {
            appContext.stopService(Intent(appContext, DiPlaySessionService::class.java))
        }

        // 4. Cancel all active notifications
        runCatching {
            val nm = appContext.getSystemService(NotificationManager::class.java)
            nm?.cancelAll()
        }

        // 5. Finish activity and remove task
        activity.finishAffinity()
        activity.finishAndRemoveTask()

        // 6. Complete process termination after brief pause to allow socket & service closure
        Handler(Looper.getMainLooper()).postDelayed({
            Process.killProcess(Process.myPid())
            System.exit(0)
        }, 300)
    }

    fun cleanServices(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            ConnectionDiagnostics.disconnected("退出应用")
            CarPlayBackgroundSession.stop()
            CarPlayBackgroundSession.clear()
        }
        runCatching {
            appContext.stopService(Intent(appContext, BorrowVpnService::class.java))
        }
        runCatching {
            appContext.stopService(Intent(appContext, CarPlayVpnService::class.java))
        }
        runCatching {
            if (hev.htproxy.TProxyService.TProxyIsRunning()) {
                hev.htproxy.TProxyService.TProxyStopService()
            }
        }
        runCatching {
            TunnelDiagnostics.cancel()
        }
        runCatching {
            ZeekrBridgeManager.stop()
        }
        runCatching {
            appContext.stopService(Intent(appContext, KrPlayConnectionService::class.java))
        }
        runCatching {
            appContext.stopService(Intent(appContext, DiPlaySessionService::class.java))
        }
        runCatching {
            appContext.getSystemService(NotificationManager::class.java)?.cancelAll()
        }
    }
}
