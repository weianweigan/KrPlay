// SPDX-License-Identifier: GPL-3.0-only
package com.krplay.update;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import com.krplay.distribution.SourceDistribution;
import java.io.*;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.util.concurrent.*;

/** Foreground update flow: check, download, verify, grant installation permission, install. */
public final class UpdateActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status;
    private ProgressBar progress;
    private Button download, retry;
    private GitHubRelease release;
    private boolean busy, waitingPermission, ready, resumed, autoDownload;
    private volatile boolean destroyed;
    private volatile HttpURLConnection connection;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        autoDownload = state == null && getIntent().getBooleanExtra("download", false);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        panel.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(this); title.setText("应用更新"); title.setTextSize(24); panel.addView(title);
        status = new TextView(this); status.setTextSize(16); status.setTextIsSelectable(true); panel.addView(status);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100); progress.setVisibility(View.GONE); panel.addView(progress);
        download = button(panel, "直接更新", this::download);
        retry = button(panel, "重新检查", this::check);
        button(panel, "前往官网下载最新安装包", () -> SourceDistribution.open(this, AppUpdates.WEBSITE));
        button(panel, "返回", this::finish);
        ScrollView scroll = new ScrollView(this); scroll.addView(panel); setContentView(scroll);
        check();
    }

    private Button button(LinearLayout parent, String text, Runnable action) {
        Button button = new Button(this); button.setText(text); button.setOnClickListener(v -> action.run());
        parent.addView(button); return button;
    }

    private void busy(boolean value) {
        busy = value; retry.setEnabled(!value);
        download.setEnabled(!value && (ready || release != null && !release.apk.isEmpty()));
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    private void check() {
        if (busy) return;
        release = null; ready = false; busy(true); progress.setIndeterminate(true);
        status.setText("当前版本：" + AppUpdates.installedVersion(this) + "\n正在检查 GitHub Releases…");
        worker.execute(() -> {
            try {
                GitHubRelease result = GitHubRelease.fetch(getPackageName());
                ui(() -> {
                    boolean newer = result != null && GitHubRelease.isNewer(result.version, AppUpdates.installedVersion(this));
                    release = newer ? result : null;
                    busy(false);
                    status.setText("当前版本：" + AppUpdates.installedVersion(this) + "\n" +
                        (result == null ? "暂无正式发布版本" : !newer ? "当前已是最新版本" :
                        "最新版本：" + result.version + "\n\n" + result.notes +
                        (result.apk.isEmpty() ? "\n\n未找到对应安装包，请前往官网下载。" :
                        "\n\n直接更新会下载安装包，并由系统确认安装。")));
                    if (newer && autoDownload && !result.apk.isEmpty()) {
                        if (resumed) { autoDownload = false; download(); }
                    }
                });
            } catch (Exception error) { failure("检查失败，请确认设备可访问互联网后重试，或前往官网下载。\n" + error.getMessage()); }
        });
    }

    private void download() {
        if (busy) return;
        if (ready) { install(); return; }
        if (release == null || release.apk.isEmpty()) return;
        GitHubRelease selected = release;
        busy(true); progress.setIndeterminate(false); progress.setProgress(0);
        status.setText("正在下载 " + selected.version + "…");
        worker.execute(() -> {
            File partial = null;
            try {
                File target = UpdateApkProvider.apk(this);
                File directory = target.getParentFile();
                if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法创建更新目录");
                if (directory.getUsableSpace() < selected.size * 2) throw new IOException("存储空间不足");
                partial = File.createTempFile("update-", ".part", directory);
                connection = GitHubRelease.connect(selected.apk);
                if (connection.getResponseCode() != 200) throw new IOException("下载返回 HTTP " + connection.getResponseCode());
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                long total = 0; int lastPercent = -1;
                try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(partial)) {
                    byte[] buffer = new byte[65536]; int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (destroyed || Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                        total += count;
                        if (total > selected.size || total > GitHubRelease.MAX_APK_BYTES) throw new IOException("安装包大小不匹配");
                        output.write(buffer, 0, count); hash.update(buffer, 0, count);
                        int percent = (int) (total * 100 / selected.size);
                        if (percent != lastPercent) { lastPercent = percent; ui(() -> progress.setProgress(percent)); }
                    }
                }
                if (total != selected.size) throw new IOException("下载未完成，请重试");
                StringBuilder actual = new StringBuilder();
                for (byte b : hash.digest()) actual.append(String.format(java.util.Locale.US, "%02x", b & 255));
                if (!selected.digest.isEmpty() && !selected.digest.equalsIgnoreCase("sha256:" + actual))
                    throw new IOException("安装包 SHA-256 校验失败");
                validateApk(partial, selected.version);
                if (destroyed || Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
                if (!partial.renameTo(target)) throw new IOException("无法保存安装包");
                ui(() -> {
                    ready = true; download.setText("安装更新"); busy(false);
                    status.setText("下载和校验完成：" + selected.version + "\n请在系统界面确认安装。");
                    if (resumed) install();
                });
            } catch (Exception error) { failure("更新失败：" + error.getMessage() + "\n可重试，或前往官网下载最新版安装包。"); }
            finally {
                HttpURLConnection active = connection; if (active != null) active.disconnect(); connection = null;
                if (partial != null) partial.delete();
            }
        });
    }

    private void validateApk(File apk, String version) throws Exception {
        PackageManager pm = getPackageManager();
        PackageInfo incoming = pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNING_CERTIFICATES);
        PackageInfo installed = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
        validatePackages(incoming, installed, version);
    }

    static void validatePackages(PackageInfo incoming, PackageInfo installed, String version) throws IOException {
        if (incoming == null || !installed.packageName.equals(incoming.packageName))
            throw new IOException("安装包与当前应用包名不兼容");
        if (incoming.versionName == null || !version.replaceFirst("^v", "").equals(incoming.versionName.replaceFirst("^v", "")))
            throw new IOException("安装包版本与发布版本不一致");
        if (incoming.getLongVersionCode() <= installed.getLongVersionCode())
            throw new IOException("安装包版本号未递增，无法覆盖更新");
        if (incoming.signingInfo == null || installed.signingInfo == null)
            throw new IOException("无法验证安装包签名");
        java.util.Set<Signature> before = new java.util.HashSet<>(java.util.Arrays.asList(installed.signingInfo.getApkContentsSigners()));
        java.util.Set<Signature> after = new java.util.HashSet<>(java.util.Arrays.asList(incoming.signingInfo.getApkContentsSigners()));
        if (before.isEmpty() || !before.equals(after))
            throw new IOException("安装包签名与已安装版本不同，无法覆盖安装。请前往官网查看安装说明");
    }

    private void install() {
        if (!ready || !resumed) return;
        if (!getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this).setTitle("允许安装更新")
                .setMessage("请允许此应用安装未知来源应用，返回后继续安装。")
                .setPositiveButton("去授权", (d, w) -> {
                    try {
                        waitingPermission = true;
                        startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                    } catch (RuntimeException error) {
                        waitingPermission = false; status.setText("无法打开安装授权设置，请前往官网下载并安装。");
                    }
                }).setNegativeButton("取消", null).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE)
                .setDataAndType(UpdateApkProvider.uri(this), "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri("KrPlay update", UpdateApkProvider.uri(this)));
            startActivity(intent);
        } catch (RuntimeException error) { status.setText("系统安装器不可用，请前往官网下载并安装。\n" + error.getMessage()); }
    }

    private void ui(Runnable action) { runOnUiThread(() -> { if (!destroyed && !isFinishing()) action.run(); }); }
    private void failure(String message) { ui(() -> { busy(false); status.setText(message); }); }
    @Override public void onResume() {
        super.onResume(); resumed = true;
        if (waitingPermission) {
            waitingPermission = false;
            if (getPackageManager().canRequestPackageInstalls()) install();
            else status.setText("尚未允许安装更新。可点击“安装更新”重新授权，或前往官网下载。");
        } else if (autoDownload && release != null && !busy) { autoDownload = false; download(); }
    }
    @Override public void onPause() { resumed = false; super.onPause(); }
    @Override public void onDestroy() {
        destroyed = true; worker.shutdownNow();
        HttpURLConnection active = connection; if (active != null) active.disconnect();
        super.onDestroy();
    }
}
