package com.duisternis.voidgrid

import androidx.compose.ui.res.painterResource
import android.content.ContentValues
import android.content.Context
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
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.duisternis.voidgrid.ui.theme.ImageSearchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun Context.getCustomImageLoader(): ImageLoader = ImageLoader.Builder(this)
    .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
    .diskCachePolicy(CachePolicy.ENABLED)
    .memoryCachePolicy(CachePolicy.ENABLED)
    .build()

class ImageSearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState
    private var currentQuery: String = ""
    private var currentVqd: String = ""

    val prioritySites = listOf("pinterest.com", "reddit.com", "imdb.com", "wall.alphacoders.com", "wallpapercave.com", "whatsafterthemovie.com", "manganime.fans", "safebooru.donmai.us")

    data class UiState(val images: List<SearchItem> = emptyList(), val isLoading: Boolean = false)

    fun sortImages(images: List<SearchItem>): List<SearchItem> {
        return images.sortedBy { item ->
            val index = prioritySites.indexOfFirst { priority -> item.source.contains(priority) }
            if (index == -1) Int.MAX_VALUE else index
        }
    }

    fun search(query: String) {
        if (query.isBlank() || _uiState.value.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.emit(UiState(isLoading = true))
            try {
                currentQuery = query
                val html = RetrofitClient.googleSearchApi.getVqdToken(query = query)
                currentVqd = """vqd=["']?([0-9-]+)["']?""".toRegex().find(html)?.groupValues?.getOrNull(1) ?: ""
                val json = RetrofitClient.googleSearchApi.getImagesJson(query, currentVqd, 0)
                val parsed = parseDuckDuckGoJson(json)
                _uiState.emit(UiState(images = sortImages(parsed), isLoading = false))
            } catch (e: Exception) { _uiState.emit(UiState(isLoading = false)) }
        }
    }

    fun loadNextPage() {
        if (currentVqd.isEmpty() || _uiState.value.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.emit(_uiState.value.copy(isLoading = true))
            try {
                val json = RetrofitClient.googleSearchApi.getImagesJson(currentQuery, currentVqd, _uiState.value.images.size)
                val next = sortImages(parseDuckDuckGoJson(json))
                val combinedList = (_uiState.value.images + next).distinctBy { it.link }
                _uiState.emit(_uiState.value.copy(images = combinedList, isLoading = false))
            } catch (e: Exception) { _uiState.emit(_uiState.value.copy(isLoading = false)) }
        }
    }

    private fun parseDuckDuckGoJson(json: String): List<SearchItem> {
        val regex = """"image"\s*:\s*"([^"]+)"[^}]+"source"\s*:\s*"([^"]+)"""".toRegex()
        return regex.findAll(json).map { SearchItem(it.groups[1]?.value?.replace("\\/", "/") ?: "", it.groups[2]?.value?.lowercase() ?: "") }
            .filter { it.link.startsWith("http") }.toList()
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel = ImageSearchViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageSearchTheme {
                val uiState by viewModel.uiState.collectAsState()
                ImageSearchScreen(uiState, { viewModel.search(it) }, { viewModel.loadNextPage() }, viewModel)
            }
        }
    }

    fun shareImage(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url) }
        startActivity(Intent.createChooser(intent, "Compartilhar"))
    }

    fun downloadImage(imageUrl: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val result = (getCustomImageLoader().execute(ImageRequest.Builder(this@MainActivity).data(imageUrl).build()) as SuccessResult).drawable
                val bitmap = (result as BitmapDrawable).bitmap
                val filename = "IMG_${System.currentTimeMillis()}.jpg"
                val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename); put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    })?.let { contentResolver.openOutputStream(it) }
                } else java.io.FileOutputStream(java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename))
                fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Salvo!", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Erro", Toast.LENGTH_SHORT).show() } }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ImageSearchScreen(uiState: ImageSearchViewModel.UiState, onSearch: (String) -> Unit, onLoadNextPage: () -> Unit, viewModel: ImageSearchViewModel) {
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<SearchItem?>(null) }
    val context = LocalContext.current
    val imageLoader = remember { context.getCustomImageLoader() }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. GRID NO FUNDO (Rola por trás de tudo)
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            // Padding top ajustado para começar logo abaixo da área da SearchBar
            contentPadding = PaddingValues(top = 115.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.images, key = { it.link }) { item ->
                LaunchedEffect(Unit) { if (uiState.images.indexOf(item) >= uiState.images.size - 5) onLoadNextPage() }
                Card(modifier = Modifier.fillMaxWidth().clickable { focusManager.clearFocus(); selectedItem = item }.animateItem()) {
                    AsyncImage(
                        model = remember(item.link) { ImageRequest.Builder(context).data(item.link).crossfade(800).build() },
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().wrapContentHeight().background(Color.Black)
                    )
                }
            }
        }

        // 2. CAMADA DE PROTEÇÃO PARA A BARRA DE STATUS (Fica no topo absoluto)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp) // Altura que cobre a status bar
                .background(Color.Black)
                .align(Alignment.TopCenter)
        )

        // 3. SearchBar Flutuante
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 6.dp)
        ) {
            SearchBar(
                query = query,
                onQueryChange = { query = it },
                onSearch = { onSearch(query); focusManager.clearFocus() },
                active = false,
                onActiveChange = {},
                placeholder = { Text("Pesquisar...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Lupa") },
                colors = SearchBarDefaults.colors(containerColor = Color(0xFF191919)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp)
            ) {}

            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 35.dp),
                    color = Color.White.copy(alpha = 0.6f),
                    trackColor = Color.Transparent
                )
            }
        }
    }
    selectedItem?.let { ImageDetailDialog(it, uiState.images, { selectedItem = null }, context as MainActivity, viewModel) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageDetailDialog(
    item: SearchItem,
    allImages: List<SearchItem>,
    onDismiss: () -> Unit,
    activity: MainActivity,
    viewModel: ImageSearchViewModel
) {
    var currentItem by remember(item) { mutableStateOf(item) }

    val suggestedItems = remember(currentItem) {
        val sitesDesejados = listOf("pinterest.com", "reddit.com", "imdb.com", "wall.alphacoders.com")
        val filtered = allImages.filter { item ->
            val ehSiteDesejado = sitesDesejados.any { site -> item.source.contains(site, ignoreCase = true) }
            ehSiteDesejado && item.link != currentItem.link
        }
        val finalSelection = if (filtered.isNotEmpty()) filtered else allImages.filter { it.link != currentItem.link }
        mutableStateListOf<SearchItem>().apply { addAll(finalSelection.shuffled().take(20)) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Column {
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
                        AsyncImage(
                            model = currentItem.link,
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            contentScale = ContentScale.FillWidth
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { activity.shareImage(currentItem.link) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF191919),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Compartilhar")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        painter = painterResource(id = R.drawable.ios_share_24),
                                        contentDescription = "Compartilhar"
                                    )
                                }
                            }

                            Button(
                                onClick = { activity.downloadImage(currentItem.link) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF191919),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Baixar")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        painter = painterResource(id = R.drawable.download_24),
                                        contentDescription = "Baixar"
                                    )
                                }
                            }
                        }
                        }
                }
                items(suggestedItems) { similar ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(similar.link)
                            .listener(onError = { _, _ -> suggestedItems.remove(similar) })
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { currentItem = similar }
                            .background(Color.DarkGray),
                        contentScale = ContentScale.FillWidth
                    )
                }
                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}