// SPDX-License-Identifier: GPL-3.0-only
package com.krplay.update;

import android.app.*;
import android.content.*;
import android.os.*;
import com.krplay.distribution.SourceDistribution;
import java.util.concurrent.*;

/** One quiet automatic check per process; manual checks remain available in settings. */
public final class AppUpdates implements Application.ActivityLifecycleCallbacks {
    public static final String WEBSITE = "https://krplay.pages.dev/";
    private static boolean checked;
    private final Activity activity;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private GitHubRelease pending;
    private boolean resumed, destroyed;
    private AlertDialog dialog;

    private AppUpdates(Activity activity) { this.activity = activity; }

    public static void checkOnStart(Activity activity) {
        if (checked) return;
        checked = true;
        AppUpdates check = new AppUpdates(activity);
        activity.getApplication().registerActivityLifecycleCallbacks(check);
        check.worker.execute(() -> {
            try {
                GitHubRelease release = GitHubRelease.fetch(activity.getPackageName());
                String installed = installedVersion(activity);
                if (release != null && GitHubRelease.isNewer(release.version, installed)) {
                    activity.runOnUiThread(() -> { check.pending = release; check.present(); });
                }
            } catch (Exception ignored) {
                // No network / no release must not interrupt startup or an active connection.
            } finally { check.worker.shutdown(); }
        });
    }

    public static String installedVersion(Context context) {
        try { return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName; }
        catch (Exception ignored) { return "unknown"; }
    }

    public static void open(Context context) {
        context.startActivity(new Intent(context, UpdateActivity.class));
    }

    private void present() {
        if (!resumed || destroyed || activity.isFinishing() || pending == null) return;
        GitHubRelease release = pending; pending = null;
        dialog = new AlertDialog.Builder(activity).setTitle("发现新版本 " + release.version)
            .setMessage("当前版本：" + installedVersion(activity) + "\n\n" + release.notes)
            .setPositiveButton("直接更新", (d, w) -> activity.startActivity(new Intent(activity, UpdateActivity.class).putExtra("download", true)))
            .setNeutralButton("前往官网下载", (d, w) -> SourceDistribution.open(activity, WEBSITE))
            .setNegativeButton("稍后", null).show();
    }

    @Override public void onActivityResumed(Activity a) { if (a == activity) { resumed = true; present(); } }
    @Override public void onActivityPaused(Activity a) { if (a == activity) resumed = false; }
    @Override public void onActivityDestroyed(Activity a) {
        if (a != activity) return;
        destroyed = true; worker.shutdownNow();
        if (dialog != null) dialog.dismiss();
        activity.getApplication().unregisterActivityLifecycleCallbacks(this);
    }
    @Override public void onActivityCreated(Activity a, Bundle b) {}
    @Override public void onActivityStarted(Activity a) {}
    @Override public void onActivityStopped(Activity a) {}
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
}
