# 极氪虚拟机网络的受控模型

这是根据描述构造的 Linux 网络模型，**不是极氪固件或虚拟化实现的仿真器**。
它验证可达性、NAT 连接方向、固定入口路由和人为注入的延迟，不能证明真实车机采用相同规则。

```text
VM 77.17.3.10/24
    │ 默认网关 77.17.3.1
宿主路由/NAT：77.17.3.1 ↔ 172.30.0.2
    │
第二级路由/NAT：172.30.0.1 ↔ 192.168.15.1
    │
接收端：192.168.15.38，另配置 6.6.6.6/32
```

所有地址仅存在于三个 `--internal` Docker 网络里，包括用于复现描述的
77.17.3.10 和固定入口 6.6.6.6；它们不向外部网络发包。

## 运行

在仓库根目录执行，需要 Docker 引擎：

```sh
docker build -t krplay-network-lab:local tools/network-lab
python3 tools/network-lab/zeekr_network_lab.py run \
  --output logs/network-lab-20261020/results.json
```

实验创建四个临时容器。每个容器丢弃全部默认 capabilities，仅增加 `NET_ADMIN`；
两个路由节点通过创建参数 `--sysctl net.ipv4.ip_forward=1` 开启转发。
无需 `--privileged`、`SYS_ADMIN`、Docker socket 挂载或可写宿主目录挂载。
挂载仅限本目录的只读脚本，结果由宿主侧 Python 写入指定文件。
脚本结束或普通异常时会清理自己创建的容器与网络；如进程被强制终止，可根据唯一
`krplaylab-<pid>-` 名称前缀检查并清理该次实验资源，勿删除其他容器。

实验以隔离接收端的 TCP 8084 回显及 1 MiB 数据传输作为测量负载，不运行 Android、
CarPlay 编码或真实视频协议。最初在单容器创建多个命名空间的方案因 `/proc/sys`
只读而停止；最终采用上述受限容器方案。单独的特权容器授权复核已通过并清理，
不属于下面七个场景的运行依赖。

## 七个场景

| 场景 | 预期 |
|---|---|
| 双层 NAT，VM 主动连接接收端 | 成功 |
| 接收端主动新建连接进入 VM | 失败；未配置入站转发 |
| 宿主丢弃 VM 到热点网段的转发 | 超时 |
| 接收端有固定地址，但上游没有相应路径 | 不可达 |
| 添加固定入口的显式上游路由 | 成功；假设只屏蔽热点目的网段 |
| 宿主丢弃全部 VM 转发 | 固定入口同样超时 |
| 两方向分别注入 50±10 ms、12 Mbit/s 的 netem | 可达，但回显和大帧传输变慢 |

2026-10-20 本机运行全部得到预期结果，原始结果见
`logs/network-lab-20261020/results.json`。同一 Linux 主机中的正常 RTT 极低，
不能与真实 Wi-Fi 数值直接比较。注入条件下的 TCP 有效吞吐也不保证等于配置速率。

## 与客户端代码和实际车机的关系

- KrPlay-Client 主动连接接收端的 8081/8082/7000/8084，视频返回发生在已建立的 TCP
  连接中；接收端不能向 VM 新建入站连接，单独这一点并不妨碍当前协议。
- 如果 VM 到接收端的主动连接也被宿主拦截，接收端侧 VPN 或固定 IP 本身不能创建
  穿过宿主防火墙的权限。需要宿主提供可达路由、获准的代理/中继或端口转发路径。
- “没有 192.168.15.0/24 专用路由”不等于没有匹配路由；还要检查默认路由以及宿主
  是否允许转发。MAC 前缀也不能单独证明具体虚拟化产品。
- 当前发现逻辑使用 `ConnectivityManager` 的 `Network` / `LinkProperties` 和 TCP
  身份探测，不直接用 `WifiManager` 的 SSID/BSSID/DHCP 租期定位接收端。
  Wi-Fi API Stub 需要另行在 Android 层验证，不能由本实验复现。
- 若 Android 同时伪造 Network/LinkProperties，需在实车比较默认 socket 路径与
  `Network.bindSocket` 后的路径，并记录实际连接目标、失败阶段和耗时。
- 要模拟虚拟机暂停、CPU 争用、系统 VPN 的用户态转发及 MTU 等因素，应分别添加
  独立实验，不把 netem 结果当成这些因素的实测。

