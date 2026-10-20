package com.zeekr.bridge
import android.app.*
import android.content.Intent
import android.os.IBinder

/** Owns LAN listeners independently of iPhone authentication and VPN consent. */
class KrPlayConnectionService:Service(){
    override fun onCreate(){super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("krplay-connection","KrPlay 连接",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,com.shilapi.xcertplay.DiPlayActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        startForeground(724,Notification.Builder(this,"krplay-connection").setSmallIcon(android.R.drawable.stat_sys_upload_done).setContentTitle("KrPlay 已就绪").setContentText("等待 KrPlay-Client 连接 · 点此管理网络").setContentIntent(open).setOngoing(true).build())
        ZeekrBridgeManager.start(this)
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int)=START_NOT_STICKY
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onTaskRemoved(rootIntent: Intent?){super.onTaskRemoved(rootIntent);stopSelf()}
    override fun onDestroy(){stopService(Intent(this,BorrowVpnService::class.java));TunnelDiagnostics.cancel();ZeekrBridgeManager.stop();super.onDestroy()}
}
