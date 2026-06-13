package com.duisternis.voidgrid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.duisternis.voidgrid.ui.theme.ImageSearchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var currentQuery = ""
    private var currentVqd = ""
    private var isCurrentlyLoadingNextPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageSearchTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var searchResults by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(false) }

                    ImageSearchScreen(
                        images = searchResults,
                        isLoading = isLoading,
                        modifier = Modifier.padding(innerPadding),
                        onSearchTriggered = { query ->
                            currentQuery = query
                            lifecycleScope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) { isLoading = true }
                                try {
                                    val htmlResponse = RetrofitClient.googleSearchApi.getVqdToken(query = query)
                                    val vqdRegex = """vqd=["']?([0-9-]+)["']?""".toRegex()
                                    val vqdMatch = vqdRegex.find(htmlResponse)
                                    currentVqd = vqdMatch?.groups[1]?.value ?: ""

                                    if (currentVqd.isNotEmpty()) {
                                        val jsonResponse = RetrofitClient.googleSearchApi.getImagesJson(query = query, vqd = currentVqd, skip = 0)
                                        val items = parseDuckDuckGoJson(jsonResponse)
                                        withContext(Dispatchers.Main) { searchResults = items }
                                    }
                                } catch (e: Exception) {
                                    Log.e("API_ERROR", "Erro na busca: ${e.message}")
                                } finally {
                                    withContext(Dispatchers.Main) { isLoading = false }
                                }
                            }
                        },
                        onLoadNextPage = { currentListSize ->
                            // Limite total de 40 itens para manter a fluidez
                            if (currentVqd.isNotEmpty() && !isCurrentlyLoadingNextPage && currentListSize < 40) {
                                isCurrentlyLoadingNextPage = true
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val jsonResponse = RetrofitClient.googleSearchApi.getImagesJson(query = currentQuery, vqd = currentVqd, skip = currentListSize)
                                        val newItems = parseDuckDuckGoJson(jsonResponse)
                                        withContext(Dispatchers.Main) {
                                            val novosFiltrados = newItems.filter { novo -> !searchResults.any { it.link == novo.link } }
                                            searchResults = (searchResults + novosFiltrados).take(40) // Garante o limite de 40
                                        }
                                    } finally {
                                        isCurrentlyLoadingNextPage = false
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun parseDuckDuckGoJson(json: String): List<SearchItem> {
        val blockRegex = """"image"\s*:\s*"([^"]+)"[^}]+"source"\s*:\s*"([^"]+)"""".toRegex()
        val matches = blockRegex.findAll(json)
        val sitesPrioritarios = listOf("amazon", "reddit", "wikipedia", "pinterest")
        val cloudflareBlacklist = listOf("wallpapercrafter", "wallpapersden", "hdwallpapers", "wallpaperflare")

        return matches.map { match ->
            SearchItem(link = match.groups[1]?.value?.replace("\\/", "/") ?: "", source = match.groups[2]?.value?.lowercase() ?: "")
        }.filter { item ->
            item.link.startsWith("http") && !cloudflareBlacklist.any { domain -> item.link.contains(domain) }
        }.distinctBy { it.link }
            .sortedByDescending { item -> sitesPrioritarios.any { site -> item.source.contains(site) } }
            .toList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSearchScreen(images: List<SearchItem>, isLoading: Boolean, modifier: Modifier, onSearchTriggered: (String) -> Unit, onLoadNextPage: (Int) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val gridState = rememberLazyStaggeredGridState()

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            query = searchQuery, onQueryChange = { searchQuery = it }, onSearch = { onSearchTriggered(searchQuery) },
            active = false, onActiveChange = {}, placeholder = { Text("Pesquisar...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {}

        if (isLoading && images.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalStaggeredGrid(
                state = gridState,
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items = images, key = { _, item -> item.link }) { index, item ->
                    if (index >= images.size - 5 && images.size < 40) {
                        LaunchedEffect(images.size) { onLoadNextPage(images.size) }
                    }
                    key(item.link) {
                        ImageGridItem(item.link)
                    }
                }
            }
        }
    }
}

@Composable
fun ImageGridItem(imageUrl: String) {
    var isError by remember { mutableStateOf(false) }

    if (!isError) {
        Card(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop, // Crop mantém o bloco estável
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f), // Fixa a proporção para evitar o pulo
                onError = { isError = true }
            )
        }
    }
}