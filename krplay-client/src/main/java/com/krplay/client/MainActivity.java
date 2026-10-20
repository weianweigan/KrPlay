package com.krplay.client;
import android.app.*;
import com.krplay.protocol.UiPalette;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.util.Locale;

public final class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final int INK=UiPalette.BACKGROUND,MINT=UiPalette.ACCENT,MUTED=UiPalette.MUTED;
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
    private int streamWidth=1920,streamHeight=1080;private boolean active,collapsed;private String host;
    private JSONObject remote=new JSONObject();
    @Override public void onCreate(Bundle b){super.onCreate(b);com.krplay.update.AppUpdates.checkOnStart(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // OEM split-screen launchers may reapply immersive flags when a dialog returns
        // focus, promoting the task to fullscreen. Let the head unit own window bounds.
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        if(!getPreferences(0).getBoolean("overlayOptionsV1",false))getPreferences(0).edit().putBoolean("overlayOptionsV1",true).putBoolean("showStatus",false).putBoolean("showPerformance",false).apply();
        videoProfile=getPreferences(0).getInt("videoProfile",1);automaticConnection=getPreferences(0).getBoolean("automaticConnection",true);host=automaticConnection?com.krplay.protocol.Discovery.FIXED_HOST:getPreferences(0).getString("host",com.krplay.protocol.Discovery.FIXED_HOST);collapsed=getPreferences(0).getBoolean("collapsed",false);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(INK);stage=new FrameLayout(this);root.addView(stage,new FrameLayout.LayoutParams(-1,-1));
        video=new SurfaceView(this);video.getHolder().addCallback(this);stage.addView(video,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));
        waiting=text("KrPlay-Client\n\n连接你的安卓设备\n自动查找 KrPlay · 固定入口 6.6.6.6\n若失败，可在右侧连接设置中手动指定地址",18,MUTED);waiting.setGravity(Gravity.CENTER);waiting.setPadding(dp(30),dp(24),dp(30),dp(24));waiting.setBackground(round(0xee1c2428,24,UiPalette.BORDER));root.addView(waiting,new FrameLayout.LayoutParams(Math.min(dp(420),getResources().getDisplayMetrics().widthPixels-dp(128)),-2,Gravity.CENTER));
        status=text("●  等待连接 · "+host,13,MUTED);status.setPadding(dp(16),dp(10),dp(16),dp(10));status.setBackground(round(0xe61c2428,20,UiPalette.BORDER));FrameLayout.LayoutParams badge=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.START);badge.setMargins(dp(20),dp(18),dp(20),0);root.addView(status,badge);showStatus=getPreferences(0).getBoolean("showStatus",false);status.setVisibility(showStatus?View.VISIBLE:View.GONE);
        performanceHud=text("",12,MINT);performanceHud.setPadding(dp(12),dp(8),dp(12),dp(8));performanceHud.setBackground(round(0xe61c2428,16,UiPalette.BORDER));FrameLayout.LayoutParams hud=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START);hud.setMargins(dp(20),0,dp(118),dp(18));root.addView(performanceHud,hud);showPerformance=getPreferences(0).getBoolean("showPerformance",false);performanceHud.setVisibility(showPerformance?View.VISIBLE:View.GONE);
        dock=new LinearLayout(this);dock.setOrientation(LinearLayout.VERTICAL);dock.setPadding(dp(8),dp(10),dp(8),dp(10));dock.setBackground(round(0xf01c2428,24,UiPalette.BORDER));dock.setElevation(dp(12));
        carButton=button(dock,"car","CarPlay",()->{control.mode(0);select(0);});
        appsButton=button(dock,"apps","设备画面",()->{if(!remote.optBoolean("rootScreen")){Toast.makeText(this,"设备画面与远程触控需要 root；CarPlay 和 VPN 无需 root",Toast.LENGTH_LONG).show();return;}control.mode(1);select(1);});appsButton.setVisibility(View.GONE);
        button(dock,"voice","Siri",()->control.key(KeyEvent.KEYCODE_ASSIST));button(dock,"back","返回",()->control.key(KeyEvent.KEYCODE_BACK));button(dock,"home","主页",()->control.key(KeyEvent.KEYCODE_HOME));
        button(dock,"performance","性能",this::showPerformance);button(dock,"network","网络",this::showNetwork);button(dock,"settings","连接设置",this::settings);button(dock,"hide","收起",()->collapse(true));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);scroll.addView(dock);
        FrameLayout.LayoutParams rail=new FrameLayout.LayoutParams(dp(92),Math.min(dp(570),getResources().getDisplayMetrics().heightPixels-dp(36)),Gravity.RIGHT|Gravity.CENTER_VERTICAL);rail.setMargins(0,dp(18),dp(18),dp(18));root.addView(scroll,rail);
        handle=new ImageButton(this);handle.setImageDrawable(new DockIcon("show",MINT));handle.setContentDescription("展开控制侧栏");handle.setPadding(dp(10),dp(14),dp(10),dp(14));handle.setBackground(round(0xee1c2428,16,UiPalette.BORDER));handle.setOnClickListener(v->collapse(false));
        FrameLayout.LayoutParams tab=new FrameLayout.LayoutParams(dp(44),dp(64),Gravity.RIGHT|Gravity.CENTER_VERTICAL);tab.rightMargin=dp(8);root.addView(handle,tab);
        ClientWindowInsets.fit(root);setContentView(root);collapse(collapsed);select(0);
        lanChanges=new android.net.ConnectivityManager.NetworkCallback(){
            @Override public void onAvailable(android.net.Network n){if(active&&LocalTransport.network==null){ticker.removeCallbacks(reconnectNetwork);ticker.postDelayed(reconnectNetwork,500);}}
            @Override public void onLost(android.net.Network n){if(java.util.Objects.equals(LocalTransport.network,n)){LocalTransport.network=null;ticker.removeCallbacks(reconnectNetwork);ticker.postDelayed(reconnectNetwork,500);}}
        };
        try{((android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).registerNetworkCallback(new android.net.NetworkRequest.Builder().addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI).addTransportType(android.net.NetworkCapabilities.TRANSPORT_ETHERNET).build(),lanChanges);}catch(RuntimeException ignored){}
