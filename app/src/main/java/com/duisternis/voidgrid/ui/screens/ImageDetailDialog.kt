package com.duisternis.voidgrid.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.duisternis.voidgrid.R
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.util.DownloadUtils
import com.duisternis.voidgrid.ui.components.DominantColorBox
import com.duisternis.voidgrid.ui.viewmodel.ImageSearchViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.core.graphics.drawable.toDrawable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageDetailDialog(
    item: SearchItem,
    allImages: List<SearchItem>,
    baseQuery: String,
    onDismiss: () -> Unit,
    imageLoader: ImageLoader,
    viewModel: ImageSearchViewModel = koinViewModel()
) {
    var currentItem by remember(item) { mutableStateOf(item) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val actionBarBg = Color(0xFF1F1F1F)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AnimatedContent(
                    targetState = currentItem,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    },
                    label = "contentFade"
                ) { activeItem ->

                    // Sugestões refinadas por domínio dominante — busca em formação de base
                    var suggestedItems by remember(activeItem.link) {
                        mutableStateOf<List<SearchItem>>(emptyList())
                    }
                    val isLoadingSuggestions = viewModel.isSuggestionsLoading(activeItem.link)

                    LaunchedEffect(activeItem.link) {
                        viewModel.loadRefinedSuggestions(
                            link = activeItem.link,
                            baseQuery = baseQuery,
                            allImages = allImages
                        ) { result ->
                            suggestedItems = result
                        }
                    }

                    LazyVerticalStaggeredGrid(
                        state = rememberLazyStaggeredGridState(),
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            top = 12.dp,
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp
                        ),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }

                                val mainIsTransparent = viewModel.isTransparent(activeItem.link)
                                var mainImageError by remember(activeItem.link) { mutableStateOf(false) }

                                if (!mainImageError) {
                                    DominantColorBox(
                                        thumbnailUrl = if (mainIsTransparent) null else activeItem.encodedThumbnail,
                                        imageLoader = imageLoader,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(activeItem.encodedLink)
                                                .crossfade(400)
                                                .build(),
                                            imageLoader = imageLoader,
                                            contentDescription = null,
                                            contentScale = ContentScale.FillWidth,
                                            onError = { mainImageError = true },
                                            onSuccess = { result ->
                                                val hwBitmap = (result.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                                if (hwBitmap != null && hwBitmap.hasAlpha()) {
                                                    val bitmap = hwBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                                                    if (bitmap != null) {
                                                        val pixels = IntArray(bitmap.width * bitmap.height)
                                                        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                                                        val transparentCount = pixels.count { (it ushr 24) and 0xFF < 200 }
                                                        if (transparentCount > pixels.size * 0.05) viewModel.markTransparent(activeItem.link)
                                                        bitmap.recycle()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                        .background(actionBarBg, RoundedCornerShape(12.dp))
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val actionItems = listOf(
                                        Triple("Compartilhar", R.drawable.ios_share_24) {
                                            DownloadUtils.shareImage(context, activeItem.link)
                                        },
                                        Triple("Baixar", R.drawable.download_24) {
                                            scope.launch {
                                                val result = DownloadUtils.downloadImage(
                                                    context, activeItem.link, imageLoader
                                                )
                                                val message = result.fold(
                                                    onSuccess = { "Imagem salva!" },
                                                    onFailure = { "Erro ao salvar: ${it.message}" }
                                                )
                                                snackbarHostState.showSnackbar(
                                                    message = message,
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        },
                                        Triple("Favorite", R.drawable.ic_favorite_24) { /* TODO */ },
                                        Triple("Abrir", R.drawable.ic_open_in_browser_24) {
                                            DownloadUtils.openInBrowser(context, activeItem.link)
                                        }
                                    )
                                    actionItems.forEach { (label, iconRes, action) ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable { action() }
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = iconRes),
                                                contentDescription = label,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = label,
                                                color = Color.White.copy(alpha = 0.85f),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Mais imagens",
                                        color = Color.White.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    if (isLoadingSuggestions) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        CircularProgressIndicator(
                                            color = Color.White.copy(alpha = 0.4f),
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 1.5.dp
                                        )
                                    }
                                }
                            }
                        }

                        items(items = suggestedItems, key = { "${it.link}_${it.source}" }) { similar ->
                            val isError = viewModel.isError(similar.link)
                            if (isError) return@items

                            val aspectRatio = if (similar.width > 0 && similar.height > 0)
                                similar.width.toFloat() / similar.height.toFloat()
                            else 0.75f

                            val similarIsTransparent = viewModel.isTransparent(similar.link)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { currentItem = similar }
                            ) {
                                DominantColorBox(
                                    thumbnailUrl = if (similarIsTransparent) null else similar.encodedThumbnail,
                                    imageLoader = imageLoader,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(aspectRatio)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(similar.encodedLink)
                                            .crossfade(200)
                                            .placeholder(android.graphics.Color.TRANSPARENT.toDrawable())
                                            .error(android.graphics.Color.TRANSPARENT.toDrawable())
                                            .build(),
                                        imageLoader = imageLoader,
                                        contentDescription = null,
                                        contentScale = ContentScale.FillWidth,
                                        onError = { viewModel.markError(similar.link) },
                                        onSuccess = { result ->
                                            val hwBitmap = (result.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                            if (hwBitmap != null && hwBitmap.hasAlpha()) {
                                                val bitmap = hwBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                                                if (bitmap != null) {
                                                    val pixels = IntArray(bitmap.width * bitmap.height)
                                                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                                                    val transparentCount = pixels.count { (it ushr 24) and 0xFF < 200 }
                                                    if (transparentCount > pixels.size * 0.05) viewModel.markTransparent(similar.link)
                                                    bitmap.recycle()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF518224),
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}