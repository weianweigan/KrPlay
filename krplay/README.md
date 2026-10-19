# KrPlay

面向闲置安卓手机的 CarPlay 接收与转发应用，Android 9+。完整派生源码由主仓库直接构建，无需 upstream/ 或 git submodule。

基于 fython/wheelplay `4ed3536822998babd086e4fcb1a384e791b0e2d4`，保留 GPL-3.0 LICENSE；历史上游说明见 README.upstream.md 和 README.zh-CN.upstream.md。KrPlay 扩展在 common/src/main/java/com/zeekr/bridge/，共用协议在项目根目录 protocol/。浏览器服务与相关配置已移除；车机通过原生 H.264/HEVC TCP 通道显示。唯一入口为启动 / iPhone / 设置三页，内置认证资源自动校验。

```sh
./gradlew :mobile:exportKrPlayDebug
```

构建需要 JDK 21、Android SDK 35/36、Build Tools 36.0.0、NDK 28.2.13676358。非 root VPN 使用项目内 native/hev-socks5-tunnel 源码构建；组件固定版本与来源记录在该目录 SOURCE.json，各组件许可证均保留。

公开构建不包含 CarPlay 认证材料。私人本地打包从项目根目录运行 scripts/build_local_server.sh。详细说明见项目根目录 docs/。
