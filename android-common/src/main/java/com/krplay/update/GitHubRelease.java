// SPDX-License-Identifier: GPL-3.0-only
package com.krplay.update;

import com.krplay.distribution.SourceDistribution;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.math.BigInteger;

/** Release selection and HTTPS I/O shared by both apps. No account or token required. */
public final class GitHubRelease {
    public static final String LATEST = SourceDistribution.REPOSITORY + "/releases/latest";
    static final String API = "https://api.github.com/repos/weianweigan/KrPlay/releases/latest";
    static final long MAX_APK_BYTES = 512L * 1024 * 1024;
    public final String version, notes, page, apk, digest;
    public final long size;

    private GitHubRelease(String version, String notes, String page, String apk, String digest, long size) {
        this.version = version; this.notes = notes; this.page = page;
        this.apk = apk; this.digest = digest; this.size = size;
    }

    public static GitHubRelease fetch(String packageName) throws Exception {
        HttpURLConnection connection = connect(API);
        try {
            int status = connection.getResponseCode();
            if (status == 404) return null;
            if (status == 403 || status == 429) throw new IOException("GitHub 请求受限，请稍后重试或前往下载页");
            if (status != 200) throw new IOException("GitHub 返回 HTTP " + status);
            try (InputStream input = connection.getInputStream()) {
                return parse(new JSONObject(new String(readLimited(input, 2 * 1024 * 1024), "UTF-8")), packageName);
            }
        } finally { connection.disconnect(); }
    }

    static GitHubRelease parse(JSONObject json, String packageName) throws Exception {
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) return null;
        String version = json.getString("tag_name");
        if (!version.matches("v?\\d+\\.\\d+\\.\\d+")) throw new IOException("发布版本号格式不受支持");
        String page = json.getString("html_url");
        if (!projectUrl(page, "/releases/tag/")) throw new IOException("发布页地址无效");
        String label = packageName.startsWith("com.krplay.server") ? "KrPlay" : "KrPlay-Client";
        String[] names = {label + "-" + version + ".apk", label + "-debug.apk"};
        JSONArray assets = json.optJSONArray("assets");
        String apk = "", digest = "";
        long size = 0;
        for (String expected : names) {
            for (int i = 0; assets != null && i < assets.length(); i++) {
                JSONObject asset = assets.getJSONObject(i);
                if (!expected.equals(asset.optString("name")) || !"uploaded".equals(asset.optString("state"))) continue;
                String candidate = asset.optString("browser_download_url");
                if (!projectUrl(candidate, "/releases/download/")) continue;
                apk = candidate; size = asset.optLong("size"); digest = asset.optString("digest");
                if (size <= 0 || size > MAX_APK_BYTES) { apk = ""; size = 0; }
                break;
            }
            if (!apk.isEmpty()) break;
        }
        return new GitHubRelease(version, json.optString("body"), page, apk, digest, size);
    }

    static boolean projectUrl(String value, String path) {
        try {
            URI uri = new URI(value);
            return "https".equals(uri.getScheme()) && "github.com".equals(uri.getHost())
                && uri.getRawUserInfo() == null && uri.getPort() == -1
                && uri.getPath().startsWith("/weianweigan/KrPlay" + path);
        } catch (Exception ignored) { return false; }
    }

    /** Numeric SemVer comparison; build metadata/debug suffix never turns an equal version into an update. */
    public static boolean isNewer(String candidate, String installed) {
        String[] next = parts(candidate), current = parts(installed);
        if (next == null || current == null) return false;
        for (int i = 0; i < 3; i++) {
            int comparison = new BigInteger(next[i]).compareTo(new BigInteger(current[i]));
            if (comparison != 0) return comparison > 0;
        }
        // A stable release can replace an installed prerelease, but not an equal development build.
        return installed.replaceFirst("^v", "").matches("\\d+\\.\\d+\\.\\d+-(?!debug(?:$|[.+-])).+");
    }

    private static String[] parts(String value) {
        if (value == null || !value.matches("v?\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.+-]+)?")) return null;
        return value.replaceFirst("^v", "").split("[-+]", 2)[0].split("\\.");
    }

    static HttpURLConnection connect(String url) throws IOException {
        URL target = new URL(url);
        if (!"https".equals(target.getProtocol())) throw new IOException("仅允许 HTTPS 更新");
        HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "KrPlay-Update");
        connection.setRequestProperty("Accept", url.equals(API) ? "application/vnd.github+json" : "application/octet-stream");
        if (url.equals(API)) connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        return connection;
    }

    static byte[] readLimited(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int count;
        while ((count = input.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            if (output.size() + count > limit) throw new IOException("响应内容过大");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
