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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

// ─── Extrai cor dominante do thumbnail com cache no ViewModel ─────────────────

@Composable
fun DominantColorBox(
    thumbnailUrl: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    viewModel: ImageSearchViewModel = koinViewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Verifica cache antes de extrair
    val cached = thumbnailUrl?.let { viewModel.colorCache[it] }
    var dominantColor by remember(thumbnailUrl) {
        mutableStateOf(cached ?: Color(0xFF1A1A1A))
    }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl == null) return@LaunchedEffect

        // Se já tem no cache, não precisa extrair novamente
        if (viewModel.colorCache.containsKey(thumbnailUrl)) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(50)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let {
                        val palette = Palette.from(it).generate()
                        val swatch = palette.dominantSwatch
                            ?: palette.vibrantSwatch
                            ?: palette.mutedSwatch
                        swatch?.let { s ->
                            val color = Color(s.rgb)
                            viewModel.cacheColor(thumbnailUrl, color)
                            dominantColor = color
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
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
            Brush.verticalGradient(
                colors = listOf(animatedColor, darkVariant)
            )
        )
    ) {
        content()
    }
}