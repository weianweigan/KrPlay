# 固定 APK 签名与 GitHub Secrets

固定签名来自一份长期复用的签名密钥库（keystore），无需购买证书。
每次发布使用同一份密钥库、同一个 alias 和对应密码，签名身份就保持一致。
KrPlay 和 KrPlay-Client 可以共用这份发布密钥，应用包名保持各自独立。

## 1. 先决定是否沿用已发布版本的密钥

若持有旧版本的原始签名私钥，优先沿用，才能覆盖安装旧版本。
仅有 APK 或证书指纹无法恢复私钥。之前 GitHub 临时 runner 自动生成的开发密钥，
若未备份，无法通过下载旧 APK 取回。

本地现有 `krplay-client/.signing/krplay.p12` 和
`.local/android/user/debug.keystore` 是两份不同的开发密钥，使用默认开发密码。
它们不应直接当作新的正式发布密钥。新建正式密钥后，签名不同的旧安装需要
一次手动卸载重装，可能丢失应用设置；之后沿用正式密钥就能覆盖更新。

## 2. 生成正式密钥（macOS / Linux）

在终端执行。项目已安装本地 JDK 时，可以先在仓库根目录运行
`eval "$(scripts/android.sh env)"`，再使用 `$JAVA_HOME/bin/keytool`。

```sh
umask 077
mkdir -p "$HOME/.krplay-signing"
chmod 700 "$HOME/.krplay-signing"
"$JAVA_HOME/bin/keytool" -genkeypair \
  -keystore "$HOME/.krplay-signing/krplay-release.p12" \
  -storetype PKCS12 -alias krplay \
  -keyalg RSA -keysize 3072 -validity 36500 \
  -dname "CN=KrPlay, O=KrPlay"
chmod 600 "$HOME/.krplay-signing/krplay-release.p12"
```

按提示输入并确认一份强随机密码，用密码管理器保存。命令不会把密码写入 shell
历史。PKCS12 使用相同的密钥库密码和私钥密码，下面两项密码 Secrets 都填这份密码。
只在首次创建时执行生成命令；之后不要重新生成或覆盖此文件。

备份 `krplay-release.p12` 至至少两个安全位置，密码另存于密码管理器。
不要把密钥库或密码提交到 Git、上传到 Release，或粘贴进聊天。
GitHub Secrets 是 CI 使用副本，不能替代自己的密钥备份。

可查看公开证书指纹，用于与 APK 核对：

```sh
"$JAVA_HOME/bin/keytool" -list -v \
  -keystore "$HOME/.krplay-signing/krplay-release.p12" -alias krplay
```

## 3. 设置 GitHub Actions Secrets

打开仓库 **Settings → Secrets and variables → Actions → New repository secret**，
创建以下四项，名称必须一致：

| Secret | 内容 |
| --- | --- |
| `KRPLAY_UPDATE_KEYSTORE_BASE64` | 密钥库文件的 Base64 文本 |
| `KRPLAY_UPDATE_KEYSTORE_PASSWORD` | 生成时设置的密钥库密码 |
| `KRPLAY_UPDATE_KEY_ALIAS` | `krplay` |
| `KRPLAY_UPDATE_KEY_PASSWORD` | PKCS12 填同一份密码 |

macOS 可直接把 Base64 复制到剪贴板，然后粘贴到第一项 Secret：

```sh
base64 < "$HOME/.krplay-signing/krplay-release.p12" | tr -d '\r\n' | pbcopy
```

Base64 只是文件的文本表示，不是加密；必须像私钥一样保管，不能贴到普通文件或日志。
使用 Linux 时可使用系统剪贴板工具，或在可信的本地编辑器中完成编码与粘贴。

## 4. 发布和验证

提交包含固定签名配置的代码后，创建新的版本标签，沿用现有发布流程。
缺少任何一项 Secrets，或者密码、alias 不匹配，发布会失败，不会回退到随机开发密钥。
CI 只在 runner 临时目录中还原 keystore，构建两端 APK，并在结束时清理临时文件。

打包阶段会验证两份 APK 的签名证书 SHA-256 都与 keystore 中导出的证书匹配，
并把公开证书指纹写入 `artifacts.json` 和 Release 说明。签名构建禁用 Gradle
configuration cache，密码不写进仓库文件、发布产物或构建日志。

固定签名不改变当前构建变体或应用 ID；版本号继续由 `scripts/release_tag.py` 生成。
升级版本还需要更高的 Android versionCode，以及用户在系统安装界面确认。

本地如需使用同一正式密钥，设置以下环境变量后运行原构建命令：
`KRPLAY_UPDATE_KEYSTORE_PATH`（密钥库绝对路径）、
`KRPLAY_UPDATE_KEYSTORE_PASSWORD`、`KRPLAY_UPDATE_KEY_ALIAS`、
`KRPLAY_UPDATE_KEY_PASSWORD`。避免在 shell 历史中写明文密码，并为签名构建加上
`--no-configuration-cache`。不设置这些变量时，本地开发构建仍使用原开发签名。

参考：[Android 应用签名](https://developer.android.com/studio/publish/app-signing)、
[GitHub Actions Secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)。
