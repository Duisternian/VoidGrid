package com.duisternis.voidgrid.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.duisternis.voidgrid.ui.viewmodel.ImageSearchViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

// ─── Extrai cor dominante do thumbnail com cache no ViewModel ─────────────────
// thumbnailUrl = null desativa o gradiente (usado pra imagens transparentes)

@Composable
fun DominantColorBox(
    thumbnailUrl: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    viewModel: ImageSearchViewModel = koinViewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val cached = thumbnailUrl?.let { viewModel.colorCache[it] }
    var dominantColor by remember(thumbnailUrl) {
        mutableStateOf(cached ?: Color(0xFF1A1A1A))
    }

    LaunchedEffect(thumbnailUrl, viewModel.colorCache.size) {
        thumbnailUrl?.let { url ->
            viewModel.colorCache[url]?.let { dominantColor = it }
        }
    }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl == null) return@LaunchedEffect
        if (viewModel.colorCache.containsKey(thumbnailUrl)) return@LaunchedEffect

        try {
            val bitmap = withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(50)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult)
                    (result.drawable as? BitmapDrawable)?.bitmap
                else null
            }

            bitmap?.let { bmp ->
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bmp).generate()
                }
                val swatch = palette.dominantSwatch
                    ?: palette.vibrantSwatch
                    ?: palette.mutedSwatch
                swatch?.let { s ->
                    val color = Color(s.rgb)
                    viewModel.cacheColor(thumbnailUrl, color)
                    dominantColor = color
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { }
    }

    // thumbnailUrl null = sem gradiente, fundo transparente
    val targetColor = if (thumbnailUrl == null) Color.Transparent else dominantColor

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(600),
        label = "dominantColor"
    )

    val darkVariant = animatedColor.copy(
        red = animatedColor.red * 0.4f,
        green = animatedColor.green * 0.4f,
        blue = animatedColor.blue * 0.4f
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(colors = listOf(animatedColor, darkVariant))
        )
    ) {
        content()
    }
}