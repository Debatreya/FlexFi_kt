package com.example.flexfi.flexcard

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FlexCardShareUtil {

    private const val CACHE_DIR = "flex_cards"

    fun saveToCache(context: Context, bitmap: android.graphics.Bitmap): File {
        val folder = File(context.cacheDir, CACHE_DIR)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        cleanupOldFiles(folder)

        val file = File(folder, "flex_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { output ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
            output.flush()
        }
        return file
    }

    fun createShareIntent(context: Context, imageFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "My FlexFi card for this month")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun cleanupOldFiles(folder: File) {
        val all = folder.listFiles().orEmpty().sortedByDescending { it.lastModified() }
        all.drop(12).forEach { stale ->
            stale.delete()
        }
    }
}
