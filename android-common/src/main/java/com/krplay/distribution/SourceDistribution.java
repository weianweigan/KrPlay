// SPDX-License-Identifier: GPL-3.0-only
package com.krplay.distribution;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

/** Shared source offer and offline legal text for both Android applications. */
public final class SourceDistribution {
    public static final String REPOSITORY = "https://github.com/weianweigan/KrPlay";
    private SourceDistribution() {}

    public static JSONObject info(Context context) {
        try (java.io.InputStream input = context.getAssets().open("distribution/build.json")) {
            return new JSONObject(read(input));
        } catch (Exception error) {
            // Never advertise an unverified revision when metadata is unavailable.
            return new JSONObject();
        }
    }

    public static String sourceUrl(Context context) {
        return info(context).optString("sourceUrl", REPOSITORY);
    }

    public static String versionLabel(Context context) {
        String revision = info(context).optString("revision", "");
        return revision.matches("[0-9a-f]{40}") ? revision.substring(0, 12) : "local working tree";
    }

    /** Accept only this project's immutable revision URLs from the remote receiver. */
    public static String receiverSourceUrl(JSONObject remote) {
        JSONObject source = remote.optJSONObject("sourceDistribution");
        String revision = source == null ? "" : source.optString("revision", "");
        return revision.matches("[0-9a-f]{40}") ? REPOSITORY + "/tree/" + revision : REPOSITORY;
    }

    public static void open(Activity activity, String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (android.content.ActivityNotFoundException error) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                activity.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("KrPlay source", url));
            Toast.makeText(activity, "URL copied: " + url, Toast.LENGTH_LONG).show();
        }
    }

    public static void showLicenses(Activity activity) {
        TextView text = new TextView(activity);
        text.setTextIsSelectable(true);
        int padding = Math.round(20 * activity.getResources().getDisplayMetrics().density);
        text.setPadding(padding, padding, padding, padding);
        try (java.io.InputStream input = activity.getAssets().open("distribution/NOTICE.txt")) {
            text.setText(read(input));
        } catch (java.io.IOException error) {
            text.setText("License text unavailable. " + REPOSITORY + "/blob/main/THIRD_PARTY_NOTICES.md");
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(text);
        new AlertDialog.Builder(activity).setTitle("Copyright & licenses / 版权与许可")
            .setView(scroll).setPositiveButton(android.R.string.ok, null).show();
    }

    private static String read(java.io.InputStream input) throws java.io.IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        return new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
