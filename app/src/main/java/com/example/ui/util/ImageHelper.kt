package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class PreparedImage(
    val localUri: String,
    val base64Data: String
)

object ImageHelper {
    suspend fun processImageUri(context: Context, uri: Uri): PreparedImage? = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            val maxDimension = 1024

            // Copy stream once to a local temp file to avoid stream reuse or permission revocation issues
            tempFile = File(context.cacheDir, "img_temp_${System.currentTimeMillis()}.tmp")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            if (!tempFile.exists() || tempFile.length() == 0L) return@withContext null

            // 1. Measure image dimensions first to calculate safe inSampleSize
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(tempFile.absolutePath, options)

            val origWidth = options.outWidth
            val origHeight = options.outHeight
            if (origWidth <= 0 || origHeight <= 0) return@withContext null

            // Calculate subsampling power-of-2
            var sampleSize = 1
            while ((origWidth / sampleSize) > maxDimension * 2 || (origHeight / sampleSize) > maxDimension * 2) {
                sampleSize *= 2
            }

            // 2. Decode the downscaled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampledBitmap = BitmapFactory.decodeFile(tempFile.absolutePath, decodeOptions)
                ?: return@withContext null

            // 3. Scale down precisely if still exceeding maxDimension
            val curWidth = sampledBitmap.width
            val curHeight = sampledBitmap.height
            val finalBitmap = if (curWidth > maxDimension || curHeight > maxDimension) {
                val ratio = curWidth.toFloat() / curHeight.toFloat()
                val targetWidth: Int
                val targetHeight: Int
                if (ratio > 1f) {
                    targetWidth = maxDimension
                    targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
                } else {
                    targetHeight = maxDimension
                    targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
                }
                val scaled = Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true)
                if (scaled != sampledBitmap) {
                    sampledBitmap.recycle()
                }
                scaled
            } else {
                sampledBitmap
            }

            // 4. Compress to JPEG
            val byteStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, byteStream)
            val byteArray = byteStream.toByteArray()
            finalBitmap.recycle()

            // 5. Save to persistent app internal storage
            val imagesDir = File(context.filesDir, "chat_images").apply { if (!exists()) mkdirs() }
            val imageFile = File(imagesDir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(imageFile).use { fos ->
                fos.write(byteArray)
            }

            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            PreparedImage(
                localUri = imageFile.absolutePath,
                base64Data = base64String
            )
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        } finally {
            try {
                tempFile?.delete()
            } catch (ignored: Exception) {}
        }
    }
}
