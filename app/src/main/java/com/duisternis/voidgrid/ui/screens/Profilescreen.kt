package com.duisternis.voidgrid.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.local.entity.PinEntity
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.ui.components.DominantColorBox
import com.duisternis.voidgrid.ui.viewmodel.FavoritesViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    imageLoader: ImageLoader,
    onImageClick: (SearchItem) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val folders by viewModel.folders.collectAsState()
    var selectedFolder by remember { mutableStateOf<FolderEntity?>(null) }

    if (selectedFolder != null) {
        FolderDetailScreen(
            folder = selectedFolder!!,
            imageLoader = imageLoader,
            onBack = { selectedFolder = null },
            onImageClick = onImageClick,
            viewModel = viewModel
        )
    } else {
        FolderListScreen(
            folders = folders,
            imageLoader = imageLoader,
            onFolderClick = { selectedFolder = it },
            onDeleteFolder = { viewModel.deleteFolder(it) },
            viewModel = viewModel
        )
    }
}

@Composable
private fun FolderListScreen(
    folders: List<FolderEntity>,
    imageLoader: ImageLoader,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    viewModel: FavoritesViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Text(
            text = "Pins",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        if (folders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Folder, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nenhum pin ainda", color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders, key = { it.id }) { folder ->
                    FolderCard(
                        folder = folder,
                        imageLoader = imageLoader,
                        onClick = { onFolderClick(folder) },
                        onDelete = { onDeleteFolder(folder) },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: FolderEntity,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    viewModel: FavoritesViewModel
) {
    val coverPin by viewModel.getCoverPin(folder.id).collectAsState(initial = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        if (coverPin != null) {
            DominantColorBox(
                thumbnailUrl = coverPin!!.thumbnail,
                imageLoader = imageLoader,
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverPin!!.link)
                        .crossfade(300)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Folder, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
            }
        }

        // Overlay com nome e botão de deletar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )
        Text(
            text = folder.name,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
        )
        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(Icons.Default.Delete, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1C1C1C),
            title = { Text("Deletar pasta?", color = Color.White) },
            text = { Text("Todos os pins em \"${folder.name}\" serão removidos.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Deletar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderDetailScreen(
    folder: FolderEntity,
    imageLoader: ImageLoader,
    onBack: () -> Unit,
    onImageClick: (SearchItem) -> Unit,
    viewModel: FavoritesViewModel
) {
    val pins by viewModel.getPinsAsSearchItems(folder.id).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        if (pins.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhuma imagem nesta pasta", color = Color.Gray)
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pins, key = { it.link }) { item ->
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
            }
        }
    }
}