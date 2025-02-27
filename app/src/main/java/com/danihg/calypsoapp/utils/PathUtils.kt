package com.danihg.calypsoapp.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File

object PathUtils {
    @JvmStatic
    fun getRecordPath(): File {
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        return File(storageDir.absolutePath + "/Calypso")
    }

    @JvmStatic
    fun updateGallery(context: Context, path: String) {
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }
}