package com.krplay.transport;

import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import com.krplay.protocol.Wire;
import java.net.Socket;
import java.net.SocketException;

/** Android/Linux-only options; unavailable options never prevent playback. */
public final class VideoSocketOptions {
    private VideoSocketOptions() {}
    // Stable Linux UAPI value from linux/tcp.h; not exposed by all Android SDKs.
    private static final int TCP_NOTSENT_LOWAT=25;
    public static void configure(Socket socket) throws SocketException {
        Wire.videoSocket(socket);
        try(ParcelFileDescriptor fd=ParcelFileDescriptor.fromSocket(socket)) {
            Os.setsockoptInt(fd.getFileDescriptor(),OsConstants.IPPROTO_TCP,TCP_NOTSENT_LOWAT,16*1024);
            Log.i("KrPlay-Video","TCP_NOTSENT_LOWAT configured: 16384 bytes");
        } catch(Exception unsupported) {
            Log.i("KrPlay-Video","TCP_NOTSENT_LOWAT unavailable: "+unsupported.getClass().getSimpleName());
        }
    }
}
