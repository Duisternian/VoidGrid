package com.duisternis.voidgrid.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.ui.components.DominantColorBox
import com.duisternis.voidgrid.ui.viewmodel.FavoritesViewModel
import com.duisternis.voidgrid.ui.viewmodel.ForYouViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ForYouScreen(
    imageLoader: ImageLoader,
    onImageClick: (SearchItem) -> Unit,
    favoritesViewModel: FavoritesViewModel = koinViewModel(),
    forYouViewModel: ForYouViewModel = koinViewModel()
) {
    val allPins by favoritesViewModel.allPins.collectAsState()
    val folders by favoritesViewModel.folders.collectAsState()

    // Atualiza a query sempre que pins ou folders mudarem
    LaunchedEffect(allPins, folders) {
        forYouViewModel.updateQueriesFromPins(allPins, folders)
    }

    val pagingItems = forYouViewModel.pagingDataFlow.collectAsLazyPagingItems()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Estado vazio ──────────────────────────────────────────────────────
        if (allPins.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "For You",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Salve imagens para ver sugestões personalizadas",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            return@Box
        }

        // ── Feed ─────────────────────────────────────────────────────────────
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(top = 56.dp, start = 8.dp, end = 8.dp, bottom = 80.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header com botão de refresh
            item(span = StaggeredGridItemSpan.FullLine) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "For You",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { forYouViewModel.refresh(allPins, folders) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Atualizar sugestões",
                            tint = Color.White
                        )
                    }
                }
            }

            // Loading inicial
            if (pagingItems.loadState.refresh is LoadState.Loading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            // Imagens
            items(count = pagingItems.itemCount, key = { index -> index }) { index ->
                val item = pagingItems[index] ?: return@items
                val aspectRatio = if (item.width > 0 && item.height > 0)
                    item.width.toFloat() / item.height.toFloat()
                else 0.75f

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onImageClick(item) }
                ) {
                    DominantColorBox(
                        thumbnailUrl = item.thumbnail,
                        imageLoader = imageLoader,
                        modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(item.encodedLink)
                                .crossfade(300)
                                .build(),
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Loading ao paginar
            if (pagingItems.loadState.append is LoadState.Loading) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}