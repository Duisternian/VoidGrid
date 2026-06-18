package com.duisternis.voidgrid.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageDetailDialog(
    item: SearchItem,
    allImages: List<SearchItem>,
    onDismiss: () -> Unit,
    imageLoader: ImageLoader
) {
    var currentItem by remember(item) { mutableStateOf(item) }

    val suggestedItems = remember(allImages) {
        allImages.shuffled()
    }.filter { it.link != currentItem.link }.take(20)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalStaggeredGrid(
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

                            // Imagem principal com DominantColorBox
                            DominantColorBox(
                                thumbnailUrl = currentItem.thumbnail,
                                imageLoader = imageLoader,
                                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(currentItem.link.replace(" ", "%20"))
                                        .crossfade(400)
                                        .build(),
                                    imageLoader = imageLoader,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 25.dp)
                                    .background(Color(0xFF1F1F1F), RoundedCornerShape(10.dp))
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val actionItems = listOf(
                                    Triple("Compartilhar", R.drawable.ios_share_24) {
                                        DownloadUtils.shareImage(context, currentItem.link)
                                    },
                                    Triple("Baixar", R.drawable.download_24) {
                                        scope.launch {
                                            val result = DownloadUtils.downloadImage(
                                                context, currentItem.link, imageLoader
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
                                    Triple("Abrir", R.drawable.ic_open_in_browser_24) { /* TODO */ }
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
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Imagens sugeridas com DominantColorBox
                    items(items = suggestedItems, key = { "${it.link}_${it.source}" }) { similar ->
                        DominantColorBox(
                            thumbnailUrl = similar.thumbnail,
                            imageLoader = imageLoader,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { currentItem = similar }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(similar.link.replace(" ", "%20"))
                                    .crossfade(200)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxSize()
                            )
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