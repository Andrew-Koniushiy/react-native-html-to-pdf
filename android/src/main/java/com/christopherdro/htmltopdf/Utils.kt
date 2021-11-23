package com.christopherdro.htmltopdf

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.*

fun savePdfPreQ(ctx: Context, file: File){
    Log.d("HOME_4", "in pre Q")
    val pdfDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

    if (!pdfDirectory.exists()){
        pdfDirectory.mkdir()
    }

    val dstFile = File(pdfDirectory, file.name)

    try {
        val inChannel = FileInputStream(file).channel
        val outChannel = FileOutputStream(dstFile).channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inChannel.close()
        outChannel.close()
        MediaScannerConnection.scanFile(ctx, arrayOf(file.toString()), null, null)
    } catch (e: Exception) {
        Log.d("HOME_5", "Pre Q error $e")
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun savePdfPostQ(ctx: Context, file: File){
    Log.d("HOME_6", "in post Q")
    val name = file.name
    val relativePath = Environment.DIRECTORY_DOWNLOADS


    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, name)

        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        put(MediaStore.MediaColumns.TITLE, name)
        put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
    }

    var uri: Uri? = null
    val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
    } else {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI;
    }
    try {
        uri = ctx.contentResolver.insert(contentUri, contentValues)

        if (uri == null){
            throw IOException("Failed to create new MediaStore record.")
        }

        ctx.contentResolver.openFileDescriptor(uri, "w", null)?.use {
            if (it.fileDescriptor != null) {
                with(mutableListOf(FileInputStream(file).channel, FileOutputStream(it.fileDescriptor).channel)) {
                    this[0].transferTo(0, this[0].size(), this[1])
                    this[0].close()
                    this[1].close()
                }
            }
        }
        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        ctx.contentResolver.update(uri, contentValues, null, null)
        MediaScannerConnection.scanFile(ctx, arrayOf(uri.toString()), null, null)

    } catch (e: IOException){
        if (uri != null)
        {
            ctx.contentResolver.delete(uri, null, null)
        }

        throw IOException(e)
    }

}