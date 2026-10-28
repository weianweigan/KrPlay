# 二层应用沙盒的视频管线

客户端位于极氪 Android 内的应用沙盒，虚拟地址可为 77.17.3.10，
固定入口为 6.6.6.6。代码中视频使用 8081，控制使用 8082，8084 是探测服务。
此处不再将该应用沙盒等同于 QNX Hypervisor。现有计时不足以单独证明
22ms 均由跨容器 IPC 引起；新旧管线需在同一车机、相同动态画面下对照。

## 客户端

ARM64 APK 默认使用 FFmpeg 7.1.1 H.264/HEVC 原生软解。
`thread_count=4`、`FF_THREAD_SLICE`、`AV_CODEC_FLAG_LOW_DELAY`，编译启用
AArch64 NEON。没有启用 FF_THREAD_FRAME，不引入帧级多线程排队。
Slice 并行实际利用率取决于上游码流，不能凭线程数保证四核并行。

网络、解码、渲染分别由独立线程处理。接收队列满时立即淘汰旧视频，保留
配置及尺寸信息；因参考链可能断裂，等待下一关键帧再恢复。队列未满时
解码保留参考依赖；渲染仅保留一个最新 AVFrame，转换为 RGBA 后直接
写入 ANativeWindow_lock 缓冲并 unlockAndPost，不经 MediaCodec。
颜色转换/窗口提交仍有成本；“提交完成”不是物理屏幕扫描完成。

软解初始化或运行失败时显式记录原因，并在下一关键帧回退 MediaCodec。
保留输入等待 60ms 告警、持续 750ms 无进展才重建的保护，避免单次调度
停顿触发 IDR 风暴。当前 APK ABI 为 arm64-v8a。

## 服务端

TCP_NODELAY 与 TCP_NOTSENT_LOWAT=16384 保持启用（内核不支持时记录日志）。
单次 SocketChannel.write 最大 8192 字节；不改变帧协议，不保证 TCP 包边界。
排队或单帧写入超过 10ms 时清空应用层未发送视频，进入等待关键帧状态。
已写出部分字节的帧必须完成，内核 TCP 缓冲不可用应用队列 Flush 撤回。
写入长期无进展仍由既有 2 秒看门狗关闭连接。

waitingKeyFrame 请求在客户端、服务端合并，正常最短 800ms；高 RTT 时
客户端延长为 2 秒。服务端会等在途帧和队列排空后再请求新的关键帧。
发送队列使用代际标记，避免 Flush 与发送线程取帧并发时发送过期参考帧。

## 上游编码边界

CarPlay 主链路仅转发 iPhone 已编码数据，当前协议/API 无 Slice 数量、QP
或 IDR 字节上限控制。不能把一个 NAL 拆成 4–8 段字节并声称生成多个 Slice。
仓库的 BoxRootMain 是另一个 Android MediaCodec 编码分支；其公开通用 API
也没有可保证支持的 Slice 数参数。故本次未宣称开启多 Slice 编码或轻量 IDR。
这些目标需要可控的源编码器，或另行设计转码管线并评估新增编码成本。

## 验证

- `scripts/test.py`：协议、冷却边界、真实 TCP 大帧暂停和恢复。
- 客户端 `testDebugUnitTest`：AVC/HEVC 接收队列淘汰、元数据保留及恢复。
- `connectedDebugAndroidTest`：设备编码真实 H.264/HEVC 单帧，经 FFmpeg JNI
  解码到 ANativeWindow，由 ImageReader 验证尺寸、时间戳和灰度像素。
- 车机验收需记录持续动态画面的解码耗时、渲染提交耗时、CPU/温度、
  丢帧及 IDR 请求率；不能用静态画面或模拟器数据承诺 1080p 性能。

2026-10-28 本地结果：客户端 JVM 单元测试、协议/TCP 集成测试、服务端
`:common:compileDebugKotlin` 均通过。ARM64 API 35 模拟器通过两个原生测试：
H.264 和 HEVC 的 JNI 加载、像素输出及渲染背压后的最新帧追赶。
Gradle connected 测试因离线缺 UTP 插件未运行，使用 `adb shell am instrument`
直接执行同一测试 APK，结果 `OK (2 tests)`。尚未连接极氪车机进行性能对照。
