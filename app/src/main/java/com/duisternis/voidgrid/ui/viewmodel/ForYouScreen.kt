package com.duisternis.voidgrid.ui.viewmodel

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
import org.koin.androidx.compose.koinViewModel

// FIX: constante nomeada no lugar do magic number 0.75f
private const val DEFAULT_ASPECT_RATIO = 4f / 3f

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

    // FIX: key derivada de IDs estáveis para evitar duplo disparo quando
    // allPins e folders mudam juntos (ambos emitidos pelo mesmo ViewModel).
    val pinsKey = remember(allPins) { allPins.map { it.id } }
    val foldersKey = remember(folders) { folders.map { it.id } }

    LaunchedEffect(pinsKey, foldersKey) {
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

            // FIX: tratamento de erro no refresh — sem isso o usuário fica
            // preso em tela vazia sem saber o que aconteceu
            if (pagingItems.loadState.refresh is LoadState.Error) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Não foi possível carregar as sugestões",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { pagingItems.retry() }) {
                            Text("Tentar novamente", color = Color.White)
                        }
                    }
                }
            }

            // Imagens
            // FIX: key baseada em link+source para identificação estável,
            // evitando recomposições desnecessárias ao paginar
            items(
                count = pagingItems.itemCount,
                key = { index ->
                    val it = pagingItems.peek(index)
                    if (it != null) "${it.link}_${it.source}" else index
                }
            ) { index ->
                val item = pagingItems[index] ?: return@items

                // FIX: constante nomeada no lugar do magic number
                val aspectRatio = if (item.width > 0 && item.height > 0)
                    item.width.toFloat() / item.height.toFloat()
                else DEFAULT_ASPECT_RATIO

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onImageClick(item) }
                ) {
                    DominantColorBox(
                        // FIX: encodedThumbnail consistente com as outras telas
                        thumbnailUrl = item.encodedThumbnail,
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
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // FIX: tratamento de erro ao paginar — permite retry sem
            // precisar sair da tela ou rolar até o topo
            if (pagingItems.loadState.append is LoadState.Error) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(onClick = { pagingItems.retry() }) {
                            Text("Erro ao carregar mais — tentar novamente", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}