实车验证首先记录 `ip route get`、实际接口地址及到接收端端口的 TCP 结果；再对
成功建立的同一条路径测 RTT、整帧大小/传输耗时及抖动。不要先假定所有延迟来自 VPN。

## 分离延迟原因的对照实验

```sh
python3 tools/network-lab/zeekr_network_lab.py latency \
  --output logs/network-lab-20261020/latency-results.json
```

复用上图的双层 NAT，VM 主动连接真实模拟端地址 192.168.15.38。
每组测 5 帧，服务端在写当前帧前记录单调时钟，客户端收完整帧后记录结束时间。
容器共享同一 Linux 单调时钟，因此无需估计时钟偏移；这模拟客户端 `transferMs`
的起止位置，但不模拟 Android 编解码和实际 KrPlay 帧内容。
独立 TCP 控制连接每次回显完成后等待 20 ms 再探测，分别记录空闲 RTT 和传帧期间 RTT。
采样间隔会受阻塞影响，其 P95 用于组内比较，不等于产品长时间采样的 P95。

八组包含：无损无限速基线、只暂停视频读取线程 650 ms、48/24/12 Mbps 带宽对照、
64/256/1024 KiB 帧大小对照，以及当前帧前发送 1 MiB 前序数据的积压对照。
带宽限制仅设置在模型宿主通往 VM 的 eth0 下行出口；使用 TBF、128 KiB 突发桶、
2 秒队列容量，不注入传播延迟或丢包。小于突发桶的独立帧可能立即发送，不能直接
用这些短帧的传输时间推算持续带宽。`tc -s` 计数可能跨 replace 累积，保留原文便于审核。
两个端点请求 512 KiB TCP 缓冲；内核可能加倍或截断，不能视为真实车机的实际缓冲大小。

2026-10-20 首轮复现 12 Mbps / 1 MiB 帧中位 678 ms，以及 24 Mbps / 前序积压中位
689 ms，未注入固定 665 ms 延迟。视频读取停顿组则是人为注入的机制验证，不能当作
发现真实车机暂停。完整结果、范围和实车关联限制见 `logs/network-lab-20261020/latency-report.md`。

## Android 实际网络路径测试（独立于 Docker）

`KrPlayPerfProbe.java` 在 Android 本机通过 TCP 8084 验证接收端身份，测 40 次回显，
可选收、发各 8 MiB。只访问传入的设备地址，不访问外网。示例使用项目已有 SDK：

```sh
mkdir -p /private/tmp/krplay-path-probe
.local/android/jdk-21/Contents/Home/bin/javac --release 8 \
  -d /private/tmp/krplay-path-probe tools/network-lab/KrPlayPerfProbe.java
JAVA_HOME="$PWD/.local/android/jdk-21/Contents/Home" \
.local/android/sdk/build-tools/36.0.0/d8 --min-api 29 \
  --output /private/tmp/krplay-path-probe/probe.jar /private/tmp/krplay-path-probe/KrPlayPerfProbe.class
.local/android/sdk/platform-tools/adb -s CLIENT_SERIAL push \
  /private/tmp/krplay-path-probe/probe.jar /data/local/tmp/krplay-core-probe.jar
.local/android/sdk/platform-tools/adb -s CLIENT_SERIAL shell \
  CLASSPATH=/data/local/tmp/krplay-core-probe.jar app_process /system/bin KrPlayPerfProbe 6.6.6.6 speed
.local/android/sdk/platform-tools/adb -s CLIENT_SERIAL shell rm /data/local/tmp/krplay-core-probe.jar
```

ADB 仅启动进程和取结果，不转发测试流量。这是 shell UID 的默认路由，不能代替应用的
`Network.bindSocket` 路径；必须同时在客户端「性能 → 双向传输测速 · 共 16 MiB」交叉验证。
内置测试使用与视频相同的目标和 `LocalTransport.bind`，保存实际目标、本地地址与绑定
网络到应用私有 `files/link-test.txt`。可通过「保存并复制诊断」携带结果，开发包也可用
`adb shell run-as com.krplay.client cat files/link-test.txt` 读取。该入口是局域网测速，
不依赖借网服务。当前只有热点上的 Android 客户端实测，不能替代极氪 VM 实测。
