package com.duisternis.voidgrid.data.util

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extrai a cor dominante de uma imagem e a normaliza para uma das categorias
 * de cor aceitas pelo filtro `f=,,,,,color:X` da API interna do DuckDuckGo
 * Images (endpoint i.js).
 *
 * Reaproveita a mesma cascata de swatches (dominant → vibrant → muted) usada
 * em DominantColorBox, para manter consistência entre a cor mostrada na UI
 * e a cor usada para gerar queries do feed "Para Você".
 */
object ColorCategorizer {

    // Categorias aceitas pela API de imagens do DuckDuckGo
    private const val RED = "Red"
    private const val ORANGE = "Orange"
    private const val YELLOW = "Yellow"
    private const val GREEN = "Green"
    private const val TEAL = "Teal"
    private const val BLUE = "Blue"
    private const val PURPLE = "Purple"
    private const val PINK = "Pink"
    private const val BROWN = "Brown"
    private const val BLACK = "Black"
    private const val WHITE = "White"
    private const val GRAY = "Gray"

    /**
     * Baixa a thumbnail (via Coil) e retorna a categoria de cor dominante,
     * ou null se a imagem não pôde ser carregada/analisada.
     */
    suspend fun categorizeFromUrl(
        thumbnailUrl: String?,
        imageLoader: ImageLoader,
        context: android.content.Context
    ): String? {
        if (thumbnailUrl.isNullOrBlank()) return null

        return try {
            val bitmap = withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(50)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                } else null
            } ?: return null

            categorizeFromBitmap(bitmap)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Roda Palette sobre um bitmap já carregado e retorna a categoria de cor.
     * Use esta versão se você já tem o bitmap em mãos (evita download duplicado).
     */
    suspend fun categorizeFromBitmap(bitmap: Bitmap): String? {
        return try {
            val palette = withContext(Dispatchers.Default) {
                Palette.from(bitmap).generate()
            }
            val swatch = palette.dominantSwatch
                ?: palette.vibrantSwatch
                ?: palette.mutedSwatch
                ?: return null

            mapRgbToCategory(swatch.rgb)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Mapeia um RGB para a categoria de cor mais próxima aceita pela API do DDG.
     */
    fun mapRgbToCategory(rgb: Int): String {
        val r = AndroidColor.red(rgb)
        val g = AndroidColor.green(rgb)
        val b = AndroidColor.blue(rgb)

        val hsv = FloatArray(3)
        AndroidColor.RGBToHSV(r, g, b, hsv)
        val hue = hsv[0]        // 0-360
        val saturation = hsv[1] // 0-1
        val value = hsv[2]      // 0-1

        return when {
            saturation < 0.15f && value > 0.85f -> WHITE
            saturation < 0.15f && value < 0.20f -> BLACK
            saturation < 0.15f -> GRAY
            // Marrom: tons de vermelho/laranja com saturação e valor baixos
            hue in 15f..45f && value < 0.55f && saturation > 0.25f -> BROWN
            hue < 15f || hue >= 345f -> RED
            hue < 45f -> ORANGE
            hue < 65f -> YELLOW
            hue < 170f -> GREEN
            hue < 200f -> TEAL
            hue < 260f -> BLUE
            hue < 320f -> PURPLE
            else -> PINK
        }
    }
}