if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO},41);
        video.setOnTouchListener((v,e)->control!=null&&control.touch(e,v.getWidth(),v.getHeight()));stage.addOnLayoutChangeListener((v,l,t,r,bt,ol,ot,or,ob)->{fit();if(active)sendViewport();});
    }
    private LinearLayout button(LinearLayout parent,String icon,String label,Runnable action){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setGravity(Gravity.CENTER);row.setPadding(0,dp(7),0,dp(7));row.setContentDescription(label);row.setFocusable(true);row.setClickable(true);
        ImageView image=new ImageView(this);image.setImageDrawable(new DockIcon(icon,UiPalette.TEXT));row.addView(image,new LinearLayout.LayoutParams(dp(24),dp(24)));TextView caption=text(label,11,UiPalette.TEXT);caption.setGravity(Gravity.CENTER);caption.setPadding(0,dp(4),0,0);row.addView(caption);
        row.setOnClickListener(v->{if(control!=null)action.run();});parent.addView(row,new LinearLayout.LayoutParams(-1,dp(58)));return row;}
    private void select(int mode){carButton.setBackground(round(mode==0?0xff364b35:Color.TRANSPARENT,16,Color.TRANSPARENT));appsButton.setBackground(round(mode==1?0xff364b35:Color.TRANSPARENT,16,Color.TRANSPARENT));}
    private void collapse(boolean value){collapsed=value;((View)dock.getParent()).setVisibility(value?View.GONE:View.VISIBLE);handle.setVisibility(value?View.VISIBLE:View.GONE);getPreferences(0).edit().putBoolean("collapsed",value).apply();}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private GradientDrawable round(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),stroke);return d;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void status(String message){runOnUiThread(()->{status.setText("●  "+message);if(message.startsWith("已连接 ·"))waiting.setVisibility(View.GONE);else if(message.startsWith("等待设备视频"))waiting.setVisibility(View.VISIBLE);});}
    private void remote(JSONObject info){JSONObject p=info.optJSONObject("probe");if(p!=null)clock.probe(p.optLong("t0"),p.optLong("t1"),p.optLong("t2"),info.optLong("receivedNs"));runOnUiThread(()->{remote=info;String stage=info.optString("iphoneStage","等待连接 iPhone"),phoneError=info.optString("iphoneError");waiting.setText("KrPlay-Client\n\n设备已连接 · 等待 CarPlay 画面\n"+stage+(phoneError.isEmpty()?"\n请在 KrPlay 中连接 iPhone":"\n最近失败："+phoneError));select(info.optInt("mode"));appsButton.setAlpha(info.optBoolean("rootScreen")?1f:.45f);renderNetwork();String error=info.optString("error");if(!error.isEmpty())status.setText(error);});}
    private void showNetwork(){networkDialog=new Dialog(this);LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(20),dp(24),dp(20));panel.setBackground(round(UiPalette.SURFACE,24,UiPalette.BORDER));TextView title=text("车载借网",24,UiPalette.TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);panel.addView(title);
        networkText=text("",15,MUTED);networkText.setPadding(0,dp(14),0,dp(14));panel.addView(networkText);
        dialogButton(panel,"连接 / 断开 VPN",()->{control.vpn(!remote.optBoolean("vpn"));status("首次连接请在安卓设备上允许 VPN 授权");});
        dialogButton(panel,"检测通道",()->control.test(false));dialogButton(panel,"下载测速 · 最多 8 MiB",()->new AlertDialog.Builder(this).setTitle("下载测速").setMessage("将通过车载网络下载最多 8 MiB。测试结果反映当前通道下载速度。").setNegativeButton("取消",null).setPositiveButton("开始",(d,w)->control.test(true)).show());dialogButton(panel,"关闭",()->networkDialog.dismiss());
        networkDialog.setContentView(panel);networkDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);networkDialog.getWindow().setLayout(Math.min(dp(460),getResources().getDisplayMetrics().widthPixels-dp(48)),-2);networkDialog.show();networkDialog.getWindow().setLayout(Math.min(dp(460),getResources().getDisplayMetrics().widthPixels-dp(48)),-2);control.query();renderNetwork();}
    private void dialogButton(LinearLayout panel,String label,Runnable click){Button button=new Button(this);button.setText(label);button.setAllCaps(false);boolean primary=label.startsWith("连接");button.setTextColor(primary?UiPalette.ON_ACCENT:MINT);button.setBackground(round(primary?MINT:Color.TRANSPARENT,24,primary?Color.TRANSPARENT:MINT));button.setOnClickListener(v->click.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.topMargin=dp(10);panel.addView(button,p);}
    private void renderNetwork(){if(networkText==null)return;JSONObject result=remote.optJSONObject("test");String detail="尚未检测";if(result!=null){String state=result.optString("state");if(state.equals("success"))detail=String.format(Locale.US,"通道可用 · TLS %.0f ms\n下载 %.2f Mbps · %.2f MiB",result.optDouble("latencyMs"),result.optDouble("mbps"),result.optLong("bytes")/1048576.0);else if(state.equals("failed"))detail="检测失败："+result.optString("error");else if(state.equals("running"))detail=result.has("mbps")?String.format(Locale.US,"测速中 · %.2f Mbps",result.optDouble("mbps")):"正在通过车载网络检测…";}
        networkText.setText(remote.optString("fixedEntryMessage","正在获取固定入口状态")+"\nTCP / DNS 借网 · UDP 暂不支持\n"+remote.optString("vpnMessage","等待设备状态")+"\n可用通道："+remote.optInt("idleWorkers")+String.format(Locale.US,"\nVPN 接收 %.2f MiB / 发送 %.2f MiB\n\n",remote.optLong("rxBytes")/1048576.0,remote.optLong("txBytes")/1048576.0)+detail);}
    private void settings(){LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(20),0,dp(20),0);CheckBox automatic=new CheckBox(this);automatic.setText("自动连接（推荐）");automatic.setChecked(automaticConnection);form.addView(automatic);EditText input=new EditText(this);input.setSingleLine();input.setSelectAllOnFocus(true);input.setText(host);input.setHint("例如 192.168.43.1");input.setEnabled(!automaticConnection);form.addView(input);automatic.setOnCheckedChangeListener((b,on)->input.setEnabled(!on));
        TextView sourceOffer=text("开源与许可：免费获取完整源码与构建脚本",14,MUTED);form.addView(sourceOffer);
        dialogButton(form,"检查更新 · "+com.krplay.update.AppUpdates.installedVersion(this),()->com.krplay.update.AppUpdates.open(this));
        dialogButton(form,"KrPlay 项目仓库",()->com.krplay.distribution.SourceDistribution.open(this,com.krplay.distribution.SourceDistribution.REPOSITORY));
        dialogButton(form,"客户端对应源码 · "+com.krplay.distribution.SourceDistribution.versionLabel(this),()->com.krplay.distribution.SourceDistribution.open(this,com.krplay.distribution.SourceDistribution.sourceUrl(this)));
        dialogButton(form,"接收端对应源码",()->com.krplay.distribution.SourceDistribution.open(this,com.krplay.distribution.SourceDistribution.receiverSourceUrl(remote)));
        dialogButton(form,"版权与许可全文",()->com.krplay.distribution.SourceDistribution.showLicenses(this));
        ScrollView settingsScroll=new ScrollView(this);settingsScroll.addView(form);
        new AlertDialog.Builder(this).setTitle("KrPlay 设备地址").setMessage("默认通过固定入口、热点网关和已知地址自动查找。虚拟机需要宿主允许访问热点；固定 IP 无法绕过完全隔离。关闭自动连接后可指定地址。").setView(settingsScroll).setNegativeButton("取消",null).setPositiveButton("连接",(d,w)->{String value=input.getText().toString().trim();if(!automatic.isChecked()&&!value.matches("[a-zA-Z0-9._:-]{1,253}")){Toast.makeText(this,"请输入有效设备地址",Toast.LENGTH_SHORT).show();return;}automaticConnection=automatic.isChecked();host=automaticConnection?com.krplay.protocol.Discovery.FIXED_HOST:value;getPreferences(0).edit().putBoolean("automaticConnection",automaticConnection).putString("host",value).apply();stopConnections();startConnections();}).show();}
    private static String metric(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f",v):"—";}
    private String performanceSummary(){NaluStreamPlayer p=player;if(p==null)return "等待连接";
        JSONObject a=remote.optJSONObject("audio");String route=a==null?"等待音频路由":("输出 "+a.optString("output")+" · 输入 "+a.optString("microphone")+"\n"+a.optString("error"));
        return String.format(Locale.US,"实际呈现 %.1f FPS · 视频 %.2f Mbps\n",fps,mbps)+"LAN RTT "+metric(clock.rttMs())+" ms · P95 "+metric(clock.rttP95Ms())+" ms\n接收端收包 → 渲染回调 "+metric(p.bridgeMs)+" ms · P95 "+metric(clock.latencyP95Ms())+" ms\n时钟误差估计 ±"+metric(clock.uncertaintyMs())+" ms\n服务端排队 "+metric(p.serverQueueMs)+" ms · 传输 "+metric(p.transferMs)+" ms\n客户端入队 → 渲染回调 "+metric(p.decodeMs)+" ms（回调延后，非物理显示）\n解码器输出就绪 "+metric(p.decodeReadyMs)+" ms · 收包→输出 "+metric(p.bridgeReadyMs)+" ms\n低延迟 Wi-Fi 锁 "+(wifiLock!=null&&wifiLock.isHeld()?"已请求":"未获得")+"\n硬解 "+p.codec+"\n"+streamWidth+" × "+streamHeight+" · "+profileLabel()+"\n服务端恢复 "+remote.optJSONObject("stream")+"\n解码队列 "+p.queueDepth+" · 溢出 "+p.overflows.get()+" · 重连 "+p.reconnects.get()+"\n"+(audio==null?"":audio.summary())+"\n"+route;
    }
    private String diagnosis(){if(player==null||!clock.synced())return "诊断：等待时钟同步和视频帧";
        if(player.overflows.get()>0)return "诊断：出现过解码积压；可降低 KrPlay 分辨率或帧率，检查设备温度。";
        if(clock.rttP95Ms()>25)return "诊断：局域网延迟有波动；检查热点信号、频段与节电设置。";
        if(player.decodeReadyMs>20)return "诊断：解码器输出耗时偏高；可降低分辨率比较。";
        return "诊断：未发现持续解码积压。渲染回调可能延后；完整触控到显示延迟需高速摄影测量。";
    }
    private final Runnable tick=new Runnable(){public void run(){if(!active||player==null)return;long now=System.nanoTime(),frames=player.renderedFrames.get(),bytes=player.receivedBytes.get();double seconds=(now-lastTick)/1e9;if(seconds>0){fps=(frames-lastFrames)/seconds;mbps=(bytes-lastBytes)*8e-6/seconds;}lastTick=now;lastFrames=frames;lastBytes=bytes;
        control.probe();if(showPerformance)performanceHud.setText(String.format(Locale.US,"%.1f FPS · %.2f Mbps · RTT %s ms\n收包→渲染回调 %s ms · 入队→渲染回调 %s ms",fps,mbps,metric(clock.rttMs()),metric(player.bridgeMs),metric(player.decodeMs)));
        if(performanceText!=null)performanceText.setText(performanceSummary()+"\n\n"+diagnosis()+"\n\n"+linkResult+"\n\n帧率反映实际更新；静止画面可能不连续发帧。上述延迟不含 iPhone 编码与屏幕扫描，不能视为完整触控到画面延迟。图像档位保持屏幕比例，需在 KrPlay 开启跟随客户端分辨率。");
        if(showPerformance&&++diagnosticTick%10==0)android.util.Log.i("KrPlay-Performance",performanceSummary().replace("\n"," | " ));
        ticker.postDelayed(this,1000);
    }};
    private void showPerformance(){performanceDialog=new Dialog(this);LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(24),dp(16),dp(24),dp(16));panel.setBackground(round(UiPalette.SURFACE,24,UiPalette.BORDER));
        TextView title=text("链路性能与诊断",23,UiPalette.TEXT);panel.addView(title);CheckBox connectionToggle=new CheckBox(this);connectionToggle.setText("显示左上角连接信息");connectionToggle.setTextColor(MINT);connectionToggle.setChecked(showStatus);connectionToggle.setOnCheckedChangeListener((b,on)->{showStatus=on;status.setVisibility(on?View.VISIBLE:View.GONE);getPreferences(0).edit().putBoolean("showStatus",on).apply();});panel.addView(connectionToggle);CheckBox toggle=new CheckBox(this);toggle.setText("显示帧率、延迟和传输速率");toggle.setTextColor(MINT);toggle.setChecked(showPerformance);toggle.setOnCheckedChangeListener((b,on)->{showPerformance=on;performanceHud.setVisibility(on?View.VISIBLE:View.GONE);getPreferences(0).edit().putBoolean("showPerformance",on).apply();});panel.addView(toggle);
        performanceText=text(performanceSummary(),13,MUTED);performanceText.setTextIsSelectable(true);panel.addView(performanceText);
        dialogButton(panel,"图像规格 · "+profileLabel(),()->new AlertDialog.Builder(this).setTitle("客户端接收分辨率").setSingleChoiceItems(new String[]{"设备原生分辨率","1080p · 默认","720p · 优先低延迟"},videoProfile,(d,w)->{videoProfile=w;getPreferences(0).edit().putInt("videoProfile",w).apply();clock.reset();sendViewport();d.dismiss();performanceDialog.dismiss();Toast.makeText(this,"已请求新分辨率，KrPlay 可能重新连接 iPhone",Toast.LENGTH_LONG).show();}).setNegativeButton("取消",null).show());
        dialogButton(panel,"局域网延迟检测",()->diagnostics.run(host,false,v->runOnUiThread(()->linkResult=v)));
        dialogButton(panel,"双向传输测速 · 共 16 MiB",()->diagnostics.run(host,true,v->runOnUiThread(()->linkResult=v)));
        dialogButton(panel,"保存并复制诊断",()->{String report=performanceSummary()+"\n\n"+diagnosis()+"\n\n"+linkResult;try(java.io.FileOutputStream out=openFileOutput("performance.txt",MODE_PRIVATE)){out.write(report.getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(java.io.IOException ignored){}android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(android.content.ClipData.newPlainText("KrPlay 性能诊断",report));Toast.makeText(this,"诊断已保存并复制，不包含音视频内容",Toast.LENGTH_SHORT).show();});
        dialogButton(panel,"关闭",()->performanceDialog.dismiss());ScrollView scroll=new ScrollView(this);scroll.addView(panel);performanceDialog.setContentView(scroll);performanceDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);performanceDialog.show();performanceDialog.getWindow().setLayout(Math.min(dp(560),getResources().getDisplayMetrics().widthPixels-dp(40)),Math.min(dp(650),getResources().getDisplayMetrics().heightPixels-dp(32)));
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){super.onRequestPermissionsResult(code,permissions,results);if(code==41&&active){if(audio!=null)audio.close();audio=new ClientAudio(this,host);audio.start();}}
    private String profileLabel(){return videoProfile==0?"原生":videoProfile==2?"720p":"1080p";}
    private void sendViewport(){if(control==null)return;int w=stage.getWidth(),h=stage.getHeight();if(w<320||h<240)return;int[] size=com.krplay.protocol.VideoPolicy.viewport(w,h,videoProfile);control.size(size[0],size[1]);}
    private void fit(){int w=stage.getWidth(),h=stage.getHeight();if(w==0||h==0)return;double ratio=Math.min((double)w/streamWidth,(double)h/streamHeight);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams((int)(streamWidth*ratio),(int)(streamHeight*ratio),Gravity.CENTER);video.setLayoutParams(p);}
    private void startConnections(){startConnections(true);}
    private void startConnections(boolean search){if(search)LocalTransport.network=LocalTransport.preferred(this);final int attempt=++connectionGeneration;try{android.net.wifi.WifiManager wifi=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);wifiLock=wifi.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY,"KrPlay-Client");wifiLock.setReferenceCounted(false);wifiLock.acquire();}catch(RuntimeException ignored){}clock.reset();remote=new JSONObject();waiting.setText("KrPlay-Client\n\n"+(automaticConnection?"正在自动查找 KrPlay\n固定入口、网关与已知地址\n若失败，请确认热点与固定入口 VPN 已开启；也可手动指定地址":"正在连接安卓设备\n"+host));status("正在连接 · "+host);waiting.setVisibility(View.VISIBLE);control=new TouchControlClient(host,this::status,this::remote);player=new NaluStreamPlayer(host,()->{if(control!=null)control.idr();},this::status,(w,h)->runOnUiThread(()->{streamWidth=w;streamHeight=h;fit();}),clock);audio=new ClientAudio(this,host);diagnostics=new LinkDiagnostics();socks=new ReverseSocks5Worker(this,host,this::status);sendViewport();control.start();if(video.getHolder().getSurface().isValid())player.surface(video.getHolder().getSurface());player.start();socks.start();audio.start();lastTick=System.nanoTime();lastFrames=0;lastBytes=0;ticker.removeCallbacks(tick);ticker.post(tick);
        if(search&&automaticConnection){String saved=getPreferences(0).getString("lastResolved",getPreferences(0).getString("host",""));discovery=new AutoDiscovery(this,saved,(value,network)->runOnUiThread(()->{if(!active||connectionGeneration!=attempt)return;getPreferences(0).edit().putString("lastResolved",value).apply();if(!value.equals(host)||!java.util.Objects.equals(LocalTransport.network,network)){stopConnections();LocalTransport.network=network;host=value;startConnections(false);}}));discovery.start();}
    }
    private void stopConnections(){connectionGeneration++;if(discovery!=null)discovery.close();discovery=null;if(wifiLock!=null&&wifiLock.isHeld())wifiLock.release();wifiLock=null;ticker.removeCallbacks(tick);if(audio!=null)audio.close();if(diagnostics!=null)diagnostics.close();if(player!=null)player.close();if(control!=null)control.close();if(socks!=null)socks.close();}
    @Override public void onStart(){super.onStart();active=true;startConnections();}
    @Override public void onStop(){active=false;ticker.removeCallbacks(reconnectNetwork);stopConnections();if(networkDialog!=null)networkDialog.dismiss();if(performanceDialog!=null)performanceDialog.dismiss();super.onStop();}
    public void surfaceCreated(SurfaceHolder h){if(active)player.surface(h.getSurface());}
    public void surfaceChanged(SurfaceHolder h,int f,int w,int ht){if(active){sendViewport();player.surface(h.getSurface());}}
    public void surfaceDestroyed(SurfaceHolder h){if(active)player.surface(null);}
    @Override public void onDestroy(){if(lanChanges!=null)try{((android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE)).unregisterNetworkCallback(lanChanges);}catch(RuntimeException ignored){}ticker.removeCallbacks(reconnectNetwork);super.onDestroy();}
    @Override public void onBackPressed(){if(control!=null)control.key(KeyEvent.KEYCODE_BACK);}
}
