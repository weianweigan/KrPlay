package com.zeekr.bridge
import android.os.ParcelFileDescriptor
import java.io.Closeable
/** WheelPlay-style retained IPv4 address; caller subsequently establishes its real/placeholder VPN. */
internal class FixedEntryAnchor:Closeable {
    private var owner:ParcelFileDescriptor?=null
    private var retained:ParcelFileDescriptor?=null
    fun establish(create:()->ParcelFileDescriptor){close();val fd=create();owner=fd
        try{retained=ParcelFileDescriptor.dup(fd.fileDescriptor)}catch(e:Exception){close();throw e}
    }
    override fun close(){runCatching{retained?.close()};runCatching{owner?.close()};retained=null;owner=null}
}
