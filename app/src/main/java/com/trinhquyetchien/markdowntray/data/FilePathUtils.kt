package com.trinhquyetchien.markdowntray.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File

object FilePathUtils {

    fun getAbsolutePathFromUri(context: Context, uri: Uri): String? {
        val docId = try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.getDocumentId(uri)
            } else if (DocumentsContract.isTreeUri(uri)) {
                DocumentsContract.getTreeDocumentId(uri)
            } else null
        } catch (e: Exception) {
            null
        }

        if (docId != null) {
            val parts = docId.split(":")
            if (parts.size >= 2) {
                val type = parts[0]
                val relativePath = parts[1]
                if ("primary".equals(type, ignoreCase = true)) {
                    return "${Environment.getExternalStorageDirectory().absolutePath}/$relativePath"
                } else {
                    return "/storage/$type/$relativePath"
                }
            }
        }

        // Direct file scheme
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }

        return null
    }
}
