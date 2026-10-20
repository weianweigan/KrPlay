// SPDX-License-Identifier: GPL-3.0-only
package com.krplay.update;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29, manifest = Config.NONE)
public class GitHubReleaseTest {
    private JSONObject release() throws Exception {
        return new JSONObject().put("tag_name", "v0.5.0")
            .put("html_url", "https://github.com/weianweigan/KrPlay/releases/tag/v0.5.0")
            .put("body", "更新说明").put("assets", new JSONArray()
                .put(asset("KrPlay-Client-debug.apk")).put(asset("KrPlay-debug.apk")));
    }
    private JSONObject asset(String name) throws Exception {
        return new JSONObject().put("name", name).put("state", "uploaded").put("size", 100)
            .put("digest", "sha256:1234")
            .put("browser_download_url", "https://github.com/weianweigan/KrPlay/releases/download/v0.5.0/" + name);
    }

    @Test public void comparesVersionsNumericallyAndHandlesBuildSuffix() {
        assertTrue(GitHubRelease.isNewer("v0.10.0", "0.9.9-debug"));
        assertTrue(GitHubRelease.isNewer("1.0.0", "0.99.99"));
        assertTrue(GitHubRelease.isNewer("0.5.0", "v0.5.0-rc.1"));
        assertFalse(GitHubRelease.isNewer("v0.5.0", "0.5.0-debug"));
        assertFalse(GitHubRelease.isNewer("v0.5.0", "0.5.0+build.12"));
        assertFalse(GitHubRelease.isNewer("0.5.0", "0.6.0"));
        assertFalse(GitHubRelease.isNewer("0.5.0", "0.5.0"));
        assertFalse(GitHubRelease.isNewer("0.5.0", "unknown"));
        assertFalse(GitHubRelease.isNewer("bad", "0.4.1"));
    }
    @Test public void choosesExactApkForEachApplication() throws Exception {
        assertTrue(GitHubRelease.parse(release(), "com.krplay.server.debug").apk.endsWith("/KrPlay-debug.apk"));
        assertTrue(GitHubRelease.parse(release(), "com.krplay.server").apk.endsWith("/KrPlay-debug.apk"));
        GitHubRelease client = GitHubRelease.parse(release(), "com.krplay.client");
        assertTrue(client.apk.endsWith("/KrPlay-Client-debug.apk"));
        assertEquals(100, client.size); assertEquals("sha256:1234", client.digest);
        assertEquals("更新说明", client.notes);
    }
    @Test public void selectsVersionedReleasePackagesBeforeLegacyNames() throws Exception {
        JSONObject json = release();
        json.getJSONArray("assets").put(asset("KrPlay-v0.5.0.apk")).put(asset("KrPlay-Client-v0.5.0.apk"));
        assertTrue(GitHubRelease.parse(json, "com.krplay.server.debug").apk.endsWith("/KrPlay-v0.5.0.apk"));
        assertTrue(GitHubRelease.parse(json, "com.krplay.client").apk.endsWith("/KrPlay-Client-v0.5.0.apk"));
    }
    @Test public void excludesDraftsAndPrereleases() throws Exception {
        assertNull(GitHubRelease.parse(release().put("draft", true), "com.krplay.client"));
        assertNull(GitHubRelease.parse(release().put("prerelease", true), "com.krplay.client"));
    }
    @Test public void missingAssetStillAllowsWebsiteDownload() throws Exception {
        GitHubRelease parsed = GitHubRelease.parse(release().put("assets", new JSONArray()), "com.krplay.client");
        assertEquals("", parsed.apk);
        assertEquals("https://krplay.pages.dev/", AppUpdates.WEBSITE);
    }
    @Test public void ignoresUnuploadedOrOversizedAssets() throws Exception {
        JSONObject json = release().put("assets", new JSONArray().put(asset("KrPlay-Client-debug.apk").put("state", "new")));
        assertEquals("", GitHubRelease.parse(json, "com.krplay.client").apk);
        json.put("assets", new JSONArray().put(asset("KrPlay-Client-debug.apk").put("size", GitHubRelease.MAX_APK_BYTES + 1)));
        assertEquals("", GitHubRelease.parse(json, "com.krplay.client").apk);
    }
    @Test public void rejectsForeignOrInsecureDownloadUrls() throws Exception {
        for (String url : new String[]{"http://github.com/weianweigan/KrPlay/releases/download/x/a.apk",
                "https://example.com/a.apk", "https://github.com/other/KrPlay/releases/download/x/a.apk",
                "https://github.com.evil.test/weianweigan/KrPlay/releases/download/x/a.apk"}) {
            JSONObject json = release().put("assets", new JSONArray().put(asset("KrPlay-Client-debug.apk").put("browser_download_url", url)));
            assertEquals("", GitHubRelease.parse(json, "com.krplay.client").apk);
        }
    }
    @Test(expected = IOException.class) public void rejectsUntrustedReleasePages() throws Exception {
        GitHubRelease.parse(release().put("html_url", "https://example.com/release"), "com.krplay.client");
    }
    @Test(expected = IOException.class) public void boundsApiResponseSize() throws Exception {
        GitHubRelease.readLimited(new ByteArrayInputStream(new byte[20]), 10);
    }
    private android.content.pm.PackageInfo packageInfo(String name, int code, String signature) {
        android.content.pm.PackageInfo info = new android.content.pm.PackageInfo();
        info.packageName = name; info.versionName = "v0.5.0"; info.setLongVersionCode(code);
        info.signingInfo = new android.content.pm.SigningInfo();
        org.robolectric.Shadows.shadowOf(info.signingInfo).setSignatures(new android.content.pm.Signature[]{new android.content.pm.Signature(signature)});
        return info;
    }
    @Test public void onlyInstallsNewerMatchingSignedPackages() throws Exception {
        android.content.pm.PackageInfo installed = packageInfo("com.krplay.client", 1, "1234");
        android.content.pm.PackageInfo incoming = packageInfo("com.krplay.client", 2, "1234");
        UpdateActivity.validatePackages(incoming, installed, "0.5.0");
        for (android.content.pm.PackageInfo invalid : new android.content.pm.PackageInfo[]{
            null, packageInfo("com.krplay.server.debug", 2, "1234"),
            packageInfo("com.krplay.client", 1, "1234"), packageInfo("com.krplay.client", 2, "5678")}) {
            try { UpdateActivity.validatePackages(invalid, installed, "v0.5.0"); fail("Incompatible APK accepted"); }
            catch (IOException expected) {}
        }
        try { UpdateActivity.validatePackages(incoming, installed, "v0.6.0"); fail("Wrong release accepted"); }
        catch (IOException expected) {}
    }
    @Test public void providerOnlyExposesExactReadOnlyApk() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        UpdateApkProvider provider = new UpdateApkProvider();
        ProviderInfo info = new ProviderInfo(); info.authority = context.getPackageName() + ".updates";
        provider.attachInfo(context, info);
        File apk = UpdateApkProvider.apk(context); apk.getParentFile().mkdirs();
        try (FileOutputStream output = new FileOutputStream(apk)) { output.write(new byte[]{1, 2, 3}); }
        Uri uri = UpdateApkProvider.uri(context);
        try (Cursor cursor = provider.query(uri, null, null, null, null)) {
            assertTrue(cursor.moveToFirst()); assertEquals(3, cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)));
        }
        try (android.os.ParcelFileDescriptor descriptor = provider.openFile(uri, "r")) { assertNotNull(descriptor); }
        try { provider.openFile(uri, "w"); fail("Write access allowed"); } catch (FileNotFoundException expected) {}
        try { provider.openFile(Uri.parse("content://" + info.authority + "/../secret"), "r"); fail("Traversal allowed"); }
        catch (IllegalArgumentException expected) {}
        apk.delete();
    }
}
