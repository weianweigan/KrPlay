package com.krplay.client;
import android.app.*;
import androidx.appcompat.app.AlertDialog;
import com.krplay.protocol.UiPalette;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

public final class MainActivity extends Activity implements SurfaceHolder.Callback {
    private SurfaceView video;private FrameLayout stage;private TextView status,waiting,networkText,performanceHud,performanceText;
    private android.net.wifi.WifiManager.WifiLock wifiLock;
    private ClientAudio audio;private LinkDiagnostics diagnostics;private final com.krplay.protocol.PerformanceStats clock=new com.krplay.protocol.PerformanceStats();
    private final Handler ticker=new Handler(Looper.getMainLooper());private boolean showStatus,showPerformance;private String linkResult="尚未进行局域网测试";private Dialog performanceDialog;
    private android.net.ConnectivityManager.NetworkCallback lanChanges;
    private final Runnable reconnectNetwork=()->{if(this.active){stopConnections();startConnections();}};
    private AutoDiscovery discovery;private boolean automaticConnection;private int connectionGeneration;
    private int videoProfile;private long lastFrames,lastBytes,lastTick;private double fps,mbps;private int diagnosticTick;
    private TouchControlClient control;private NaluStreamPlayer player;private ReverseSocks5Worker socks;
    private LinearLayout dock,carButton,appsButton;private ImageButton handle;private Dialog networkDialog;
    private int streamWidth=1920,streamHeight=1080;private boolean active,collapsed,exiting;private String host;
    private JSONObject remote=new JSONObject();
    private int currentMode=0;
    private final List<DockItem> dockItems=new ArrayList<>();
    private static class DockItem {
        final ImageView iconView;final TextView captionView;final String iconName;
        DockItem(ImageView iconView,TextView captionView,String iconName){this.iconView=iconView;this.captionView=captionView;this.iconName=iconName;}
    }

    private boolean isDark(){return UiPalette.isDark(getResources().getConfiguration().uiMode);}

