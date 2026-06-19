package com.duisternis.voidgrid.ui.screens

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.ui.components.DominantColorBox
import com.duisternis.voidgrid.ui.viewmodel.ImageSearchViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSearchScreen(
    pagingItems: LazyPagingItems<SearchItem>,
    onSearch: (String) -> Unit,
    imageLoader: ImageLoader,
    hasQuery: Boolean,
    viewModel: ImageSearchViewModel = koinViewModel()
) {
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<SearchItem?>(null) }
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyStaggeredGridState()
    val context = LocalContext.current

    LaunchedEffect(hasQuery, pagingItems.loadState.source.refresh) {
        val isNewSearch = pagingItems.loadState.source.refresh is LoadState.Loading && hasQuery
        if (isNewSearch) gridState.scrollToItem(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            state = gridState,
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(top = 113.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = pagingItems.itemCount,
                key = { index -> index }
            ) { index ->
                val item = pagingItems[index] ?: return@items
                val isError = viewModel.isError(item.link)

                if (!isError) {
                    val aspectRatio = if (item.width > 0 && item.height > 0)
                        item.width.toFloat() / item.height.toFloat()
                    else 0.75f

                    val imageRequest = remember(item.link) {
                        ImageRequest.Builder(context)
                            .data(item.encodedLink)
                            .crossfade(300)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .precision(Precision.INEXACT)
                            .build()
                    }

                    // Lê do cache reativo — recompõe quando markTransparent é chamado
                    val isTransparent = viewModel.isTransparent(item.link)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                focusManager.clearFocus()
                                selectedItem = item
                            }
                    ) {
                        DominantColorBox(
                            thumbnailUrl = if (isTransparent) null else item.encodedThumbnail,
                            imageLoader = imageLoader,
                            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)
                        ) {
                            AsyncImage(
                                model = imageRequest,
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                onSuccess = { result ->
                                    viewModel.markLoaded(item.link)
                                    // Detecta transparência — copia pra SOFTWARE antes de ler pixels
                                    // pois bitmaps HARDWARE não suportam getPixels()
                                    val hwBitmap =
                                        (result.result.drawable as? BitmapDrawable)?.bitmap
                                    if (hwBitmap != null && hwBitmap.hasAlpha()) {
                                        val bitmap = hwBitmap.copy(
                                            android.graphics.Bitmap.Config.ARGB_8888,
                                            false
                                        )
                                        if (bitmap != null) {
                                            val pixels = IntArray(bitmap.width * bitmap.height)
                                            bitmap.getPixels(
                                                pixels,
                                                0,
                                                bitmap.width,
                                                0,
                                                0,
                                                bitmap.width,
                                                bitmap.height
                                            )
                                            val transparentCount =
                                                pixels.count { (it ushr 24) and 0xFF < 200 }
                                            if (transparentCount > pixels.size * 0.05) {
                                                viewModel.markTransparent(item.link)
                                            }
                                            bitmap.recycle()
                                        }
                                    }
                                },
                                onError = {
                                    viewModel.markError(item.link)
                                    android.util.Log.e(
                                        "VoidGrid",
                                        "Erro ao carregar imagem: ${item.encodedLink} | erro: ${it.result.throwable}"
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            if (pagingItems.loadState.append is LoadState.Loading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding())
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                placeholder = { Text("Pesquisar...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch(query)
                    focusManager.clearFocus()
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF191919),
                    unfocusedContainerColor = Color(0xFF191919),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(25.dp)
            )

            val isLoading = hasQuery && (
                    pagingItems.loadState.source.refresh is LoadState.Loading ||
                            pagingItems.loadState.append is LoadState.Loading
                    )
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500))
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(bottom = 5.dp),
                        color = Color.White.copy(alpha = 0.8f),
                        trackColor = Color.Transparent
                    )
                }
            }
        }

        selectedItem?.let { item ->
            ImageDetailDialog(
                item = item,
                allImages = pagingItems.itemSnapshotList.items,
                baseQuery = query,
                onDismiss = { selectedItem = null },
                imageLoader = imageLoader
            )
        }
    }
}