package com.shilapi.xcertplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
/** Bounded redacted diagnostics survive Activity switches and reconnect attempts. */
internal object ConnectionDiagnostics {
    @Volatile var stage="等待连接 iPhone";private set
    @Volatile var lastFailure:String?=null;private set
    private val events=ArrayDeque<String>()
    @Synchronized fun begin(){lastFailure=null;stage="开始连接 iPhone";events.clear();record(stage)}
    @Synchronized fun stage(message:String){stage=clean(message)?:return;record(message)}
    @Synchronized fun failure(message:String){lastFailure=clean(message,true)?:"连接失败，详细字段已隐藏";record("失败：$message")}
    @Synchronized fun status(status:com.shilapi.xcertplay.orchestration.CarPlayStatus,description:String){
        when(status){
            com.shilapi.xcertplay.orchestration.CarPlayStatus.WirelessActive,
            com.shilapi.xcertplay.orchestration.CarPlayStatus.WirelessActiveFallback->lastFailure=null
            is com.shilapi.xcertplay.orchestration.CarPlayStatus.Failed->failure(description)
            else->Unit
        }
        stage(description)
    }
    fun disconnected(reason:String){stage("iPhone 已断开");failure(reason)}
    @Synchronized fun record(message:String){val safe=clean(message)?:return
        events.addLast(SimpleDateFormat("HH:mm:ss",Locale.US).format(Date())+" · "+safe)
        while(events.size>40)events.removeFirst()
        if(Regex("(?i)failed|error|exception|timeout|timed out|disconnected|connection reset|not ready").containsMatchIn(safe))lastFailure=safe
    }
    fun summary():String=stage+(lastFailure?.let{"\n最近失败：$it\n"+hint(it)}?:"")
    @Synchronized fun report()=summary()+"\n\n"+events.joinToString("\n")
    private fun clean(message:String,sensitiveFailure:Boolean=false):String? {
        val single=message.replace('\n',' ').replace('\r',' ')
        return DiagnosticRedactor.redact(single) ?: if(!sensitiveFailure) null else when {
            single.contains("password",true)||single.contains("passphrase",true)->"系统热点密码配置错误（凭据已隐藏）"
            single.contains("SSID",true)->"系统热点名称配置需要检查（名称已隐藏）"
            single.contains("certificate",true)||single.contains("private key",true)->"CarPlay 认证资源验证失败（资源内容已隐藏）"
            else->null
        }
    }
    internal fun hint(reason:String):String=when{
        reason.contains("hotspot",true)||reason.contains("interface",true)->"检查系统热点模式、热点名称与密码，以及热点接口地址。Wi-Fi 上网地址不能替代热点地址。"
        reason.contains("Bluetooth",true)||reason.contains("RFCOMM",true)->"解锁 iPhone，检查蓝牙配对、附近设备权限，再选择正确的 iPhone。"
        reason.contains("timeout",true)||reason.contains("timed out",true)->"检查 iPhone 是否加入热点、系统热点名称与密码是否一致，并查看上面的连接阶段。"
        else->"保留具体错误后重试；可在设置中导出连接诊断。"
    }
}