    @Override public void onCreate(Bundle b){super.onCreate(b);com.krplay.update.AppUpdates.checkOnStart(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ClientWindowInsets.configure(getWindow());
        if(!getPreferences(0).getBoolean("overlayOptionsV1",false))getPreferences(0).edit().putBoolean("overlayOptionsV1",true).putBoolean("showStatus",false).putBoolean("showPerformance",false).apply();
        videoProfile=getPreferences(0).getInt("videoProfile",2);automaticConnection=getPreferences(0).getBoolean("automaticConnection",true);host=automaticConnection?com.krplay.protocol.Discovery.FIXED_HOST:getPreferences(0).getString("host",com.krplay.protocol.Discovery.FIXED_HOST);collapsed=getPreferences(0).getBoolean("collapsed",false);
        boolean dark=isDark();
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(UiPalette.background(dark));stage=new FrameLayout(this);root.addView(stage,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout controls=new FrameLayout(this);root.addView(controls,new FrameLayout.LayoutParams(-1,-1));
        video=new SurfaceView(this);video.getHolder().addCallback(this);stage.addView(video,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        waiting=text("KrPlay-Client\n\n连接你的安卓设备\n自动查找 KrPlay · 固定入口 6.6.6.6\n若失败，可在右侧连接设置中手动指定地址",18,UiPalette.muted(dark));waiting.setGravity(Gravity.CENTER);waiting.setPadding(dp(30),dp(24),dp(30),dp(24));waiting.setBackground(round(UiPalette.overlayCard(dark),24,UiPalette.border(dark)));controls.addView(waiting,new FrameLayout.LayoutParams(Math.min(dp(420),getResources().getDisplayMetrics().widthPixels-dp(128)),-2,Gravity.CENTER));
        status=text("●  等待连接 · "+host,13,UiPalette.muted(dark));status.setPadding(dp(16),dp(10),dp(16),dp(10));status.setBackground(round(UiPalette.overlayBadge(dark),20,UiPalette.border(dark)));FrameLayout.LayoutParams badge=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);badge.setMargins(dp(20),dp(18),dp(20),0);controls.addView(status,badge);showStatus=getPreferences(0).getBoolean("showStatus",false);status.setVisibility(showStatus?View.VISIBLE:View.GONE);
        performanceHud=text("",12,UiPalette.accent(dark));performanceHud.setPadding(dp(12),dp(8),dp(12),dp(8));performanceHud.setBackground(round(UiPalette.overlayBadge(dark),16,UiPalette.border(dark)));FrameLayout.LayoutParams hud=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START);hud.setMargins(dp(20),0,dp(118),dp(18));controls.addView(performanceHud,hud);showPerformance=getPreferences(0).getBoolean("showPerformance",false);performanceHud.setVisibility(showPerformance?View.VISIBLE:View.GONE);
        dock=new LinearLayout(this);dock.setOrientation(LinearLayout.VERTICAL);dock.setPadding(dp(8),dp(10),dp(8),dp(10));dock.setBackground(round(UiPalette.overlaySurface(dark),24,UiPalette.border(dark)));dock.setElevation(dp(12));
        carButton=button(dock,"car","CarPlay",()->{control.mode(0);select(0);});
        appsButton=button(dock,"apps","设备画面",()->{if(!remote.optBoolean("rootScreen")){Toast.makeText(this,"设备画面与远程触控需要 root；CarPlay 无需 root",Toast.LENGTH_LONG).show();return;}control.mode(1);select(1);});appsButton.setVisibility(View.GONE);
        button(dock,"voice","Siri",()->control.key(KeyEvent.KEYCODE_ASSIST));button(dock,"back","返回",()->control.key(KeyEvent.KEYCODE_BACK));button(dock,"home","主页",()->control.key(KeyEvent.KEYCODE_HOME));
        button(dock,"performance","性能",this::showPerformance);if(com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED)button(dock,"network","网络",this::showNetwork);button(dock,"settings","连接设置",this::settings);button(dock,"hide","收起",()->collapse(true));
        button(dock,"exit","退出",this::exitApp,false);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);scroll.addView(dock);
        FrameLayout.LayoutParams rail=new FrameLayout.LayoutParams(dp(92),Math.min(dp(570),getResources().getDisplayMetrics().heightPixels-dp(36)),Gravity.RIGHT|Gravity.CENTER_VERTICAL);rail.setMargins(0,dp(18),dp(18),dp(18));controls.addView(scroll,rail);
        handle=new ImageButton(this);handle.setImageDrawable(new DockIcon("show",UiPalette.accent(dark)));handle.setContentDescription("展开控制侧栏");handle.setPadding(dp(10),dp(14),dp(10),dp(14));handle.setBackground(round(UiPalette.overlayCard(dark),16,UiPalette.border(dark)));handle.setOnClickListener(v->collapse(false));
        FrameLayout.LayoutParams tab=new FrameLayout.LayoutParams(dp(44),dp(64),Gravity.RIGHT|Gravity.CENTER_VERTICAL);tab.rightMargin=dp(8);controls.addView(handle,tab);
        ClientWindowInsets.fit(root,controls);setContentView(root);collapse(collapsed);select(0);
        lanChanges=new android.net.ConnectivityManager.NetworkCallback(){
            @Override public void onAvailable(android.net.Network n){if(active&&LocalTransport.network==null){ticker.removeCallbacks(reconnectNetwork);ticker.postDelayed(reconnectNetwork,500);}}
            @Override public void onLost(android.net.Network n){if(active&&java.util.Objects.equals(LocalTransport.network,n)){LocalTransport.network=null;ticker.removeCallbacks(reconnectNetwork);ticker.postDelayed(reconnectNetwork,500);}}
        };
        try{((android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).registerNetworkCallback(new android.net.NetworkRequest.Builder().addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI).addTransportType(android.net.NetworkCapabilities.TRANSPORT_ETHERNET).build(),lanChanges);}catch(RuntimeException ignored){}
        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},41);
        video.setOnTouchListener((v,e)->control!=null&&control.touch(e,v.getWidth(),v.getHeight()));stage.addOnLayoutChangeListener((v,l,t,r,bt,ol,ot,or,ob)->{fit();if(active)sendViewport();});
    }

    @Override public void onConfigurationChanged(android.content.res.Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        applyTheme();
    }

    private void applyTheme(){
        boolean dark=isDark();
        int bg=UiPalette.background(dark),border=UiPalette.border(dark),text=UiPalette.text(dark),muted=UiPalette.muted(dark),accent=UiPalette.accent(dark);
        if(stage!=null)stage.setBackgroundColor(bg);
        if(waiting!=null){waiting.setTextColor(muted);waiting.setBackground(round(UiPalette.overlayCard(dark),24,border));}
        if(status!=null){status.setTextColor(muted);status.setBackground(round(UiPalette.overlayBadge(dark),20,border));}
        if(performanceHud!=null){performanceHud.setTextColor(accent);performanceHud.setBackground(round(UiPalette.overlayBadge(dark),16,border));}
        if(dock!=null){
            dock.setBackground(round(UiPalette.overlaySurface(dark),24,border));
            for(DockItem item:dockItems){
                item.iconView.setImageDrawable(new DockIcon(item.iconName,text));
                item.captionView.setTextColor(text);
            }
            select(currentMode);
        }
        if(handle!=null){
            handle.setImageDrawable(new DockIcon("show",accent));
            handle.setBackground(round(UiPalette.overlayCard(dark),16,border));
        }
    }

    private LinearLayout button(LinearLayout parent,String icon,String label,Runnable action){return button(parent,icon,label,action,true);}
    private LinearLayout button(LinearLayout parent,String icon,String label,Runnable action,boolean requiresControl){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setGravity(Gravity.CENTER);row.setPadding(0,dp(7),0,dp(7));row.setContentDescription(label);row.setFocusable(true);row.setClickable(true);
        boolean dark=isDark();
        ImageView image=new ImageView(this);image.setImageDrawable(new DockIcon(icon,UiPalette.text(dark)));row.addView(image,new LinearLayout.LayoutParams(dp(24),dp(24)));
        TextView caption=text(label,11,UiPalette.text(dark));caption.setGravity(Gravity.CENTER);caption.setPadding(0,dp(4),0,0);row.addView(caption);
        dockItems.add(new DockItem(image,caption,icon));
        row.setOnClickListener(v->{if(!exiting&&(!requiresControl||control!=null))action.run();});parent.addView(row,new LinearLayout.LayoutParams(-1,dp(58)));return row;
    }
    private void select(int mode){
        currentMode=mode;boolean dark=isDark();int sel=UiPalette.selected(dark);
        if(carButton!=null)carButton.setBackground(round(mode==0?sel:Color.TRANSPARENT,16,Color.TRANSPARENT));
        if(appsButton!=null)appsButton.setBackground(round(mode==1?sel:Color.TRANSPARENT,16,Color.TRANSPARENT));
    }
    private void collapse(boolean value){collapsed=value;((View)dock.getParent()).setVisibility(value?View.GONE:View.VISIBLE);handle.setVisibility(value?View.VISIBLE:View.GONE);getPreferences(0).edit().putBoolean("collapsed",value).apply();}
    private MaterialTextView text(String value,int size,int color){
        MaterialTextView t=new MaterialTextView(this);
        int appearance;
        if(size>=28)appearance=com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall;
        else if(size>=21)appearance=com.google.android.material.R.style.TextAppearance_Material3_TitleLarge;
        else if(size<=12)appearance=com.google.android.material.R.style.TextAppearance_Material3_BodySmall;
        else appearance=com.google.android.material.R.style.TextAppearance_Material3_BodyLarge;
        t.setTextAppearance(appearance);t.setText(value);t.setTextColor(color);return t;
    }
    private GradientDrawable round(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void status(String message){runOnUiThread(()->{status.setText("●  "+message);if(message.startsWith("已连接 ·"))waiting.setVisibility(View.GONE);else if(message.startsWith("等待设备视频"))waiting.setVisibility(View.VISIBLE);});}
    private void remote(JSONObject info){JSONObject p=info.optJSONObject("probe");if(p!=null)clock.probe(p.optLong("t0"),p.optLong("t1"),p.optLong("t2"),info.optLong("receivedNs"));runOnUiThread(()->{remote=info;JSONObject stream=info.optJSONObject("stream");if(player!=null&&stream!=null&&stream.has("connected"))player.serverHasVideoClient(stream.optBoolean("connected"));String stage=info.optString("iphoneStage","等待连接 iPhone"),phoneError=info.optString("iphoneError");waiting.setText("KrPlay-Client\n\n设备已连接 · 等待 CarPlay 画面\n"+stage+(phoneError.isEmpty()?"\n请在 KrPlay 中连接 iPhone":"\n最近失败："+phoneError));select(info.optInt("mode"));appsButton.setAlpha(info.optBoolean("rootScreen")?1f:.45f);renderNetwork();String error=info.optString("error");if(!error.isEmpty())status.setText(error);});}
    private void showNetwork(){
        if(!com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED)return;
        boolean dark=isDark();
        networkDialog=new Dialog(this);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(20),dp(24),dp(20));panel.setBackground(round(UiPalette.surface(dark),24,UiPalette.border(dark)));
        TextView title=text("车载借网",24,UiPalette.text(dark));title.setTypeface(Typeface.DEFAULT_BOLD);panel.addView(title);
        networkText=text("",15,UiPalette.muted(dark));networkText.setPadding(0,dp(14),0,dp(14));panel.addView(networkText);
        dialogButton(panel,"连接 / 断开 VPN",()->{control.vpn(!remote.optBoolean("vpn"));status("首次连接请在安卓设备上允许 VPN 授权");});
        dialogButton(panel,"检测通道",()->control.test(false));
        dialogButton(panel,"下载测速 · 最多 8 MiB",()->new MaterialAlertDialogBuilder(this).setTitle("下载测速").setMessage("将通过车载网络下载最多 8 MiB。测试结果反映当前通道下载速度。").setNegativeButton("取消",null).setPositiveButton("开始",(d,w)->control.test(true)).show());
        dialogButton(panel,"关闭",()->networkDialog.dismiss());
        networkDialog.setContentView(panel);networkDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        int width=Math.min(dp(460),getResources().getDisplayMetrics().widthPixels-dp(48));
        networkDialog.getWindow().setLayout(width,-2);networkDialog.show();networkDialog.getWindow().setLayout(width,-2);control.query();renderNetwork();
    }
    private void dialogButton(LinearLayout panel,String label,Runnable click){
        MaterialButton button=new MaterialButton(this);button.setText(label);button.setAllCaps(false);button.setCornerRadius(dp(24));
        boolean primary=label.startsWith("连接");boolean dark=isDark();int accent=UiPalette.accent(dark),onAccent=UiPalette.onAccent(dark);
        if(primary){button.setBackgroundColor(accent);button.setTextColor(onAccent);button.setStrokeWidth(0);}
        else{button.setBackgroundColor(Color.TRANSPARENT);button.setTextColor(accent);button.setStrokeColor(android.content.res.ColorStateList.valueOf(accent));button.setStrokeWidth(dp(1));}
        button.setOnClickListener(v->click.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.topMargin=dp(10);panel.addView(button,p);
    }
    private void renderNetwork(){if(networkText==null)return;JSONObject result=remote.optJSONObject("test");String detail="尚未检测";if(result!=null){String state=result.optString("state");if(state.equals("success"))detail=String.format(Locale.US,"通道可用 · TLS %.0f ms\n下载 %.2f Mbps · %.2f MiB",result.optDouble("latencyMs"),result.optDouble("mbps"),result.optLong("bytes")/1048576.0);else if(state.equals("failed"))detail="检测失败："+result.optString("error");else if(state.equals("running"))detail=result.has("mbps")?String.format(Locale.US,"测速中 · %.2f Mbps",result.optDouble("mbps")):"正在通过车载网络检测…";}
        networkText.setText(remote.optString("fixedEntryMessage","正在获取固定入口状态")+"\nTCP / DNS 借网 · UDP 暂不支持\n"+remote.optString("vpnMessage","等待设备状态")+"\n可用通道："+remote.optInt("idleWorkers")+String.format(Locale.US,"\nVPN 接收 %.2f MiB / 发送 %.2f MiB\n\n",remote.optLong("rxBytes")/1048576.0,remote.optLong("txBytes")/1048576.0)+detail);}
    private int musicBufferMs(){return com.krplay.protocol.AudioPolicy.bufferMs(getSharedPreferences("audio",0).getInt("musicBufferMs",300));}
    private void audioSettings(){
        int[] values={300,500,1000};int selected=musicBufferMs()==1000?2:musicBufferMs()==500?1:0;
        new MaterialAlertDialogBuilder(this).setTitle("客户端音乐缓冲")
            .setSingleChoiceItems(new String[]{"300 ms · 默认","500 ms · 更耐网络波动","1000 ms · 优先连续播放"},selected,(dialog,which)->{
                getSharedPreferences("audio",0).edit().putInt("musicBufferMs",values[which]).apply();
                if(audio!=null)audio.close();if(active){audio=new ClientAudio(this,host);audio.start();}
                dialog.dismiss();Toast.makeText(this,"音乐缓冲已更新，音频正在重新连接",Toast.LENGTH_SHORT).show();
            }).setNeutralButton("说明",(dialog,which)->new MaterialAlertDialogBuilder(this).setTitle("音乐缓冲")
                .setMessage("更大的缓冲可减少网络波动造成的断音，但会增加音乐启动与切歌的等待时间。保持原采样率与声道；通话、Siri 和导航提示使用独立的低延迟缓冲。此设置只影响本客户端播放。")
                .setPositiveButton("知道了",null).show()).setNegativeButton("取消",null).show();
    }
    private void settings(){
        boolean dark=isDark();
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(20),dp(4),dp(20),dp(16));
        TextView desc=text("默认通过固定入口、热点网关和已知地址自动查找。虚拟机需要宿主允许访问热点；固定 IP 无法绕过完全隔离。关闭自动连接后可指定地址。",13,UiPalette.muted(dark));
        desc.setPadding(0,0,0,dp(12));form.addView(desc);
        MaterialCheckBox automatic=new MaterialCheckBox(this);automatic.setText("自动连接（推荐）");automatic.setTextColor(UiPalette.text(dark));automatic.setChecked(automaticConnection);form.addView(automatic);
        EditText input=new EditText(this);input.setSingleLine();input.setSelectAllOnFocus(true);input.setText(host);input.setHint("例如 192.168.43.1");input.setTextColor(UiPalette.text(dark));input.setHintTextColor(UiPalette.muted(dark));input.setEnabled(!automaticConnection);form.addView(input);
        automatic.setOnCheckedChangeListener((b,on)->input.setEnabled(!on));
        dialogButton(form,"音乐缓冲 · "+musicBufferMs()+" ms",this::audioSettings);
        TextView sourceOffer=text("开源与许可：免费获取完整源码与构建脚本",14,UiPalette.muted(dark));sourceOffer.setPadding(0,dp(12),0,dp(4));form.addView(sourceOffer);
        dialogButton(form,"检查更新 · "+com.krplay.update.AppUpdates.installedVersion(this),()->com.krplay.update.AppUpdates.open(this));
        dialogButton(form,"KrPlay 项目仓库",()->com.krplay.distribution.SourceDistribution.open(this,com.krplay.distribution.SourceDistribution.REPOSITORY));
        dialogButton(form,"客户端对应源码 · "+com.krplay.distribution.SourceDistribution.versionLabel(this),()->com.krplay.distribution.SourceDistribution.open(this,com.krplay.distribution.SourceDistribution.sourceUrl(this)));
        dialogButton(form,"接收端对应源码",()->com.krplay.distribution.SourceDistribution.open(this,com.krplay.distribution.SourceDistribution.receiverSourceUrl(remote)));
        dialogButton(form,"版权与许可全文",()->com.krplay.distribution.SourceDistribution.showLicenses(this));
        ScrollView settingsScroll=new ScrollView(this);
        settingsScroll.setVerticalScrollBarEnabled(true);
        settingsScroll.setScrollbarFadingEnabled(false);
        settingsScroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        settingsScroll.setFillViewport(true);
        GradientDrawable thumb=new GradientDrawable();
        thumb.setColor(dark?0x80c1c9be:0x80546253);
        thumb.setCornerRadius(dp(3));
        thumb.setSize(dp(4),dp(40));
        settingsScroll.setVerticalScrollbarThumbDrawable(thumb);
        settingsScroll.addView(form);
        AlertDialog dialog=new MaterialAlertDialogBuilder(this).setTitle("KrPlay 设备地址").setView(settingsScroll).setNegativeButton("取消",null).setPositiveButton("连接",(d,w)->{String value=input.getText().toString().trim();if(!automatic.isChecked()&&!value.matches("[a-zA-Z0-9._:-]{1,253}")){Toast.makeText(this,"请输入有效设备地址",Toast.LENGTH_SHORT).show();return;}automaticConnection=automatic.isChecked();host=automaticConnection?com.krplay.protocol.Discovery.FIXED_HOST:value;getPreferences(0).edit().putBoolean("automaticConnection",automaticConnection).putString("host",value).apply();stopConnections();startConnections();}).show();
        int width=Math.min(dp(540),getResources().getDisplayMetrics().widthPixels-dp(32));
        int height=Math.min(dp(540),getResources().getDisplayMetrics().heightPixels-dp(32));
        dialog.getWindow().setLayout(width,height);
    }
    private static String metric(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f",v):"—";}
    private String performanceSummary(){NaluStreamPlayer p=player;if(p==null)return "等待连接";
        JSONObject a=remote.optJSONObject("audio");String route=a==null?"等待音频路由":("输出 "+a.optString("output")+" · 输入 "+a.optString("microphone")+"\n"+a.optString("error"));
        return String.format(Locale.US,"实际呈现 %.1f FPS · 视频 %.2f Mbps\n",fps,mbps)+"LAN RTT "+metric(clock.rttMs())+" ms · P95 "+metric(clock.rttP95Ms())+" ms\n接收端收包 → 渲染回调 "+metric(p.bridgeMs)+" ms · P95 "+metric(clock.latencyP95Ms())+" ms\n时钟误差估计 ±"+metric(clock.uncertaintyMs())+" ms\n服务端排队 "+metric(p.serverQueueMs)+" ms · 传输 "+metric(p.transferMs)+" ms\n客户端入队 → 渲染回调 "+metric(p.decodeMs)+" ms（回调延后，非物理显示）\n解码器输出就绪 "+metric(p.decodeReadyMs)+" ms · 收包→输出 "+metric(p.bridgeReadyMs)+" ms\n低延迟 Wi-Fi 锁 "+(wifiLock!=null&&wifiLock.isHeld()?"已请求":"未获得")+"\n解码 "+p.codec+" · "+p.decoderTuning+"\n"+streamWidth+" × "+streamHeight+" · "+profileLabel()+"\n服务端恢复 "+remote.optJSONObject("stream")+"\n解码队列 "+p.queueDepth+" · 累计溢出 "+p.overflows.get()+" · 累计重连 "+p.reconnects.get()+"\n等待关键帧 "+p.waitingForKeyFrame()+" · 请求 "+p.keyframeRequestCount.get()+" · 持续解码停顿 "+p.codecStalls.get()+"\n输入等待≥60ms "+p.inputWaits.get()+" · 解码器重建 "+p.decoderResets.get()+" · 追帧未呈现 "+p.catchupFrames.get()+" · 非参考帧丢弃 "+p.nonReferenceDrops.get()+"\n最近视频包距今 "+p.videoIdleMs()+" ms · 最近渲染距今 "+p.renderIdleMs()+" ms（-1 表示尚无记录）\n最近恢复原因："+p.lastRecoveryReason+"\n延迟数值取最近一帧，停帧后不代表当前实时延迟。\n"+(audio==null?"":audio.summary())+"\n"+route;
    }
    private String diagnosis(){if(player==null)return "诊断：等待连接";
        JSONObject stream=remote.optJSONObject("stream");
        if(player.waitingForKeyFrame()||(stream!=null&&stream.optBoolean("waitingKeyFrame")))return "诊断：正在等待关键帧恢复，已启用周期重试；请结合最近收包时间判断是否停流。";
        if(player.videoIdleMs()>1000)return "诊断：超过 1 秒未收到视频帧。静止画面可能正常；若操作后仍不更新，请检查上游视频与关键帧状态。延迟数值为历史帧。";
        if(!clock.synced())return "诊断：等待时钟同步和视频帧";
        if(player.transferMs>100)return "诊断：最近一帧的传输/接收耗时偏高，优先检查局域网拥塞与大帧传输；累计溢出不能证明当前解码过慢。";
        if(player.queueDepth>=3)return "诊断：当前客户端视频队列积压；可比较较低分辨率或帧率。";
        if(player.decodeReadyMs>20)return "诊断：最近一帧解码器输出耗时偏高；可降低分辨率比较。";
        if(clock.rttP95Ms()>25)return "诊断：控制通道 RTT 历史有波动，其中也包含客户端发送排队；可与独立局域网测试比较。";
        return "诊断：当前未发现持续解码积压。溢出与重连为累计值；渲染回调延迟不等于完整触控到画面延迟。";
    }
    private final Runnable tick=new Runnable(){public void run(){if(!active||player==null)return;long now=System.nanoTime(),frames=player.renderedFrames.get(),bytes=player.receivedBytes.get();double seconds=(now-lastTick)/1e9;if(seconds>0){fps=(frames-lastFrames)/seconds;mbps=(bytes-lastBytes)*8e-6/seconds;}lastTick=now;lastFrames=frames;lastBytes=bytes;
        control.probe();if(showPerformance)performanceHud.setText(String.format(Locale.US,"%.1f FPS · %.2f Mbps · RTT %s ms\n收包→渲染回调 %s ms · 入队→渲染回调 %s ms",fps,mbps,metric(clock.rttMs()),metric(player.bridgeMs),metric(player.decodeMs)));
        if(performanceText!=null)performanceText.setText(performanceSummary()+"\n\n"+diagnosis()+"\n\n"+linkResult+"\n\n帧率反映实际更新；静止画面可能不连续发帧。上述延迟不含 iPhone 编码与屏幕扫描，不能视为完整触控到画面延迟。图像档位保持屏幕比例，需在 KrPlay 开启跟随客户端分辨率。");
        if(showPerformance&&++diagnosticTick%10==0)android.util.Log.i("KrPlay-Performance",performanceSummary().replace("\n"," | " ));
        ticker.postDelayed(this,1000);
    }};
    private void showPerformance(){
        boolean dark=isDark();
        performanceDialog=new Dialog(this);
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(16),dp(24),dp(16));panel.setBackground(round(UiPalette.surface(dark),24,UiPalette.border(dark)));
        TextView title=text("链路性能与诊断",23,UiPalette.text(dark));panel.addView(title);
        MaterialCheckBox connectionToggle=new MaterialCheckBox(this);connectionToggle.setText("显示左上角连接信息");connectionToggle.setTextColor(UiPalette.accent(dark));connectionToggle.setChecked(showStatus);connectionToggle.setOnCheckedChangeListener((b,on)->{showStatus=on;status.setVisibility(on?View.VISIBLE:View.GONE);getPreferences(0).edit().putBoolean("showStatus",on).apply();});panel.addView(connectionToggle);
        MaterialCheckBox toggle=new MaterialCheckBox(this);toggle.setText("显示帧率、延迟和传输速率");toggle.setTextColor(UiPalette.accent(dark));toggle.setChecked(showPerformance);toggle.setOnCheckedChangeListener((b,on)->{showPerformance=on;performanceHud.setVisibility(on?View.VISIBLE:View.GONE);getPreferences(0).edit().putBoolean("showPerformance",on).apply();});panel.addView(toggle);
        performanceText=text(performanceSummary(),13,UiPalette.muted(dark));performanceText.setTextIsSelectable(true);panel.addView(performanceText);
        dialogButton(panel,"图像规格 · "+profileLabel(),()->new MaterialAlertDialogBuilder(this).setTitle("客户端接收分辨率").setSingleChoiceItems(new String[]{"设备原生分辨率","1080p","720p · 默认低延迟"},videoProfile,(d,w)->{videoProfile=w;getPreferences(0).edit().putInt("videoProfile",w).apply();clock.reset();sendViewport();d.dismiss();performanceDialog.dismiss();Toast.makeText(this,"已请求新分辨率，KrPlay 可能重新连接 iPhone",Toast.LENGTH_LONG).show();}).setNegativeButton("取消",null).show());
        dialogButton(panel,"音乐缓冲 · "+musicBufferMs()+" ms",this::audioSettings);
        dialogButton(panel,"局域网延迟检测",()->diagnostics.run(host,false,this::recordLinkResult));
        dialogButton(panel,"双向传输测速 · 共 16 MiB",()->diagnostics.run(host,true,this::recordLinkResult));
        dialogButton(panel,"保存并复制诊断",()->{String report=performanceSummary()+"\n\n"+diagnosis()+"\n\n"+linkResult;try(java.io.FileOutputStream out=openFileOutput("performance.txt",MODE_PRIVATE)){out.write(report.getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(java.io.IOException ignored){}android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(android.content.ClipData.newPlainText("KrPlay 性能诊断",report));Toast.makeText(this,"诊断已保存并复制，不包含音视频内容",Toast.LENGTH_SHORT).show();});
        dialogButton(panel,"关闭",()->performanceDialog.dismiss());
        ScrollView scroll=new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        GradientDrawable perfThumb=new GradientDrawable();
        perfThumb.setColor(dark?0x80c1c9be:0x80546253);
        perfThumb.setCornerRadius(dp(3));
        perfThumb.setSize(dp(4),dp(40));
        scroll.setVerticalScrollbarThumbDrawable(perfThumb);
        scroll.addView(panel);
        performanceDialog.setContentView(scroll);performanceDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);performanceDialog.show();performanceDialog.getWindow().setLayout(Math.min(dp(560),getResources().getDisplayMetrics().widthPixels-dp(40)),Math.min(dp(650),getResources().getDisplayMetrics().heightPixels-dp(32)));
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==41&&active){if(audio!=null)audio.close();audio=new ClientAudio(this,host);audio.start();}}
    private void recordLinkResult(String result){runOnUiThread(()->{linkResult=result;try(java.io.FileOutputStream out=openFileOutput("link-test.txt",MODE_PRIVATE)){out.write(("时间："+new java.util.Date()+"\n"+result+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(java.io.IOException e){android.util.Log.w("KrPlay-LAN-Test","Cannot save result",e);}});}
    private String profileLabel(){return videoProfile==0?"原生":videoProfile==2?"720p":"1080p";}
    private void sendViewport(){if(control==null)return;int w=stage.getWidth(),h=stage.getHeight();if(w<320||h<240)return;int[] size=com.krplay.protocol.VideoPolicy.viewport(w,h,videoProfile);control.size(size[0],size[1]);}
    private void fit(){int w=stage.getWidth(),h=stage.getHeight();if(w==0||h==0)return;double ratio=Math.min((double)w/streamWidth,(double)h/streamHeight);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams((int)(streamWidth*ratio),(int)(streamHeight*ratio),Gravity.CENTER);video.setLayoutParams(p);}
    private void startConnections(){startConnections(true);}
    private void startConnections(boolean search){if(exiting)return;if(search)LocalTransport.network=LocalTransport.preferred(this);final int attempt=++connectionGeneration;try{android.net.wifi.WifiManager wifi=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);wifiLock=wifi.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY,"KrPlay-Client");wifiLock.setReferenceCounted(false);wifiLock.acquire();}catch(RuntimeException ignored){}clock.reset();remote=new JSONObject();waiting.setText("KrPlay-Client\n\n"+(automaticConnection?"正在自动查找 KrPlay\n固定入口、网关与已知地址\n若失败，请确认热点与固定入口 VPN 已开启；也可手动指定地址":"正在连接安卓设备\n"+host));status("正在连接 · "+host);waiting.setVisibility(View.VISIBLE);control=new TouchControlClient(host,this::status,this::remote);player=new NaluStreamPlayer(host,()->{if(control!=null)control.idr();},this::status,(w,h)->runOnUiThread(()->{streamWidth=w;streamHeight=h;fit();}),clock);audio=new ClientAudio(this,host);diagnostics=new LinkDiagnostics();socks=new ReverseSocks5Worker(this,host,this::status);sendViewport();control.start();if(video.getHolder().getSurface().isValid())player.surface(video.getHolder().getSurface());player.start();if(com.krplay.protocol.NetworkFeatures.BORROWING_ENABLED)socks.start();audio.start();lastTick=System.nanoTime();lastFrames=0;lastBytes=0;ticker.removeCallbacks(tick);ticker.post(tick);
        if(search&&automaticConnection){String saved=getPreferences(0).getString("lastResolved",getPreferences(0).getString("host",""));discovery=new AutoDiscovery(this,saved,(value,network)->runOnUiThread(()->{if(!active||connectionGeneration!=attempt)return;getPreferences(0).edit().putString("lastResolved",value).apply();if(!value.equals(host)||!java.util.Objects.equals(LocalTransport.network,network)){stopConnections();LocalTransport.network=network;host=value;startConnections(false);}}));discovery.start();}
    }
    private void stopConnections(){connectionGeneration++;if(discovery!=null)discovery.close();discovery=null;if(wifiLock!=null&&wifiLock.isHeld())wifiLock.release();wifiLock=null;ticker.removeCallbacks(tick);if(audio!=null)audio.close();if(diagnostics!=null)diagnostics.close();if(player!=null)player.close();if(control!=null)control.close();if(socks!=null)socks.close();}
    private void unregisterLanChanges(){if(lanChanges==null)return;try{((android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(lanChanges);}catch(RuntimeException ignored){}lanChanges=null;}
    private void exitApp(){if(exiting)return;exiting=true;active=false;ticker.removeCallbacksAndMessages(null);unregisterLanChanges();stopConnections();LocalTransport.network=null;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);if(networkDialog!=null)networkDialog.dismiss();if(performanceDialog!=null)performanceDialog.dismiss();finishAndRemoveTask();}
    @Override public void onStart(){super.onStart();if(exiting)return;active=true;startConnections();}
    @Override public void onStop(){active=false;ticker.removeCallbacks(reconnectNetwork);stopConnections();if(networkDialog!=null)networkDialog.dismiss();if(performanceDialog!=null)performanceDialog.dismiss();super.onStop();}
    public void surfaceCreated(SurfaceHolder h){if(active)player.surface(h.getSurface());}
    public void surfaceChanged(SurfaceHolder h,int f,int w,int ht){if(active){sendViewport();player.surface(h.getSurface());}}
    public void surfaceDestroyed(SurfaceHolder h){if(active)player.surface(null);}
    @Override public void onDestroy(){active=false;unregisterLanChanges();ticker.removeCallbacksAndMessages(null);stopConnections();super.onDestroy();
        // Only an explicit exit ends the process; backgrounding keeps normal Android behavior.
        if(exiting)android.os.Process.killProcess(android.os.Process.myPid());}
    @Override public void onBackPressed(){if(control!=null)control.key(KeyEvent.KEYCODE_BACK);}
}
