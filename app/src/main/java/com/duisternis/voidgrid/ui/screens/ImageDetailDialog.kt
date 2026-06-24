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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.duisternis.voidgrid.R
import com.duisternis.voidgrid.data.local.entity.FolderEntity
import com.duisternis.voidgrid.data.model.SearchItem
import com.duisternis.voidgrid.data.util.DownloadUtils
import com.duisternis.voidgrid.ui.components.DominantColorBox
import com.duisternis.voidgrid.ui.viewmodel.FavoritesViewModel
import com.duisternis.voidgrid.ui.viewmodel.ImageSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

// FIX: constante nomeada no lugar do magic number 0.75f
private const val DEFAULT_ASPECT_RATIO = 4f / 3f

// FIX: lógica de deteção de transparência extraída para evitar duplicação
private suspend fun checkTransparency(
    drawable: android.graphics.drawable.Drawable,
    onTransparent: () -> Unit
) = withContext(Dispatchers.Default) {
    val hwBitmap = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return@withContext
    if (!hwBitmap.hasAlpha()) return@withContext
    val bitmap = hwBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: return@withContext
    try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val transparentCount = pixels.count { (it ushr 24) and 0xFF < 200 }
        if (transparentCount > pixels.size * 0.05) {
            withContext(Dispatchers.Main) { onTransparent() }
        }
    } finally {
        bitmap.recycle()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageDetailDialog(
    item: SearchItem,
    allImages: List<SearchItem>,
    baseQuery: String,
    onDismiss: () -> Unit,
    imageLoader: ImageLoader,
    viewModel: ImageSearchViewModel = koinViewModel(),
    favoritesViewModel: FavoritesViewModel = koinViewModel()
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

                    val isPinned by favoritesViewModel.isPinned(activeItem.link).collectAsState(initial = false)
                    val folders by favoritesViewModel.folders.collectAsState()

                    var suggestedItems by remember(activeItem.link) {
                        mutableStateOf<List<SearchItem>>(emptyList())
                    }
                    val isLoadingSuggestions = viewModel.isSuggestionsLoading(activeItem.link)

                    // FIX: showFolderPicker movido para dentro do AnimatedContent,
                    // evitando que o estado vaze entre transições de item
                    var showFolderPicker by remember { mutableStateOf(false) }

                    // FIX: estado de erro da imagem principal persistido via ViewModel,
                    // para não ser perdido ao navegar entre itens e voltar
                    val mainImageError = viewModel.isError(activeItem.link)

                    LaunchedEffect(activeItem.link) {
                        viewModel.loadRefinedSuggestions(
                            link = activeItem.link,
                            baseQuery = baseQuery,
                            allImages = allImages
                        ) { result ->
                            suggestedItems = result
                        }
                    }

                    // FIX: estado do scroll vinculado ao item ativo para resetar ao trocar imagem
                    val gridState = rememberLazyStaggeredGridState()

                    LazyVerticalStaggeredGrid(
                        state = gridState,
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
                                            contentDescription = activeItem.title,
                                            contentScale = ContentScale.FillWidth,
                                            // FIX: erro persistido via ViewModel
                                            onError = { viewModel.markError(activeItem.link) },
                                            onSuccess = { result ->
                                                // FIX: processamento de pixels movido para Dispatchers.Default
                                                scope.launch {
                                                    checkTransparency(result.result.drawable) {
                                                        viewModel.markTransparent(activeItem.link)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                // ── Título da imagem ──────────────────────────
                                activeItem.title?.let { title ->
                                    Text(
                                        text = title,
                                        color = Color.White.copy(alpha = 0.85f),
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 10.dp)
                                    )
                                }

                                // ── Barra de ações ────────────────────────────
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp)
                                            .background(actionBarBg, RoundedCornerShape(12.dp))
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Compartilhar — FIX: snackbar de feedback adicionado
                                        ActionButton(
                                            label = "Compartilhar",
                                            iconRes = R.drawable.ios_share_24,
                                            onClick = {
                                                DownloadUtils.shareImage(context, activeItem.link)
                                            }
                                        )

                                        // Baixar
                                        ActionButton(
                                            label = "Baixar",
                                            iconRes = R.drawable.download_24,
                                            onClick = {
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
                                            }
                                        )

                                        // Favorite — abre picker ou desativa pin
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clickable {
                                                    if (isPinned) {
                                                        favoritesViewModel.unpinItem(activeItem.link)
                                                    } else {
                                                        showFolderPicker = true
                                                    }
                                                }
                                                .padding(horizontal = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPinned) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Favorite",
                                                tint = if (isPinned) Color(0xFFE91E63) else Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = if (isPinned) "Salvo" else "Favorite",
                                                color = if (isPinned) Color(0xFFE91E63) else Color.White.copy(alpha = 0.85f),
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }

                                        // Abrir
                                        ActionButton(
                                            label = "Abrir",
                                            iconRes = R.drawable.ic_open_in_browser_24,
                                            onClick = { DownloadUtils.openInBrowser(context, activeItem.link) }
                                        )
                                    }

                                    // Dropdown de pastas
                                    DropdownMenu(
                                        expanded = showFolderPicker,
                                        onDismissRequest = { showFolderPicker = false },
                                        modifier = Modifier.background(Color(0xFF2A2A2A))
                                    ) {
                                        FolderPickerContent(
                                            folders = folders,
                                            onSelectFolder = { folder ->
                                                favoritesViewModel.pinItem(
                                                    item = activeItem,
                                                    folderId = folder.id,
                                                    sourceQuery = baseQuery
                                                )
                                                showFolderPicker = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Salvo em \"${folder.name}\"",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            },
                                            onCreateFolder = { name ->
                                                favoritesViewModel.createFolder(name) { folderId ->
                                                    favoritesViewModel.pinItem(
                                                        item = activeItem,
                                                        folderId = folderId,
                                                        sourceQuery = baseQuery
                                                    )
                                                }
                                                showFolderPicker = false
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        message = "Pasta criada e imagem salva!",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 4.dp, bottom = 8.dp),
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

                            // FIX: constante nomeada no lugar do magic number
                            val aspectRatio = if (similar.width > 0 && similar.height > 0)
                                similar.width.toFloat() / similar.height.toFloat()
                            else DEFAULT_ASPECT_RATIO

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
                                        contentDescription = similar.title,
                                        contentScale = ContentScale.FillWidth,
                                        onError = { viewModel.markError(similar.link) },
                                        onSuccess = { result ->
                                            // FIX: reutiliza função extraída, roda em background
                                            scope.launch {
                                                checkTransparency(result.result.drawable) {
                                                    viewModel.markTransparent(similar.link)
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

// ─── Botão de ação genérico ───────────────────────────────────────────────────

@Composable
private fun ActionButton(label: String, iconRes: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
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

// ─── Conteúdo do picker de pastas ────────────────────────────────────────────

@Composable
private fun FolderPickerContent(
    folders: List<FolderEntity>,
    onSelectFolder: (FolderEntity) -> Unit,
    onCreateFolder: (String) -> Unit
) {
    // FIX: inicialização direta baseada em folders, sem LaunchedEffect para evitar flash de frame
    var showNewFolderField by remember(folders) { mutableStateOf(folders.isEmpty()) }
    var newFolderName by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        folders.forEach { folder ->
            DropdownMenuItem(
                text = { Text(folder.name, color = Color.White) },
                onClick = { onSelectFolder(folder) }
            )
        }

        if (folders.isNotEmpty()) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        }

        if (showNewFolderField) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .width(220.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = {
                        Text(
                            "Nome da pasta",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF3A3A3A),
                        unfocusedContainerColor = Color(0xFF3A3A3A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateFolder(newFolderName.trim())
                            newFolderName = ""
                            showNewFolderField = false
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Criar", tint = Color.White)
                }
            }
        } else {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nova pasta", color = Color.White)
                    }
                },
                onClick = { showNewFolderField = true }
            )
        }
    }
}