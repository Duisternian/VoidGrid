package com.duisternis.voidgrid.data.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object DownloadUtils {

    fun shareImage(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar"))
    }

    suspend fun downloadImage(
        context: Context,
        imageUrl: String,
        imageLoader: ImageLoader
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val result = imageLoader.execute(
                ImageRequest.Builder(context).data(imageUrl).build()
            ) as? SuccessResult ?: error("Falha ao carregar imagem")

            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                ?: error("Drawable não é um BitmapDrawable")

            val filename = "IMG_${System.currentTimeMillis()}.jpg"

            val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                ) ?: error("Falha ao criar URI no MediaStore")
                context.contentResolver.openOutputStream(uri)
            } else {
                @Suppress("DEPRECATION")
                FileOutputStream(
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        filename
                    )
                )
            }

            outputStream?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                ?: error("OutputStream nulo")

            filename
        }
    }
}