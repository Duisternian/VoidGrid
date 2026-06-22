package com.duisternis.voidgrid.data.local

import android.content.Context
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.URL
import kotlin.coroutines.resume

/**
 * Usa o ML Kit para classificar uma imagem a partir da URL da thumbnail
 * e retornar tags em inglês. Roda local, sem internet extra, sem custo.
 */
object ImageLabeler {

    // Só aceita labels com confiança >= 70%
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.70f)
            .build()
    )

    suspend fun labelsFromUrl(thumbnailUrl: String): List<String> {
        return try {
            // Baixa a thumbnail (já é pequena, rápido)
            val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val stream = URL(thumbnailUrl).openStream()
                BitmapFactory.decodeStream(stream)
            } ?: return emptyList()

            val image = InputImage.fromBitmap(bitmap, 0)

            suspendCancellableCoroutine { cont ->
                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        val tags = labels
                            .map { it.text.lowercase().replace(" ", "_") }
                            .take(6) // máximo 6 tags por imagem
                        cont.resume(tags)
                    }
                    .addOnFailureListener {
                        cont.resume(emptyList())
                    }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}