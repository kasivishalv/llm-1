package com.example.ui.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageDownloadHelper {

    suspend fun downloadAndSaveImage(
        context: Context,
        imageUrlOrBase64: String,
        fileNamePrefix: String = "LLM1_Generated"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = when {
                imageUrlOrBase64.startsWith("data:image") -> {
                    val base64Data = imageUrlOrBase64.substringAfter("base64,")
                    val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                }
                imageUrlOrBase64.startsWith("http://") || imageUrlOrBase64.startsWith("https://") -> {
                    val url = URL(imageUrlOrBase64)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 15000
                    connection.readTimeout = 20000
                    connection.doInput = true
                    connection.connect()
                    connection.inputStream.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
                imageUrlOrBase64.startsWith("/") -> {
                    val file = File(imageUrlOrBase64)
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                imageUrlOrBase64.startsWith("file://") -> {
                    val parsedUri = imageUrlOrBase64.toUri()
                    val file = File(parsedUri.path ?: "")
                    if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else {
                        context.contentResolver.openInputStream(parsedUri)?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    }
                }
                imageUrlOrBase64.startsWith("content://") -> {
                    val uri = imageUrlOrBase64.toUri()
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }
                // Try as raw base64 if lengthy string without spaces or urls
                imageUrlOrBase64.length > 100 && !imageUrlOrBase64.contains(" ") -> {
                    try {
                        val decodedBytes = Base64.decode(imageUrlOrBase64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    } catch (t: Throwable) {
                        null
                    }
                }
                else -> null
            } ?: return@withContext false

            val filename = "${fileNamePrefix}_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LLM1")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext false

                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "LLM1")
                if (!appDir.exists()) appDir.mkdirs()
                val destFile = File(appDir, filename)
                FileOutputStream(destFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }

            try {
                bitmap.recycle()
            } catch (ignored: Exception) {}

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Image saved to Pictures/LLM1", Toast.LENGTH_SHORT).show()
            }
            true
        } catch (t: Throwable) {
            t.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save image: ${t.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
            false
        }
    }
}
