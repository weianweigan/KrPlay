// SPDX-License-Identifier: GPL-3.0-only
package com.krplay.update;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Grants the system installer read-only access to exactly one private APK. */
public final class UpdateApkProvider extends ContentProvider {
    static File apk(Context context) { return new File(context.getFilesDir(), "updates/update.apk"); }
    static Uri uri(Context context) { return Uri.parse("content://" + context.getPackageName() + ".updates/update.apk"); }
    @Override public boolean onCreate() { return true; }
    private void check(Uri uri) {
        if (!uri(getContext()).equals(uri)) throw new IllegalArgumentException("Unknown update URI");
    }
    @Override public String getType(Uri uri) { check(uri); return "application/vnd.android.package-archive"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        check(uri);
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(apk(getContext()), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        check(uri);
        String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(columns);
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = "KrPlay-update.apk";
            if (OpenableColumns.SIZE.equals(columns[i])) values[i] = apk(getContext()).length();
        }
        cursor.addRow(values); return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
