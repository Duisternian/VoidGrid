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
import androidx.compose.ui.Alignment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
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
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.duisternis.voidgrid.ui.theme.ImageSearchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ImageSearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")

    // Esta é a nova "fonte da verdade" que substitui o uiState
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagingDataFlow = _query.flatMapLatest { query ->
        Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20
            ),
            pagingSourceFactory = { SearchPagingSource(query, RetrofitClient.googleSearchApi) }
        ).flow
    }.cachedIn(viewModelScope)

    // O método de busca agora apenas atualiza a query
    fun search(newQuery: String) {
        _query.value = newQuery
    }
}



class MainActivity : ComponentActivity() {
    private val viewModel: ImageSearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ImageSearchTheme {
                val imageLoader = remember { createCustomImageLoader(this) }
                CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                    // Coleta o fluxo de PagingData do ViewModel
                    val pagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()

                    ImageSearchScreen(
                        pagingItems = pagingItems,
                        onSearch = { viewModel.search(it) },
                        imageLoader = imageLoader
                    )
                }
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
                val loader = createCustomImageLoader(this@MainActivity)
                val result = (loader.execute(ImageRequest.Builder(this@MainActivity).data(imageUrl).build()) as SuccessResult).drawable
                val bitmap = (result as BitmapDrawable).bitmap
                val filename = "IMG_${System.currentTimeMillis()}.jpg"
                val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename); put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    })?.let { contentResolver.openOutputStream(it) }
                } else FileOutputStream(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename))
                fos?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Salvo!", Toast.LENGTH_SHORT).show() }
            } catch (_: Exception) { withContext(Dispatchers.Main) { Toast.makeText(this@MainActivity, "Erro", Toast.LENGTH_SHORT).show() } }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageSearchScreen(
    pagingItems: androidx.paging.compose.LazyPagingItems<SearchItem>,
    onSearch: (String) -> Unit,
    imageLoader: ImageLoader
) {
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<SearchItem?>(null) }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            contentPadding = PaddingValues(top = 115.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalItemSpacing = 8.dp,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(count = pagingItems.itemCount) { index ->
                val item = pagingItems[index]
                if (item != null) {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { focusManager.clearFocus(); selectedItem = item }
                            .animateItem(
                                placementSpec = tween(durationMillis = 700) // 1000ms = 1 segundo de deslize
                            )
                    ) {

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(item.link).crossfade(0).build(),
                            imageLoader = imageLoader,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight().background(Color.Black)
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. O Spacer cuida exclusivamente da área da barra de status (cor preta)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()

            )

            // 2. A SearchBar com o padding de 6.dp para não ficar colada na câmera
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp)
                    .padding(top = 10.dp), // Separar os paddings evita erros de sintaxe
                placeholder = { Text("Pesquisar...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query); focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF191919),
                    unfocusedContainerColor = Color(0xFF191919),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(25.dp)
            )

            // Agora o loadState é calculado dentro do escopo do Composable
            val loadState = pagingItems.loadState
            val isLoading = loadState.source.refresh is androidx.paging.LoadState.Loading ||
                    loadState.append is androidx.paging.LoadState.Loading

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(top = 8.dp),
                        color = Color.White.copy(alpha = 100f),
                        trackColor = Color.Transparent
                    )
                }
            }
            }
}
    selectedItem?.let { item ->
        ImageDetailDialog(
            item = item,
            allImages = pagingItems.itemSnapshotList.items.filterNotNull(),
            onDismiss = { selectedItem = null },
            activity = LocalContext.current as MainActivity,
            imageLoader = imageLoader
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageDetailDialog(
    item: SearchItem,
    allImages: List<SearchItem>,
    onDismiss: () -> Unit,
    activity: MainActivity,
    imageLoader: ImageLoader
) {
    var currentItem by remember(item) { mutableStateOf(item) }

    val suggestedItems = remember(currentItem) {
        val sitesDesejados = listOf("pinterest.com", "reddit.com", "imdb.com", "wall.alphacoders.com")
        val filtered = allImages.filter { img -> sitesDesejados.any { site -> img.source.contains(site) } && img.link != currentItem.link }
        val result = filtered.ifEmpty { allImages.filter { it.link != currentItem.link } }
        result.shuffled().take(20)
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
                        AsyncImage(model = currentItem.link, imageLoader = imageLoader, contentDescription = null, modifier = Modifier.fillMaxWidth().wrapContentHeight(), contentScale = ContentScale.FillWidth)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { activity.shareImage(currentItem.link) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF191919), contentColor = Color.White), shape = RoundedCornerShape(12.dp)) { Text("Compartilhar") }
                            Button(onClick = { activity.downloadImage(currentItem.link) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF191919), contentColor = Color.White), shape = RoundedCornerShape(12.dp)) { Text("Baixar") }
                        }
                    }
                }
                items(suggestedItems) { similar ->
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(similar.link).build(),
                        imageLoader = imageLoader,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { currentItem = similar }.background(Color.DarkGray),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}