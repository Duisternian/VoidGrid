package com.duisternis.voidgrid

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import com.duisternis.voidgrid.ui.theme.ImageSearchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// ─── ViewModel ───────────────────────────────────────────────────────────────

class ImageSearchViewModel : ViewModel() {
    private val _query = MutableStateFlow<String?>(null)
    val currentQuery: StateFlow<String?> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = _query
        .filterNotNull()
        .filter { it.isNotBlank() }
        .flatMapLatest { query ->
            Pager(
                config = PagingConfig(pageSize = 20, initialLoadSize = 20, prefetchDistance = 5, enablePlaceholders = false),
                pagingSourceFactory = { SearchPagingSource(query, RetrofitClient.duckDuckGoApi) }
            ).flow
        }
        .cachedIn(viewModelScope)

    fun search(newQuery: String) { _query.value = newQuery }
}

// ─── Activity ─────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    private val viewModel: ImageSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageSearchTheme {
                val imageLoader = remember { createCustomImageLoader(this) }
                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    val pagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()
                    val currentQuery by viewModel.currentQuery.collectAsState()
                    ImageSearchScreen(
                        pagingItems = pagingItems,
                        onSearch = { viewModel.search(it) },
                        imageLoader = imageLoader,
                        hasQuery = !currentQuery.isNullOrBlank()
                    )
                }
            }
        }
    }

    fun shareImage(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar"))
    }

    fun downloadImage(imageUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val loader = createCustomImageLoader(this@MainActivity)
                val result = (loader.execute(ImageRequest.Builder(this@MainActivity).data(imageUrl).build()) as SuccessResult).drawable
                val bitmap = (result as BitmapDrawable).bitmap
                val filename = "IMG_${System.currentTimeMillis()}.jpg"
                val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    })?.let { contentResolver.openOutputStream(it) }
                } else {
                    FileOutputStream(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename))
                }
                fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Salvo!", Toast.LENGTH_SHORT).show() }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Erro ao salvar", Toast.LENGTH_SHORT).show() }
            }
        }
    }
}

// ─── Shimmer ──────────────────────────────────────────────────────────────────

@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(0f, 1000f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart))
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF191919), Color(0xFF2C2C2C), Color(0xFF191919)),
        start = androidx.compose.ui.geometry.Offset(translateAnim - 500f, 0f),
        end = androidx.compose.ui.geometry.Offset(translateAnim, 0f)
    )
    Box(modifier = modifier.background(brush))
}

// ─── Tela Principal ───────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSearchScreen(
    pagingItems: androidx.paging.compose.LazyPagingItems<SearchItem>,
    onSearch: (String) -> Unit,
    imageLoader: ImageLoader,
    hasQuery: Boolean
) {
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<SearchItem?>(null) }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(top = 135.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(count = pagingItems.itemCount, key = { index -> "${pagingItems.peek(index)?.link}_$index" }) { index ->
                val item = pagingItems[index] ?: return@items
                var isError by remember(item.link) { mutableStateOf(false) }
                var isLoaded by remember(item.link) { mutableStateOf(false) }

                if (!isError) {
                    val aspectRatio = if (item.width > 0 && item.height > 0) item.width.toFloat() / item.height.toFloat() else 0.75f
                    Card(modifier = Modifier.fillMaxWidth().clickable { focusManager.clearFocus(); selectedItem = item }) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)) {
                            if (!isLoaded) ShimmerBox(modifier = Modifier.fillMaxSize())
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.link.replace(" ", "%20"))
                                    .crossfade(100)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .precision(Precision.INEXACT)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                onSuccess = { isLoaded = true },
                                onError = { isError = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding())
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 10.dp),
                placeholder = { Text("Pesquisar...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query); focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF191919), unfocusedContainerColor = Color(0xFF191919),
                    focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(25.dp)
            )

            val isLoading = hasQuery && (pagingItems.loadState.source.refresh is LoadState.Loading || pagingItems.loadState.append is LoadState.Loading)
            AnimatedVisibility(visible = isLoading, enter = fadeIn(tween(500)), exit = fadeOut(tween(500))) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.9f).padding(top = 8.dp), color = Color.White.copy(alpha = 0.8f), trackColor = Color.Transparent)
                }
            }
        }

        selectedItem?.let { item ->
            ImageDetailDialog(item, pagingItems.itemSnapshotList.items, { selectedItem = null }, LocalContext.current as MainActivity, imageLoader)
        }
    }
}

// ─── Dialog de detalhe ────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageDetailDialog(item: SearchItem, allImages: List<SearchItem>, onDismiss: () -> Unit, activity: MainActivity, imageLoader: ImageLoader) {
    var currentItem by remember(item) { mutableStateOf(item) }
    val suggestedItems = remember(currentItem) {
        allImages.filter { it.link != currentItem.link }.shuffled().take(20)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                // AQUI: Reduzimos o top para o padrão, o espaçamento será feito dentro do item
                contentPadding = PaddingValues(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    // AQUI: Adicionamos um padding no bottom deste container para afastar o grid
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }

                        var mainLoaded by remember(currentItem.link) { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                            if (!mainLoaded) ShimmerBox(modifier = Modifier.fillMaxWidth().height(300.dp))
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(currentItem.link.replace(" ", "%20")).crossfade(400)
                                    .build(),
                                imageLoader = imageLoader,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                onSuccess = { mainLoaded = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Barra de Ações
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 25.dp)
                                .background(Color(0xFF1F1F1F), RoundedCornerShape(10.dp))
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val actionItems = listOf(
                                Triple("Compartilhar", R.drawable.ios_share_24) { activity.shareImage(currentItem.link) },
                                Triple("Baixar", R.drawable.download_24) { activity.downloadImage(currentItem.link) },
                                Triple("Favoritar", R.drawable.ic_favorite_24) { /* Lógica */ },
                                Triple("Abrir", R.drawable.ic_open_in_browser_24) { /* Lógica */ }
                            )

                            actionItems.forEach { (label, iconRes, action) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { action() }.padding(horizontal = 4.dp)
                                ) {
                                    Icon(painter = painterResource(id = iconRes), contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Text(text = label, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }

                // Grid de sugestões
                items(items = suggestedItems, key = { "${it.link}_${it.source}" }) { similar ->
                    // ... (seu código permanece igual)
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