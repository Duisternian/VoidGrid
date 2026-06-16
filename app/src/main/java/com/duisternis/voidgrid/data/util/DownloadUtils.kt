package com.duisternis.voidgrid.data.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ADICIONE ESTA LINHA AQUI:
object DownloadUtils {

    fun shareImage(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar"))
    }

    suspend fun downloadImage(context: Context, imageUrl: String, imageLoader: ImageLoader) {
        withContext(Dispatchers.IO) {
            try {
                val result = (imageLoader.execute(ImageRequest.Builder(context).data(imageUrl).build()) as SuccessResult).drawable
                val bitmap = (result as BitmapDrawable).bitmap
                val filename = "IMG_${System.currentTimeMillis()}.jpg"

                val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    })?.let { context.contentResolver.openOutputStream(it) }
                } else {
                    @Suppress("DEPRECATION")
                    FileOutputStream(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename))
                }
                fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Salvo!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro ao salvar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} // E FECHE A CHAVE AQUI NO FINAL