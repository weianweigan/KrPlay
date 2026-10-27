// SPDX-License-Identifier: AGPL-3.0-only
// UI copy and visual language adapted from DiAuto. See docs/THIRD_PARTY_NOTICES.md.
package com.shilapi.xcertplay

import com.shilapi.xcertplay.shared.R as UiR
import com.shilapi.xcertplay.i18n.*

import android.Manifest


import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.compose.ui.platform.ComposeView
import com.shilapi.xcertplay.settings.*
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.shilapi.xcertplay.host.R
import com.shilapi.xcertplay.orchestration.WirelessHotspotMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Connection and preference pages in the shared server navigation. */
class DiPlayActivity : AppCompatActivity() {
    private val ui by lazy { ServerUi(this) }
    private val handler=Handler(Looper.getMainLooper())
    private var page="service"
    private lateinit var shell:ServerUi.Shell
    private val settingsModel by lazy { ViewModelProvider(this)[SettingsViewModel::class.java] }
    private var settingsView:ComposeView?=null
    private val settingsDialogs=SettingsDialogHost()
    private var setupError:String?=null
    private var status:TextView?=null
    private var details:TextView?=null
    private var serviceState:TextView?=null
    private var addresses:TextView?=null
    private var networkState:TextView?=null
    private var vpnButton:Button?=null
    private var connectButton:com.google.android.material.button.MaterialButton?=null
    private var disconnectButton:Button?=null
    private var pendingWireless=false
    private var notificationTransport=true
    private var exportInProgress=false
    private var initialLaunch=true
    private var vpnRequestPending=false
    private var requestedBorrow=false
    private var fixedEntryState:TextView?=null
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){connect(notificationTransport)}
    private val bluetoothPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->if(granted)choosePhone()else permissionHelp("附近设备权限","请允许蓝牙附近设备权限后重试")}
    private val export=registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")){uri->if(uri!=null)exportDiagnostics(uri)}
    private val vpnConsent=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
        vpnRequestPending=false
        if(result.resultCode==RESULT_OK)startVpn(requestedBorrow)else toast("VPN 授权已取消，仍可通过局域网自动查找连接")
        handler.post{continueInitialLaunch()}
    }
    private val oemImagePicker=registerForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)runCatching{oemImageCrop.launch(Intent(this,ImageCropActivity::class.java).setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}.onFailure{toast("无法打开图标编辑器")}}
    private val oemImageCrop=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){if(it.resultCode==RESULT_OK)render()}
    private val tick=object:Runnable{override fun run(){refreshStatus();refreshService();refreshSettingsRuntime();handler.postDelayed(this,1000)}}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
        com.krplay.update.AppUpdates.checkOnStart(this)
        ServerWindow.showSystemBars(window);refreshAuthentication()
        page=savedInstanceState?.getString("page")?:intent.getStringExtra("page")?:"service"
        createShell();startForegroundService(Intent(this,com.zeekr.bridge.KrPlayConnectionService::class.java))
        handleWirelessRecovery()
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){override fun handleOnBackPressed(){when{page=="settings"&&settingsModel.state.value.category!=null->settingsModel.open(null);page!="service"->navigate("service");else->moveTaskToBack(true)}}})
    }
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);navigate(intent.getStringExtra("page")?:"service")}
    override fun onSaveInstanceState(outState:Bundle){outState.putString("page",page);super.onSaveInstanceState(outState)}
    override fun onConfigurationChanged(newConfig:Configuration){settingsView?.disposeComposition();super.onConfigurationChanged(newConfig);createShell()}
    override fun onResume(){super.onResume();refreshAuthentication();render();ServerWindow.showSystemBars(window);handler.removeCallbacks(tick);handler.post(tick)
        if(com.zeekr.bridge.ZeekrBridgeManager.consumeVpnRequest())requestVpn()
        continueInitialLaunch()
    }
    override fun onPause(){handler.removeCallbacks(tick);super.onPause()}
    override fun onDestroy(){settingsDialogs.cancel();super.onDestroy()}
    private fun normalizedPage(value:String)=when(value){"home","wireless-recovery"->"phone";"about"->"settings";"phone","settings"->value;else->"service"}
    private fun navigate(destination:String){page=normalizedPage(destination);if(page=="settings")settingsModel.open(null);shell.select(page);if(destination=="wireless-recovery")wirelessHelp()}
    private fun createShell(){page=normalizedPage(page);shell=ui.Shell(page){page=it};shell.put("service",ui.content(::service));refreshPhone()
        settingsView=ComposeView(this).apply{id=R.id.settings_compose;setContent{SettingsRoute(settingsModel,settingsActions,settingsDialogs){shell.setPageScroll("settings",it)}}}.also{shell.put("settings",it)}
        setContentView(shell.root);refreshSettings();refreshService()
    }
    private fun render(){refreshPhone();refreshSettings()}
    private fun refreshPhone(){shell.put("phone",ui.content(::home));refreshStatus()}
    private fun refreshSettings(){settingsModel.reload();refreshSettingsRuntime()}
    private fun refreshSettingsRuntime(){settingsModel.updateRuntime(SettingsRuntime(authenticationError=setupError,exporting=exportInProgress,version=version(),audioStatus=if(AirPlayPersistence.loadAudioOutput(this)==CarPlayAudioOutput.CLIENT)"音频通过 KrPlay-Client 播放；客户端断开时静音" else "音频由本设备播放",microphoneStatus=when(AirPlayPersistence.loadMicrophoneSource(this)){CarPlayMicrophoneSource.CLIENT->"由 KrPlay-Client 提供，仅在 Siri / 通话请求时采集";CarPlayMicrophoneSource.ANDROID->"使用本设备麦克风";CarPlayMicrophoneSource.DISABLED->"麦克风已关闭"}))}
    private val settingsActions by lazy {SettingsActions(
        choice={s,i->changeSettings{applySettingsEffect(settingsModel.choose(s,i))}?.let(::toast)},
        toggle={s,v->changeSettings{applySettingsEffect(settingsModel.toggle(s,v))}?.let(::toast)},
        text={s,v->changeSettings{applySettingsEffect(settingsModel.edit(s,v))}},
        credentials={s,p->changeSettings{settingsModel.saveCredentials(s,p)}},
        margins={m->changeSettings{applySettingsEffect(settingsModel.saveMargins(m))}?.let(::toast)},
        resetBranding={v->changeSettings{settingsModel.resetBranding(v)}?.let(::toast)},
        command={c->changeSettings{settingsCommand(c)}?.let(::toast)}
    )}
    private fun changeSettings(action:()->Unit):String?=runCatching{action();refreshSettings();refreshService()}.exceptionOrNull()?.let{it.userMessage(this)}
    private fun applySettingsEffect(effect:SettingsEffect){when(effect){SettingsEffect.None->Unit;is SettingsEffect.Reconnect->if(CarPlayBackgroundSession.hasSession()){val wireless=effect.wireless?:AirPlayPersistence.loadWirelessEnabled(this);if(effect.stopBeforeConnect)CarPlayBackgroundSession.stop{runOnUiThread{connect(wireless)}}else connect(wireless)}}}
    private fun settingsCommand(command:SettingsCommand){when(command){
        SettingsCommand.SELECT_PHONE->choosePhone();SettingsCommand.HOTSPOT_SETTINGS->openCarWifiSettings()
        SettingsCommand.SELECT_ICON->oemImagePicker.launch("image/*")
        SettingsCommand.APP_PERMISSIONS->openSystem(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))
        SettingsCommand.BLUETOOTH_SETTINGS->openSystem(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        SettingsCommand.WIRELESS_HELP->wirelessHelp()
        SettingsCommand.EXPORT_REPORT->if(Build.VERSION.SDK_INT>=29)exportDiagnostics()else chooseReportDestination()
        SettingsCommand.SOURCE->com.krplay.distribution.SourceDistribution.open(this,com.krplay.distribution.SourceDistribution.REPOSITORY)
        SettingsCommand.VERSION_SOURCE->com.krplay.distribution.SourceDistribution.open(this,com.krplay.distribution.SourceDistribution.sourceUrl(this))
        SettingsCommand.LICENSES->com.krplay.distribution.SourceDistribution.showLicenses(this)
    }}
    private fun service(content:LinearLayout){
        content.addView(label("KrPlay",28,TEXT,true));content.addView(label("连接 iPhone，使用 KrPlay-Client 在车机显示 CarPlay",16,MUTED));content.addView(space(24))
        ui.card(content,"车机连接",R.drawable.ic_server_server){body->
            body.addView(label("KrPlay-Client 默认自动连接：优先固定入口 6.6.6.6，再查找网关与已知地址。请让车机宿主连接本设备热点；自动连接失败时可手动填写下方地址。",16,MUTED))
            addresses=label("正在读取局域网地址",18,ACCENT,true).apply{setTextIsSelectable(true)};body.addView(addresses)
            serviceState=label("",15,TEXT);body.addView(serviceState)
            body.addView(label("Wi-Fi 与热点同时开启时可能有多个地址：车机连本设备热点时用热点接口地址；连接同一 Wi-Fi 时用 Wi-Fi 接口地址。",14,MUTED))
        }
        ui.card(content,"固定连接入口",R.drawable.ic_server_wifi){body->
            fixedEntryState=label("",15,TEXT);body.addView(fixedEntryState)
            val automatic=android.widget.Switch(this).apply{text="启动时开启固定入口 VPN";setTextColor(TEXT);isChecked=com.zeekr.bridge.NetworkAccessSettings.automatic(this@DiPlayActivity)
                setOnCheckedChangeListener{_,on->com.zeekr.bridge.NetworkAccessSettings.automatic(this@DiPlayActivity,on);if(on)requestVpn(false)else if(!com.zeekr.bridge.BorrowVpnService.active)stopService(Intent(this@DiPlayActivity,com.zeekr.bridge.BorrowVpnService::class.java))}}
            body.addView(automatic)
            body.addView(button("开启 / 重试固定入口",false){requestVpn(false)},matchButton(12))
            body.addView(label("首次需要系统 VPN 授权。固定入口不接管外网。虚拟机仍需能路由到热点，固定 IP 无法绕过宿主的网络隔离。",14,MUTED))
        }
        ui.card(content,"iPhone 连接",R.drawable.ic_server_phone){body->
            body.addView(button("管理 iPhone",true,R.drawable.ic_server_phone){navigate("phone")},matchButton())
            body.addView(label("先在系统蓝牙配对 iPhone，再到 iPhone 页选择设备并连接。已有系统热点请使用系统热点模式，并填写准确的热点名称与密码。",15,MUTED))
        }
        if(com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED)ui.card(content,"车载借网",R.drawable.ic_server_wifi){body->
            networkState=label("",15,TEXT);body.addView(networkState)
            vpnButton=button("连接车载网络 VPN",true){if(com.zeekr.bridge.BorrowVpnService.active){if(com.zeekr.bridge.NetworkAccessSettings.automatic(this))startVpn(false)else stopService(Intent(this,com.zeekr.bridge.BorrowVpnService::class.java))}else requestVpn()};body.addView(vpnButton,matchButton(12))
            body.addView(button("检测通道",false){com.zeekr.bridge.TunnelDiagnostics.start(false)},matchButton(12))
            body.addView(button("下载测速 · 最多 8 MiB",false){MaterialAlertDialogBuilder(this).setTitle("使用车载流量测速").setMessage("通过车载网络下载最多 8 MiB，仅检测当前通道下载速度。").setNegativeButton("取消",null).setPositiveButton("开始"){_,_->com.zeekr.bridge.TunnelDiagnostics.start(true)}.show()},matchButton(12))
            body.addView(label("CarPlay 和借网 VPN 无需 root。借网支持 TCP / DNS，UDP 暂未支持；设备画面和触控需 root。",14,MUTED))
        }
        ui.card(content,"中止服务",R.drawable.ic_media_stop){body->
            body.addView(label("断开 iPhone 与车机连接，停止固定入口 VPN，停止所有后台服务，清理相关内容并退出应用。",15,MUTED))
            val exitButton=button("中止服务并退出",false,R.drawable.ic_media_stop){promptStopServicesAndExit()}
            exitButton.setTextColor(android.graphics.Color.parseColor("#F2B8B5"))
            exitButton.strokeColor=android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B3261E"))
            body.addView(exitButton,matchButton(12))
        }
        setupError?.let{content.addView(label(it,16,WARNING))}
    }
    private fun promptStopServicesAndExit(){
        MaterialAlertDialogBuilder(this)
            .setTitle("中止服务")
            .setMessage("确定要断开 iPhone 连接、停止所有 VPN 及后台服务，并完整退出应用吗？")
            .setPositiveButton("中止并退出"){_,_->KrPlayAppExit.stopAndExit(this)}
            .setNegativeButton("取消",null)
            .show()
    }
    private fun refreshService(){
        val list=com.shilapi.xcertplay.connection.DeviceAddresses.discover(this)
        addresses?.text=list.joinToString("\n"){"${it.address} · ${it.interfaceName}"}.ifEmpty{"没有可用局域网地址，请开启热点或连接 Wi-Fi"}
        fixedEntryState?.text=com.zeekr.bridge.BorrowVpnService.accessMessage
        serviceState?.text="${if(com.zeekr.bridge.ZeekrBridgeManager.clientConnected())"KrPlay-Client 已连接"else"等待 KrPlay-Client 连接"}\n${ConnectionDiagnostics.stage}"
        vpnButton?.text=if(com.zeekr.bridge.BorrowVpnService.active)"断开车载网络 VPN"else"连接车载网络 VPN"
        networkState?.text=com.zeekr.bridge.BorrowVpnService.message+"\n可用通道：${com.zeekr.bridge.ZeekrBridgeManager.idleWorkers()}\n"+com.zeekr.bridge.TunnelDiagnostics.description()
    }
    private fun continueInitialLaunch(){if(initialLaunch&&!vpnRequestPending&&!ensureAutomaticEntry()){initialLaunch=false;if(setupError==null&&DiPlayPreferences.autoConnect(this)&&!CarPlayBackgroundSession.hasSession()&&intent.getStringExtra("page")==null)handler.post{connect(AirPlayPersistence.loadWirelessEnabled(this))}}}
    private fun ensureAutomaticEntry():Boolean {
        if(!com.zeekr.bridge.NetworkAccessSettings.automatic(this)||!AirPlayPersistence.loadWirelessEnabled(this)||com.zeekr.bridge.BorrowVpnService.accessActive)return false
        val cm=getSystemService(android.net.ConnectivityManager::class.java)
        if(cm.allNetworks.any{cm.getNetworkCapabilities(it)?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)==true})return false
        val consent=android.net.VpnService.prepare(this)
        if(consent==null){startVpn(false);return false}
        if(com.zeekr.bridge.NetworkAccessSettings.asked(this))return false
        requestedBorrow=false;vpnRequestPending=true;com.zeekr.bridge.NetworkAccessSettings.markAsked(this);vpnConsent.launch(consent);return true
    }
    private fun requestVpn(borrow:Boolean=false){if(vpnRequestPending||borrow&&!com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED)return
        if(!borrow&&!AirPlayPersistence.loadWirelessEnabled(this)){toast("USB 连接使用独立 VPN；请使用自动查找或切换到系统热点后启用固定入口");return}
        requestedBorrow=borrow;val consent=android.net.VpnService.prepare(this)
        if(consent==null)startVpn(borrow)else{vpnRequestPending=true;com.zeekr.bridge.NetworkAccessSettings.markAsked(this);vpnConsent.launch(consent)}
    }
    private fun startVpn(borrow:Boolean=false){startForegroundService(Intent(this,com.zeekr.bridge.BorrowVpnService::class.java).apply{if(!borrow)action="access"})}
    private fun home(content:LinearLayout){
        content.addView(label("连接 iPhone",28,TEXT,true));content.addView(label("蓝牙完成无线握手，iPhone 随后连接本设备的热点接收 CarPlay。Wi-Fi 上网与热点可并发的设备不需要先断开 Wi-Fi。",16,MUTED));content.addView(space(24))
        ui.card(content,"连接状态",R.drawable.ic_server_phone){body->status=label("",20,ACCENT,true);body.addView(status);details=label("",15,WARNING).apply{setTextIsSelectable(true)};body.addView(details);body.addView(button("查看连接详情",false){MaterialAlertDialogBuilder(this).setTitle("连接诊断").setMessage(ConnectionDiagnostics.report()).setPositiveButton("关闭",null).show()},matchButton(12))
            val exitButton=button("中止服务并退出",false,R.drawable.ic_media_stop){promptStopServicesAndExit()}
            exitButton.setTextColor(android.graphics.Color.parseColor("#F2B8B5"))
            exitButton.strokeColor=android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B3261E"))
            body.addView(exitButton,matchButton(12))
        }
        ui.card(content,"无线连接",R.drawable.ic_server_wifi){body->
            body.addView(ui.preference("iPhone",selectedPhoneName()){choosePhone()},matchButton())
            connectButton=button("连接 iPhone",true,R.drawable.ic_server_phone){connect(true)};body.addView(connectButton,matchButton(12))
            disconnectButton=button("断开 iPhone",false){ConnectionDiagnostics.disconnected("用户手动断开");CarPlayBackgroundSession.stop{runOnUiThread{refreshStatus()}}};body.addView(disconnectButton,matchButton(12))
            body.addView(button("热点与连接设置",false){navigate("settings");settingsModel.open(SettingsCategory.CONNECTION)},matchButton(12))
            val hotspotOn=com.shilapi.xcertplay.network.CarHotspotStatus.isEnabled(this)
            val configured=com.shilapi.xcertplay.orchestration.ManualHotspotValidation.validate(AirPlayPersistence.loadManualHotspotSsid(this),AirPlayPersistence.loadManualHotspotPassphrase(this))==null
            body.addView(label(if(!configured)"尚未配置热点：先开启系统热点，再填写相同的名称与密码。" else if(hotspotOn==false)"系统热点未开启：请在系统设置开启热点后重试。" else if(hotspotOn==null)"请确认系统热点已开启；此设备不允许读取热点开关状态。" else "系统热点已开启，请确认系统热点名称与密码和 KrPlay 配置一致。",15,if(!configured||hotspotOn!=true)WARNING else MUTED))
            body.addView(button("打开热点设置",false){openCarWifiSettings()},matchButton(12))
        }
        ui.card(content,"USB 连接",R.drawable.ic_server_usb){body->body.addView(label("使用数据线将 iPhone 连接此安卓设备。",15,MUTED));body.addView(button("通过 USB 连接",false){connect(false)},matchButton())}
    }
    private fun refreshAuthentication(){setupError=runCatching{DiPlayBootstrap.ensure(this)}.exceptionOrNull()?.let{"内置 CarPlay 认证资源无法加载：${it.userMessage(this)}。请安装包含认证资源的 KrPlay 版本。"}}
    private fun connect(wireless:Boolean){refreshAuthentication();if(setupError!=null){toast(setupError!!);return}
        if(wireless && !checkHotspotReady())return
        if(wireless&&DiPlayPreferences.phoneAddress(this)==null){pendingWireless=true;choosePhone();return}
        val p=getSharedPreferences("diplay",MODE_PRIVATE)
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED&&!p.getBoolean("notification_asked",false)){p.edit().putBoolean("notification_asked",true).apply();notificationTransport=wireless;notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);return}
        ConnectionDiagnostics.begin();AirPlayPersistence.saveWirelessEnabled(this,wireless)
        val open={startActivity(Intent(this,CarPlayHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION))}
        if(CarPlayBackgroundSession.hasSession())CarPlayBackgroundSession.stop{runOnUiThread{open()}}else open()
    }
    private fun refreshStatus(){val running=CarPlayBackgroundSession.hasSession();status?.text=when{setupError!=null->"认证资源加载失败";CarPlayBackgroundSession.active->"CarPlay 已连接";running->"正在连接 iPhone";else->"iPhone 未连接"}
        details?.text=setupError?:ConnectionDiagnostics.summary();connectButton?.visibility=if(running)View.GONE else View.VISIBLE;disconnectButton?.visibility=if(running)View.VISIBLE else View.GONE;connectButton?.isEnabled=setupError==null
    }
    private fun choosePhone() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT); return
        }
        val adapter = getSystemService(BluetoothManager::class.java)?.adapter
        if (adapter == null || !adapter.isEnabled) {
            ConnectionDiagnostics.failure("Bluetooth 不可用或未开启，请打开蓝牙后重新配对");refreshStatus()
            MaterialAlertDialogBuilder(this).setTitle(getString(UiR.string.ui_turn_on_bluetooth))
                .setMessage(getString(UiR.string.ui_turn_on_bluetooth_on_this_android_device_and_pair))
                .setPositiveButton(getString(UiR.string.ui_open_bluetooth_settings)) { _, _ -> openSystem(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                .setNegativeButton(getString(UiR.string.ui_later), null).show(); return
        }
        val devices = runCatching { adapter.bondedDevices.sortedBy { it.name ?: "" } }.getOrDefault(emptyList())
        if (devices.isEmpty()) {
            ConnectionDiagnostics.failure("Bluetooth 没有已配对设备，请先在系统蓝牙配对 iPhone");refreshStatus()
            MaterialAlertDialogBuilder(this).setTitle(getString(UiR.string.ui_pair_your_iphone_first))
                .setMessage(getString(UiR.string.ui_pair_this_android_device_in_settings_bluetooth_on_your))
                .setPositiveButton(getString(UiR.string.ui_open_bluetooth_settings)) { _, _ -> openSystem(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                .setNegativeButton(getString(UiR.string.ui_ok), null).show(); return
        }
        MaterialAlertDialogBuilder(this).setTitle(getString(UiR.string.ui_choose_iphone))
            .setItems(devices.map { device ->
                val name = device.name ?: getString(UiR.string.ui_paired_device)
                if (devices.count { it.name == device.name } > 1) "$name · ${device.address.takeLast(5)}" else name
            }.toTypedArray()) { _, index ->
                val device = devices[index]
                DiPlayPreferences.savePhone(this, device.address, device.name ?: "iPhone")
                val start = pendingWireless; pendingWireless = false
                render()
                if (start) connect(true)
            }.setNeutralButton(getString(UiR.string.ui_pair_another_device)) { _, _ -> openSystem(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            .setNegativeButton(getString(UiR.string.ui_cancel)) { _, _ -> pendingWireless = false }.show()
    }


    private fun wirelessHelp() {
        MaterialAlertDialogBuilder(this).setTitle("系统热点连接指南")
            .setMessage("1. 在本设备系统设置中开启热点。\n2. 在 KrPlay 的连接设置填写完全相同的热点名称与密码；开放热点留空密码。\n3. 在系统蓝牙配对 iPhone，再返回选择并连接。\n支持并发的设备可保持 Wi-Fi 上网，也可改用 USB 数据线连接。")
            .setPositiveButton("打开热点设置") { _, _ -> openCarWifiSettings() }
            .setNeutralButton("配置热点") { _, _ -> openHotspotConfiguration() }
            .setNegativeButton("关闭",null).show()
    }
    private fun handleWirelessRecovery() {
        if(intent.getStringExtra("page")!="wireless-recovery")return
        intent.removeExtra("page");navigate("phone");wirelessHelp()
    }
    private fun openHotspotConfiguration(){navigate("settings");settingsModel.open(SettingsCategory.CONNECTION)}
    private fun checkHotspotReady():Boolean {
        return when(HotspotConnectionGuide.issue(com.shilapi.xcertplay.network.CarHotspotStatus.isEnabled(this),AirPlayPersistence.loadManualHotspotSsid(this),AirPlayPersistence.loadManualHotspotPassphrase(this))){
            HotspotConnectionGuide.Issue.DISABLED->{carHotspotOffDialog();false}
            HotspotConnectionGuide.Issue.CONFIGURATION->{
                ConnectionDiagnostics.failure("系统热点名称或密码尚未正确配置，请填写与系统热点一致的配置");refreshStatus()
                MaterialAlertDialogBuilder(this).setTitle("先配置系统热点")
                    .setMessage("请在连接设置中点击“配置系统热点”，填写与本设备系统热点完全一致的名称和密码。开放热点的密码留空。配置完成并开启热点后，再连接 iPhone。")
                    .setPositiveButton("配置热点"){_,_->openHotspotConfiguration()}
                    .setNeutralButton("打开热点设置"){_,_->openCarWifiSettings()}
                    .setNegativeButton("取消",null).show();false
            }
            null->true
        }
    }

    private fun reportFileName() = "KrPlay-${SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())}.txt"


    private fun chooseReportDestination() {
        // Some head units omit or disable DocumentsUI. Launch itself can throw, before
        // the result callback and the background writer's exception handler ever run.
        runCatching { export.launch(reportFileName()) }.onFailure {
            toast(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                getString(UiR.string.ui_could_not_open_the_save_location_try_saving_to)
                else getString(UiR.string.ui_no_file_picker_is_available_on_this_device))
        }
    }


    private fun exportDiagnostics(uri: Uri? = null) {
        if (exportInProgress) return
        exportInProgress = true
        refreshSettingsRuntime()
        val appContext = applicationContext
        val fileName = reportFileName()
        Thread({
            val result = runCatching {
                val report = buildString {
                    appendLine("KrPlay ${version()} · private beta diagnostic report")
                    appendLine("Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}")
                    appendLine("Head unit: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Connection: ${if (AirPlayPersistence.loadWirelessEnabled(appContext)) "wireless" else "USB"}")
                    appendLine("Authentication: local experimental beta identity; no remote fallback")
                    appendLine("Saved video preference (may differ from active session): ${if (AirPlayPersistence.loadHevcEnabled(appContext)) "HEVC" else "H.264"}; ${AirPlayPersistence.loadFps(appContext)} fps")
                    appendLine("CarPlay size: ${com.shilapi.xcertplay.airplay.CarPlaySize.fromWidthMillimeters(AirPlayPersistence.loadWidthPhysicalMm(appContext)).name}")
                    appendLine("Saved resolution preference (may differ from active session): ${AirPlayPersistence.loadDisplayScaleTenths(appContext) * 10}%")
                    appendLine("Session: ${if (CarPlayBackgroundSession.active) "active" else if (CarPlayBackgroundSession.hasSession()) "connecting" else "stopped"}")
                    appendLine("Head-unit board: ${Build.BOARD}; hardware: ${Build.HARDWARE}; build: ${Build.DISPLAY}")
                    appendLine()
                    appendLine(ConnectionDiagnostics.report())
                    appendLine()
                    appendLine("--- Last display negotiation (timestamps distinguish it from current settings) ---")
                    appendLine(DisplayDiagnosticSnapshot.report(appContext))
                    appendLine()
                    for (name in SessionLogFile.REPORT_NAMES) {
                        val file = File(appContext.filesDir, "logs/$name")
                        if (file.isFile) {
                            appendLine("--- $name ---")
                            file.useLines { lines -> lines.forEach { line -> DiagnosticRedactor.redact(line)?.let { appendLine(it) } } }
                        }
                    }
                }
                if (uri != null) DiagnosticExportStore.write(appContext.contentResolver, uri, report)
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    DiagnosticExportStore.saveToDownloads(appContext.contentResolver, fileName, report)
                } else error("A save location is required")
            }
            runOnUiThread {
                exportInProgress = false
                if (isFinishing || isDestroyed) return@runOnUiThread
                refreshSettingsRuntime()
                if (result.isSuccess) {
                    MaterialAlertDialogBuilder(this).setTitle(getString(UiR.string.ui_diagnostic_report_saved))
                        .setMessage(if (uri == null) "Downloads/KrPlay/$fileName" else getString(UiR.string.ui_report_saved_to_the_selected_location))
                        .setPositiveButton(getString(UiR.string.ui_done), null).show()
                } else {
                    MaterialAlertDialogBuilder(this).setTitle(getString(UiR.string.ui_could_not_save_report))
                        .setMessage(getString(UiR.string.ui_check_storage_space_or_choose_another_save_location))
                        .setPositiveButton(getString(UiR.string.ui_choose_location)) { _, _ -> chooseReportDestination() }
                        .setNegativeButton(getString(UiR.string.ui_close), null).show()
                }
            }
        }, "diplay-export").start()
    }

    private fun permissionHelp(title: String, body: String) {
        MaterialAlertDialogBuilder(this).setTitle(title).setMessage(body).setPositiveButton(getString(UiR.string.ui_app_settings)) { _, _ ->
            openSystem(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }.setNegativeButton(getString(UiR.string.ui_later), null).show()
    }

    private fun openSystem(intent: Intent) { runCatching { startActivity(intent) }.onFailure { toast(getString(UiR.string.ui_open_this_option_in_the_device_s_system_settings)) } }

    private fun toast(message: String) { com.google.android.material.snackbar.Snackbar.make(shell.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show() }

    private fun version() = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.1.0-beta.1"

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(-1, -2) }

    private fun label(value: String, size: Int, color: Int, bold: Boolean = false) = ui.text(value, size, color, bold)

    private fun button(title: String, primary: Boolean, @androidx.annotation.DrawableRes iconRes: Int = 0, click: () -> Unit) = ui.button(title, primary, iconRes, click)

    private fun matchButton(top: Int = 0) = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }

    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private val isDark get() = com.krplay.protocol.UiPalette.isDark(resources.configuration.uiMode)
    private val ACCENT get() = com.krplay.protocol.UiPalette.accent(isDark)
    private val TEXT get() = com.krplay.protocol.UiPalette.text(isDark)
    private val MUTED get() = com.krplay.protocol.UiPalette.muted(isDark)
    private val WARNING get() = com.krplay.protocol.UiPalette.warning(isDark)

    private fun carHotspotOffDialog() {
        ConnectionDiagnostics.failure("系统热点未开启，请先到系统设置开启热点");refreshStatus()
        MaterialAlertDialogBuilder(this).setTitle("请先开启系统热点")
            .setMessage("KrPlay 通过本设备的系统热点连接 iPhone。请在系统设置中开启热点，并确认 KrPlay 配置的热点名称与密码一致。支持并发的设备可保持 Wi-Fi 上网；也可使用 USB 数据线。")
            .setPositiveButton("打开热点设置"){_,_->openCarWifiSettings()}
            .setNeutralButton("配置热点"){_,_->openHotspotConfiguration()}
            .setNegativeButton("取消",null).show()
    }

    // BYD maps the AOSP tether action to its own hotspot screen; other firmware falls back to Wi-Fi settings.
    // BYD shows that screen as a dialog and closes it unless its own settings or the car home screen is on top,
    // so the home screen goes first.

    private fun openCarWifiSettings() {
        val hotspot = Intent("com.android.settings.WIFI_TETHER_SETTINGS")
        val target = packageManager.resolveActivity(hotspot, 0)?.activityInfo?.packageName
        if (target == null) {
            openSystem(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            return
        }
        if (target == "com.byd.carsettings") {
            runCatching { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)) }
        }
        if (runCatching { startActivity(hotspot) }.isSuccess) return
        openSystem(Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }


    private fun selectedPhoneName() = if (DiPlayPreferences.phoneAddress(this) == null) getString(UiR.string.ui_not_selected) else DiPlayPreferences.phoneName(this)


}
