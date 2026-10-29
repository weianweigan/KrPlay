package com.zeekr.bridge

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.*
import hev.htproxy.TProxyService
import java.io.File

/** Non-root device-wide TCP forwarding. KrPlay's own LAN sockets are excluded to avoid loops. */
class BorrowVpnService:VpnService() {
    private var tun:ParcelFileDescriptor?=null
    private val anchor=FixedEntryAnchor()
    private var borrowing=false
    private val handler=Handler(Looper.getMainLooper())
    private var closing=false
    private val monitor=object:Runnable {
        override fun run(){if(tun==null||closing)return
            if(!TProxyService.TProxyIsRunning()){fail("VPN 转发进程已停止");return}
            if(ZeekrBridgeManager.idleWorkers()==0)update("VPN 已建立 · 等待车载网络") else update("VPN 已建立 · 车载网络通道就绪")
            handler.postDelayed(this,2000)
        }
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if(intent?.action=="stop"){stopSelf();return START_NOT_STICKY}
        // Old launch intents also fall back to fixed-entry-only operation.
        val nextBorrowing=com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED && intent?.action!="access"
        if(tun!=null&&borrowing==nextBorrowing)return START_NOT_STICKY
        // Android has one active VPN. Reconfigure this owner without starting a second service.
        if(RootNetworkManager.isRunning)RootNetworkManager.stop()
        handler.removeCallbacks(monitor);runCatching{TProxyService.TProxyStopService()};runCatching{tun?.close()};tun=null;anchor.close();active=false;accessActive=false
        borrowing=nextBorrowing
        closing=false
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"KrPlay 网络",NotificationManager.IMPORTANCE_LOW))
        startForeground(725,notification(if(borrowing)"正在建立借网 VPN"else"正在建立固定入口 VPN"))
        try {
            ZeekrBridgeManager.start(this)
            check(prepare(this)==null){"请先在手机上允许 VPN 连接"}
            anchor.establish {
                Builder().setSession("KrPlay 固定入口").allowBypass()
                    .allowFamily(android.system.OsConstants.AF_INET).allowFamily(android.system.OsConstants.AF_INET6)
                    .addAddress(com.krplay.protocol.Discovery.FIXED_HOST,32).addRoute(com.krplay.protocol.Discovery.FIXED_HOST,32)
                    .establish()?:error("固定入口 VPN 未获授权")
            }
            if(!borrowing){
                tun=establishNetwork(NetworkVpnPlan.forBorrowing(false))
                check(addressPresent()){ "系统未保留固定地址，请使用自动查找或局域网地址" }
                accessActive=true;accessMessage="固定入口已就绪 · ${com.krplay.protocol.Discovery.FIXED_HOST}:8084"
                message="借网未启用";getSystemService(NotificationManager::class.java).notify(725,notification(accessMessage));ZeekrBridgeManager.publishStatus()
                return START_NOT_STICKY
            }
            val descriptor=establishNetwork(NetworkVpnPlan.forBorrowing(true))
            tun=descriptor
            val config=File(cacheDir,"krplay-tunnel.yml")
            config.writeText("""
                tunnel:
                  mtu: 1500
                  ipv4: 198.18.0.1
                  ipv6: 'fd00:6b72::1'
                socks5:
                  address: 127.0.0.1
                  port: 1080
                  udp: udp
                mapdns:
                  address: 198.18.0.2
                  port: 53
                  network: 198.19.0.0
                  netmask: 255.255.0.0
                  cache-size: 4096
                misc:
                  max-session-count: 64
                  connect-timeout: 5000
                  log-file: stderr
                  log-level: warn
            """.trimIndent())
            check(TProxyService.TProxyStartService(config.absolutePath,descriptor.fd)){"无法启动 VPN 转发"}
            accessActive=addressPresent();accessMessage=if(accessActive)"固定入口已就绪 · ${com.krplay.protocol.Discovery.FIXED_HOST}:8084"else"系统未保留固定入口，请使用局域网地址"
            ZeekrBridgeManager.vpnAuthorized();active=true;message="VPN 已建立 · 正在检测通道";ZeekrBridgeManager.publishStatus()
            handler.postDelayed(monitor,1000)
        }catch(e:Throwable){fail(e.message?:"无法启动 VPN")}
        return START_NOT_STICKY
    }
    private fun establishNetwork(plan:NetworkVpnPlan):ParcelFileDescriptor {
        val builder=Builder().setSession(if(plan.forwarding)"KrPlay · 车载网络"else"KrPlay 固定入口 · 不接管外网").setMtu(1500)
            .allowFamily(android.system.OsConstants.AF_INET).allowFamily(android.system.OsConstants.AF_INET6)
        plan.addresses.forEach{builder.addAddress(it.address,it.prefix)}
        plan.routes.forEach{builder.addRoute(it.address,it.prefix)}
        plan.dns.forEach{builder.addDnsServer(it)}
        if(plan.forwarding)builder.addDisallowedApplication(packageName)else builder.allowBypass()
        builder.setConfigureIntent(PendingIntent.getActivity(this,0,Intent(this,com.shilapi.xcertplay.DiPlayActivity::class.java),PendingIntent.FLAG_IMMUTABLE))
        return builder.establish()?:error("VPN 授权已撤销")
    }
    private fun notification(text:String):Notification {
        val open=PendingIntent.getActivity(this,0,Intent(this,com.shilapi.xcertplay.DiPlayActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,1,Intent(this,BorrowVpnService::class.java).setAction("stop"),PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.stat_sys_upload_done).setContentTitle(if(borrowing)"KrPlay · 车载借网"else"KrPlay · 固定入口")
            .setContentText(text).setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null,"断开",stop).build()).build()
    }
    private fun update(text:String){if(message==text)return;message=text;getSystemService(NotificationManager::class.java).notify(725,notification(text));ZeekrBridgeManager.publishStatus()}
    private fun fail(reason:String){message=reason;active=false;accessActive=false;accessMessage=reason;ZeekrBridgeManager.publishStatus();stopSelf()}
    override fun onRevoke(){message="VPN 已让位或授权已撤销";accessMessage=message;stopSelf()}
    override fun onTaskRemoved(rootIntent: Intent?){super.onTaskRemoved(rootIntent);stopSelf()}
    override fun onDestroy(){closing=true;handler.removeCallbacks(monitor)
        runCatching{TProxyService.TProxyStopService()};runCatching{tun?.close()};tun=null;anchor.close();active=false;accessActive=false;if(accessMessage.startsWith("固定入口已就绪"))accessMessage="固定入口 VPN 未开启"
        if(message.startsWith("VPN 已建立"))message="VPN 已断开"
        ZeekrBridgeManager.publishStatus();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()
    }
    private fun addressPresent()=java.net.NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any{n->n.inetAddresses.toList().any{it.hostAddress==com.krplay.protocol.Discovery.FIXED_HOST}}
    companion object {
        @Volatile var accessActive=false;internal set
        @Volatile var accessMessage="固定入口 VPN 未开启";internal set
        fun setAccessState(active:Boolean,message:String){accessActive=active;accessMessage=message}
        private const val CHANNEL="krplay-network"
        @Volatile var active=false;internal set
        @Volatile var message="VPN 未连接";internal set
        fun stats():LongArray=runCatching{if(active)TProxyService.TProxyGetStats()else LongArray(4)}.getOrDefault(LongArray(4))
    }
}
