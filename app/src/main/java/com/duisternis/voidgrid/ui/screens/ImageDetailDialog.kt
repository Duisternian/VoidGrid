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
import com.duisternis.voidgrid.ui.components.ShimmerBox
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
    val suggestedItems = remember(currentItem) { allImages.filter { it.link != currentItem.link }.shuffled().take(20) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }

                        var mainLoaded by remember(currentItem.link) { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                            if (!mainLoaded) ShimmerBox(modifier = Modifier.fillMaxWidth().height(300.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(currentItem.link.replace(" ", "%20")).crossfade(400).build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                onSuccess = { mainLoaded = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        val scope = rememberCoroutineScope()
                        val context = LocalContext.current

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 25.dp).background(Color(0xFF1F1F1F), RoundedCornerShape(10.dp)).padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val actionItems = listOf(
                                Triple("Compartilhar", R.drawable.ios_share_24) { DownloadUtils.shareImage(context, currentItem.link) },
                                Triple("Baixar", R.drawable.download_24) { scope.launch { DownloadUtils.downloadImage(context, currentItem.link, imageLoader) } },
                                Triple("Favorite", R.drawable.ic_favorite_24) { /* Lógica */ },
                                Triple("Abrir", R.drawable.ic_open_in_browser_24) { /* Lógica */ }
                            )
                            actionItems.forEach { (label, iconRes, action) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { action() }.padding(horizontal = 4.dp)) {
                                    Icon(painter = painterResource(id = iconRes), contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
                items(items = suggestedItems, key = { "${it.link}_${it.source}" }) { similar ->
                    var simLoaded by remember(similar.link) { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { currentItem = similar }) {
                        if (!simLoaded) ShimmerBox(modifier = Modifier.fillMaxWidth().height(120.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(similar.link.replace(" ", "%20")).crossfade(200).build(),
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            onSuccess = { simLoaded = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}