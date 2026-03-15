package com.twinmindx.util

import android.os.Environment
import android.os.StatFs
import android.util.Log

object StorageUtils {

    private const val MIN_FREE_BYTES = 50L * 1024 * 1024

    fun hasEnoughStorage(): Boolean {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
            bytesAvailable > MIN_FREE_BYTES
        } catch (e: Exception) {
            Log.w("StorageUtils", "Error checking storage: ${e.message}")
            false
        }
    }
}
