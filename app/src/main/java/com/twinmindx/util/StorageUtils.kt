package com.twinmindx.util

import android.os.Environment
import android.os.StatFs

object StorageUtils {

    private const val MIN_FREE_BYTES = 50L * 1024 * 1024

    fun hasEnoughStorage(): Boolean {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            bytesAvailable > MIN_FREE_BYTES
        } catch (e: Exception) {
            false // Assume not enough storage if we can't determine
        }
    }

    fun getAvailableBytes(): Long {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            stat.blockSizeLong * stat.availableBlocksLong
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }
}
